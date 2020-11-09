import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

public class Client extends CompilerMain {
    public static void main(String[] args) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement button = document.createElement("button");
        button.appendChild(document.createTextNode("Run!"));
        button.addEventListener("click", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                SyntaxTree.getFunctions().clear();
                SyntaxTree.getVariables().clear();
                SyntaxTree.getClassesParameters().clear();
                Compiler compiler = new Compiler(null, true, null, null, null);
                compile(compiler);
            }
        });
        document.getBody().appendChild(button);
    }
}
