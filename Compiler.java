import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class Compiler extends CompilerBase {
	private int lines;
	private String fileName;
	private boolean isShell;
	private String compiledFileName;
  private String serializeFileName;

	public int getLines() {
		return lines;
	}

	public Compiler(String fileName, boolean isShell, String compiledFileName, String serializeFileName) {
		this.fileName = fileName;
		this.isShell = isShell;
    this.compiledFileName = compiledFileName;
		this.serializeFileName = serializeFileName;
	}

	public String getInputCode() {
		if (isShell) {
			try {
				System.out.print("> ");
				return new Scanner(System.in).nextLine() + "\n";
			} catch (java.util.NoSuchElementException e) {
				System.out.println("Running file [if available] (ctrl+c to quit)");
			}
		}
		File file = new File(fileName);
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.err.println("Can\'t open the file");
			System.exit(1);
		}
		scanner.useDelimiter("\\Z");
		return scanner.nextLine() + "\n";
	}

	public void initLexer(Lexer lexer) {
		//Text
		lexer.add("TXT", "\".*?(\")|\'.*?(\')");
		//Number
		lexer.add("NUM", "\\d+\\.?\\d*");
		//Boolean
		lexer.add("BOOL", "true|false");
		//Null
		lexer.add("NULL", "null");
		//spaces (ignore)
		lexer.add("IGNORE", " +");
    //print
		lexer.add("PRINT", "print ");
		//id
		lexer.add("ID", "([A-Za-z]+\\d*_*)+");
		//operators
		lexer.add("SET", "=");
    //seperator
		lexer.add("SEP", new SeperatorChecker());
	}

	public void afterLex(Parser result) {}

	public void parse(Parser parser) {}

	@ParserEvent(map = "exp : NUM", priority = 0)
	public Object number(Parser parser) {
		return new SyntaxTree.Number(new BigDecimal(parser.getTokens().get(0).getText()));
	}

	@ParserEvent(map = "exp : TXT", priority = 1)
	public Object text(Parser parser) {
    String text = parser.getTokens().get(0).getText();
		return new SyntaxTree.Text(text.substring(1, text.length() - 1));
	}

	@ParserEvent(map = "exp : BOOL", priority = 2)
	public Object booleanExpression(Parser parser) {
		return new SyntaxTree.Boolean(parser.getTokens().get(0).getText().equals("true")? true:false);
	}

	@ParserEvent(map = "exp : NULL", priority = 3)
	public Object nullExpression(Parser parser) {
		return new SyntaxTree.Null();
	}

	@ParserEvent(map = "set : ID SET", priority = 4)
	public Object set(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "exp : ID", priority = 5)
	public Object variable(Parser parser) {
		return new SyntaxTree.Variable(parser.getTokens().get(0).getText());
	}

	@ParserEvent(map = "program : set exp SEP", priority = 6)
	public Object setVariable(Parser parser) {
		return new SyntaxTree.SetVariable((String)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : PRINT exp SEP", priority = 7)
	public Object print(Parser parser) {
		return new SyntaxTree.Print((ValueBase)parser.getTokens().get(1).getObject());
	}

  @ParserEvent(map = "program : program program", priority = 8)
  public Object programs(Parser parser) {
    return new SyntaxTree.Programs((ProgramBase)parser.getTokens().get(0).getObject(), (ProgramBase)parser.getTokens().get(1).getObject());
  }

	public void afterParse(Parser result) {
		if (result.getTokens().size() != 1) {
			syntaxError("Syntax Error");
			return ;
		}
		if (result.getTokens().get(0).getName().equals("program")) {
      if (compiledFileName != null) {
        try {
          FileWriter writer = new FileWriter(compiledFileName);
          VMTools vmTools = new VMTools();
          writer.write(vmTools.SyntaxTreeToVMByteCode((ProgramBase)result.getTokens().get(0).getObject()));
          writer.close();
        } catch (IOException e) {
          System.out.println("ERROR: can\'t create output file");
          e.printStackTrace();
        }
      } else {
				((ProgramBase)result.getTokens().get(0).getObject()).eval();
      }
			if (serializeFileName != null) {
				SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
				serializer.serialize(serializeFileName, (ProgramBase)result.getTokens().get(0).getObject());
			}
		} else if (result.getTokens().get(0).getName().equals("SEP")) {
      // empty line
    } else {
			System.out.println("Syntax is:\n" + result);
			syntaxError("Syntax Error");
		}
	}

	public static void syntaxError(int errorChar, String line) {
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\n') {
				errorChar++;
			}
		}
		System.err.println(line.replace("\n", "\\n"));
		for(; errorChar > 0; errorChar--) {
			System.err.print(" ");
		}
		System.err.println("^");
	}

	public static void syntaxError(String line) {
		System.err.println("ERROR:\t" + line);
	}
}
