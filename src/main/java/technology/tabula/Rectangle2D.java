package technology.tabula;

public class Rectangle2D {
    private float x, y, width, height;

    public Rectangle2D() {
        this(0, 0, 0, 0);
    }

    public Rectangle2D(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public void setRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    @Override
    public String toString() {
        return String.format("Rectangle2D[x=%.2f, y=%.2f, width=%.2f, height=%.2f]", x, y, width, height);
    }

    public boolean intersectsLine(Ruling line) {
        float x1 = line.getStart().getX(), y1 = line.getStart().getY(), x2 = line.getEnd().getX(), y2 = line.getEnd().getY();
        return contains(x1, y1) || contains(x2, y2) ||
                lineIntersects(x, y, x + width, y, x1, y1, x2, y2) || // top edge
                lineIntersects(x, y, x, y + height, x1, y1, x2, y2) || // left edge
                lineIntersects(x + width, y, x + width, y + height, x1, y1, x2, y2) || // right edge
                lineIntersects(x, y + height, x + width, y + height, x1, y1, x2, y2); // bottom edge
    }


    private boolean lineIntersects(float x1, float y1, float x2, float y2,
                                   float x3, float y3, float x4, float y4) {
        float den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (den == 0) return false; // Równoległe linie
        float t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den;
        float u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / den;
        return (t >= 0 && t <= 1 && u >= 0 && u <= 1);
    }
}
