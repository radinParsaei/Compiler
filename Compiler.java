import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class Compiler extends CompilerBase {
	private final String fileName;
	private final boolean isShell;
	private final String compiledFileName;
	private final String serializeFileName;

	public Compiler(String fileName, boolean isShell, String compiledFileName, String serializeFileName) {
		this.fileName = fileName;
		this.isShell = isShell;
		this.compiledFileName = compiledFileName;
		this.serializeFileName = serializeFileName;
	}

	public String getInputCode() {
		if (isShell) {
			try {
				String line = REPLReader.readLine();
				if (line.startsWith("/run")) {
					if (fileName == null || fileName.equals("")) {
						System.out.println("the filename is not set");
						return "";
					}
					System.out.println("Running " + fileName);
					System.out.println();
					File file = new File(fileName);
					Scanner scanner = null;
					try {
						scanner = new Scanner(file);
					} catch (FileNotFoundException e) {
						System.err.println("Can't open the file");
						return "";
					}
					scanner.useDelimiter("\\Z");
					return scanner.next() + "\n";
				}
				return line + "\n";
			} catch (java.util.NoSuchElementException e) {
				System.exit(0);
			}
		}
		File file = new File(fileName);
		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.err.println("Can't open the file");
			System.exit(1);
		}
		scanner.useDelimiter("\\Z");
		return scanner.next() + "\n";
	}

	public void initLexer(Lexer lexer) {
		//Text
		lexer.add("TXT", "\".*?(\")|'.*?(')");
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
		//operators
		lexer.add("COMP", "==|<|>");
		lexer.add("SET", "=");
		lexer.add("OP1", "\\*|\\/|%");
		lexer.add("OP2", "\\-|\\+");
		//if (keyword)
		lexer.add("IF", "if ");
		//else (keyword)
		lexer.add("ELSE", "else");
		//brackets
		lexer.add("OP_BRACKET", "\\{");
		lexer.add("CL_BRACKET", "\\}");
		//id
		lexer.add("ID", "([A-Za-z]*\\d*_*)+");
		//separator
		lexer.add("SEP", new SeperatorChecker());
	}

	public void afterLex(Parser result) {}

	public void parse(Parser parser) {
		if (getCounter() == 0) {
			parser.on("SEP SEP", "SEP", (parser1) -> null);
		} else if (getCounter() == 5) {
			for (int i = 0; i < parser.getTokens().size(); i++) {
				if (parser.getTokens().get(i).getName().equals("OP2")
						&& !parser.getTokens().get(i - 1).getName().equals("exp")) {
					ValueBase value = (ValueBase) parser.getTokens().get(i + 1).getObject();
					if (parser.getTokens().get(i).getText().equals("-")) {
						value = new SyntaxTree.Negative(value);
					}
					parser.getTokens().remove(i);
					parser.getTokens().remove(i);
					Token token = new Token("exp", null);
					token.setObject(value);
					parser.getTokens().add(i, token);
				}
			}
		}
	}

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
		return new SyntaxTree.Boolean(parser.getTokens().get(0).getText().equals("true"));
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

	@ParserEvent(map = "exp : exp OP1 exp", priority = 6)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP2 exp", priority = 7)
	public Object additionAndSubtraction(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp COMP exp", priority = 8)
	public Object comparison(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("==")) {
			return new SyntaxTree.Equals((ValueBase)parser.getTokens().get(0).getObject(),
					(ValueBase)parser.getTokens().get(2).getObject());
		} else if (parser.getTokens().get(1).getText().equals(">")) {
			return new SyntaxTree.GreaterThan((ValueBase)parser.getTokens().get(0).getObject(),
					(ValueBase)parser.getTokens().get(2).getObject());
		} else {
			return new SyntaxTree.LesserThan((ValueBase)parser.getTokens().get(0).getObject(),
					(ValueBase)parser.getTokens().get(2).getObject());
		}
	}

	@ParserEvent(map = "program : set exp SEP", priority = 9)
	public Object setVariable(Parser parser) {
		return new SyntaxTree.SetVariable((String)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : PRINT exp SEP", priority = 10)
	public Object print(Parser parser) {
		return new SyntaxTree.Print((ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : program program", priority = 11)
	public Object programs(Parser parser) {
		return new SyntaxTree.Programs((ProgramBase)parser.getTokens().get(0).getObject(), (ProgramBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "ifprogram : IF exp OP_BRACKET program CL_BRACKET|IF exp SEP OP_BRACKET program CL_BRACKET|IF exp SEP OP_BRACKET SEP program CL_BRACKET|IF exp OP_BRACKET SEP program CL_BRACKET", priority = 12)
	public Object _if(Parser parser) {
		setCounter(11);
		parser.remove("SEP");
		return new SyntaxTree.If((ValueBase)parser.getTokens().get(1).getObject(), (ProgramBase)parser.getTokens().get(3).getObject());
	}

	@ParserEvent(map = "ifprogram : ifprogram ELSE ifprogram|ifprogram SEP ELSE ifprogram", priority = 13)
	public Object elseif(Parser parser) {
		setCounter(11);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(2).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram ELSE OP_BRACKET program CL_BRACKET SEP|ifprogram SEP ELSE OP_BRACKET program CL_BRACKET SEP|ifprogram ELSE SEP OP_BRACKET program CL_BRACKET SEP|ifprogram SEP ELSE OP_BRACKET SEP program CL_BRACKET SEP|ifprogram SEP ELSE SEP OP_BRACKET SEP program CL_BRACKET SEP|ifprogram ELSE OP_BRACKET SEP program CL_BRACKET SEP", priority = 14)
	public Object _else(Parser parser) {
		setCounter(11);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(3).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram SEP", priority = 15)
	public Object ifToProgram(Parser parser) {
		setCounter(11);
		return parser.getTokens().get(0).getObject();
	}

	public void afterParse(Parser result) {
		result.remove("SEP");
		if (result.getTokens().size() == 0) {
			return ;
		}
		if (result.getTokens().size() != 1) {
			System.out.println("Syntax is:\n" + result);
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
					System.out.println("ERROR: can't create output file");
					e.printStackTrace();
				}
			} else {
				((ProgramBase)result.getTokens().get(0).getObject()).eval();
			}
			if (serializeFileName != null) {
				SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
				serializer.serialize(serializeFileName, (ProgramBase)result.getTokens().get(0).getObject());
			}
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
