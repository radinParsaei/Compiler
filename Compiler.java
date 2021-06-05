import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.math.BigDecimal;

public class Compiler extends CompilerBase {
	private final String fileName;
	private final String xmlOutput;
	private boolean isShell;
	private boolean isCodeRunning = false;
	private final String compiledFileName;
	private final String serializeFileName;
	private final String classFileName;
	static boolean parsingImportedFile = false;
	private static final HashMap<String, SyntaxTree.Programs> importedFiles = new HashMap<>();

	public Compiler(String fileName, boolean isShell, String compiledFileName, String classFileName, String serializeFileName, String xmlOutput) {
		this.fileName = fileName;
		this.isShell = isShell;
		this.compiledFileName = compiledFileName;
		this.serializeFileName = serializeFileName;
		this.classFileName = classFileName;
		this.xmlOutput = xmlOutput;
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
					if (!Targets.isWeb) {
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
				}
				return line + "\n";
			} catch (java.util.NoSuchElementException e) {
				if (!Targets.isWeb) System.exit(0);
			}
		}
//		if (!Targets.isWeb) {
//			File file = new File(fileName);
//			Scanner scanner = null;
//			try {
//				scanner = new Scanner(file);
//			} catch (FileNotFoundException e) {
//				System.err.println("Can't open the file");
//				System.exit(1);
//			}
//			scanner.useDelimiter("\\Z");
//			return scanner.next() + "\n";
//		} else {
//			return "";
//		}
		return Targets.readFile(fileName);
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
		lexer.add("IGNORE", "(\t| |\\/\\/.*|\\/\\*(\\s*.?)*\\*\\/)+");
		//lambda arrow
		lexer.add("ARROW", "->");
		//operators
		lexer.add("E", "\\*\\*");//exponentiation
		lexer.add("OP1", "\\*|\\/|%");
		lexer.add("OP2", "\\-|\\+");
		lexer.add("OP3", "\\|\\||\\||and|&&|&|or|\\^|>>|<<");
		lexer.add("COMP", "!=\\?|!=|==|<=|>=|=\\?|<|>");
		lexer.add("SET", "=");
		//while (keyword)
		lexer.add("WH", "while ");
		lexer.add("FOR", "for ");
		lexer.add("IMPORT", "import ");
		//exclamation
		lexer.add("!", "!");
		//bitwise not sign(~)
		lexer.add("~", "~");
		//if (keyword)
		lexer.add("IF", "if ");
		//else (keyword)
		lexer.add("ELSE", "else");
		//class (keyword)
		lexer.add("CLASS", "class ");
		//static (keyword)
		lexer.add("STATIC", "static ");
		//this (keyword)
		lexer.add("THIS", "this");
		//init (keyword)
		lexer.add("INIT", "init");
		//class (keyword)
		lexer.add("NEW", "new ");
		//func (keyword)
		lexer.add("FN", "func ");
		//var (keyword)
		lexer.add("VAR", "var ");
		//return (keyword)
		lexer.add("RET", "return");
		//break (keyword)
		lexer.add("BR", "break");
		//break (keyword)
		lexer.add("CON", "continue");
		//square brackets
		lexer.add("OP_SQ_BRACKET", "\\[");
		lexer.add("CL_SQ_BRACKET", "\\]");
		//brackets
		lexer.add("OP_BRACKET", "\\{");
		lexer.add("CL_BRACKET", "\\}");
		//parentheses
		lexer.add("OP_PAREN", "\\(");
		lexer.add("CL_PAREN", "\\)");
		lexer.add("COMMA", ",");
		//id
		lexer.add("ID", "([A-Za-z]*\\d*_*)+");
		//dot
		lexer.add("DOT", "\\.");
		//separator
		lexer.add("SEP", new SeperatorChecker());
	}

	public void afterLex(Parser result) {
		result.on("THIS", "exp", (parser) -> new SyntaxTree.This());
		SyntaxTree.Import.doImport = fileName1 -> {
			if (fileName1.equals("random")) {
				SyntaxTree.declareNativeFunction("random", "randint", 2);
				SyntaxTree.declareNativeFunction("random", "random", 0);
				SyntaxTree.declareNativeFunction("random", "seed", 1);
				SyntaxTree.CallFunction callFunction = new SyntaxTree.CallFunction("seed", new SyntaxTree.Variable("a"));
				callFunction.findFunction();
				new SyntaxTree.Function("randomSeed", new SyntaxTree.ExecuteValue(callFunction), "a").eval();
				SyntaxTree.CallFunction callFunction1 = new SyntaxTree.CallFunction("randint", new SyntaxTree.Variable("a"), new SyntaxTree.Variable("b"));
				callFunction1.findFunction();
				new SyntaxTree.Function("random", new SyntaxTree.Return(callFunction1), "a", "b").eval();
				SyntaxTree.deleteNativeFunction("random", "seed", 1);
				SyntaxTree.deleteNativeFunction("random", "randint", 2);
			} else {
				return importedFiles.get(fileName1);
			}
			return new SyntaxTree.Programs();
		};
	}

	private void filterImportedProgram(ProgramBase object, ArrayList<ProgramBase> result) {
		if (object instanceof SyntaxTree.Function || object instanceof SyntaxTree.SetVariable || object instanceof SyntaxTree.CreateClass || object instanceof SyntaxTree.Import) {
			result.add(object);
		} else if (object instanceof SyntaxTree.Programs) {
			for (ProgramBase program : ((SyntaxTree.Programs) object).getPrograms()) {
				filterImportedProgram(program, result);
			}
		}
	}

	public void parse(Parser parser) {
		if (!Targets.isWeb) {
			if (getCounter() == 0) {
				parser.on("SEP SEP", "SEP", (parser1) -> null);
				parser.setSaveTexts(true);
			} else if (getCounter() == 13) {
				for (int i = 0; i < parser.getTokens().size(); i++) {
					if (parser.getTokens().get(i).getName().equals("OP2")
							&& (i == 0 || (!parser.getTokens().get(i - 1).getName().equals("exp")
							&& !parser.getTokens().get(i - 1).getName().equals("CL_PAREN")))
							&& parser.getTokens().get(i + 1).getObject() != null) {
						ValueBase value = (ValueBase) parser.getTokens().get(i + 1).getObject();
						if (parser.getTokens().get(i).getText().equals("-")) {
							if (value instanceof SyntaxTree.Number)
								value = new SyntaxTree.Number(((BigDecimal) value.getData()).negate());
							else
								value = new SyntaxTree.Negative(value);
						}
						parser.getTokens().remove(i);
						parser.getTokens().remove(i);
						Token token = new Token("exp", null);
						token.setObject(value);
						parser.getTokens().add(i, token);
					}
				}
			} else if (getCounter() == 6) {
				parser.setSingleRunPerLocation(false);
			} else if (getCounter() == 7) {
				parser.setSingleRunPerLocation(true);
			}
		}
		if (Targets.isWeb) {
			Web.parse(parser, this);
		}
	}

	@ParserEvent(map = "program : IMPORT TXT SEP", priority = 0)
	public Object _import(Parser parser) {
		parser.on("TXT", "exp", this::text);
		String fileName = parser.getTokens().get(1).getObject().toString();
		if (!fileName.equals("random")) {
			parsingImportedFile = true;
			ArrayList<ProgramBase> result1 = new ArrayList<>();
			filterImportedProgram(((ProgramBase) CompilerMain.compile(new Compiler(fileName, false, null, null, null, null)).getTokens().get(0).getObject()), result1);
			parsingImportedFile = false;
			ProgramBase[] resultArray = new ProgramBase[result1.size()];
			resultArray = result1.toArray(resultArray);
			importedFiles.put(fileName, new SyntaxTree.Programs(resultArray));
		}
		return new SyntaxTree.Import(fileName);
	}

	@ParserEvent(map = "exp : NUM", priority = 1)
	public Object number(Parser parser) {
		return new SyntaxTree.Number(new BigDecimal(parser.getTokens().get(0).getText()));
	}

	@ParserEvent(map = "exp : TXT", priority = 2)
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
		int i = string.indexOf("\\u");
		while (i != -1) {
			StringBuilder tmp = new StringBuilder(string.substring(i + 2, i + 6));
			i = string.indexOf("\\u", i + 1);
			try {
				int code = Integer.parseInt(tmp.toString(), 16);
				string = string.replace("\\u" + tmp, Character.toString((char) code));
			} catch (NumberFormatException ignore) {}
		}
		return new SyntaxTree.Text(string);
	}

	@ParserEvent(map = "exp : BOOL", priority = 3)
	public Object booleanExpression(Parser parser) {
		return new SyntaxTree.Boolean(parser.getTokens().get(0).getText().equals("true"));
	}

	@ParserEvent(map = "exp : NULL", priority = 4)
	public Object nullExpression(Parser parser) {
		return new SyntaxTree.Null();
	}

	@ParserEvent(map = "set : ID SET", priority = 5)
	public Object set(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "vard : VAR ID", priority = 6)
	public Object varDeclaration(Parser parser) {
		return parser.getTokens().get(1).getText();
	}

	@ParserEvent(map = "class : CLASS ID", priority = 7)
	public Object createClass(Parser parser) {
		String name = parser.getTokens().get(1).getText();
		SyntaxTree.getClassesParameters().put(name, null);
		return name;
	}

	@ParserEvent(map = "fn : (FN ID|INIT) OP_PAREN( vard| ID)?", priority = 8)
	public Object function(Parser parser) {
		if (parser.getTokens().get(0).getName().equals("INIT")) {
			parser.getTokens().add(1, new Token("ID", "<init>"));
		}
		ArrayList<String> stringArrayList = new ArrayList<>();
		stringArrayList.add(parser.getTokens().get(1).getText());
		if (parser.getTokens().size() == 4 && parser.getTokens().get(3).getName().equals("vard")) {
			stringArrayList.add((String) parser.getTokens().get(3).getObject());
		} else if (parser.getTokens().size() == 4) {
			stringArrayList.add(parser.getTokens().get(3).getText());
		}
		return stringArrayList;
	}

	@ParserEvent(map = "fn : fn COMMA (ID|vard)", priority = 9)
	public Object function2(Parser parser) {
		ArrayList<String> stringArrayList = (ArrayList<String>) parser.getTokens().get(0).getObject();
		if (parser.getTokens().get(2).getName().equals("ID")) stringArrayList.add(parser.getTokens().get(2).getText());
		else stringArrayList.add((String) parser.getTokens().get(2).getObject());
		return stringArrayList;
	}

	@ParserEvent(map = "new : NEW ID", priority = 10)
	public Object createInstance(Parser parser) {
		return parser.getTokens().get(1).getText();
	}

	@ParserEvent(map = "fnc : ID OP_PAREN", priority = 11)
	public Object functionCall(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "lambda : OP_PAREN ID( COMMA ID)* CL_PAREN ARROW", priority = 12)
	public Object lambdaArgs(Parser parser) {
		ArrayList<String> arrayList = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("ID")) {
				arrayList.add(token.getText());
			}
		}
		return arrayList;
	}

	@ParserEvent(map = "inc : ID (OP2|OP1|OP3|E) SET", priority = 13)
	public Object increaseAndOthers(Parser parser) {
		return parser;
	}

	@ParserEvent(map = "exp : (exp DOT )?ID|exp DOT (ID|exp)", priority = 14)
	public Object variable(Parser parser) {
		parser.remove("DOT");
		parser.setSaveTexts(true);
		if (parser.getTokens().size() == 2) {
			if (parser.getTokens().get(1).getName().equals("exp")) {
				ValueBase data = (ValueBase) parser.getTokens().get(1).getObject();
				if (data instanceof SyntaxTree.Variable) {
					((SyntaxTree.Variable) data).fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
				} else if (data instanceof SyntaxTree.CallFunction) {
					((SyntaxTree.CallFunction) data).fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
				}
				return data;
			}
			String name = null;
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable)
				name = ((SyntaxTree.Variable) parser.getTokens().get(0).getObject()).getVariableName();
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable &&
					SyntaxTree.getClassesParameters().containsKey(name)) {
				String finalName = name;
				return new SyntaxTree.AwaitedValue(() -> {
					boolean staticParameterExists = false;
					for (String string : SyntaxTree.staticParameters) {
						if (string.startsWith("#C" + finalName + "#" + parser.getTokens().get(1).getText())) {
							staticParameterExists = true;
							break;
						}
					}
					if (staticParameterExists) {
						return new SyntaxTree.Variable("#C" + finalName + "#" + parser.getTokens().get(1).getText());
					} else {
						return new SyntaxTree.Variable(parser.getTokens().get(1).getText()).fromInstance((ValueBase) parser.getTokens().get(0).getObject())
								.setAddInstanceName(true);
					}
				});
			}
			return new SyntaxTree.Variable(parser.getTokens().get(1).getText()).fromInstance((ValueBase) parser.getTokens().get(0).getObject())
					.setAddInstanceName(true);
		}
		return new SyntaxTree.Variable(parser.getTokens().get(0).getText());
	}

	@ParserEvent(map = "exp : exp E exp(?! DOT)", priority = 15)
	public Object exponentiation(Parser parser) {
		setCounter(14);
		return new SyntaxTree.Pow((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP1 exp(?! DOT)", priority = 16)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		setCounter(14);
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP2 exp(?! DOT)", priority = 17)
	public Object additionAndSubtraction(Parser parser) {
		setCounter(14);
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp COMP exp(?! DOT)", priority = 18)
	public Object comparison(Parser parser) {
		setCounter(14);
		switch (parser.getTokens().get(1).getText()) {
			case "==":
				return new SyntaxTree.StrictEquals((ValueBase) parser.getTokens().get(0).getObject(),
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
				return new SyntaxTree.Not(new SyntaxTree.StrictEquals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject()));
			case "=?":
				return new SyntaxTree.Equals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
			case "!=?":
				return new SyntaxTree.Not(new SyntaxTree.Equals((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject()));
//			case ">=":
			default:
				return new SyntaxTree.GreaterThanOrEqual((ValueBase) parser.getTokens().get(0).getObject(),
						(ValueBase) parser.getTokens().get(2).getObject());
		}
	}

	@ParserEvent(map = "exp : exp OP3 exp(?! DOT)", priority = 19)
	public Object bitwiseAnd(Parser parser) {
		setCounter(14);
		switch (parser.getTokens().get(1).getText()) {
			case ">>":
				return new SyntaxTree.RightShift((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
			case "<<":
				return new SyntaxTree.LeftShift((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
			case "^":
				return new SyntaxTree.Xor((ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
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

	@ParserEvent(map = "exp : new OP_PAREN ((exp COMMA )*exp )?CL_PAREN", priority = 20)
	public Object createInstance1(Parser parser) {
		setCounter(14);
		parser.remove("SEP");
		ArrayList<ValueBase> tmp = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("exp")) {
				tmp.add((ValueBase) token.getObject());
			}
		}
		ValueBase[] args = new ValueBase[tmp.size()];
		for (int i = 0; i < tmp.size(); i++) {
			args[i] = tmp.get(i);
		}
		return new SyntaxTree.CreateInstance((String) parser.getTokens().get(0).getObject(), args);
	}

	@ParserEvent(map = "program : exp (OP_SQ_BRACKET( SEP)? exp( SEP)? CL_SQ_BRACKET( SEP)? )+SET( SEP)? exp SEP", priority = 21)
	public Object setInArray(Parser parser) {
		setCounter(14);
		parser.remove("SEP");
		if (parser.getTokens().size() == 6) {
			return new SyntaxTree.ExecuteValue(new SyntaxTree.CallFunction("set", (ValueBase) parser.getTokens().get(2).getObject(), (ValueBase) parser.getTokens().get(5).getObject())
					.fromInstance((ValueBase)parser.getTokens().get(0).getObject()).setAddInstanceName(true));
		} else {
			ValueBase result = null;
			boolean isFirst = true;
			int valueLocation = parser.getTokens().size() - 1;
			ValueBase value = (ValueBase) parser.getTokens().get(valueLocation).getObject();
			parser.getTokens().remove(valueLocation);
			int indexLocation = parser.getTokens().size() - 3;
			ValueBase index = (ValueBase) parser.getTokens().get(indexLocation).getObject();
			parser.getTokens().remove(indexLocation);
			for (Token token : parser.getTokens()) {
				if (isFirst) {
					result = (ValueBase) token.getObject();
					isFirst = false;
					continue;
				}
				if (token.getName().equals("exp")) {
					result = new SyntaxTree.CallFunction("get", (ValueBase) token.getObject()).fromInstance(result).setAddInstanceName(true);
				}
			}
			return new SyntaxTree.ExecuteValue(new SyntaxTree.CallFunction("set", index, value).fromInstance(result).setAddInstanceName(true));
		}
	}

	@ParserEvent(map = "exp : exp OP_SQ_BRACKET( SEP)? exp( SEP)? CL_SQ_BRACKET", priority = 22)
	public Object getFromArray(Parser parser) {
		setCounter(14);
		parser.remove("SEP");
		return new SyntaxTree.CallFunction("get", (ValueBase) parser.getTokens().get(2).getObject())
				.fromInstance((ValueBase)parser.getTokens().get(0).getObject()).setAddInstanceName(true);
	}

	@ParserEvent(map = "exp : (exp DOT )?fnc ((exp COMMA )*exp )?CL_PAREN", priority = 23)
	public Object functionCall2(Parser parser) {
		setCounter(14);
		ArrayList<ValueBase> tmp = new ArrayList<>();
		boolean isFirst = true;
		for (Token token : parser.getTokens()) {
			if (isFirst) {
				isFirst = false;
				continue;
			}
			if (token.getName().equals("exp")) {
				tmp.add((ValueBase) token.getObject());
			}
		}
		ValueBase[] args = new ValueBase[tmp.size()];
		for (int i = 0; i < tmp.size(); i++) {
			args[i] = tmp.get(i);
		}
		SyntaxTree.CallFunction res;
		if (parser.getTokens().get(0).getName().equals("exp")) {
			String name = null;
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable)
				name = ((SyntaxTree.Variable) parser.getTokens().get(0).getObject()).getVariableName();
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable &&
					SyntaxTree.getClassesParameters().containsKey(name)) {
				String finalName = name;
				return new SyntaxTree.AwaitedValue(() -> {
					boolean staticFunctionExists = false;
					for (String string : SyntaxTree.staticFunctions) {
						if (string.startsWith("#C" + finalName + "#" + parser.getTokens().get(2).getObject() + ":")) {
							staticFunctionExists = true;
							break;
						}
					}
					if (staticFunctionExists) {
						return new SyntaxTree.CallFunction("#C" + finalName + "#" + parser.getTokens().get(2).getObject(), args);
					} else {
						return new SyntaxTree.CallFunction((String) parser.getTokens().get(2).getObject(), args)
								.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
					}
				});
			} else {
				res = new SyntaxTree.CallFunction((String) parser.getTokens().get(2).getObject(), args)
						.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
			}
		} else if ((parser.getTokens().get(0).getObject()).toString().equals("declareNativeFunction") && args.length == 3) {
			if (!(args[0] instanceof SyntaxTree.Text && args[1] instanceof SyntaxTree.Text && args[2] instanceof SyntaxTree.Number)) {
				syntaxError("USE declareNativeFunction(TEXT, TEXT, NUMBER)");
			}
			SyntaxTree.declareNativeFunction((String) args[0].getData(), (String) args[1].getData(), ((BigDecimal) args[2].getData()).intValue());
			return new SyntaxTree.Null();
		} else if ((parser.getTokens().get(0).getObject()).toString().equals("deleteNativeFunction") && args.length == 3) {
			if (!(args[0] instanceof SyntaxTree.Text && args[1] instanceof SyntaxTree.Text && args[2] instanceof SyntaxTree.Number)) {
				syntaxError("USE deleteNativeFunction(TEXT, TEXT, NUMBER)");
			}
			SyntaxTree.deleteNativeFunction((String) args[0].getData(), (String) args[1].getData(), ((BigDecimal) args[2].getData()).intValue());
			return new SyntaxTree.Null();
		} else if ((parser.getTokens().get(0).getObject()).toString().equals("print") && args.length == 1) {
			return new SyntaxTree.PrintFunction(new SyntaxTree.Print(args[0]));
		} else if ((parser.getTokens().get(0).getObject()).toString().equals("exit") && (args.length == 1 || args.length == 0)) {
			if (args.length == 0)
				return new SyntaxTree.ExitFunction(new SyntaxTree.Exit(new SyntaxTree.Null()));
			return new SyntaxTree.ExitFunction(new SyntaxTree.Exit(args[0]));
		} else {
			res = new SyntaxTree.CallFunction((String) parser.getTokens().get(0).getObject(), args);
			String tmp1 = parser.getTokens().get(0).getObject() + ":N#" + args.length + "#";
			for (String key : SyntaxTree.getFunctions().keySet()) {
				if (key.startsWith(tmp1)) {
					res.findFunction();
				}
			}
		}
		return res;
	}

	@ParserEvent(map = "exp : OP_SQ_BRACKET( SEP)?( exp( SEP)? COMMA( SEP)?)*( exp)?( SEP)? CL_SQ_BRACKET", priority = 24)
	public Object array(Parser parser) {
		setCounter(14);
		parser.exitCheckLoop();
		parser.remove("SEP");
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("exp")) {
				arrayList.add((ValueBase) token.getObject());
			}
		}
		return new SyntaxTree.CreateInstance("%Array", SyntaxTree.List.fromArrayList(arrayList));
	}

	@ParserEvent(map = "exp : OP_PAREN exp CL_PAREN", priority = 25)
	public Object parentheses(Parser parser) {
		setCounter(14);
		return parser.getTokens().get(1).getObject();
	}

	@ParserEvent(map = "exp : exp ! OP_PAREN (exp )?(COMMA exp )*CL_PAREN", priority = 26)
	public Object callFunctionFromPointer(Parser parser) {
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("exp")) {
				arrayList.add((ValueBase) token.getObject());
			}
		}
		ValueBase[] args = new ValueBase[arrayList.size()];
		args = arrayList.toArray(args);
		return new SyntaxTree.CallFunctionFromPointer((ValueBase) parser.getTokens().get(0).getObject(), args);
	}

	@ParserEvent(map = "program : inc exp( SEP)?", priority = 27)
	public Object increaseAndOthers1(Parser parser) {
		switch (((Parser) parser.getTokens().get(0).getObject()).getTokens().get(1).getText()) {
			case "+":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Add(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "-":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Sub(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "*":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Mul(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "/":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Div(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "%":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Mod(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "&":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.BitwiseAnd(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "|":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.BitwiseOr(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "^":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Xor(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "<<":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.LeftShift(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case ">>":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.RightShift(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			case "**":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Pow(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			default:
				syntaxError("Use +=, -=, *=, /=, %=, **=, &=, |=");
				if (!Targets.isWeb) System.exit(1);
		}
		return null;
	}

	@ParserEvent(map = "exp : (!|~) exp", priority = 28)
	public Object not(Parser parser) {
		setCounter(14);
		if (parser.getTokens().get(0).getText().equals("!"))
			return new SyntaxTree.Not((ValueBase) parser.getTokens().get(1).getObject());
		else
			return new SyntaxTree.BitwiseNot((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : (((STATIC )?VAR )?set exp|(STATIC )?vard( exp)?|exp DOT set exp) SEP", priority = 29)
	public Object setVariable(Parser parser) {
		boolean isStatic = parser.getTokens().get(0).getName().equals("STATIC");
		parser.remove("STATIC");
		if (parser.getTokens().get(0).getName().equals("exp")) {
			String name = null;
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable)
				name = ((SyntaxTree.Variable) parser.getTokens().get(0).getObject()).getVariableName();
			if (parser.getTokens().get(0).getObject() instanceof SyntaxTree.Variable &&
					SyntaxTree.getClassesParameters().containsKey(name)) {
				String finalName = name;
				return new SyntaxTree.AwaitedProgram(() -> {
					boolean staticFunctionExists = false;
					for (String string : SyntaxTree.staticParameters) {
						if (string.startsWith("#C" + finalName + "#" + parser.getTokens().get(2).getObject())) {
							staticFunctionExists = true;
							break;
						}
					}
					if (staticFunctionExists) {
						return new SyntaxTree.SetVariable("#C" + finalName + "#" + parser.getTokens().get(2).getObject(), (ValueBase) parser.getTokens().get(3).getObject());
					} else {
						return new SyntaxTree.SetVariable((String) parser.getTokens().get(2).getObject(),
								(ValueBase) parser.getTokens().get(3).getObject())
								.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
					}
				});
			}
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(2).getObject(),
					(ValueBase) parser.getTokens().get(3).getObject())
					.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
		}
		if (parser.getTokens().get(0).getName().equals("vard")) {
			ValueBase value;
			if (parser.getTokens().size() == 3) {
				value = (ValueBase) parser.getTokens().get(1).getObject();
			} else {
				value = new SyntaxTree.Null();
			}
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(), value, true, true).setStatic(isStatic);
		} else if (parser.getTokens().get(0).getName().equals("VAR")) {
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(1).getObject(),
					(ValueBase) parser.getTokens().get(2).getObject(), true, true).setStatic(isStatic);
		}
		return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(),
				(ValueBase) parser.getTokens().get(1).getObject(), false, true);
	}

	@ParserEvent(map = "program : RET (exp )?SEP", priority = 30)
	public Object _return(Parser parser) {
		if (parser.getTokens().size() == 2) {
			return new SyntaxTree.Return(new SyntaxTree.Null());
		}
		return new SyntaxTree.Return((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : BR SEP", priority = 31)
	public Object _break(Parser parser) {
		return new SyntaxTree.Break();
	}

	@ParserEvent(map = "program : CON SEP", priority = 32)
	public Object _continue(Parser parser) {
		return new SyntaxTree.Continue();
	}

	@ParserEvent(map = "program : program (SEP )?(program ?)+", priority = 33)
	public Object programs(Parser parser) {
		parser.remove("SEP");
		ProgramBase[] programs = new ProgramBase[parser.getTokens().size()];
		for (int i = 0; i < programs.length; i++) {
			programs[i] = (ProgramBase) parser.getTokens().get(i).getObject();
		}
		return new SyntaxTree.Programs(programs);
	}

	@ParserEvent(map = "program : WH exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 34)
	public Object _while(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		return new SyntaxTree.While((ValueBase)parser.getTokens().get(1).getObject(), parser.getTokens().get(3).getName().equals("program")? (ProgramBase)parser.getTokens().get(3).getObject():new SyntaxTree.Programs());
	}

	@ParserEvent(map = "program : FOR (OP_PAREN )?program exp SEP program (CL_PAREN )?(SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 35)
	public Object _for(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		String map = parser.getMap();
		if (map.contains("CL_PAREN") && map.contains("OP_PAREN")) {
			parser.remove("CL_PAREN");
			parser.remove("OP_PAREN");
		} else if (map.contains("CL_PAREN") || map.contains("OP_PAREN")) {
			syntaxError("use for () {} | for {}");
			if (!Targets.isWeb) System.exit(1);
		}
		return new SyntaxTree.For((ValueBase) parser.getTokens().get(2).getObject(), (ProgramBase) parser.getTokens().get(3).getObject(), (ProgramBase) parser.getTokens().get(1).getObject(), parser.getTokens().get(5).getName().equals("program")? (ProgramBase)parser.getTokens().get(5).getObject():new SyntaxTree.Programs());
	}

	@ParserEvent(map = "program : IF exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET( ELSE IF exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET)*( ELSE( SEP)? OP_BRACKET( SEP)? (program )?CL_BRACKET)? SEP", priority = 36)
	public Object _if(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		byte sub = (byte) (9 - parser.getTokens().size());
		if (parser.getTokens().size() == 4 || parser.getTokens().size() == 5) {
			return new SyntaxTree.If((ValueBase) parser.getTokens().get(1).getObject(),
					parser.getTokens().get(3).getName().equals("program") ? (ProgramBase) parser.getTokens().get(3).getObject() : new SyntaxTree.Programs());
		} else if (parser.getTokens().size() == 7 || parser.getTokens().size() == 8 || parser.getTokens().size() == 9) {
			return new SyntaxTree.If((ValueBase) parser.getTokens().get(1).getObject(),
					parser.getTokens().get(3).getName().equals("program") ? (ProgramBase) parser.getTokens().get(3).getObject() : new SyntaxTree.Programs())
					.addElse(parser.getTokens().get(7 - sub).getName().equals("program") ? (ProgramBase) parser.getTokens().get(7 - sub).getObject() : new SyntaxTree.Programs());
		}
		SyntaxTree.If _if = new SyntaxTree.If((ValueBase) parser.getTokens().get(1).getObject(),
				parser.getTokens().get(3).getName().equals("program") ? (ProgramBase) parser.getTokens().get(3).getObject() : new SyntaxTree.Programs());
		SyntaxTree.If tmp = _if;
		int pI = 0;
		for (int i = 5; i < parser.getTokens().size(); i += 6) {
			if (!parser.getTokens().get(i).getName().equals("program")) i--;
			if (parser.getTokens().get(i).getName().equals("program") && parser.getTokens().get(i - 2).getName().equals("exp")) {
				pI = i;
				tmp.addElse(new SyntaxTree.If((ValueBase) parser.getTokens().get(i - 2).getObject(), (ProgramBase) parser.getTokens().get(i).getObject()));
				tmp = (SyntaxTree.If) tmp.getElseProgram();
			}
		}
		if (parser.getTokens().size() > pI + 2 && parser.getTokens().get(pI + 2).getName().equals("ELSE")) {
			tmp.addElse((ProgramBase) parser.getTokens().get(pI + 4).getObject());
		}
		return _if;
	}

	@ParserEvent(map = "program : (STATIC )?fn CL_PAREN (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 37)
	public Object funcDeclaration(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		boolean isStatic = parser.getTokens().get(0).getName().equals("STATIC");
		parser.remove("STATIC");
		ArrayList<String> stringArrayList = (ArrayList<String>)parser.getTokens().get(0).getObject();
		String functionName = stringArrayList.get(0);
		stringArrayList.remove(0);
		String[] args = new String[stringArrayList.size()];
		for (int i = 0; i < stringArrayList.size(); i++) {
			args[i] = stringArrayList.get(i);
		}
		return new SyntaxTree.Function(functionName,
				parser.getTokens().get(3).getName().equals("program")?(ProgramBase)parser.getTokens().get(3).getObject():new SyntaxTree.Programs(), args).setStatic(isStatic);
	}

	@ParserEvent(map = "exp : (OP_PAREN CL_PAREN ARROW|lambda) OP_BRACKET (SEP )?(program )?CL_BRACKET", priority = 38)
	public Object lambda(Parser parser) {
		setCounter(14);
		parser.remove("SEP");
		if (parser.getTokens().size() == 6 || parser.getTokens().size() == 5)
			return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(4).getName().equals("program")? (ProgramBase) parser.getTokens().get(4).getObject():new SyntaxTree.Programs()));
		String[] args = new String[((ArrayList<String>)parser.getTokens().get(0).getObject()).size()];
		args = ((ArrayList<String>)parser.getTokens().get(0).getObject()).toArray(args);
		return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(2).getName().equals("program")? (ProgramBase) parser.getTokens().get(2).getObject():new SyntaxTree.Programs(), args));
	}

	@ParserEvent(map = "program : class (SEP )?OP_BRACKET (SEP )?program CL_BRACKET", priority = 39)
	public Object createClass1(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		return new SyntaxTree.CreateClass((String) parser.getTokens().get(0).getObject(), (ProgramBase) parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "program : exp SEP", priority = 40)
	public Object executeValue(Parser parser) {
		setCounter(33);
		return new SyntaxTree.ExecuteValue((ValueBase) parser.getTokens().get(0).getObject());
	}

	public void afterParse(Parser result) {
		if (parsingImportedFile) return;
		if (isCodeRunning) {
			isShell = true;
			isCodeRunning = false;
		}
		result.remove("SEP");
		if (result.getTokens().size() == 0) {
			if (CustomCompileStep.used) {
				CustomCompileStep.run(new SyntaxTree.Programs());
			}
			return ;
		}
		if (result.getTokens().size() != 1) {
			System.out.println("Syntax is:\n" + result);
			syntaxError("Syntax Error");
			return ;
		}
		if (result.getTokens().get(0).getName().equals("program")) {
			if (!Targets.isWeb) {
				if (serializeFileName != null) {
					SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
					serializer.serialize(serializeFileName, (ProgramBase) result.getTokens().get(0).getObject());
				}
			}
			SyntaxTree.declareNativeFunction("input", "input", 0);
			boolean addRandInt = !SyntaxTree.getFunctions().containsKey("randint:N#2#random");
			if (addRandInt) SyntaxTree.declareNativeFunction("random", "randint", 2);
			result.getTokens().get(0).setObject(new SyntaxTree.Programs(
					new SyntaxTree.CreateClass("%Array",
							new SyntaxTree.SetVariable("data", new SyntaxTree.List()).setIsDeclaration(true),
							new SyntaxTree.Function("toString", new SyntaxTree.Return(new SyntaxTree.Variable("data"))),
							new SyntaxTree.Function("<init>", new SyntaxTree.SetVariable("data", new SyntaxTree.Variable("value")), "value"),
							new SyntaxTree.Function("<init>", new SyntaxTree.Programs()),
							new SyntaxTree.Function("append", new SyntaxTree.SetVariable("data", new SyntaxTree.Append(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("value"))), "value"),
							new SyntaxTree.Function("insert", new SyntaxTree.SetVariable("data", new SyntaxTree.Insert(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("value"), new SyntaxTree.Variable("index"))), "index", "value"),
							new SyntaxTree.Function("indexOf", new SyntaxTree.Return(new SyntaxTree.IndexOf(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("a"))), "a"),
							new SyntaxTree.Function("indexOf", new SyntaxTree.Return(new SyntaxTree.IndexOf(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("a"), new SyntaxTree.Variable("b"))), "a", "b"),
							new SyntaxTree.Function("lastIndexOf", new SyntaxTree.Return(new SyntaxTree.IndexOf(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("a")).setLastIndexOf(true)), "a"),
							new SyntaxTree.Function("lastIndexOf", new SyntaxTree.Return(new SyntaxTree.IndexOf(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("a"), new SyntaxTree.Variable("b")).setLastIndexOf(true)), "a", "b"),
							new SyntaxTree.Function("get", new SyntaxTree.Return(new SyntaxTree.Get(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("index"))), "index"),
							new SyntaxTree.Function("getFromEnd", new SyntaxTree.Return(new SyntaxTree.Get(new SyntaxTree.Variable("data"), new SyntaxTree.Sub(new SyntaxTree.GetSize(new SyntaxTree.Variable("data")), new SyntaxTree.Add(new SyntaxTree.Number(1), new SyntaxTree.Variable("index"))))), "index"),
							new SyntaxTree.Function("getRandomItem", new SyntaxTree.Return(new SyntaxTree.Get(new SyntaxTree.Variable("data"), new SyntaxTree.CallFunction("randint", new SyntaxTree.Number(0), new SyntaxTree.GetSize(new SyntaxTree.Variable("data")))))),
							new SyntaxTree.Function("getFirstItem", new SyntaxTree.Return(new SyntaxTree.Get(new SyntaxTree.Variable("data"), new SyntaxTree.Number(0)))),
							new SyntaxTree.Function("getLastItem", new SyntaxTree.Return(new SyntaxTree.Get(new SyntaxTree.Variable("data"), new SyntaxTree.Sub(new SyntaxTree.GetSize(new SyntaxTree.Variable("data")), new SyntaxTree.Number(1))))),
							new SyntaxTree.Function("length", new SyntaxTree.Return(new SyntaxTree.GetSize(new SyntaxTree.Variable("data")))),
							new SyntaxTree.Function("isEmpty", new SyntaxTree.Return(new SyntaxTree.StrictEquals(new SyntaxTree.GetSize(new SyntaxTree.Variable("data")), new SyntaxTree.Number(0)))),
							new SyntaxTree.Function("reverse", new SyntaxTree.SetVariable("data", new SyntaxTree.Negative(new SyntaxTree.Variable("data")))),
							new SyntaxTree.Function("sort", new SyntaxTree.SetVariable("data", new SyntaxTree.Sort(new SyntaxTree.Variable("data"), false))),
							new SyntaxTree.Function("numericSort", new SyntaxTree.SetVariable("data", new SyntaxTree.Sort(new SyntaxTree.Variable("data"), true))),
							new SyntaxTree.Function("set", new SyntaxTree.SetVariable("data", new SyntaxTree.Set(new SyntaxTree.Variable("data"), new SyntaxTree.Variable("value"), new SyntaxTree.Variable("index"))), "index", "value")
					),
					(ProgramBase) result.getTokens().get(0).getObject()
			));
			if (addRandInt) SyntaxTree.deleteNativeFunction("random", "randint", 2);
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
			if (xmlOutput != null) {
				compiled = true;
				try {
					FileWriter writer = new FileWriter(xmlOutput);
					XMLGenerator xmlGenerator = new XMLGenerator();
					writer.write(xmlGenerator.syntaxTreeToXML((ProgramBase) result.getTokens().get(0).getObject()));
					writer.close();
				} catch (IOException e) {
					System.out.println("ERROR: can't create output file");
					e.printStackTrace();
				}
			}
			if (CustomCompileStep.used) {
				compiled |= CustomCompileStep.run((ProgramBase)result.getTokens().get(0).getObject());
			}
			if (!compiled) {
				((ProgramBase)result.getTokens().get(0).getObject()).eval();
				if (Targets.isWeb) System.out.println("\nCode Running Done");
			}
			SyntaxTree.deleteNativeFunction("input", "input", 0);
		} else {
			System.out.println("Syntax is:\n" + result);
			syntaxError("Syntax Error");
		}
	}

	public static void syntaxError(int errorChar, String line) {
		int lineStart = line.lastIndexOf("\n", errorChar) + 1;
		line = line.substring(0, line.indexOf("\n", errorChar));
		long lineNumber = line.codePoints().filter(ch -> ch == '\n').count() + 1;
		line = line.substring(lineStart);
		System.err.println("error at line: " + lineNumber);
		errorChar -= lineStart;
		System.err.println(line);
		for(; errorChar > 0; errorChar--) {
			System.err.print(" ");
		}
		System.err.println("^");
	}

	public static void syntaxError(String line) {
		if (Targets.isWeb) Targets.error("ERROR:\t" + line);
		System.err.println("ERROR:\t" + line);
	}
}
