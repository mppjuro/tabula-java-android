package technology.tabula;

public class AndroidRectangle {
    public float x, y, width, height;

    public AndroidRectangle(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setRect(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public AndroidRectangle createUnion(AndroidRectangle other) {
        float minX = Math.min(this.x, other.x);
        float minY = Math.min(this.y, other.y);
        float maxX = Math.max(this.x + this.width, other.x + other.width);
        float maxY = Math.max(this.y + this.height, other.y + other.height);

        return new AndroidRectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public boolean contains(float px, float py) {
        return (px >= x && px <= x + width && py >= y && py <= y + height);
    }

    public float getMinX() { return x; }
    public float getMinY() { return y; }
    public float getMaxX() { return x + width; }
    public float getMaxY() { return y + height; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
}
