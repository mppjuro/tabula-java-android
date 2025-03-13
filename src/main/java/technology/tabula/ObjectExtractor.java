package technology.tabula;

import java.io.IOException;
import java.util.List;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;

public class ObjectExtractor implements java.io.Closeable {

    private final PDDocument pdfDocument;

    public ObjectExtractor(PDDocument pdfDocument) {
        this.pdfDocument = pdfDocument;
    }

    protected Page extractPage(Integer pageNumber) throws IOException {
        if (pageNumber > pdfDocument.getNumberOfPages() || pageNumber < 1) {
            throw new java.lang.IndexOutOfBoundsException("Page number does not exist.");
        }

        PDPage page = pdfDocument.getPage(pageNumber - 1);

        // Utwórz silnik do ekstrakcji obiektów graficznych
        ObjectExtractorStreamEngine streamEngine = new ObjectExtractorStreamEngine(page);
        streamEngine.extractRulings(); // Wywołanie ekstrakcji linii

        // Uruchom ekstraktor tekstu
        TextStripper textStripper = new TextStripper(pdfDocument, pageNumber);
        textStripper.process();

        // Sortowanie tekstu
        List<TextElement> textElements = textStripper.getTextElements();
        Utils.sort(textElements, Rectangle.ILL_DEFINED_ORDER);

        // Ustal wymiary strony i rotację
        float width, height;
        int rotation = page.getRotation();
        if (Math.abs(rotation) == 90 || Math.abs(rotation) == 270) {
            width = page.getCropBox().getHeight();
            height = page.getCropBox().getWidth();
        } else {
            width = page.getCropBox().getWidth();
            height = page.getCropBox().getHeight();
        }

        // ✅ Poprawione tworzenie obiektu `Page` z właściwymi argumentami
        Rectangle pageRectangle = new Rectangle(0, 0, width, height);
        return new Page(
                pageRectangle,
                rotation,
                pageNumber,
                page,
                pdfDocument,
                textElements,
                streamEngine.getRulings(),
                textStripper.getMinCharWidth(),
                textStripper.getMinCharHeight(),
                textStripper.getSpatialIndex()
        );
    }

    public PageIterator extract(Iterable<Integer> pages) {
        return new PageIterator(this, pages);
    }

    public PageIterator extract() {
        return extract(Utils.range(1, pdfDocument.getNumberOfPages() + 1));
    }

    public Page extract(int pageNumber) {
        return extract(Utils.range(pageNumber, pageNumber + 1)).next();
    }

    public void close() throws IOException {
        pdfDocument.close();
    }
}
