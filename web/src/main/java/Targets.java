import org.teavm.jso.dom.html.HTMLDocument;

public class Targets {
    public static final boolean isWeb = true;
    public static final boolean systemPrint = false;

    public static void print(ValueBase value) {
        HTMLDocument document = HTMLDocument.current();
        document.getElementById("console2")
                .setInnerHTML((document.getElementById("console2").getInnerHTML() + value)
                        .replace("\n", "<br>"));
    }
}