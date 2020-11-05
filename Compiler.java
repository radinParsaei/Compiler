import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.math.BigDecimal;

public class Compiler extends CompilerBase {
	private final String fileName;
	private boolean isShell;
	private boolean isCodeRunning = false;
	private final String compiledFileName;
	private final String serializeFileName;
	private final String classFileName;

	public Compiler(String fileName, boolean isShell, String compiledFileName, String classFileName, String serializeFileName) {
		this.fileName = fileName;
		this.isShell = isShell;
		this.compiledFileName = compiledFileName;
		this.serializeFileName = serializeFileName;
		this.classFileName = classFileName;
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
					isShell = false;
					isCodeRunning = true;
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
		lexer.add("TXT", new TextChecker());
		//Number
		lexer.add("NUM", "\\d+\\.?\\d*");
		//Boolean
		lexer.add("BOOL", "true|false");
		//Null
		lexer.add("NULL", "null");
		//spaces (ignore)
		lexer.add("IGNORE", "(\t| |\\/\\/.*|\\/\\*.*\\*\\/)+");
		//print
		lexer.add("PRINT", "print ");
		//operators
		lexer.add("COMP", "!==|===|!=|<=|>=|==|<|>");
		lexer.add("SET", "=");
		lexer.add("OP1", "\\*|\\/|%");
		lexer.add("OP2", "\\-|\\+");
		lexer.add("OP3", "\\|\\||\\||and|&&|&|or");
		//while (keyword)
		lexer.add("WH", "while ");
		//if (keyword)
		lexer.add("IF", "if ");
		//else (keyword)
		lexer.add("ELSE", "else");
		//func (keyword)
		lexer.add("FN", "func ");
		//var (keyword)
		lexer.add("VAR", "var ");
		//return (keyword)
		lexer.add("RET", "return");
		//brackets
		lexer.add("OP_BRACKET", "\\{");
		lexer.add("CL_BRACKET", "\\}");
		//parentheses
		lexer.add("OP_PAREN", "\\(");
		lexer.add("CL_PAREN", "\\)");
		lexer.add("COMMA", ",");
		//id
		lexer.add("ID", "([A-Za-z]*\\d*_*)+");
		//separator
		lexer.add("SEP", new SeperatorChecker());
	}

	public void afterLex(Parser result) {}

	public void parse(Parser parser) {
		if (getCounter() == 0) {
			parser.on("SEP SEP", "SEP", (parser1) -> null);
		} else if (getCounter() == 9) {
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
		} else if (getCounter() == 6 || getCounter() == 16) {
			parser.setSingleRunPerLocation(false);
		} else if (getCounter() == 7 || getCounter() == 17) {
			parser.setSingleRunPerLocation(true);
		}
	}

	@ParserEvent(map = "exp : NUM", priority = 0)
	public Object number(Parser parser) {
		return new SyntaxTree.Number(new BigDecimal(parser.getTokens().get(0).getText()));
	}

