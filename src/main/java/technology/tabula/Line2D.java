package technology.tabula;

public class Line2D {
    public static class Float {
        protected float x1, y1, x2, y2;

        public Float() {
            this(0, 0, 0, 0);
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D p1, Point2D p2) {
            this(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public void setLine(Point2D p1, Point2D p2) {
            setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
        }

        public Point2D getP1() {
            return new Point2D(x1, y1);
        }

        public Point2D getP2() {
            return new Point2D(x2, y2);
        }

        public float getX1() {
            return x1;
        }

        public float getY1() {
            return y1;
        }

        public float getX2() {
            return x2;
        }

        public float getY2() {
            return y2;
        }

        public double getAngle() {
            double angle = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
            return angle < 0 ? angle + 360 : angle;
        }

        public double length() {
            return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        }

        public boolean intersects(Line2D.Float other) {
            float denom = (other.y2 - other.y1) * (x2 - x1) - (other.x2 - other.x1) * (y2 - y1);
            if (denom == 0) return false;

            float ua = ((other.x2 - other.x1) * (y1 - other.y1) - (other.y2 - other.y1) * (x1 - other.x1)) / denom;
            float ub = ((x2 - x1) * (y1 - other.y1) - (y2 - y1) * (x1 - other.x1)) / denom;

            return (ua >= 0 && ua <= 1) && (ub >= 0 && ub <= 1);
        }

        @Override
        public String toString() {
            return String.format("Line2D.Float[x1=%.2f, y1=%.2f, x2=%.2f, y2=%.2f]", x1, y1, x2, y2);
        }
    }
}
