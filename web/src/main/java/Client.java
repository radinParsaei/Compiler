import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.*;

import java.util.ArrayList;

public class Client extends CompilerMain {
    public static void main(String[] args) {
        HTMLDocument document = HTMLDocument.current();
        HTMLElement button = document.getElementById("run");
        button.addEventListener("click", evt -> {
            SyntaxTree.setId(SyntaxTree.getId() + 1);
            Compiler compiler = new Compiler(null, true, null, null, null, null);
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                SyntaxTree.getFunctions().clear();
                SyntaxTree.getVariables().clear();
                SyntaxTree.getClassesParameters().clear();
                SyntaxTree.staticFunctions.clear();
                SyntaxTree.CreateLambda.setCounter(0);
                SyntaxTree.resetNameSpaces();
                document.getElementById("console2").setInnerHTML("");
                if (REPLReader.readLine().trim().split("\n")[0].matches("^\\s*< *\\?*xml.*>.*")) {
                    XMLToSyntaxTree xmlToSyntaxTree = new XMLToSyntaxTree();
                    xmlToSyntaxTree.xmlToProgram(REPLReader.readLine()).eval();
                    return;
                }
                compile(compiler);
            });
            thread.start();
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
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
        Compiler compiler = new Compiler(null, false, null, null, null, null);
        Lexer lexer = new Lexer(compiler);
        compiler.initLexer(lexer);
        lexer.setError(false);
        StringBuilder coloredText = new StringBuilder();
        ArrayList<Token> tokens = lexer.lex(text);
        int i = 0;
        for (Token token : tokens) {
            i++;
            switch (token.getName()) {
                case "TXT":
                    coloredText.append("<font color=\"#7B986A\">").append(token.getText()).append("</font>");
                    break;
                case "NUM":
                    coloredText.append("<font color=\"#6897BB\">").append(token.getText()).append("</font>");
                    break;
                case "IF":
                case "ELSE":
                case "FN":
                case "VAR":
                case "BOOL":
                case "IN":
                case "EXT":
                case "NULL":
                case "CLASS":
                case "IMPORT":
                case "INIT":
                case "THIS":
                case "NEW":
                case "RET":
                case "BR":
                case "STATIC":
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
                case "ID":
                    if (tokens.get(i).getName().equals("OP_PAREN") && token.getText().equals("print")) {
                        coloredText.append("<font color=\"#9f73ae\">").append(token.getText()).append("</font>");
                    } else {
                        coloredText.append(token.getText());
                    }
                    break;
                default:
                    coloredText.append(token.getText());
                    break;
            }
        }
        document.getElementById("editor").setInnerHTML(coloredText.toString());
    }
}