	@ParserEvent(map = "exp : TXT", priority = 1)
	public Object text(Parser parser) {
		String text = parser.getTokens().get(0).getText();
		String string = text.substring(1, text.length() - 1)
				.replace("\\n", "\n")
				.replace("\\\n", "\\n")
				.replace("\\t", "\t")
				.replace("\\\t", "\\t")
				.replace("\\r", "\r")
				.replace("\\\r", "\\r")
				.replace("\\b", "\b")
				.replace("\\\b", "\\b")
				.replace("\\f", "\f")
				.replace("\\\f", "\\f")
				.replace("\\'", "'")
				.replace("\\\"", "\"")
				.replace("\\\\", "\\");
		if (string.contains("\\u")) {
			for (int i = 0xFFFF; i >= 0; i--) {
				StringBuilder tmp = new StringBuilder("\\u");
				tmp.append(Integer.toHexString(i));
				for (byte j = (byte) (4 - (tmp.length() - 2)); j > 0; j--) {
					tmp.insert(2, "0");
				}
				string = string.replace(tmp, Character.toString((char) i));
			}
		}
		return new SyntaxTree.Text(string);
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

	@ParserEvent(map = "vard : VAR ID", priority = 5)
	public Object varDeclaration(Parser parser) {
		return parser.getTokens().get(1).getText();
	}

	@ParserEvent(map = "fn : FN ID OP_PAREN( vard| ID)?", priority = 6)
	public Object function(Parser parser) {
		ArrayList<String> stringArrayList = new ArrayList<>();
		stringArrayList.add(parser.getTokens().get(1).getText());
		if (parser.getTokens().size() == 4 && parser.getTokens().get(3).getName().equals("vard")) {
			stringArrayList.add((String) parser.getTokens().get(3).getObject());
		} else if (parser.getTokens().size() == 4) {
			stringArrayList.add(parser.getTokens().get(3).getText());
		}
		return stringArrayList;
	}

	@ParserEvent(map = "fn : fn COMMA (ID|vard)", priority = 7)
	public Object function2(Parser parser) {
		ArrayList<String> stringArrayList = (ArrayList<String>) parser.getTokens().get(0).getObject();
		if (parser.getTokens().get(2).getName().equals("ID")) stringArrayList.add(parser.getTokens().get(2).getText());
		else stringArrayList.add((String) parser.getTokens().get(2).getObject());
		return stringArrayList;
	}

	@ParserEvent(map = "fnc : ID OP_PAREN", priority = 8)
	public Object functionCall(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "exp : ID", priority = 9)
	public Object variable(Parser parser) {
		return new SyntaxTree.Variable(parser.getTokens().get(0).getText());
	}

	@ParserEvent(map = "exp : exp OP1 exp", priority = 10)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP2 exp", priority = 11)
	public Object additionAndSubtraction(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp COMP exp", priority = 12)
	public Object comparison(Parser parser) {
		switch (parser.getTokens().get(1).getText()) {
			case "==":
				return new SyntaxTree.Equals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case ">":
				return new SyntaxTree.GreaterThan((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case "<":
				return new SyntaxTree.LesserThan((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case "<=":
				return new SyntaxTree.LesserThanOrEqual((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case "!=":
				return new SyntaxTree.Not(new SyntaxTree.Equals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject()));
			case "===":
				return new SyntaxTree.StrictEquals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case "!==":
				return new SyntaxTree.Not(new SyntaxTree.StrictEquals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject()));
//			case ">=":
			default:
				return new SyntaxTree.GreaterThanOrEqual((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
		}
	}

	@ParserEvent(map = "exp : exp OP3 exp", priority = 13)
	public Object bitwiseAnd(Parser parser) {
		switch (parser.getTokens().get(1).getText()) {
			case "&":
				return new SyntaxTree.BitwiseAnd((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
			case "and":
			case "&&":
				return new SyntaxTree.And((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
			case "or":
			case "||":
				return new SyntaxTree.Or((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
			default:
				return new SyntaxTree.BitwiseOr((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
		}
	}
	
	@ParserEvent(map = "exp : OP_PAREN exp CL_PAREN", priority = 14)
	public Object parentheses(Parser parser) {
		setCounter(10);
		return parser.getTokens().get(1).getObject();
	}

	@ParserEvent(map = "program : ((VAR )?set exp|vard( exp)?) SEP", priority = 15)
	public Object setVariable(Parser parser) {
		if (parser.getTokens().get(0).getName().equals("vard")) {
			ValueBase value;
			if (parser.getTokens().size() == 3) {
				value = (ValueBase) parser.getTokens().get(1).getObject();
			} else {
				value = new SyntaxTree.Null();
			}
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(), value, true, true);
		} else if (parser.getTokens().get(0).getName().equals("VAR")) {
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(1).getObject(),
					(ValueBase) parser.getTokens().get(2).getObject(), true, true);
		}
		return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(),
				(ValueBase) parser.getTokens().get(1).getObject(), false, true);
	}

	@ParserEvent(map = "program : PRINT exp SEP", priority = 16)
	public Object print(Parser parser) {
		return new SyntaxTree.Print((ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : RET (exp )?SEP", priority = 17)
	public Object _return(Parser parser) {
		if (parser.getTokens().size() == 2) {
			return new SyntaxTree.Return(new SyntaxTree.Null());
		}
		return new SyntaxTree.Return((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "fnc : fnc exp", priority = 18)
	public Object functionCall2(Parser parser) {
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		arrayList.add(new SyntaxTree.Text(parser.getTokens().get(0).getObject().toString()));
		arrayList.add((ValueBase) parser.getTokens().get(1).getObject());
		return arrayList;
	}

	@ParserEvent(map = "fnc : fnc COMMA exp", priority = 19)
	public Object functionCall3(Parser parser) {
		ArrayList<ValueBase> arrayList = (ArrayList<ValueBase>) parser.getTokens().get(0).getObject();
		arrayList.add((ValueBase) parser.getTokens().get(2).getObject());
		return arrayList;
	}

	@ParserEvent(map = "program : program (SEP )?(program ?)+", priority = 20)
	public Object programs(Parser parser) {
		parser.remove("SEP");
		ProgramBase[] programs = new ProgramBase[parser.getTokens().size()];
		for (int i = 0; i < programs.length; i++) {
			programs[i] = (ProgramBase) parser.getTokens().get(i).getObject();
		}
		return new SyntaxTree.Programs(programs);
	}

	@ParserEvent(map = "program : WH exp (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", priority = 21)
	public Object _while(Parser parser) {
		setCounter(18);
		parser.remove("SEP");
		return new SyntaxTree.While((ValueBase)parser.getTokens().get(1).getObject(), (ProgramBase)parser.getTokens().get(3).getObject());
	}

	@ParserEvent(map = "ifprogram : IF exp (SEP )?OP_BRACKET (SEP )?program CL_BRACKET", priority = 22)
	public Object _if(Parser parser) {
		setCounter(18);
		parser.remove("SEP");
		return new SyntaxTree.If((ValueBase)parser.getTokens().get(1).getObject(), (ProgramBase)parser.getTokens().get(3).getObject());
	}

	@ParserEvent(map = "ifprogram : ifprogram (SEP )?ELSE ifprogram", priority = 23)
	public Object elseif(Parser parser) {
		setCounter(18);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(2).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram (SEP )?ELSE (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", priority = 24)
	public Object _else(Parser parser) {
		setCounter(18);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(3).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram SEP", priority = 25)
	public Object ifToProgram(Parser parser) {
		setCounter(18);
		return parser.getTokens().get(0).getObject();
	}

	@ParserEvent(map = "program : fn CL_PAREN (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", priority = 26)
	public Object funcDeclaration(Parser parser) {
		setCounter(18);
		parser.remove("SEP");
		ArrayList<String> stringArrayList = (ArrayList<String>)parser.getTokens().get(0).getObject();
		String functionName = stringArrayList.get(0);
		stringArrayList.remove(0);
		String[] args = new String[stringArrayList.size()];
		for (int i = 0; i < stringArrayList.size(); i++) {
			args[i] = stringArrayList.get(i);
		}
		return new SyntaxTree.Function(functionName,
				(ProgramBase)parser.getTokens().get(3).getObject(), args);
	}

	@ParserEvent(map = "exp : fnc CL_PAREN", priority = 27)
	public Object functionCall4(Parser parser) {
		setCounter(10);
		ArrayList<ValueBase> arrayList;
		Object tmp = parser.getTokens().get(0).getObject();
		if (tmp instanceof ArrayList) {
			arrayList = (ArrayList<ValueBase>) tmp;
		} else {
			arrayList = new ArrayList<>();
			arrayList.add(new SyntaxTree.Text(tmp.toString()));
		}
		String functionName = arrayList.get(0).toString();
		arrayList.remove(0);
		ValueBase[] args = new ValueBase[arrayList.size()];
		for (int i = 0; i < arrayList.size(); i++) {
			args[i] = arrayList.get(i);
		}
		return new SyntaxTree.CallFunction(functionName, args);
	}

	@ParserEvent(map = "program : exp SEP", priority = 28)
	public Object executeValue(Parser parser) {
		setCounter(18);
		return new SyntaxTree.ExecuteValue((ValueBase) parser.getTokens().get(0).getObject());
	}

	public void afterParse(Parser result) {
		if (isCodeRunning) {
			isShell = true;
			isCodeRunning = false;
		}
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
			if (serializeFileName != null) {
				SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
				serializer.serialize(serializeFileName, (ProgramBase)result.getTokens().get(0).getObject());
			}
			boolean compiled = false;
			if (classFileName != null) {
				compiled = true;
				try {
					JVMTool jvmTool = new JVMTool();
					byte[] out = jvmTool.syntaxTreeToJVMClass((ProgramBase) result.getTokens().get(0).getObject(), classFileName);
					FileOutputStream fileOutputStream = new FileOutputStream(classFileName + ".class");
					fileOutputStream.write(out);
					fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (compiledFileName != null) {
				compiled = true;
				try {
					FileWriter writer = new FileWriter(compiledFileName);
					VMTools vmTools = new VMTools();
					writer.write(vmTools.syntaxTreeToVMByteCode((ProgramBase) result.getTokens().get(0).getObject()));
					writer.close();
				} catch (IOException e) {
					System.out.println("ERROR: can't create output file");
					e.printStackTrace();
				}
			}
			if (!compiled) {
				((ProgramBase)result.getTokens().get(0).getObject()).eval();
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
