import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

public class REPLReader {
	public static String readLine() {
		HTMLDocument document = HTMLDocument.current();
		return ((HTMLInputElement) document.getElementById("code")).getValue(); //Window.current().prompt("Code:");
	}
}
