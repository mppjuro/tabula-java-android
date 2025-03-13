package technology.tabula.extractors;

import technology.tabula.*;
import java.util.*;

public class SpreadsheetExtractionAlgorithm implements ExtractionAlgorithm {

    private static final float MAGIC_HEURISTIC_NUMBER = 0.65f;

    private static final Comparator<Point2D> Y_FIRST_POINT_COMPARATOR = (point1, point2) -> {
        int compareY = compareRounded(point1.getY(), point2.getY());
        if (compareY == 0) {
            return compareRounded(point1.getX(), point2.getX());
        }
        return compareY;
    };

    private static final Comparator<Point2D> X_FIRST_POINT_COMPARATOR = (point1, point2) -> {
        int compareX = compareRounded(point1.getX(), point2.getX());
        if (compareX == 0) {
            return compareRounded(point1.getY(), point2.getY());
        }
        return compareX;
    };

    private static int compareRounded(double d1, double d2) {
        float d1Rounded = Utils.round(d1, 2);
        float d2Rounded = Utils.round(d2, 2);

        return Float.compare(d1Rounded, d2Rounded);
    }

    @Override
    public List<Table> extract(Page page) {
        return extract(page, page.getRulings());
    }

    public List<Table> extract(Page page, List<Ruling> rulings) {
        List<Ruling> horizontalR = new ArrayList<>();
        List<Ruling> verticalR = new ArrayList<>();

        for (Ruling r: rulings) {
            if (r.horizontal()) {
                horizontalR.add(r);
            }
            else if (r.vertical()) {
                verticalR.add(r);
            }
        }
        horizontalR = Ruling.collapseOrientedRulings(horizontalR);
        verticalR = Ruling.collapseOrientedRulings(verticalR);

        List<Cell> cells = findCells(horizontalR, verticalR);
        List<Rectangle> spreadsheetAreas = findSpreadsheetsFromCells(cells);

        List<Table> spreadsheets = new ArrayList<>();
        for (Rectangle area: spreadsheetAreas) {
            List<Cell> overlappingCells = new ArrayList<>();
            for (Cell c: cells) {
                if (c.intersects(area)) {
                    c.setTextElements(TextElement.mergeWords(page.getText(c)));
                    overlappingCells.add(c);
                }
            }

            List<Ruling> horizontalOverlappingRulings = new ArrayList<>();
            for (Ruling hr: horizontalR) {
                if (area.intersects(hr)) {
                    horizontalOverlappingRulings.add(hr);
                }
            }
            List<Ruling> verticalOverlappingRulings = new ArrayList<>();
            for (Ruling vr: verticalR) {
                if (area.intersects(vr)) {
                    verticalOverlappingRulings.add(vr);
                }
            }

            TableWithRulingLines t = new TableWithRulingLines(area, overlappingCells, horizontalOverlappingRulings, verticalOverlappingRulings, this, page.getPageNumber());
            spreadsheets.add(t);
        }
        Utils.sort(spreadsheets, Rectangle.ILL_DEFINED_ORDER);
        return spreadsheets;
    }

