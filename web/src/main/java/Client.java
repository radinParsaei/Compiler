import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.*;

public class Client extends CompilerMain {
    public static void main(String[] args) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement button = document.getElementById("run");
        button.addEventListener("click", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                SyntaxTree.getFunctions().clear();
                SyntaxTree.getVariables().clear();
                SyntaxTree.getClassesParameters().clear();
                SyntaxTree.CreateLambda.setCounter(0);
                SyntaxTree.resetNameSpaces();
                for (int i : Targets.getIntervalCodes()) {
                    Window.clearInterval(i);
                }
                document.getElementById("console2").setInnerHTML("");
                Compiler compiler = new Compiler(null, true, null, null, null);
                compile(compiler);
            }
        });

        HTMLElement buttonTmp = document.getElementById("callColor");
        buttonTmp.addEventListener("click", new EventListener<Event>() {
            @Override
            public void handleEvent(Event evt) {
                color();
            }
        });
        color();
    }

    public static void color() {
        HTMLDocument document = HTMLDocument.current();
        String text = REPLReader.readLine();
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        Compiler compiler = new Compiler(null, false, null, null, null);
        Lexer lexer = new Lexer(compiler);
        compiler.initLexer(lexer);
        lexer.setError(false);
        StringBuilder coloredText = new StringBuilder();
        for (Token token : lexer.lex(text)) {
            switch (token.getName()) {
                case "TXT":
                    coloredText.append("<font color=\"#7B986A\">").append(token.getText()).append("</font>");
                    break;
                case "NUM":
                    coloredText.append("<font color=\"#6897BB\">").append(token.getText()).append("</font>");
                    break;
                case "PRINT":
                case "IF":
                case "ELSE":
                case "FN":
                case "VAR":
                case "BOOL":
                case "NULL":
                case "CLASS":
                case "INIT":
                case "RET":
                case "BR":
                case "CON":
                case "WH":
                case "FOR":
                    coloredText.append("<font color=\"#dc8842\">").append(token.getText()).append("</font>");
                    break;
                case "IGNORE":
                    if (token.getText().trim().startsWith("//") || token.getText().trim().startsWith("/*"))
                        coloredText.append("<font color=\"#808080\">").append(token.getText()).append("</font>");
                    else
                        coloredText.append(token.getText());
                    break;
                default:
                    coloredText.append(token.getText());
                    break;
            }
        }
        document.getElementById("editor").setInnerHTML(coloredText.toString());
    }
}