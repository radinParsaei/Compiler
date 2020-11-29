import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public class Targets {
    public static final boolean isWeb = true;
    public static final boolean systemPrint = false;

    public static void print(ValueBase value) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement htmlParagraphElement = document.createElement("span");
        htmlParagraphElement.setInnerHTML(value.toString().replace("\n", "<br>"));
        document.getElementById("console2").appendChild(htmlParagraphElement);
    }
}