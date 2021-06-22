import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import java.math.BigDecimal;
import java.util.ArrayList;

public class XMLToSyntaxTree {
    @JSBody(params = { "xml" }, script = "return new DOMParser().parseFromString(xml, \"text/xml\");")
    private static native JSObject getDocumentFromXml(String xml);

    @JSBody(params = { "xml" }, script = "return xml.firstElementChild;")
    private static native JSObject getFirstChild(JSObject xml);

    @JSBody(params = { "xml" }, script = "return xml.nodeName;")
    private static native String getName(JSObject xml);

    @JSBody(params = { "xml" }, script = "return xml.innerHTML;")
    private static native String getNodeValue(JSObject xml);

    @JSBody(params = { "xml" }, script = "return xml.getAttribute('data');")
    private static native String getData(JSObject xml);

    @JSBody(params = { "xml" }, script = "return xml.nextElementSibling;")
    private static native JSObject nextElement(JSObject xml);

    public ProgramBase xmlToProgram(String xml) {
        JSObject node = getFirstChild(getDocumentFromXml(xml));
        if (!getName(node).equals("program") && !getName(node).equals("pr")) return new SyntaxTree.Programs();
        node = getFirstChild(node);
        return xmlToProgram(node);
    }

    public ProgramBase xmlToProgram(JSObject node) {
        ArrayList<ProgramBase> programs = new ArrayList<>();
        while (node != null) {
            if (getName(node).equals("print") || getName(node).equals("p")) {
                ValueBase separator = null;
                ArrayList<ValueBase> values = new ArrayList<>();
                JSObject node1 = getFirstChild(node);
                if (getName(node1).equals("separator") || getName(node1).equals("s")) {
                    separator = getValueFromNode(getFirstChild(node1));
                }
                node1 = nextElement(node1);
                while (node1 != null) {
                    if (getName(node1).equals("data") || getName(node1).equals("d"))
                        values.add(getValueFromNode(getFirstChild(node1)));
                    node1 = nextElement(node1);
                }
                ValueBase[] valuesArray = new ValueBase[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    valuesArray[i] = values.get(i);
                }
                programs.add(new SyntaxTree.Print(valuesArray).setSeparator(separator));
            } else if (getName(node).equals("executeValue") || getName(node).equals("ev")) {
                ValueBase value = null;
                JSObject node1 = getFirstChild(node);
                if (getName(node1).equals("value") || getName(node1).equals("v")) {
                    value = getValueFromNode(getFirstChild(node1));
                }
                programs.add(new SyntaxTree.ExecuteValue(value));
            }
            node = nextElement(node);
        }
        ProgramBase[] programsArray = new ProgramBase[programs.size()];
        for (int i = 0; i < programs.size(); i++) {
            programsArray[i] = programs.get(i);
        }
        return new SyntaxTree.Programs(programsArray);
    }

    private ValueBase getValueFromNode(JSObject node) {
        switch (getName(node)) {
            case "number":
            case "n":
                return new SyntaxTree.Number(new BigDecimal(getNodeValue(node)));
            case "bool":
            case "b":
                return new SyntaxTree.Boolean(getNodeValue(node).toLowerCase().equals("true"));
            case "text":
            case "t":
                return new SyntaxTree.Text(getData(node));
            case "printFunction":
            case "pf":
                return new SyntaxTree.PrintFunction((SyntaxTree.Print) ((SyntaxTree.Programs) xmlToProgram(getFirstChild(node))).getPrograms()[0]);
        }
        return new SyntaxTree.Null();
    }
}