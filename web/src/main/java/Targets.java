import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.ArrayList;

public class Targets {
    public static final boolean isWeb = true;
    public static final boolean systemPrint = false;
    public static final boolean isInThread = false;
    private static final ArrayList<Integer> intervalCodes = new ArrayList<>();
    public static int count = 0;

    public static ArrayList<Integer> getIntervalCodes() {
        return intervalCodes;
    }

    public static void print(ValueBase value) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement htmlParagraphElement = document.createElement("span");
        htmlParagraphElement.setInnerHTML(value.toString().replace("\n", "<br>"));
        document.getElementById("console2").appendChild(htmlParagraphElement);
    }

    public static final boolean customWhile = true;
    public interface CustomWhileInterface {
        boolean run();
    }

    public static void _while(CustomWhileInterface customWhileInterface) {
        final int intervalCount = count++;
        intervalCodes.add(Window.setInterval(() -> {
            if (!customWhileInterface.run()) {
                Window.clearInterval(intervalCodes.get(intervalCount));
            }
        }, 1));
    }
}