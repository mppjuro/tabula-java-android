package technology.tabula;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Rectangle {

	public static final Comparator<Rectangle> ILL_DEFINED_ORDER = new Comparator<Rectangle>() {
		@Override
		public int compare(Rectangle o1, Rectangle o2) {
			if (o1.equals(o2)) return 0;
			if (o1.verticalOverlap(o2) > VERTICAL_COMPARISON_THRESHOLD) {
				return o1.isLtrDominant() == -1 && o2.isLtrDominant() == -1
						? -Float.compare(o1.getLeft(), o2.getLeft())
						: Float.compare(o1.getLeft(), o2.getLeft());
			} else {
				return Float.compare(o1.getBottom(), o2.getBottom());
			}
		}
	};

	protected static final float VERTICAL_COMPARISON_THRESHOLD = 0.4f;

	public float top;
	public float left;
	public float width;
	public float height;

	public Rectangle() {
		this.top = 0;
		this.left = 0;
		this.width = 0;
		this.height = 0;
	}

	public boolean contains(Rectangle rect) {
		return rect.getLeft() >= this.getLeft() &&
				rect.getRight() <= this.getRight() &&
				rect.getTop() >= this.getTop() &&
				rect.getBottom() <= this.getBottom();
	}

	public Rectangle(float top, float left, float width, float height) {
		this.top = top;
		this.left = left;
		this.width = width;
		this.height = height;
	}

	public void setBounds(Rectangle rect) {
		this.top = rect.top;
		this.left = rect.left;
		this.width = rect.width;
		this.height = rect.height;
	}

	public void setRect(float top, float left, float width, float height) {
		this.top = top;
		this.left = left;
		this.width = width;
		this.height = height;
	}

	public boolean intersects(Rectangle other) {
		return this.getLeft() < other.getRight() &&
				this.getRight() > other.getLeft() &&
				this.getTop() < other.getBottom() &&
				this.getBottom() > other.getTop();
	}

	public void setRect(Rectangle rect) {
		this.setRect(rect.top, rect.left, rect.width, rect.height);
	}

	public int compareTo(Rectangle other) {
		return ILL_DEFINED_ORDER.compare(this, other);
	}

	public int isLtrDominant() {
		return 0;
	}

	public float getArea() {
		return this.width * this.height;
	}

	public float verticalOverlap(Rectangle other) {
		return Math.max(0, Math.min(this.getBottom(), other.getBottom()) - Math.max(this.getTop(), other.getTop()));
	}

	public boolean verticallyOverlaps(Rectangle other) {
		return verticalOverlap(other) > 0;
	}

	public float horizontalOverlap(Rectangle other) {
		return Math.max(0, Math.min(this.getRight(), other.getRight()) - Math.max(this.getLeft(), other.getLeft()));
	}

	public boolean horizontallyOverlaps(Rectangle other) {
		return horizontalOverlap(other) > 0;
	}

	public float overlapRatio(Rectangle other) {
		float intersectionWidth = Math.max(0,
				Math.min(this.getRight(), other.getRight()) - Math.max(this.getLeft(), other.getLeft()));
		float intersectionHeight = Math.max(0,
				Math.min(this.getBottom(), other.getBottom()) - Math.max(this.getTop(), other.getTop()));
		float intersectionArea = intersectionWidth * intersectionHeight;
		float unionArea = this.getArea() + other.getArea() - intersectionArea;

		return intersectionArea / unionArea;
	}

	public Rectangle merge(Rectangle other) {
		float newTop = Math.min(this.top, other.top);
		float newLeft = Math.min(this.left, other.left);
		float newRight = Math.max(this.getRight(), other.getRight());
		float newBottom = Math.max(this.getBottom(), other.getBottom());

		this.top = newTop;
		this.left = newLeft;
		this.width = newRight - newLeft;
		this.height = newBottom - newTop;

		return this;
	}

	public float getTop() {
		return top;
	}

	public void setTop(float top) {
		this.height += (this.top - top);
		this.top = top;
	}

	public float getRight() {
		return left + width;
	}

	public void setRight(float right) {
		this.width = right - this.left;
	}

	public float getLeft() {
		return left;
	}

	public void setLeft(float left) {
		this.width += (this.left - left);
		this.left = left;
	}

	public float getBottom() {
		return top + height;
	}

	public void setBottom(float bottom) {
		this.height = bottom - this.top;
	}

	public Point2D[] getPoints() {
		return new Point2D[] { new Point2D(left, top), new Point2D(getRight(), top),
				new Point2D(getRight(), getBottom()), new Point2D(left, getBottom()) };
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "Rectangle[top=%.2f, left=%.2f, width=%.2f, height=%.2f, bottom=%.2f, right=%.2f]",
				this.top, this.left, this.width, this.height, this.getBottom(), this.getRight());
	}

	public static Rectangle boundingBoxOf(List<? extends Rectangle> rectangles) {
		float minx = Float.MAX_VALUE;
		float miny = Float.MAX_VALUE;
		float maxx = Float.MIN_VALUE;
		float maxy = Float.MIN_VALUE;

		for (Rectangle r : rectangles) {
			minx = Math.min(r.getLeft(), minx);
			miny = Math.min(r.getTop(), miny);
			maxx = Math.max(r.getRight(), maxx);
			maxy = Math.max(r.getBottom(), maxy);
		}
		return new Rectangle(miny, minx, maxx - minx, maxy - miny);
	}

	public boolean intersects(Ruling ruling) {
		return this.getLeft() <= ruling.getRight() &&
				this.getRight() >= ruling.getLeft() &&
				this.getTop() <= ruling.getBottom() &&
				this.getBottom() >= ruling.getTop();
	}

	public float verticalOverlapRatio(Rectangle other) {
		float overlap = verticalOverlap(other);
		float minHeight = Math.min(this.getHeight(), other.getHeight());
		return minHeight > 0 ? overlap / minHeight : 0;
	}

	public float getHeight() {
		return this.height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getWidth() {
		return this.width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public boolean contains(Point2D point) {
		return point.getX() >= this.getLeft() && point.getX() <= this.getRight() &&
				point.getY() >= this.getTop() && point.getY() <= this.getBottom();
	}

	public float distance(Rectangle other) {
		float centerX1 = this.getLeft() + this.getWidth() / 2;
		float centerY1 = this.getTop() + this.getHeight() / 2;
		float centerX2 = other.getLeft() + other.getWidth() / 2;
		float centerY2 = other.getTop() + other.getHeight() / 2;

		return (float) Math.sqrt(Math.pow(centerX1 - centerX2, 2) + Math.pow(centerY1 - centerY2, 2));
	}
}
