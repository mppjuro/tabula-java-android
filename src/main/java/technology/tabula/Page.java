package technology.tabula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class Page extends Rectangle {

    private int number;
    private int rotation;
    private float minCharWidth;
    private float minCharHeight;
    private List<TextElement> textElements;
    private List<Ruling> rulings, cleanRulings = null;
    private PDPage pdPage;
    private PDDocument pdDoc;
    private RectangleSpatialIndex<TextElement> spatialIndex;

    private static final float DEFAULT_MIN_CHAR_LENGTH = 7;

    public Page(Rectangle area, int rotation, int number, PDPage pdPage, PDDocument doc,
                List<TextElement> characters, List<Ruling> rulings,
                float minCharWidth, float minCharHeight, RectangleSpatialIndex<TextElement> index) {
        super(area.getTop(), area.getLeft(), (float) area.getWidth(), (float) area.getHeight());
        this.rotation = rotation;
        this.number = number;
        this.pdPage = pdPage;
        this.pdDoc = doc;
        this.textElements = characters;
        this.rulings = rulings;
        this.minCharWidth = minCharWidth;
        this.minCharHeight = minCharHeight;
        this.spatialIndex = index;
    }

    public Page getArea(Rectangle area) {
        List<TextElement> areaTextElements = getText(area);
        float minimumCharWidth = getMinimumCharWidthFrom(areaTextElements);
        float minimumCharHeight = getMinimumCharHeightFrom(areaTextElements);

        Page page = new Page(area, rotation, number, pdPage, pdDoc,
                areaTextElements, Ruling.cropRulingsToArea(getRulings(), area),
                minimumCharWidth, minimumCharHeight, spatialIndex);

        addBorderRulingsTo(page);
        return page;
    }

    private float getMinimumCharWidthFrom(List<TextElement> areaTextElements) {
        return areaTextElements.isEmpty() ? DEFAULT_MIN_CHAR_LENGTH :
                Collections.min(areaTextElements, Comparator.comparingDouble(te -> te.width)).width;
    }

    private float getMinimumCharHeightFrom(List<TextElement> areaTextElements) {
        return areaTextElements.isEmpty() ? DEFAULT_MIN_CHAR_LENGTH :
                Collections.min(areaTextElements, Comparator.comparingDouble(te -> te.height)).height;
    }

    private void addBorderRulingsTo(Page page) {
        Point2D leftTop = new Point2D(page.getLeft(), page.getTop());
        Point2D rightTop = new Point2D(page.getRight(), page.getTop());
        Point2D rightBottom = new Point2D(page.getRight(), page.getBottom());
        Point2D leftBottom = new Point2D(page.getLeft(), page.getBottom());

        page.addRuling(new Ruling(leftTop, rightTop));
        page.addRuling(new Ruling(rightTop, rightBottom));
        page.addRuling(new Ruling(rightBottom, leftBottom));
        page.addRuling(new Ruling(leftBottom, leftTop));
    }

    public List<TextElement> getText(Rectangle area) {
        return spatialIndex.contains(area);
    }

    public List<Ruling> getRulings() {
        if (cleanRulings != null) return cleanRulings;
        if (rulings == null || rulings.isEmpty()) return new ArrayList<>();

        cleanRulings = new ArrayList<>(rulings);
        return cleanRulings;
    }

    public void addRuling(Ruling ruling) {
        if (ruling.getStartPoint().equals(ruling.getEndPoint())) {
            throw new UnsupportedOperationException("Can't add a point ruling.");
        }
        rulings.add(ruling);
        cleanRulings = null;
    }

    public PDPage getPDPage() {
        return pdPage;
    }

    public PDDocument getPDDoc() {
        return pdDoc;
    }

    public Rectangle getTextBounds() {
        if (textElements.isEmpty()) return new Rectangle();
        return Utils.bounds(textElements);
    }

    public List<Ruling> getVerticalRulings() {
        List<Ruling> verticalLines = new ArrayList<>();
        for (Ruling r : rulings) {
            if (r.vertical()) {
                verticalLines.add(r);
            }
        }
        return verticalLines;
    }

    public List<Ruling> getHorizontalRulings() {
        List<Ruling> horizontalLines = new ArrayList<>();
        for (Ruling r : rulings) {
            if (r.horizontal()) {
                horizontalLines.add(r);
            }
        }
        return horizontalLines;
    }

    public int getPageNumber() {
        return number;
    }

    public List<TextElement> getText() {
        return textElements;
    }
}