    public static List<Cell> findCells(List<Ruling> horizontalRulingLines, List<Ruling> verticalRulingLines) {
        List<Cell> cellsFound = new ArrayList<>();
        Map<Point2D, Ruling[]> intersectionPoints = Ruling.findIntersections(horizontalRulingLines, verticalRulingLines);
        List<Point2D> intersectionPointsList = new ArrayList<>(intersectionPoints.keySet());
        intersectionPointsList.sort(Y_FIRST_POINT_COMPARATOR);

        for (int i = 0; i < intersectionPointsList.size(); i++) {
            Point2D topLeft = intersectionPointsList.get(i);
            Ruling[] hv = intersectionPoints.get(topLeft);

            List<Point2D> xPoints = new ArrayList<>();
            List<Point2D> yPoints = new ArrayList<>();

            for (Point2D p: intersectionPointsList.subList(i, intersectionPointsList.size())) {
                if (p.getX() == topLeft.getX() && p.getY() > topLeft.getY()) {
                    xPoints.add(p);
                }
                if (p.getY() == topLeft.getY() && p.getX() > topLeft.getX()) {
                    yPoints.add(p);
                }
            }
            outer:
            for (Point2D xPoint: xPoints) {
                if (!hv[1].equals(intersectionPoints.get(xPoint)[1])) {
                    continue;
                }
                for (Point2D yPoint: yPoints) {
                    if (!hv[0].equals(intersectionPoints.get(yPoint)[0])) {
                        continue;
                    }
                    Point2D btmRight = new Point2D(yPoint.getX(), xPoint.getY());
                    if (intersectionPoints.containsKey(btmRight)
                            && intersectionPoints.get(btmRight)[0].equals(intersectionPoints.get(xPoint)[0])
                            && intersectionPoints.get(btmRight)[1].equals(intersectionPoints.get(yPoint)[1])) {
                        cellsFound.add(new Cell(topLeft, btmRight));
                        break outer;
                    }
                }
            }
        }
        return cellsFound;
    }

    public static List<Rectangle> findSpreadsheetsFromCells(List<? extends Rectangle> cells) {
        List<Rectangle> rectangles = new ArrayList<>();
        Set<Point2D> pointSet = new HashSet<>();
        Map<Point2D, Point2D> edgesH = new HashMap<>();
        Map<Point2D, Point2D> edgesV = new HashMap<>();
        int i = 0;

        cells = new ArrayList<>(new HashSet<>(cells));
        Utils.sort(cells, Rectangle.ILL_DEFINED_ORDER);

        for (Rectangle cell: cells) {
            for(Point2D pt: cell.getPoints()) {
                if (pointSet.contains(pt)) {
                    pointSet.remove(pt);
                }
                else {
                    pointSet.add(pt);
                }
            }
        }

        for (List<Point2D> poly : Utils.getPolygonsFromEdges(edgesH, edgesV)) {
            float top = Float.MAX_VALUE, left = Float.MAX_VALUE;
            float bottom = Float.MIN_VALUE, right = Float.MIN_VALUE;
            for (Point2D pt : poly) {
                top = Math.min(top, pt.getY());
                left = Math.min(left, pt.getX());
                bottom = Math.max(bottom, pt.getY());
                right = Math.max(right, pt.getX());
            }
            rectangles.add(new Rectangle(top, left, right - left, bottom - top));
        }

        return rectangles;
    }

    @Override
    public String toString() {
        return "lattice";
    }

    public boolean isTabular(Page page) {

        // if there's no text at all on the page, it's not a table
        // (we won't be able to do anything with it though)
        if (page.getText().isEmpty()){
            return false;
        }

        // get minimal region of page that contains every character (in effect,
        // removes white "margins")
        Page minimalRegion = page.getArea(Utils.bounds(page.getText()));

        List<? extends Table> tables = new SpreadsheetExtractionAlgorithm().extract(minimalRegion);
        if (tables.isEmpty()) {
            return false;
        }
        Table table = tables.get(0);
        int rowsDefinedByLines = table.getRowCount();
        int colsDefinedByLines = table.getColCount();

        tables = new BasicExtractionAlgorithm().extract(minimalRegion);
        if (tables.isEmpty()) {
            return false;
        }
        table = tables.get(0);
        int rowsDefinedWithoutLines = table.getRowCount();
        int colsDefinedWithoutLines = table.getColCount();

        float ratio = (((float) colsDefinedByLines / colsDefinedWithoutLines) +
                ((float) rowsDefinedByLines / rowsDefinedWithoutLines)) / 2.0f;

        return ratio > MAGIC_HEURISTIC_NUMBER && ratio < (1 / MAGIC_HEURISTIC_NUMBER);
    }
}
