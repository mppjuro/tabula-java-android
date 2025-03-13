package technology.tabula.detectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;

import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class NurminenDetectionAlgorithm implements DetectionAlgorithm {

    private static final float POINT_SNAP_DISTANCE_THRESHOLD = 8f;
    private static final float TABLE_PADDING_AMOUNT = 1.0f;
    private static final int REQUIRED_CELLS_FOR_TABLE = 4;
    private static final float IDENTICAL_TABLE_OVERLAP_RATIO = 0.9f;
    private static final int HORIZONTAL_EDGE_WIDTH_MINIMUM = 50;
    private static final int VERTICAL_EDGE_HEIGHT_MINIMUM = 10;

    @Override
    public List<Rectangle> detect(Page page) {
        List<Ruling> horizontalRulings = page.getHorizontalRulings();
        List<Ruling> verticalRulings = page.getVerticalRulings();

        List<Ruling> allEdges = new ArrayList<>(horizontalRulings);
        allEdges.addAll(verticalRulings);

        List<Rectangle> tableAreas = new ArrayList<>();

        if (!allEdges.isEmpty()) {
            List<Point2D> rulingPoints = new ArrayList<>();
            for (Ruling r : allEdges) {
                rulingPoints.add(r.getStartPoint());
                rulingPoints.add(r.getEndPoint());
            }
            Utils.snapPoints(rulingPoints, POINT_SNAP_DISTANCE_THRESHOLD, POINT_SNAP_DISTANCE_THRESHOLD);

            horizontalRulings = Ruling.collapseOrientedRulings(horizontalRulings);
            verticalRulings = Ruling.collapseOrientedRulings(verticalRulings);

            List<? extends Rectangle> cells = SpreadsheetExtractionAlgorithm.findCells(horizontalRulings, verticalRulings);
            tableAreas = this.getTableAreasFromCells(cells);
        }

        for (Ruling verticalRuling : verticalRulings) {
            for (Rectangle tableArea : tableAreas) {
                if (tableArea.intersects(verticalRuling) &&
                        !(tableArea.contains(verticalRuling.getStartPoint()) && tableArea.contains(verticalRuling.getEndPoint()))) {

                    tableArea.setTop(Math.min(tableArea.getTop(), verticalRuling.getTop()));
                    tableArea.setBottom(Math.max(tableArea.getBottom(), verticalRuling.getBottom()));
                    break;
                }
            }
        }

        for (Rectangle area : tableAreas) {
            area.setLeft(area.getLeft() - TABLE_PADDING_AMOUNT);
            area.setTop(area.getTop() - TABLE_PADDING_AMOUNT);
            area.setWidth(area.getWidth() + 2 * TABLE_PADDING_AMOUNT);
            area.setHeight(area.getHeight() + 2 * TABLE_PADDING_AMOUNT);
        }

        Set<Rectangle> tableSet = new TreeSet<>(new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle o1, Rectangle o2) {
                if (o1.equals(o2)) {
                    return 0;
                }

                if (o2.contains(o1) || o1.contains(o2)) {
                    return 0;
                }

                if (o1.overlapRatio(o2) >= IDENTICAL_TABLE_OVERLAP_RATIO) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        tableSet.addAll(tableAreas);
        return new ArrayList<>(tableSet);
    }

    private List<Rectangle> getTableAreasFromCells(List<? extends Rectangle> cells) {
        List<List<Rectangle>> cellGroups = new ArrayList<>();
        for (Rectangle cell : cells) {
            boolean addedToGroup = false;

            cellCheck:
            for (List<Rectangle> cellGroup : cellGroups) {
                for (Rectangle groupCell : cellGroup) {
                    if (groupCell.distance(cell) < 10) {
                        cellGroup.add(cell);
                        addedToGroup = true;
                        break cellCheck;
                    }
                }
            }

            if (!addedToGroup) {
                List<Rectangle> cellGroup = new ArrayList<>();
                cellGroup.add(cell);
                cellGroups.add(cellGroup);
            }
        }

        List<Rectangle> tableAreas = new ArrayList<>();
        for (List<Rectangle> cellGroup : cellGroups) {
            if (cellGroup.size() < REQUIRED_CELLS_FOR_TABLE) {
                continue;
            }

            float top = Float.MAX_VALUE;
            float left = Float.MAX_VALUE;
            float bottom = Float.MIN_VALUE;
            float right = Float.MIN_VALUE;

            for (Rectangle cell : cellGroup) {
                top = Math.min(top, cell.getTop());
                left = Math.min(left, cell.getLeft());
                bottom = Math.max(bottom, cell.getBottom());
                right = Math.max(right, cell.getRight());
            }

            tableAreas.add(new Rectangle(top, left, right - left, bottom - top));
        }

        return tableAreas;
    }

    private PDDocument removeText(PDPage page) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(page);
        parser.parse();

        PDDocument document = new PDDocument();
        PDPage newPage = document.importPage(page);
        newPage.setResources(page.getResources());

        PDStream newContents = new PDStream(document);
        OutputStream out = newContents.createOutputStream(COSName.FLATE_DECODE);
        ContentStreamWriter writer = new ContentStreamWriter(out);
        List<Object> tokensWithoutText = createTokensWithoutText(page);
        writer.writeTokens(tokensWithoutText);
        out.close();
        newPage.setContents(newContents);
        return document;
    }

    private static List<Object> createTokensWithoutText(PDContentStream contentStream) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(contentStream);
        Object token = parser.parseNextToken();
        List<Object> newTokens = new ArrayList<>();
        while (token != null) {
            if (token instanceof Operator) {
                Operator op = (Operator) token;
                String opName = op.getName();
                if (OperatorName.SHOW_TEXT_ADJUSTED.equals(opName)
                        || OperatorName.SHOW_TEXT.equals(opName)
                        || OperatorName.SHOW_TEXT_LINE.equals(opName)) {
                    newTokens.remove(newTokens.size() - 1);
                    token = parser.parseNextToken();
                    continue;
                } else if (OperatorName.SHOW_TEXT_LINE_AND_SPACE.equals(opName)) {
                    newTokens.remove(newTokens.size() - 1);
                    newTokens.remove(newTokens.size() - 1);
                    newTokens.remove(newTokens.size() - 1);
                    token = parser.parseNextToken();
                    continue;
                }
            }
            newTokens.add(token);
            token = parser.parseNextToken();
        }
        return newTokens;
    }

    private static final class TextEdge extends Line {
        public static final int LEFT = 0;
        public static final int MID = 1;
        public static final int RIGHT = 2;
        public static final int NUM_TYPES = 3;

        public int intersectingTextRowCount;

        public TextEdge(float x1, float y1, float x2, float y2) {
            super();
            this.setBounds(new Rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1)));
            this.intersectingTextRowCount = 0;
        }
    }

    private TextEdges getTextEdges(List<Line> lines) {
        List<TextEdge> leftTextEdges = new ArrayList<>();
        List<TextEdge> midTextEdges = new ArrayList<>();
        List<TextEdge> rightTextEdges = new ArrayList<>();

        Map<Integer, List<TextChunk>> currLeftEdges = new HashMap<>();
        Map<Integer, List<TextChunk>> currMidEdges = new HashMap<>();
        Map<Integer, List<TextChunk>> currRightEdges = new HashMap<>();

        int numOfLines = lines.size();
        for (Line textRow : lines) {
            for (TextChunk text : textRow.getTextElements()) {
                int left = (int) Math.floor(text.getLeft());
                int right = (int) Math.floor(text.getRight());
                int mid = left + ((right - left) / 2);

                currLeftEdges.computeIfAbsent(left, k -> new ArrayList<>()).add(text);
                currMidEdges.computeIfAbsent(mid, k -> new ArrayList<>()).add(text);
                currRightEdges.computeIfAbsent(right, k -> new ArrayList<>()).add(text);

                leftTextEdges.addAll(calculateExtendedEdges(numOfLines, currLeftEdges, left, right));
                midTextEdges.addAll(calculateExtendedEdges(numOfLines, currMidEdges, left, right, mid, 2));
                rightTextEdges.addAll(calculateExtendedEdges(numOfLines, currRightEdges, left, right));
            }
        }

        leftTextEdges.addAll(calculateLeftoverEdges(numOfLines, currLeftEdges));
        midTextEdges.addAll(calculateLeftoverEdges(numOfLines, currMidEdges));
        rightTextEdges.addAll(calculateLeftoverEdges(numOfLines, currRightEdges));

        return new TextEdges(leftTextEdges, midTextEdges, rightTextEdges);
    }

    private Rectangle getTableFromText(List<Line> lines, List<TextEdge> relevantEdges, int relevantEdgeCount, List<Ruling> horizontalRulings) {
        Rectangle table = new Rectangle();
        Line prevRow = null;
        Line firstTableRow = null;
        Line lastTableRow = null;
        int tableSpaceCount = 0;
        float totalRowSpacing = 0;

        for (Line textRow : lines) {
            int numRelevantEdges = 0;

            if (firstTableRow != null && tableSpaceCount > 0) {
                float tableLineThreshold = (totalRowSpacing / tableSpaceCount) * 2.5f;
                float lineDistance = textRow.getTop() - prevRow.getTop();

                if (lineDistance > tableLineThreshold) {
                    lastTableRow = prevRow;
                    break;
                }
            }

            for (TextEdge edge : relevantEdges) {
                if (textRow.horizontallyOverlaps(edge)) {
                    numRelevantEdges++;
                }
            }

            if (numRelevantEdges >= (relevantEdgeCount - 1)) {
                if (prevRow != null && firstTableRow != null) {
                    tableSpaceCount++;
                    totalRowSpacing += (textRow.getTop() - prevRow.getTop());
                }

                if (table.getArea() == 0) {
                    firstTableRow = textRow;
                    table.setBounds(textRow);
                } else {
                    table.setLeft(Math.min(table.getLeft(), textRow.getLeft()));
                    table.setBottom(Math.max(table.getBottom(), textRow.getBottom()));
                    table.setRight(Math.max(table.getRight(), textRow.getRight()));
                }
            } else {
                if (firstTableRow != null && lastTableRow == null) {
                    lastTableRow = prevRow;
                }
            }

            prevRow = textRow;
        }

        if (table.getArea() == 0) {
            return null;
        }

        if (lastTableRow == null) {
            lastTableRow = prevRow;
        }

        return table;
    }

    private List<TextEdge> calculateExtendedEdges(int numOfLines, Map<Integer, List<TextChunk>> currEdges, int left, int right) {
        List<TextEdge> extendedEdges = new ArrayList<>();

        for (Map.Entry<Integer, List<TextChunk>> entry : currEdges.entrySet()) {
            int key = entry.getKey();
            if (key > left && key < right) {
                List<TextChunk> edgeChunks = entry.getValue();
                if (edgeChunks.size() >= 4) { // Minimalna liczba linii do uznania za krawędź
                    TextChunk first = edgeChunks.get(0);
                    TextChunk last = edgeChunks.get(edgeChunks.size() - 1);
                    extendedEdges.add(new TextEdge(key, first.getTop(), key, last.getBottom()));
                }
            }
        }

        return extendedEdges;
    }

    private List<TextEdge> calculateLeftoverEdges(Map<Integer, List<TextChunk>> currEdges) {
        List<TextEdge> leftoverEdges = new ArrayList<>();

        for (Map.Entry<Integer, List<TextChunk>> entry : currEdges.entrySet()) {
            int key = entry.getKey();
            List<TextChunk> edgeChunks = entry.getValue();
            if (edgeChunks.size() >= 4) {
                TextChunk first = edgeChunks.get(0);
                TextChunk last = edgeChunks.get(edgeChunks.size() - 1);
                leftoverEdges.add(new TextEdge(key, first.getTop(), key, last.getBottom()));
            }
        }

        return leftoverEdges;
    }

    private List<TextEdge> calculateExtendedEdges(Map<Integer, List<TextChunk>> currEdges, int left, int right) {
        List<TextEdge> extendedEdges = new ArrayList<>();

        for (Map.Entry<Integer, List<TextChunk>> entry : currEdges.entrySet()) {
            int key = entry.getKey();
            if (key > left && key < right) {
                List<TextChunk> edgeChunks = entry.getValue();
                if (edgeChunks.size() >= 4) { // Minimalna liczba linii do uznania za krawędź
                    TextChunk first = edgeChunks.get(0);
                    TextChunk last = edgeChunks.get(edgeChunks.size() - 1);
                    extendedEdges.add(new TextEdge(key, first.getTop(), key, last.getBottom()));
                }
            }
        }

        return extendedEdges;
    }

    private List<TextEdge> calculateLeftoverEdges(int numOfLines, Map<Integer, List<TextChunk>> currEdges) {
        List<TextEdge> leftoverEdges = new ArrayList<>();

        for (Map.Entry<Integer, List<TextChunk>> entry : currEdges.entrySet()) {
            int key = entry.getKey();
            List<TextChunk> edgeChunks = entry.getValue();
            if (edgeChunks.size() >= 4) {
                TextChunk first = edgeChunks.get(0);
                TextChunk last = edgeChunks.get(edgeChunks.size() - 1);
                leftoverEdges.add(new TextEdge(key, first.getTop(), key, last.getBottom()));
            }
        }

        return leftoverEdges;
    }

    private static final class TextEdges {
        public final List<TextEdge> leftEdges;
        public final List<TextEdge> midEdges;
        public final List<TextEdge> rightEdges;

        public TextEdges(List<TextEdge> leftEdges, List<TextEdge> midEdges, List<TextEdge> rightEdges) {
            this.leftEdges = leftEdges;
            this.midEdges = midEdges;
            this.rightEdges = rightEdges;
        }
    }

    private List<TextEdge> calculateExtendedEdges(int numOfLines, Map<Integer, List<TextChunk>> currEdges,
                                                  int left, int right, int mid, int minDistToMid) {
        List<TextEdge> extendedEdges = new ArrayList<>();

        for (Map.Entry<Integer, List<TextChunk>> entry : currEdges.entrySet()) {
            int key = entry.getKey();

            // Sprawdzamy, czy odległość od środka jest wystarczająca
            boolean hasMinDistToMid = Math.abs(key - mid) > minDistToMid;

            if (key > left && key < right && hasMinDistToMid) {
                List<TextChunk> edgeChunks = entry.getValue();
                if (edgeChunks.size() >= 4) { // Minimalna liczba linii do uznania za krawędź
                    TextChunk first = edgeChunks.get(0);
                    TextChunk last = edgeChunks.get(edgeChunks.size() - 1);
                    extendedEdges.add(new TextEdge(key, first.getTop(), key, last.getBottom()));
                }
            }
        }

        return extendedEdges;
    }
}


