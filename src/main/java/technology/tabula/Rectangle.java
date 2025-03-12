package technology.tabula;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("serial")
public class Rectangle extends AndroidRectangle {

	@Deprecated
	public static final Comparator<Rectangle> ILL_DEFINED_ORDER = new Comparator<Rectangle>() {
		@Override
		public int compare(Rectangle o1, Rectangle o2) {
			if (o1.equals(o2)) return 0;
			if (o1.verticalOverlap(o2) > VERTICAL_COMPARISON_THRESHOLD) {
				return o1.isLtrDominant() == -1 && o2.isLtrDominant() == -1
						? -Float.compare(o1.getX(), o2.getX())
						: Float.compare(o1.getX(), o2.getX());
			} else {
				return Float.compare(o1.getBottom(), o2.getBottom());
			}
		}
	};

	protected static final float VERTICAL_COMPARISON_THRESHOLD = 0.4f;

	public Rectangle() {
		super(0, 0, 0, 0);
	}

	public Rectangle(float top, float left, float width, float height) {
		super(left, top, width, height);
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

	public float verticalOverlapRatio(Rectangle other) {
		float rv = 0;
		float delta = Math.min(this.getBottom() - this.getTop(), other.getBottom() - other.getTop());

		if (other.getTop() <= this.getTop() && this.getTop() <= other.getBottom()
				&& other.getBottom() <= this.getBottom()) {
			rv = (other.getBottom() - this.getTop()) / delta;
		} else if (this.getTop() <= other.getTop() && other.getTop() <= this.getBottom()
				&& this.getBottom() <= other.getBottom()) {
			rv = (this.getBottom() - other.getTop()) / delta;
		} else if (this.getTop() <= other.getTop() && other.getTop() <= other.getBottom()
				&& other.getBottom() <= this.getBottom()) {
			rv = (other.getBottom() - other.getTop()) / delta;
		} else if (other.getTop() <= this.getTop() && this.getTop() <= this.getBottom()
				&& this.getBottom() <= other.getBottom()) {
			rv = (this.getBottom() - this.getTop()) / delta;
		}

		return rv;
	}

	public float overlapRatio(Rectangle other) {
		float intersectionWidth = Math.max(0,
				Math.min(this.getRight(), other.getRight()) - Math.max(this.getLeft(), other.getLeft()));
		float intersectionHeight = Math.max(0,
				Math.min(this.getBottom(), other.getBottom()) - Math.max(this.getTop(), other.getTop()));
		float intersectionArea = Math.max(0, intersectionWidth * intersectionHeight);
		float unionArea = this.getArea() + other.getArea() - intersectionArea;

		return intersectionArea / unionArea;
	}

	public Rectangle merge(Rectangle other) {
		AndroidRectangle union = this.createUnion(other);
		this.setRect(union.x, union.y, union.width, union.height);
		return this;
	}

	public float getTop() {
		return this.y;
	}

	public void setTop(float top) {
		float deltaHeight = top - this.y;
		this.setRect(this.x, top, this.width, this.height - deltaHeight);
	}

	public float getRight() {
		return this.x + this.width;
	}

	public void setRight(float right) {
		this.setRect(this.x, this.y, right - this.x, this.height);
	}

	public float getLeft() {
		return this.x;
	}

	public void setLeft(float left) {
		float deltaWidth = left - this.x;
		this.setRect(left, this.y, this.width - deltaWidth, this.height);
	}

	public float getBottom() {
		return this.y + this.height;
	}

	public void setBottom(float bottom) {
		this.setRect(this.x, this.y, this.width, bottom - this.y);
	}

	public AndroidPoint[] getPoints() {
		return new AndroidPoint[] {
				new AndroidPoint(this.getLeft(), this.getTop()),
				new AndroidPoint(this.getRight(), this.getTop()),
				new AndroidPoint(this.getRight(), this.getBottom()),
				new AndroidPoint(this.getLeft(), this.getBottom())
		};
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "Rectangle[x=%f, y=%f, width=%f, height=%f, bottom=%f, right=%f]",
				this.x, this.y, this.width, this.height, this.getBottom(), this.getRight());
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
}
