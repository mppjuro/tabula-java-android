package technology.tabula;

import java.util.*;

public class Ruling {

    private Point2D start;
    private Point2D end;

    private static final int PERPENDICULAR_PIXEL_EXPAND_AMOUNT = 2;
    private static final int COLINEAR_OR_PARALLEL_PIXEL_EXPAND_AMOUNT = 1;

    private enum SOType { VERTICAL, HRIGHT, HLEFT }

    public Ruling(float top, float left, float width, float height) {
        this.start = new Point2D(left, top);
        this.end = new Point2D(left + width, top + height);
        this.normalize();
    }

    public Ruling(Point2D p1, Point2D p2) {
        this.start = new Point2D(p1.getX(), p1.getY());
        this.end = new Point2D(p2.getX(), p2.getY());
        this.normalize();
    }

    public void normalize() {
        double angle = this.getAngle();
        if (Utils.within(angle, 0, 1) || Utils.within(angle, 180, 1)) {
            this.end = new Point2D(this.end.getX(), this.start.getY());
        }
        else if (Utils.within(angle, 90, 1) || Utils.within(angle, 270, 1)) {
            this.end = new Point2D(this.start.getX(), this.end.getY());
        }
    }

    public double getAngle() {
        return Math.toDegrees(Math.atan2(end.getY() - start.getY(), end.getX() - start.getX()));
    }

    public boolean vertical() {
        return this.length() > 0 && Utils.feq(start.getX(), end.getX());
    }

    public boolean horizontal() {
        return this.length() > 0 && Utils.feq(start.getY(), end.getY());
    }

    public boolean oblique() {
        return !(this.vertical() || this.horizontal());
    }

    public float getTop() {
        return Math.min(start.getY(), end.getY());
    }

    public float getLeft() {
        return Math.min(start.getX(), end.getX());
    }

    public float getBottom() {
        return Math.max(start.getY(), end.getY());
    }

    public float getRight() {
        return Math.max(start.getX(), end.getX());
    }

    public Point2D getStart() {
        return start;
    }

    public Point2D getEnd() {
        return end;
    }

    public void setStartPoint(Point2D start) {
        this.start = start;
    }

    public void setStart(Point2D start) {
        this.start = start;
    }

    public void setEndPoint(Point2D end) {
        this.end = end;
    }

    public void setEnd(Point2D end) {
        this.end = end;
    }

    public double length() {
        return Math.sqrt(Math.pow(start.getX() - end.getX(), 2) + Math.pow(start.getY() - end.getY(), 2));
    }

    public boolean intersects(Rectangle area) {
        return (start.getX() >= area.getLeft() && start.getX() <= area.getRight() &&
                start.getY() >= area.getTop() && start.getY() <= area.getBottom()) ||
                (end.getX() >= area.getLeft() && end.getX() <= area.getRight() &&
                        end.getY() >= area.getTop() && end.getY() <= area.getBottom());
    }

    public boolean intersects(Ruling another) {
        return checkLineIntersection(
                this.start.getX(), this.start.getY(), this.end.getX(), this.end.getY(),
                another.start.getX(), another.start.getY(), another.end.getX(), another.end.getY()
        );
    }

    private boolean checkLineIntersection(float x1, float y1, float x2, float y2,
                                          float x3, float y3, float x4, float y4) {
        float denominator = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denominator == 0) {
            return false;
        }
        float ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denominator;
        float ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denominator;
        return (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1);
    }

    public Point2D intersectionPoint(Ruling other) {
        if (!this.intersects(other)) {
            return null;
        }
        return new Point2D(other.getLeft(), this.getTop());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Ruling)) return false;
        Ruling o = (Ruling) obj;
        return start.equals(o.start) && end.equals(o.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return String.format("Ruling[start=%s, end=%s]", start, end);
    }

    public static List<Ruling> collapseOrientedRulings(List<Ruling> lines) {
        List<Ruling> result = new ArrayList<>();
        lines.sort(Comparator.comparing(Ruling::getTop).thenComparing(Ruling::getLeft));

        for (Ruling nextLine : lines) {
            if (nextLine.length() == 0) continue;
            result.add(nextLine);
        }
        return result;
    }

    public static Map<Point2D, Ruling[]> findIntersections(List<Ruling> horizontals, List<Ruling> verticals) {
        Map<Point2D, Ruling[]> intersections = new HashMap<>();

        for (Ruling h : horizontals) {
            for (Ruling v : verticals) {
                if (h.intersects(v)) {
                    Point2D intersection = new Point2D(v.getLeft(), h.getTop());
                    intersections.put(intersection, new Ruling[]{h, v});
                }
            }
        }

        return intersections;
    }

    public static List<Ruling> cropRulingsToArea(List<Ruling> rulings, Rectangle area) {
        List<Ruling> result = new ArrayList<>();
        for (Ruling r : rulings) {
            if (r.intersects(area)) {
                result.add(r);
            }
        }
        return result;
    }

    public Point2D getStartPoint() {
        return start;
    }

    public Point2D getEndPoint() {
        return end;
    }

    public float getPosition() {
        return vertical() ? start.getX() : start.getY();
    }
}
