package technology.tabula;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class ObjectExtractorStreamEngine {

    protected List<Ruling> rulings;
    private float[] pageTransform;
    private boolean extractRulingLines = true;
    private Logger logger;
    private List<Point2D> currentPath = new ArrayList<>();
    private PDPage page;

    private static final float RULING_MINIMUM_LENGTH = 0.01f;

    protected ObjectExtractorStreamEngine(PDPage page) {
        this.page = page;
        logger = LoggerFactory.getLogger(ObjectExtractorStreamEngine.class);
        rulings = new ArrayList<>();

        // Oblicz transformację strony:
        PDRectangle pageCropBox = page.getCropBox();
        int rotationAngleInDegrees = page.getRotation();

        pageTransform = new float[]{1, 0, 0, 1, 0, 0};

        if (Math.abs(rotationAngleInDegrees) == 90 || Math.abs(rotationAngleInDegrees) == 270) {
            float rotationAngleInRadians = (float) (rotationAngleInDegrees * (Math.PI / 180.0));
            applyRotationTransform(rotationAngleInRadians);
        } else {
            float deltaX = 0;
            float deltaY = pageCropBox.getHeight();
            applyTranslationTransform(deltaX, deltaY);
        }

        applyScaleTransform(1, -1);
        applyTranslationTransform(-pageCropBox.getLowerLeftX(), -pageCropBox.getLowerLeftY());
    }

    private void applyTranslationTransform(float dx, float dy) {
        pageTransform[4] += dx;
        pageTransform[5] += dy;
    }

    private void applyScaleTransform(float sx, float sy) {
        pageTransform[0] *= sx;
        pageTransform[3] *= sy;
    }

    private void applyRotationTransform(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        pageTransform[0] = cos;
        pageTransform[1] = -sin;
        pageTransform[2] = sin;
        pageTransform[3] = cos;
    }

    public void appendRectangle(float x, float y, float width, float height) {
        moveTo(x, y);
        lineTo(x + width, y);
        lineTo(x + width, y + height);
        lineTo(x, y + height);
        closePath();
    }

    public void clip(int windingRule) {
        // Ignorowane dla uproszczenia
    }

    public void closePath() {
        if (!currentPath.isEmpty()) {
            currentPath.add(currentPath.get(0)); // Zamknięcie ścieżki
        }
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        // Ignorowane: niepotrzebne do ekstrakcji linii
    }

    public void drawImage(PDImage arg0) {}

    public void endPath() {
        currentPath.clear();
    }

    public void fillAndStrokePath(int arg0) {
        strokeOrFillPath(true);
    }

    public void fillPath(int arg0) {
        strokeOrFillPath(true);
    }

    public Point2D getCurrentPoint() {
        return currentPath.isEmpty() ? null : currentPath.get(currentPath.size() - 1);
    }

    public void lineTo(float x, float y) {
        currentPath.add(new Point2D(x, y));
    }

    public void moveTo(float x, float y) {
        currentPath.clear();
        currentPath.add(new Point2D(x, y));
    }

    public void shadingFill(COSName arg0) {}

    public void strokePath() {
        strokeOrFillPath(false);
    }

    private void strokeOrFillPath(boolean isFill) {
        if (!extractRulingLines || currentPath.size() < 2) {
            currentPath.clear();
            return;
        }

        for (int i = 1; i < currentPath.size(); i++) {
            Point2D start = currentPath.get(i - 1);
            Point2D end = currentPath.get(i);
            Ruling ruling = new Ruling(start, end);
            if (ruling.length() > RULING_MINIMUM_LENGTH) {
                rulings.add(ruling);
            }
        }

        currentPath.clear();
    }

    public float[] getPageTransform() {
        return pageTransform;
    }

    public Rectangle2D currentClippingPath() {
        return new Rectangle2D(0, 0, page.getCropBox().getWidth(), page.getCropBox().getHeight());
    }

    public PDPage getPage() {
        return page;
    }

    // 🔹 NOWE METODY 🔹

    /**
     * Ekstrakcja linii z PDF
     */
    public void extractRulings() {
        rulings.clear(); // Wyczyść poprzednie linie
        strokeOrFillPath(false); // Wywołaj ekstrakcję
    }

    /**
     * Pobranie wykrytych linii
     */
    public List<Ruling> getRulings() {
        return rulings;
    }

    class PointComparator implements Comparator<Point2D> {
        @Override
        public int compare(Point2D p1, Point2D p2) {
            float p1X = Utils.round(p1.getX(), 2);
            float p1Y = Utils.round(p1.getY(), 2);
            float p2X = Utils.round(p2.getX(), 2);
            float p2Y = Utils.round(p2.getY(), 2);

            if (p1Y > p2Y)
                return 1;
            if (p1Y < p2Y)
                return -1;
            if (p1X > p2X)
                return 1;
            if (p1X < p2X)
                return -1;
            return 0;
        }
    }
}
