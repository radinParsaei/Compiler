import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.w3c.dom.Node;

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

    @JSBody(params = { "xml", "parameter" }, script = "return xml.getAttribute(parameter);")
    private static native String getParameter(JSObject xml, String parameter);

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
            } else if (getName(node).equals("if") || getName(node).equals("i")) {
                ProgramBase elseProgram = null;
                ValueBase condition = getValueFromNode(getFirstChild(getFirstChild(node)));
                ProgramBase program = xmlToProgram(getFirstChild(nextElement(getFirstChild(node))));
                try {
                    elseProgram = xmlToProgram(getFirstChild(nextElement(nextElement(getFirstChild(node)))));
                } catch (Exception ignore) {
                }
                SyntaxTree.If _if = new SyntaxTree.If(condition, program);
                if (elseProgram != null) _if.addElse(elseProgram);
                programs.add(_if);
            } else if (getName(node).equals("while") || getName(node).equals("w")) {
                ValueBase condition = getValueFromNode(getFirstChild(getFirstChild(node)));
                ProgramBase program = xmlToProgram(getFirstChild(nextElement(getFirstChild(node))));
                programs.add(new SyntaxTree.While(condition, program));
            } else if (getName(node).equals("function") || getName(node).equals("fun")) {
                programs.add(new SyntaxTree.Function(getParameter(node, getName(node).equals("fun")? "n":"name"), xmlToProgram(getFirstChild(node)), getParameter(node, getName(node).equals("fun")? "a":"args").split(",")));
            } else if (getName(node).equals("break") || getName(node).equals("br")) {
                programs.add(new SyntaxTree.Break());
            } else if (getName(node).equals("continue") || getName(node).equals("con")) {
                programs.add(new SyntaxTree.Continue());
            } else if (getName(node).equals("set-variable") || getName(node).equals("set")) {
                programs.add(new SyntaxTree.SetVariable(getParameter(node, getName(node).equals("set")? "n":"name"), getValueFromNode(getFirstChild(getFirstChild(node)))));
            } else if (getName(node).equals("repeat") || getName(node).equals("r")) {
                programs.add(new SyntaxTree.Repeat(getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))), xmlToProgram(getFirstChild(getFirstChild(node)))));
            } else if (getName(node).equals("for") || getName(node).equals("f")) {
                ValueBase condition;
                ProgramBase step, init, program;
                init = xmlToProgram(getFirstChild(getFirstChild(node)));
                step = xmlToProgram(getFirstChild(nextElement(getFirstChild(node))));
                program = xmlToProgram(getFirstChild(nextElement(nextElement(getFirstChild(node)))));
                condition = getValueFromNode(getFirstChild(nextElement(nextElement(nextElement(getFirstChild(node))))));
                programs.add(new SyntaxTree.For(condition, step, init, program));
            } else if (getName(node).equals("executeValue") || getName(node).equals("ev")) {
                ValueBase value = null;
                JSObject node1 = getFirstChild(node);
                if (getName(node1).equals("value") || getName(node1).equals("v")) {
                    value = getValueFromNode(getFirstChild(node1));
                }
                programs.add(new SyntaxTree.ExecuteValue(value));
            } else if (getName(node).equals("exit") || getName(node).equals("e")) {
                JSObject node1 = getFirstChild(node);
                programs.add(new SyntaxTree.Exit(getValueFromNode(node1)));
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
                return new SyntaxTree.Boolean(getNodeValue(node).equalsIgnoreCase("true"));
            case "null":
            case "nl":
                return new SyntaxTree.Null();
            case "list":
            case "l": {
                ArrayList<ValueBase> res = new ArrayList<>();
                JSObject node1 = getFirstChild(node);
                while (node1 != null) {
                    if (getName(node1).equals("data") || getName(node1).equals("d"))
                        res.add(getValueFromNode(getFirstChild(node1)));
                    node1 = nextElement(node1);
                }
                ValueBase[] valuesArray = new ValueBase[res.size()];
                for (int i = 0; i < res.size(); i++) {
                    valuesArray[i] = res.get(i);
                }
                return new SyntaxTree.List(valuesArray);
            }
            case "text":
            case "t":
                return new SyntaxTree.Text(getData(node));
            case "printFunction":
            case "pf":
                return new SyntaxTree.PrintFunction((SyntaxTree.Print) ((SyntaxTree.Programs) xmlToProgram(getFirstChild(node))).getPrograms()[0]);
            case "add":
            case "a":
                return new SyntaxTree.Add(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "sub":
            case "s":
                return new SyntaxTree.Sub(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "mul":
            case "m":
                return new SyntaxTree.Mul(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "div":
            case "d":
                return new SyntaxTree.Div(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "mod":
            case "m1":
                return new SyntaxTree.Mod(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "pow":
            case "p1":
                return new SyntaxTree.Pow(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "equals":
            case "eq":
                return new SyntaxTree.Equals(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "strict-equals":
            case "seq":
                return new SyntaxTree.StrictEquals(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "greater-than":
            case "gt":
                return new SyntaxTree.GreaterThan(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "lesser-than":
            case "lt":
                return new SyntaxTree.LesserThan(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "lesser-than-or-equal":
            case "le":
                return new SyntaxTree.LesserThanOrEqual(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "greater-than-or-equal":
            case "ge":
                return new SyntaxTree.GreaterThanOrEqual(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "and":
            case "a1":
                return new SyntaxTree.And(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "or":
            case "o":
                return new SyntaxTree.Or(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "bitwise-and":
            case "ba":
                return new SyntaxTree.BitwiseAnd(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "bitwise-or":
            case "bo":
                return new SyntaxTree.BitwiseOr(getValueFromNode(getFirstChild(getFirstChild(node))), getValueFromNode(getFirstChild(nextElement(getFirstChild(node)))));
            case "not":
            case "n1":
                return new SyntaxTree.Not(getValueFromNode(getFirstChild(node)));
            case "exitFunction":
            case "ef":
                return new SyntaxTree.ExitFunction((SyntaxTree.Exit) ((SyntaxTree.Programs) xmlToProgram(getFirstChild(node))).getPrograms()[0]);
            case "variable":
            case "v":
                return new SyntaxTree.Variable(getNodeValue(node));
            case "increase":
            case "in":
                return new SyntaxTree.Increase((SyntaxTree.Variable) getValueFromNode(getFirstChild(node)), getParameter(node, getName(node).equals("in")? "p":"is-postfix").equals("true"));
            case "decrease":
            case "de":
                return new SyntaxTree.Decrease((SyntaxTree.Variable) getValueFromNode(getFirstChild(node)), getParameter(node, getName(node).equals("de")? "p":"is-postfix").equals("true"));
            case "call-function":
            case "c": {
                ArrayList<ValueBase> values = new ArrayList<>();
                JSObject node_ = getFirstChild(node);
                while (node_ != null) {
                    values.add(getValueFromNode(getFirstChild(node_)));
                    node_ = nextElement(node_);
                }
                ValueBase[] valuesArray = new ValueBase[values.size()];
                valuesArray = values.toArray(valuesArray);
                return new SyntaxTree.CallFunction(getParameter(node, getName(node).equals("c")? "n":"name"), valuesArray);
            }
        }
        return new SyntaxTree.Null();
    }
}