import org.teavm.jso.JSBody;

public class REPLReader {
	@JSBody(script = "return document.getElementsByClassName(\"editor\")[0].textContent")
	public static native String readLine();
}
