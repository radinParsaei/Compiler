import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Highlighter implements org.jline.reader.Highlighter {
    HashMap<String, String> colorConfigs = null;

    @Override
    public AttributedString highlight(LineReader lineReader, String s) {
        init();
        AttributedStyle attributedStyle = new AttributedStyle();
        AttributedStringBuilder builder = new AttributedStringBuilder();
        Compiler compiler = new Compiler(null, false, null, null, null, null);
        Lexer lexer = new Lexer(compiler);
        compiler.initLexer(lexer);
        lexer.setError(false);
        ArrayList<Token> tokens = lexer.lex(s.replace("\u200B", ""));
        int i = 0;
        for (Token token : tokens) {
            i++;
            if (colorConfigs == null) {
                switch (token.getName()) {
                    case "TXT":
                        builder.styled(attributedStyle.foreground(2), token.getText());
                        break;
                    case "NUM":
                        builder.styled(attributedStyle.foreground(AttributedStyle.BLUE), token.getText());
                        break;
                    case "BOOL":
                    case "NULL":
                        builder.styled(attributedStyle.foreground(215), token.getText());
                        break;
                    case "OP1":
                    case "OP2":
                    case "OP3":
                    case "E":
                    case "COMP":
                    case "SET":
                        builder.styled(attributedStyle.foreground(AttributedStyle.MAGENTA), token.getText());
                        break;
                    case "IF":
                    case "ELSE":
                    case "FN":
                    case "IMPORT":
                    case "VAR":
                    case "RET":
                    case "WH":
                    case "FOR":
                    case "THIS":
                    case "STATIC":
                    case "INIT":
                    case "NEW":
                    case "BR":
                    case "CON":
                        builder.styled(attributedStyle.foreground(140), token.getText());
                        break;
                    case "IGNORE":
                        if (token.getText().startsWith("//") || token.getText().startsWith("/*"))
                            builder.styled(attributedStyle.foreground(242), token.getText());
                        else
                            builder.styled(AttributedStyle.DEFAULT, token.getText());
                        break;
                    default:
                        if ((token.getText().equals("print") || token.getText().equals("write")) &&
                                token.getName().equals("ID") && tokens.size() > i &&
                                tokens.get(i).getName().equals("OP_PAREN")) {
                            builder.styled(attributedStyle.foreground(147), token.getText());
                        } else {
                            builder.styled(AttributedStyle.DEFAULT, token.getText());
                        }
                        break;
                }
            } else {
                boolean add = true;
                for (Map.Entry<String, String> entry : colorConfigs.entrySet()) {
                    if (entry.getKey().equals(token.getName())) {
                        add = false;
                        AttributedStyle attributedStyle1 = new AttributedStyle();
                        int bgIndex = entry.getValue().indexOf("bg=");
                        if (bgIndex != -1) {
                            attributedStyle1 = attributedStyle1.background(Integer.parseInt(entry.getValue().substring(bgIndex + 3, bgIndex + 6)));
                        }
                        int fgIndex = entry.getValue().indexOf("fg=");
                        if (fgIndex != -1) {
                            attributedStyle1 = attributedStyle1.foreground(Integer.parseInt(entry.getValue().substring(fgIndex + 3, fgIndex + 6)));
                        }
                        if (entry.getValue().contains("bold")) {
                            attributedStyle1 = attributedStyle1.bold();
                        }
                        if (entry.getValue().contains("inverse")) {
                            attributedStyle1 = attributedStyle1.inverse();
                        }
                        if (entry.getValue().contains("underline")) {
                            attributedStyle1 = attributedStyle1.underline();
                        }
                        builder.styled(attributedStyle1, token.getText());
                    }
                }
                if (add) {
                    builder.styled(AttributedStyle.DEFAULT, token.getText());
                }
            }
        }
        return builder.toAttributedString();
    }

    @Override
    public void setErrorPattern(Pattern pattern) {

    }

    @Override
    public void setErrorIndex(int i) {

    }

    private void init() {
        if (colorConfigs == null) {
            BufferedReader bufferedReader;
            try {
                bufferedReader = new BufferedReader(new FileReader("./.colors"));
            } catch (FileNotFoundException e) {
                return;
            }
            colorConfigs = new HashMap<>();
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    colorConfigs.put(line.split(":")[0].trim(), line.split(":")[1].trim().toLowerCase());
                    line = bufferedReader.readLine();
                }
            } catch (IOException ignore) {

            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {

                }
            }
        }
    }
}
