//import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.ArrayList;

public class Targets {
    public static final boolean isWeb = true;
    public static final boolean isInThread = true;
    public static final boolean systemPrint = false;
    public static final boolean useAnalyzer = false;
    public static int count = 0;

    public static void print(ValueBase value) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement htmlParagraphElement = document.createElement("span");
        htmlParagraphElement.setInnerHTML(value.toString().replace("\n", "<br>"));
        document.getElementById("console2").appendChild(htmlParagraphElement);
    }

    public static final boolean customWhile = false;

    public static void error(String error) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement htmlParagraphElement = document.createElement("div");
        htmlParagraphElement.setInnerHTML("<img src=\"error.png\" height=\"20\"/><span style=\"position: absolute\">&nbsp;" + error.replace("\n", "<br>").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;") + "</span>");
        htmlParagraphElement.setAttribute("style", "color: #f55; position: relative");
        document.getElementById("console2").appendChild(htmlParagraphElement);
    }

    public interface CustomWhileInterface {
        boolean run();
    }

    public static void _while(CustomWhileInterface customWhileInterface) { }
}