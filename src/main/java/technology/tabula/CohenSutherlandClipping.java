package technology.tabula;

/**
 * Implementacja algorytmu Cohen-Sutherland do przycinania linii względem prostokąta.
 */
public final class CohenSutherlandClipping {

    private double xMin;
    private double yMin;
    private double xMax;
    private double yMax;

    private static final int INSIDE = 0;
    private static final int LEFT   = 1;
    private static final int RIGHT  = 2;
    private static final int BOTTOM = 4;
    private static final int TOP    = 8;

    private static final float MINIMUM_DELTA = 0.01f;

    public CohenSutherlandClipping() {}

    public CohenSutherlandClipping(Rectangle2D clipWindow) {
        setClip(clipWindow);
    }

    public void setClip(Rectangle2D clipWindow) {
        xMin = clipWindow.getX();
        xMax = xMin + clipWindow.getWidth();
        yMin = clipWindow.getY();
        yMax = yMin + clipWindow.getHeight();
    }

    public boolean clip(Line2D.Float line) {
        Point point1 = new Point(line.getX1(), line.getY1());
        Point point2 = new Point(line.getX2(), line.getY2());
        Point outsidePoint = new Point(0d, 0d);

        boolean lineIsVertical = (point1.x == point2.x);
        double lineSlope = lineIsVertical ? 0d : (point2.y - point1.y) / (point2.x - point1.x);

        while (point1.region != INSIDE || point2.region != INSIDE) {
            if ((point1.region & point2.region) != 0) return false;

            outsidePoint.region = (point1.region == INSIDE) ? point2.region : point1.region;

            if ((outsidePoint.region & LEFT) != 0) {
                outsidePoint.x = xMin;
                outsidePoint.y = delta(outsidePoint.x, point1.x) * lineSlope + point1.y;
            }
            else if ((outsidePoint.region & RIGHT) != 0) {
                outsidePoint.x = xMax;
                outsidePoint.y = delta(outsidePoint.x, point1.x) * lineSlope + point1.y;
            }
            else if ((outsidePoint.region & BOTTOM) != 0) {
                outsidePoint.y = yMin;
                outsidePoint.x = lineIsVertical ? point1.x : delta(outsidePoint.y, point1.y) / lineSlope + point1.x;
            }
            else if ((outsidePoint.region & TOP) != 0) {
                outsidePoint.y = yMax;
                outsidePoint.x = lineIsVertical ? point1.x : delta(outsidePoint.y, point1.y) / lineSlope + point1.x;
            }

            if (outsidePoint.isInTheSameRegionAs(point1)) {
                point1.setPositionAndRegion(outsidePoint.x, outsidePoint.y);
            } else {
                point2.setPositionAndRegion(outsidePoint.x, outsidePoint.y);
            }
        }
        line.setLine((float)point1.x, (float)point1.y, (float)point2.x, (float)point2.y);
        return true;
    }

    private static double delta(double value1, double value2) {
        return (Math.abs(value1 - value2) < MINIMUM_DELTA) ? 0 : (value1 - value2);
    }

    class Point {
        double x, y;
        int region;

        Point(double x, double y) {
            setPositionAndRegion(x, y);
        }

        void setPositionAndRegion(double x, double y) {
            this.x = x;
            this.y = y;
            region = (x < xMin) ? LEFT : (x > xMax) ? RIGHT : INSIDE;
            if (y < yMin) region |= BOTTOM;
            else if (y > yMax) region |= TOP;
        }

        boolean isInTheSameRegionAs(Point otherPoint) {
            return this.region == otherPoint.region;
        }
    }
}
