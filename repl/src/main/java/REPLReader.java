import org.jline.builtins.Completers;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class REPLReader {
    private static String[] shellvars = null;
    private static Terminal terminal = null;
    private static String previousLine = null;
    private static int openBracketCounter = 0;
    public static String colorParse(String input) {
        for (int i = 256; i >= 0; i--) {
            input = input.replace("@" + i, "\u001b[38;5;" + i + "m");
            input = input.replace("#" + i, "\u001b[38;5;" + i + "m");
        }
        return input;
    }

    public static String variablesParse(String input) {
        for (Map.Entry<String, String> variable : System.getenv().entrySet()) {
            input = input.replace("$" + variable.getKey(), variable.getValue());
        }
        return input;
    }

    public static String ltrim(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    public static void init() {
        if (shellvars == null) {
            terminal = null;
            try {
                terminal = TerminalBuilder.terminal();
            } catch (IOException ignore) {}
            shellvars = new String[System.getenv().size() + 1];
            int varCounter = 0;
            for (Map.Entry<String, String> shellvar : System.getenv().entrySet()) {
                shellvars[varCounter++] = "$" + shellvar.getKey();
            }
            shellvars[varCounter] = "$DATE";
        }
    }

    public static String readLine() {
        init();
        boolean hasIf = false;
        Map<String, Completer> complesion = new HashMap<>();
        String line = "";
        String prompt1 = "> ";
        String prompt2 = "";
        do {
            complesion.put("SC", new StringsCompleter("/hist", "/help", "/colorhelp", "/prompt1", "/prompt2", "/run"));
            complesion.put("SC1", new StringsCompleter("/prompt1", "/prompt2"));
            complesion.put("SC1P", new StringsCompleter(shellvars));
            complesion.put("C1", new StringsCompleter("print"));
            complesion.put("ST", new StringsCompleter("if"));
            complesion.put("OPBR", new StringsCompleter("{"));
            complesion.put("CLBR", new StringsCompleter("}"));
            complesion.put("CLBRP", new StringsCompleter("else"));
            complesion.put("CLBRP2", new StringsCompleter("else if"));
            String[] variables = new String[SyntaxTree.getVariables().size() + 3];
            int counter = 0;
            for (String variable : SyntaxTree.getVariables().keySet()) {
                variables[counter++] = variable;
            }
            variables[counter++] = "null";
            variables[counter++] = "true";
            variables[counter] = "false";
            complesion.put("vars", new StringsCompleter(variables));
            DefaultParser parser = new DefaultParser();
            char[] nothing = new char[1];
            nothing[0] = '\u200B'; //It must to set and set to a hidden value (if anyone has a better idea help me at https://stackoverflow.com/questions/63757353/jline-problems-with-escape-character-in-completion)
            parser.setEscapeChars(nothing);
            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new Completers.RegexCompleter("SC|SC1 SC1P|C1 vars|ST vars OPBR" + (openBracketCounter > 0? "|CLBR" + (hasIf? " (CLBRP OPBR|CLBRP2 vars OPBR)":""):""), complesion::get))
                    .variable(LineReader.HISTORY_FILE, Paths.get("./.history"))
                    .appName("Compiler")
                    .parser(parser)
                    .highlighter(new Highlighter())
                    .build();
            try {
                line = lineReader.readLine((openBracketCounter > 0? "... ":prompt1).replace("$DATE", new Date().toString()), prompt2.replace("$DATE", new Date().toString()), (Character) null, "").replace("else\u200B if", "else if");
            } catch (EndOfFileException e) {
                throw new NoSuchElementException();
            } catch (UserInterruptException e) {
                openBracketCounter = 0;
                previousLine = null;
                line = "";
            }
            if (line.startsWith("/")) {
                if (line.startsWith("/p1") || line.startsWith("/prompt1")) {
                    line = ltrim(line.replace("/p1", "").replace("/prompt1", ""));
                    prompt1 = variablesParse(colorParse(line));
                } else if (line.startsWith("/p2") || line.startsWith("/prompt2")) {
                    line = ltrim(line.replace("/p2", "").replace("/prompt2", ""));
                    prompt2 = variablesParse(colorParse(line));
                } else if (line.startsWith("/colorhelp")) {
                    StringBuilder stirng = new StringBuilder();
                    for (byte i = 0; i < 16; i++) {
                        for (byte j = 0; j < 16; j++) {
                            stirng.append(colorParse("@" + (j + i * 16) + (j + i * 16) + "\t"));
                        }
                    }
                    System.out.println(stirng);
                } else if (line.startsWith("/hist")) {
                    System.out.println(lineReader.getHistory());
                } else if (line.startsWith("/help")) {
                    System.out.println("/hist -> prints history");
                    System.out.println("/colorhelp -> create a color/number table");
                    System.out.println("/prompt1 | /p1 -> set prompt 1 (left side) (@ for setting foreground color number and # for background color number)");
                    System.out.println("/prompt2 | /p2 -> set prompt 1 (right side)");
                    System.out.println("/help -> prints this help");
                } else if (line.startsWith("/run")) {
                    return line;
                }
                return "";
            }
            Compiler compiler = new Compiler(null, false, null, null);
            Lexer lexer = new Lexer(compiler);
            compiler.initLexer(lexer);
            lexer.setError(false);
            String tmpline = line;
            if (openBracketCounter > 0) {
                if (previousLine == null) {
                    previousLine = "";
                }
                line = previousLine + "\n" + line + "\n";
            }
            for (Token token : lexer.lex(tmpline)) {
                if (token.getName().equals("OP_BRACKET")) {
                    openBracketCounter++;
                } else if (token.getName().equals("CL_BRACKET")) {
                    openBracketCounter--;
                }
                if (token.getName().equals("IF")) {
                    hasIf = true;
                }
            }
            if (openBracketCounter > 0) {
                previousLine = line;
            }
        } while (openBracketCounter > 0);
        return line;
    }
}
