import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class Compiler extends CompilerBase {
	private final String fileName;
	private final boolean isShell;
	private final String compiledFileName;
	private final String serializeFileName;

	private SyntaxTree.Programs addNameSpaces(String nameSpace, ProgramBase program, ArrayList<String> declaredVariables) {
		if (declaredVariables == null) {
			declaredVariables = new ArrayList<>();
		}
		if (program instanceof SyntaxTree.SetVariable) {
			declaredVariables.add(((SyntaxTree.SetVariable)program).getVariableName());
			((SyntaxTree.SetVariable)program).setVariableName(nameSpace + ":" +
					((SyntaxTree.SetVariable)program).getVariableName());
		} else if (program instanceof SyntaxTree.Programs) {
			for (ProgramBase program2 : ((SyntaxTree.Programs)program).getPrograms()) {
				addNameSpaces(nameSpace, program2, declaredVariables);
			}
		} else {
			Field[] fields = program.getClass().getDeclaredFields();
			for (Field field : fields) {
				field.setAccessible(true);
				try {
					if (field.get(program) instanceof SyntaxTree.Variable) {
						SyntaxTree.Variable tmp = (SyntaxTree.Variable) field.get(program);
						if (declaredVariables.contains(tmp.getVariableName())) {
							tmp.setVariableName(nameSpace + ":" + tmp.getVariableName());
						}
					} else if (field.get(program) instanceof ValueBase[]) {
						ValueBase[] values = (ValueBase[]) field.get(program);
						for (ValueBase value : values) {
							if (value instanceof SyntaxTree.Variable) {
								SyntaxTree.Variable tmp = (SyntaxTree.Variable) value;
								if (declaredVariables.contains(tmp.getVariableName())) {
									tmp.setVariableName(nameSpace + ":" + tmp.getVariableName());
								}
							}
						}
					}
				} catch (IllegalAccessException ignore) {}
			}
		}
		return new SyntaxTree.Programs(program);
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
		lexer.add("TXT", new TextChecker());
		//Number
		lexer.add("NUM", "\\d+\\.?\\d*");
		//Boolean
		lexer.add("BOOL", "true|false");
		//Null
		lexer.add("NULL", "null");
		//spaces (ignore)
		lexer.add("IGNORE", "(\t| )+");
		//print
		lexer.add("PRINT", "print ");
		//operators
		lexer.add("COMP", "<=|>=|==|<|>");
		lexer.add("SET", "=");
		lexer.add("OP1", "\\*|\\/|%");
		lexer.add("OP2", "\\-|\\+");
		//if (keyword)
		lexer.add("IF", "if ");
		//else (keyword)
		lexer.add("ELSE", "else");
		//func (keyword)
		lexer.add("FN", "func ");
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
		} else if (getCounter() == 8) {
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
		} else if (getCounter() == 5 || getCounter() == 14) {
			parser.setSingleRunPerLocation(false);
		} else if (getCounter() == 6 || getCounter() == 15) {
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
		return new SyntaxTree.Text(text.substring(1, text.length() - 1)
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
				.replace("\\\\", "\\"));
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

	@ParserEvent(map = "fn : FN ID OP_PAREN ID|FN ID OP_PAREN", priority = 5)
	public Object function(Parser parser) {
		ArrayList<String> stringArrayList = new ArrayList<>();
		stringArrayList.add(parser.getTokens().get(1).getText());
		if (parser.getTokens().size() == 4) {
			stringArrayList.add(parser.getTokens().get(3).getText());
		}
		return stringArrayList;
	}

	@ParserEvent(map = "fn : fn COMMA ID", priority = 6)
	public Object function2(Parser parser) {
		ArrayList<String> stringArrayList = (ArrayList<String>) parser.getTokens().get(0).getObject();
		stringArrayList.add(parser.getTokens().get(2).getText());
		return stringArrayList;
	}

	@ParserEvent(map = "fnc : ID OP_PAREN", priority = 7)
	public Object functionCall(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "exp : ID", priority = 8)
	public Object variable(Parser parser) {
		return new SyntaxTree.Variable(parser.getTokens().get(0).getText());
	}

	@ParserEvent(map = "exp : exp OP1 exp", priority = 9)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP2 exp", priority = 10)
	public Object additionAndSubtraction(Parser parser) {
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp COMP exp", priority = 11)
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
//			case ">=":
			default:
				return new SyntaxTree.GreaterThanOrEqual((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
		}
	}

	@ParserEvent(map = "program : set exp SEP", priority = 12)
	public Object setVariable(Parser parser) {
		return new SyntaxTree.SetVariable((String)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : PRINT exp SEP", priority = 13)
	public Object print(Parser parser) {
		return new SyntaxTree.Print((ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "fnc : fnc exp|fnc", priority = 14)
	public Object functionCall2(Parser parser) {
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		arrayList.add(new SyntaxTree.Text(parser.getTokens().get(0).getObject().toString()));
		if (parser.getTokens().size() == 2) {
			arrayList.add((ValueBase) parser.getTokens().get(1).getObject());
		}
		return arrayList;
	}

	@ParserEvent(map = "fnc : fnc COMMA exp", priority = 15)
	public Object functionCall3(Parser parser) {
		ArrayList<ValueBase> arrayList = (ArrayList<ValueBase>) parser.getTokens().get(0).getObject();
		arrayList.add((ValueBase) parser.getTokens().get(2).getObject());
		return arrayList;
	}

	@ParserEvent(map = "program : program program", priority = 16)
	public Object programs(Parser parser) {
		return new SyntaxTree.Programs((ProgramBase)parser.getTokens().get(0).getObject(), (ProgramBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "ifprogram : IF exp OP_BRACKET program CL_BRACKET|IF exp SEP OP_BRACKET program CL_BRACKET|IF exp SEP OP_BRACKET SEP program CL_BRACKET|IF exp OP_BRACKET SEP program CL_BRACKET", priority = 17)
	public Object _if(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		return new SyntaxTree.If((ValueBase)parser.getTokens().get(1).getObject(), (ProgramBase)parser.getTokens().get(3).getObject());
	}

	@ParserEvent(map = "ifprogram : ifprogram ELSE ifprogram|ifprogram SEP ELSE ifprogram", priority = 18)
	public Object elseif(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(2).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram ELSE OP_BRACKET program CL_BRACKET SEP|ifprogram SEP ELSE OP_BRACKET program CL_BRACKET SEP|ifprogram ELSE SEP OP_BRACKET program CL_BRACKET SEP|ifprogram SEP ELSE OP_BRACKET SEP program CL_BRACKET SEP|ifprogram SEP ELSE SEP OP_BRACKET SEP program CL_BRACKET SEP|ifprogram ELSE OP_BRACKET SEP program CL_BRACKET SEP", priority = 19)
	public Object _else(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		SyntaxTree.If tmp = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		SyntaxTree.If tmp2 = (SyntaxTree.If)parser.getTokens().get(0).getObject();
		while (tmp.getElseProgram() != null) {
			tmp = (SyntaxTree.If)tmp.getElseProgram();
		}
		tmp.addElse((ProgramBase)parser.getTokens().get(3).getObject());
		return tmp2;
	}

	@ParserEvent(map = "program : ifprogram SEP", priority = 20)
	public Object ifToProgram(Parser parser) {
		setCounter(16);
		return parser.getTokens().get(0).getObject();
	}

	@ParserEvent(map = "program : fn CL_PAREN OP_BRACKET program CL_BRACKET SEP|fn CL_PAREN SEP OP_BRACKET program CL_BRACKET SEP|fn CL_PAREN OP_BRACKET SEP program CL_BRACKET SEP|fn CL_PAREN SEP OP_BRACKET SEP program CL_BRACKET SEP", priority = 21)
	public Object funcDeclaration(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		ArrayList<String> stringArrayList = (ArrayList<String>)parser.getTokens().get(0).getObject();
		String tmp = stringArrayList.get(0);
		StringBuilder functionName = new StringBuilder(tmp + ":");
		stringArrayList.remove(0);
		for (String string : stringArrayList) {
			functionName.append(",").append(string);
		}
		return new SyntaxTree.Function(functionName.toString(),
				addNameSpaces("FN" + fileName + tmp,
						(ProgramBase)parser.getTokens().get(3).getObject(),
						stringArrayList));
	}

	@ParserEvent(map = "program : fnc CL_PAREN SEP", priority = 22)
	public Object functionCall4(Parser parser) {
		setCounter(16);
		ArrayList<ValueBase> arrayList = (ArrayList<ValueBase>) parser.getTokens().get(0).getObject();
		String functionName = arrayList.get(0).getData().toString();
		String functionName2 = null;
		arrayList.remove(0);
		ProgramBase[] programs = new ProgramBase[arrayList.size() + 1];
		ArrayList<String> params = new ArrayList<>();
		for (Map.Entry<String, ProgramBase> entry : SyntaxTree.getFunctions().entrySet()) {
			if (entry.getKey().split(":")[0].equals(functionName)) {
				functionName2 = entry.getKey();
				if (entry.getKey().split(":").length > 1) {
					for (String string : entry.getKey().split(":")[1].split(",")) {
						if (string.equals("")) continue;
						params.add(string);
					}
				}
			}
		}
		if (params.size() != arrayList.size()) {
			syntaxError("not enough arguments for function " + functionName);
		}
		int i = 0;
		for (ValueBase value : arrayList) {
			programs[i] = addNameSpaces("FN" + fileName + functionName,
					new SyntaxTree.SetVariable(params.get(i++), value), null);
		}
		programs[i] = new SyntaxTree.CallFunction(functionName2);
		return new SyntaxTree.Programs(programs);
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
