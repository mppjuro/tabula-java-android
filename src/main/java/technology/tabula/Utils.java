package technology.tabula;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.io.IOException;

public class Utils {
    private final static float EPSILON = 0.01f;

    public static boolean within(double first, double second, double variance) {
        return second < first + variance && second > first - variance;
    }

    public static boolean feq(double f1, double f2) {
        return (Math.abs(f1 - f2) < EPSILON);
    }

    public static float round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public static List<Integer> range(final int begin, final int end) {
        return new AbstractList<Integer>() {
            @Override
            public Integer get(int index) {
                return begin + index;
            }

            @Override
            public int size() {
                return end - begin;
            }
        };
    }

    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        Collections.sort(list);
    }

    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        Collections.sort(list, comparator);
    }

    public static BufferedImage pageConvertToImage(PDDocument doc, PDPage page, int dpi, ImageType imageType) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        return renderer.renderImageWithDPI(doc.getPages().indexOf(page), dpi, imageType);
    }

    /**
     * Przekształca punkty `start` i `end` dla każdego `Ruling`, aby wyrównać blisko położone linie
     */
    public static void snapRulings(List<Ruling> rulings, float xThreshold, float yThreshold) {
        List<Point2D> points = new ArrayList<>();
        Map<Ruling, Point2D[]> rulingToPoints = new HashMap<>();

        // Pobranie wszystkich punktów startowych i końcowych
        for (Ruling r : rulings) {
            Point2D p1 = r.getStartPoint();
            Point2D p2 = r.getEndPoint();
            rulingToPoints.put(r, new Point2D[]{p1, p2});
            points.add(p1);
            points.add(p2);
        }

        // Grupowanie i wyrównywanie punktów w osi X
        snapPointsByAxis(points, xThreshold, true);
        // Grupowanie i wyrównywanie punktów w osi Y
        snapPointsByAxis(points, yThreshold, false);

        // Aktualizacja rulings po wyrównaniu punktów
        for (Map.Entry<Ruling, Point2D[]> entry : rulingToPoints.entrySet()) {
            Point2D[] updatedPoints = entry.getValue();
            entry.getKey().setStartPoint(updatedPoints[0]);
            entry.getKey().setEndPoint(updatedPoints[1]);
        }
    }

    /**
     * Grupowanie punktów w osi X lub Y i wyrównanie ich do średniej wartości
     */
    private static void snapPointsByAxis(List<Point2D> points, float threshold, boolean isXAxis) {
        points.sort(Comparator.comparingDouble(p -> isXAxis ? p.getX() : p.getY()));

        List<List<Point2D>> groupedPoints = new ArrayList<>();
        groupedPoints.add(new ArrayList<>(Collections.singletonList(points.get(0))));

        for (Point2D p : points.subList(1, points.size())) {
            List<Point2D> lastGroup = groupedPoints.get(groupedPoints.size() - 1);
            if (Math.abs((isXAxis ? p.getX() : p.getY()) - (isXAxis ? lastGroup.get(0).getX() : lastGroup.get(0).getY())) < threshold) {
                lastGroup.add(p);
            } else {
                groupedPoints.add(new ArrayList<>(Collections.singletonList(p)));
            }
        }

        for (List<Point2D> group : groupedPoints) {
            float avgLoc = 0;
            for (Point2D p : group) {
                avgLoc += isXAxis ? p.getX() : p.getY();
            }
            avgLoc /= group.size();

            for (Point2D p : group) {
                if (isXAxis) {
                    p.setX(avgLoc);
                } else {
                    p.setY(avgLoc);
                }
            }
        }
    }

    public static List<List<Point2D>> getPolygonsFromEdges(Map<Point2D, Point2D> edgesH, Map<Point2D, Point2D> edgesV) {
        List<List<Point2D>> polygons = new ArrayList<>();
        Set<Point2D> visited = new HashSet<>();

        for (Point2D start : edgesH.keySet()) {
            if (visited.contains(start)) continue;

            List<Point2D> polygon = new ArrayList<>();
            Point2D current = start;

            while (current != null && !visited.contains(current)) {
                polygon.add(current);
                visited.add(current);

                current = edgesH.get(current);
                if (current == null) break;

                polygon.add(current);
                visited.add(current);

                current = edgesV.get(current);
            }

            if (!polygon.isEmpty()) {
                polygons.add(polygon);
            }
        }

        return polygons;
    }

    public static List<Integer> parsePagesOption(String pagesSpec) {
        if (pagesSpec.equals("all")) {
            return null;
        }

        List<Integer> rv = new ArrayList<>();
        String[] ranges = pagesSpec.split(",");
        for (String range : ranges) {
            String[] r = range.split("-");
            if (r.length == 0 || !isNumeric(r[0]) || (r.length > 1 && !isNumeric(r[1]))) {
                throw new IllegalArgumentException("Syntax error in page range specification");
            }

            if (r.length < 2) {
                rv.add(Integer.parseInt(r[0]));
            } else {
                int t = Integer.parseInt(r[0]);
                int f = Integer.parseInt(r[1]);
                if (t > f) {
                    throw new IllegalArgumentException("Syntax error in page range specification");
                }
                rv.addAll(range(t, f + 1));
            }
        }

        Collections.sort(rv);
        return rv;
    }

    public static boolean isNumeric(final CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String join(String delimiter, String... elements) {
        if (elements == null || elements.length == 0) {
            return "";
        }
        return String.join(delimiter, elements);
    }

    public static void snapPoints(List<Point2D> points, float xThreshold, float yThreshold) {
        if (points == null || points.isEmpty()) {
            return;
        }

        // Sortowanie według X
        points.sort(Comparator.comparingDouble(Point2D::getX));

        // Grupowanie i "przyciąganie" punktów X
        List<List<Point2D>> groupedX = new ArrayList<>();
        for (Point2D p : points) {
            if (!groupedX.isEmpty() && Math.abs(p.getX() - groupedX.get(groupedX.size() - 1).get(0).getX()) < xThreshold) {
                groupedX.get(groupedX.size() - 1).add(p);
            } else {
                groupedX.add(new ArrayList<>(Collections.singletonList(p)));
            }
        }

        // Uśrednienie i aktualizacja wartości X
        for (List<Point2D> group : groupedX) {
            float avgX = (float) group.stream().mapToDouble(Point2D::getX).average().orElse(0);
            for (Point2D p : group) {
                p.setX(avgX);
            }
        }

        // Sortowanie według Y
        points.sort(Comparator.comparingDouble(Point2D::getY));

        // Grupowanie i "przyciąganie" punktów Y
        List<List<Point2D>> groupedY = new ArrayList<>();
        for (Point2D p : points) {
            if (!groupedY.isEmpty() && Math.abs(p.getY() - groupedY.get(groupedY.size() - 1).get(0).getY()) < yThreshold) {
                groupedY.get(groupedY.size() - 1).add(p);
            } else {
                groupedY.add(new ArrayList<>(Collections.singletonList(p)));
            }
        }

        // Uśrednienie i aktualizacja wartości Y
        for (List<Point2D> group : groupedY) {
            float avgY = (float) group.stream().mapToDouble(Point2D::getY).average().orElse(0);
            for (Point2D p : group) {
                p.setY(avgY);
            }
        }
    }

    public static void snapPointPairs(List<Point2D[]> pointPairs, float xThreshold, float yThreshold) {
        if (pointPairs == null || pointPairs.isEmpty()) return;

        List<Point2D> points = new ArrayList<>();
        for (Point2D[] pair : pointPairs) {
            points.add(pair[0]);
            points.add(pair[1]);
        }

        snapPoints(points, xThreshold, yThreshold);
    }

    public static Rectangle bounds(Collection<? extends Rectangle> rectangles) {
        if (rectangles == null || rectangles.isEmpty()) {
            return new Rectangle();
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (Rectangle rect : rectangles) {
            minX = Math.min(minX, rect.getLeft());
            minY = Math.min(minY, rect.getTop());
            maxX = Math.max(maxX, rect.getRight());
            maxY = Math.max(maxY, rect.getBottom());
        }

        return new Rectangle(minY, minX, maxX - minX, maxY - minY);
    }

    public static boolean overlap(float y1, float height1, float y2, float height2) {
        return (y1 < y2 + height2) && (y1 + height1 > y2);
    }
}
