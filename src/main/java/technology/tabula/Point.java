package technology.tabula;

public class Point {
    private final float x;
    private final float y;

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public double distance(Point other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return Float.compare(point.x, x) == 0 && Float.compare(point.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }

    @Override
    public String toString() {
        return String.format("Point[x=%.2f, y=%.2f]", x, y);
    }
}
