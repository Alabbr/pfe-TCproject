import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class PdfTest {
    public static void main(String[] args) throws Exception {
        File file = new File("C:\\Users\\sreou\\OneDrive\\Bureau\\pfe-project\\RAG pdf\\Facturation.pdf");
        if(file.exists()) {
            try (PDDocument doc = PDDocument.load(file)) {
                PDFRenderer pdfRenderer = new PDFRenderer(doc);
                BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 150);
                System.out.println("Rendered page 0 with width: " + bim.getWidth());
            }
        } else {
            System.out.println("File not found");
        }
    }
}
