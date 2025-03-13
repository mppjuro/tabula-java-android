package technology.tabula;

public class Point2D {
    private float x;
    private float y;

    public Point2D(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point2D)) return false;
        Point2D other = (Point2D) obj;
        return Float.compare(x, other.x) == 0 && Float.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
