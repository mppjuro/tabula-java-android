package technology.tabula.debug;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.pdfbox.Loader;
import technology.tabula.*;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import javax.imageio.ImageIO;

public class Debug {

    private static final float CIRCLE_RADIUS = 5f;

    public static void debugIntersections(FileWriter writer, Page page) throws IOException {
        int i = 0;
        for (Point2D p : Ruling.findIntersections(page.getHorizontalRulings(), page.getVerticalRulings()).keySet()) {
            writer.write("Intersection at: " + p.getX() + ", " + p.getY() + "\n");
            i++;
        }
    }

    private static void debugRulings(FileWriter writer, Page page) throws IOException {
        List<Ruling> rulings = new ArrayList<>(page.getHorizontalRulings());
        rulings.addAll(page.getVerticalRulings());
        for (Ruling r : rulings) {
            writer.write("Ruling: " + r.toString() + "\n");
        }
    }

    private static void debugColumns(FileWriter writer, Page page) throws IOException {
        List<TextChunk> textChunks = TextElement.mergeWords(page.getText());
        List<Line> lines = TextChunk.groupByLines(textChunks);
        List<Float> columns = BasicExtractionAlgorithm.columnPositions(lines);
        for (float p : columns) {
            writer.write("Column at: " + p + "\n");
        }
    }

    private static void debugDetectedTables(FileWriter writer, Page page) throws IOException {
        NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();
        List<Rectangle> tables = detectionAlgorithm.detect(page);
        for (Rectangle t : tables) {
            writer.write("Detected table: " + t.toString() + "\n");
        }
    }

    public static void renderPage(String pdfPath, String outPath, int pageNumber, Rectangle area,
                                  boolean drawTextChunks, boolean drawSpreadsheets, boolean drawRulings, boolean drawIntersections,
                                  boolean drawColumns, boolean drawDetectedTables) throws IOException {
        PDDocument document = Loader.loadPDF(new File(pdfPath));
        ObjectExtractor oe = new ObjectExtractor(document);
        Page page = oe.extract(pageNumber + 1);

        if (area != null) {
            page = page.getArea(area);
        }

        try (FileWriter writer = new FileWriter(outPath.replace(".jpg", ".txt"))) {
            if (drawTextChunks) {
                writer.write("Text Chunks:\n");
                for (TextChunk tc : TextElement.mergeWords(page.getText())) {
                    writer.write(tc.toString() + "\n");
                }
            }
            if (drawSpreadsheets) {
                writer.write("Spreadsheets:\n");
                for (Table table : new SpreadsheetExtractionAlgorithm().extract(page)) {
                    writer.write(table.toString() + "\n");
                }
            }
            if (drawRulings) {
                writer.write("Rulings:\n");
                debugRulings(writer, page);
            }
            if (drawIntersections) {
                writer.write("Intersections:\n");
                debugIntersections(writer, page);
            }
            if (drawColumns) {
                writer.write("Columns:\n");
                debugColumns(writer, page);
            }
            if (drawDetectedTables) {
                writer.write("Detected Tables:\n");
                debugDetectedTables(writer, page);
            }
        }

        document.close();
    }

    public static void main(String[] args) throws IOException {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(buildOptions(), args);
            List<Integer> pages = new ArrayList<>();
            if (line.hasOption('p')) {
                pages = Utils.parsePagesOption(line.getOptionValue('p'));
            } else {
                pages.add(1);
            }

            if (line.hasOption('h')) {
                printHelp();
                System.exit(0);
            }

            if (line.getArgs().length != 1) {
                throw new ParseException("Need one filename\nTry --help for help");
            }

            File pdfFile = new File(line.getArgs()[0]);
            if (!pdfFile.exists()) {
                throw new ParseException("File does not exist");
            }

            Rectangle area = null;
            if (line.hasOption('a')) {
                List<Float> f = CommandLineApp.parseFloatList(line.getOptionValue('a'));
                if (f.size() != 4) {
                    throw new ParseException("area parameters must be top,left,bottom,right");
                }
                area = new Rectangle(f.get(0), f.get(1), f.get(3) - f.get(1), f.get(2) - f.get(0));
            }

            for (int i : pages) {
                renderPage(pdfFile.getAbsolutePath(),
                        new File(pdfFile.getParent(), removeExtension(pdfFile.getName()) + "-" + (i) + ".txt")
                                .getAbsolutePath(),
                        i - 1, area, line.hasOption('t'), line.hasOption('s'), line.hasOption('r'), line.hasOption('i'),
                        line.hasOption('c'), line.hasOption('d'));
            }
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tabula-debug", "Generate debugging output", buildOptions(), "", true);
    }

    private static Options buildOptions() {
        Options o = new Options();
        o.addOption("h", "help", false, "Print this help text.");
        o.addOption("r", "rulings", false, "Show detected rulings.");
        o.addOption("i", "intersections", false, "Show intersections.");
        o.addOption("s", "spreadsheets", false, "Show detected spreadsheets.");
        o.addOption("t", "textchunks", false, "Show detected text chunks.");
        o.addOption("c", "columns", false, "Show detected columns.");
        o.addOption("d", "detected-tables", false, "Show detected tables.");
        o.addOption(Option.builder("a").longOpt("area")
                .desc("Analyze specific area (top,left,bottom,right). Example: --area 269.875,12.75,790.5,561.")
                .hasArg()
                .argName("AREA")
                .build());
        o.addOption(Option.builder("p").longOpt("pages")
                .desc("Comma-separated list of pages, or 'all'. Example: --pages 1-3,5 or --pages all.")
                .hasArg()
                .argName("PAGES")
                .build());
        return o;
    }

    private static String removeExtension(String s) {
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex == -1) ? s : s.substring(0, extensionIndex);
    }
}
