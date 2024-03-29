import com.sun.tools.javac.util.StringUtils;

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
	private boolean isTestModeOn = false;
	static String testCode = "";
	private static final HashMap<String, SyntaxTree.Programs> importedFiles = new HashMap<>();
	private Parser lexResult;

	public boolean isIsTestModeOn() {
		return isTestModeOn;
	}

	public void setIsTestModeOn(boolean isTestModeOn) {
		this.isTestModeOn = isTestModeOn;
	}

	public Compiler(String fileName, boolean isShell, String compiledFileName, String classFileName, String serializeFileName, String xmlOutput) {
		this.fileName = fileName;
		this.isShell = isShell;
		this.compiledFileName = compiledFileName;
		this.serializeFileName = serializeFileName;
		this.classFileName = classFileName;
		this.xmlOutput = xmlOutput;
	}

	public String getInputCode() {
		if (isTestModeOn) {
			return testCode;
		}
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
		lexer.add("NUM", "\\d+(\\.\\d+)?");
		//Boolean
		lexer.add("BOOL", "true|false");
		//Null
		lexer.add("NULL", "null");
		//spaces (ignore)
		lexer.add("IGNORE", "(\t| |\\/\\/.*|\\/\\*(\\s*.?((?!\\/)))*\\*\\/)+");
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
		//for (keyword)
		lexer.add("FOR", "for ");
		//in (keyword)
		lexer.add("IN", "in ");
		//import (keyword)
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
		//extends (keyword)
		lexer.add("EXT", "extends ");
		//Parents (keyword)
		lexer.add("PAR", "Parents");
		//static (keyword)
		lexer.add("STATIC", "static ");
		//this (keyword)
		lexer.add("THIS", "this");
		//init (keyword)
		lexer.add("INIT", "init");
		//new (keyword)
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
		//range
		lexer.add("RANGE", "\\.\\.\\.");
		//dot
		lexer.add("DOT", "\\.");
		//separator
		lexer.add("SEP", new SeperatorChecker());
	}

	public void afterLex(Parser result) {
		lexResult = new Parser((ArrayList<Token>) result.getTokens().clone());
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
			} else if (getCounter() == 18) {
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
					if (parser.getTokens().get(i).getName().equals("FOR") && parser.getTokens().get(i + 1).getName().equals("OP_PAREN")) {
						int x = 0;
						int j = i + 1;
						boolean success = true;
						while (x != -1) {
							j++;
							if (j >= parser.getTokens().size()) {
								success = false;
								break;
							}
							if (parser.getTokens().get(j).getName().equals("OP_BRACKET") ||
									parser.getTokens().get(j).getName().equals("in")) {
								success = false;
								break;
							}
							if (parser.getTokens().get(j).getName().equals("CL_PAREN")) {
								x--;
							} else if (parser.getTokens().get(j).getName().equals("OP_PAREN")) {
								x++;
							}
						}
						if (success) parser.getTokens().get(j).setName("SEP");
					}
				}
			} else if (getCounter() == 7) {
				parser.setSingleRunPerLocation(false);
			} else if (getCounter() == 8) {
				parser.setSingleRunPerLocation(true);
			} else if (getCounter() == 9) {
				parser.setSingleRunPerLocation(false);
			} else if (getCounter() == 10) {
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
			if (Targets.fileExists(fileName)) {
				parsingImportedFile = true;
				ArrayList<ProgramBase> result1 = new ArrayList<>();
				filterImportedProgram(((ProgramBase) CompilerMain.compile(new Compiler(fileName, false, null, null, null, null)).getTokens().get(0).getObject()), result1);
				parsingImportedFile = false;
				ProgramBase[] resultArray = new ProgramBase[result1.size()];
				resultArray = result1.toArray(resultArray);
				importedFiles.put(fileName, new SyntaxTree.Programs(resultArray));
			} else if (!Targets.isWeb) {
				boolean found = false;
				for (Package aPackage : PackageManager.findPackageWithTags("library")) {
					if (aPackage.getName().equals(fileName)) {
						File srcDir = new File(PackageManager.getPackageDirectory(aPackage), "src");
						if (srcDir.isDirectory()) {
							parsingImportedFile = true;
							ArrayList<ProgramBase> result1 = new ArrayList<>();
							filterImportedProgram(((ProgramBase) CompilerMain.compile(new Compiler(new File(srcDir, aPackage.getName()).getPath(), false, null, null, null, null)).getTokens().get(0).getObject()), result1);
							parsingImportedFile = false;
							ProgramBase[] resultArray = new ProgramBase[result1.size()];
							resultArray = result1.toArray(resultArray);
							importedFiles.put(fileName, new SyntaxTree.Programs(resultArray));
							found = true;
						}
					}
				}
				if (!found) {
					syntaxError("Package " + fileName + " does not exist");
					System.exit(-1);
				}
			}
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

	@ParserEvent(map = "class : CLASS ID( EXT( ID( COMMA)?)+)?", priority = 7)
	public Object createClass(Parser parser) {
		String name = parser.getTokens().get(1).getText();
		SyntaxTree.getClassesParameters().put(name, null);
		SyntaxTree.getClassesParents().put(name, new ArrayList<>());
		for (int i = 3; i < parser.getTokens().size(); i += 2) {
			if (parser.getTokens().get(i).getName().equals("ID")) {
				SyntaxTree.getClassesParents().get(name).add(parser.getTokens().get(i).getText());
			}
		}
		return name;
	}

	@ParserEvent(map = "parinit : PAR DOT ID OP_PAREN", priority = 8)
	public Object initParent(Parser parser) {
		return parser.getTokens().get(2).getText();
	}

	@ParserEvent(map = "fn : (FN ID|INIT) OP_PAREN( vard| ID)?", priority = 9)
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

	@ParserEvent(map = "fn : fn COMMA (ID|vard)", priority = 10)
	public Object function2(Parser parser) {
		ArrayList<String> stringArrayList = (ArrayList<String>) parser.getTokens().get(0).getObject();
		if (parser.getTokens().get(2).getName().equals("ID")) stringArrayList.add(parser.getTokens().get(2).getText());
		else stringArrayList.add((String) parser.getTokens().get(2).getObject());
		return stringArrayList;
	}

	@ParserEvent(map = "new : NEW ID", priority = 11)
	public Object createInstance(Parser parser) {
		return parser.getTokens().get(1).getText();
	}

	@ParserEvent(map = "in : ID IN", priority = 12)
	public Object in(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "fnc : ID OP_PAREN", priority = 13)
	public Object functionCall(Parser parser) {
		return parser.getTokens().get(0).getText();
	}

	@ParserEvent(map = "lambda : OP_PAREN ID( COMMA ID)* CL_PAREN ARROW", priority = 14)
	public Object lambdaArgs(Parser parser) {
		ArrayList<String> arrayList = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("ID")) {
				arrayList.add(token.getText());
			}
		}
		return arrayList;
	}

	@ParserEvent(map = "inc : ID (OP2|OP1|OP3|E) SET", priority = 15)
	public Object increaseAndOthers(Parser parser) {
		return parser;
	}

	@ParserEvent(map = "exp : ID OP2 OP2|OP2 OP2 ID", priority = 16)
	public Object incrementAndDecrement(Parser parser) {
		if (parser.getTokens().get(0).getName().equals("ID")) {
			if (parser.getTokens().get(1).getText().equals("+"))
				return new SyntaxTree.Increase(new SyntaxTree.Variable(parser.getTokens().get(0).getText()), true);
			else
				return new SyntaxTree.Decrease(new SyntaxTree.Variable(parser.getTokens().get(0).getText()), true);
		} else {
			if (parser.getTokens().get(1).getText().equals("+"))
				return new SyntaxTree.Increase(new SyntaxTree.Variable(parser.getTokens().get(2).getText()), false);
			else
				return new SyntaxTree.Decrease(new SyntaxTree.Variable(parser.getTokens().get(2).getText()), false);
		}
	}

	@ParserEvent(map = "exp : PAR DOT ID", priority = 17)
	public Object parent(Parser parser) {
		return new SyntaxTree.Parent(parser.getTokens().get(2).getText());
	}

	@ParserEvent(map = "exp : (exp DOT )?ID|exp DOT (ID|exp)", priority = 18)
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

	@ParserEvent(map = "exp : exp E exp(?! DOT)", priority = 19)
	public Object exponentiation(Parser parser) {
		setCounter(16);
		return new SyntaxTree.Pow((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP1 exp(?! DOT)", priority = 20)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		setCounter(16);
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp( SEP)? OP2( SEP)? exp(?! DOT)", priority = 21)
	public Object additionAndSubtraction(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp( SEP)? COMP( SEP)? exp(?! DOT)", priority = 22)
	public Object comparison(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
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

	@ParserEvent(map = "exp : exp( SEP)? OP3( SEP)? exp(?! DOT)", priority = 23)
	public Object bitwiseAnd(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
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

	@ParserEvent(map = "program : parinit ((exp COMMA )*exp )?CL_PAREN SEP", priority = 24)
	public Object initParent1(Parser parser) {
		setCounter(16);
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
		return new SyntaxTree.InitParentClass((String) parser.getTokens().get(0).getObject(), new SyntaxTree.CreateInstance((String) parser.getTokens().get(0).getObject(), args));
	}

	@ParserEvent(map = "exp : new OP_PAREN ((exp COMMA )*exp )?CL_PAREN", priority = 25)
	public Object createInstance1(Parser parser) {
		setCounter(16);
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

	@ParserEvent(map = "program : exp (OP_SQ_BRACKET( SEP)* exp( SEP)* CL_SQ_BRACKET( SEP)* )+SET( SEP)* exp SEP", priority = 26)
	public Object setInArray(Parser parser) {
		setCounter(16);
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

	@ParserEvent(map = "exp : exp OP_SQ_BRACKET( SEP)* exp( SEP)* CL_SQ_BRACKET", priority = 27)
	public Object getFromArray(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		return new SyntaxTree.CallFunction("get", (ValueBase) parser.getTokens().get(2).getObject())
				.fromInstance((ValueBase)parser.getTokens().get(0).getObject()).setAddInstanceName(true);
	}

	@ParserEvent(map = "exp : (exp DOT )?fnc ((exp COMMA )*exp )?CL_PAREN", priority = 28)
	public Object functionCall2(Parser parser) {
		setCounter(16);
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
			return new SyntaxTree.PrintFunction(new SyntaxTree.Print(args[0], new SyntaxTree.Text("\n")).setSeparator(new SyntaxTree.Text("")));
		} else if ((parser.getTokens().get(0).getObject()).toString().equals("write") && args.length == 1) {
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

	@ParserEvent(map = "exp : exp RANGE exp", priority = 29)
	public Object range(Parser parser) {
		setCounter(16);
		return new SyntaxTree.CreateInstance("%Range", (ValueBase) parser.getTokens().get(0).getObject(), (ValueBase) parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : OP_SQ_BRACKET( SEP)*( exp( SEP)* COMMA( SEP)*)*( exp)?( SEP)* CL_SQ_BRACKET", priority = 30)
	public Object array(Parser parser) {
		setCounter(16);
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

	@ParserEvent(map = "exp : OP_PAREN exp CL_PAREN", priority = 31)
	public Object parentheses(Parser parser) {
		try {
			if (parser.getParent().getTokens().get(parser.getParent().getListIndex() - 1).getName().equals("!") &&
					(parser.getParent().getTokens().get(parser.getParent().getListIndex() - 2).getName().equals("CL_BRACKET") ||
							parser.getParent().getTokens().get(parser.getParent().getListIndex() - 2).getName().equals("exp"))) {
				setCounter(33);
				return null;
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {}
		setCounter(16);
		return parser.getTokens().get(1).getObject();
	}

	@ParserEvent(map = "exp : exp ! OP_PAREN (exp )?(COMMA exp )*CL_PAREN", priority = 32)
	public Object callFunctionFromPointer(Parser parser) {
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		boolean isFirst = true;
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("exp")) {
				if (isFirst) {
					isFirst = false;
					continue;
				}
				arrayList.add((ValueBase) token.getObject());
			}
		}
		ValueBase[] args = new ValueBase[arrayList.size()];
		args = arrayList.toArray(args);
		return new SyntaxTree.CallFunctionFromPointer((ValueBase) parser.getTokens().get(0).getObject(), args);
	}

	@ParserEvent(map = "program : inc exp( SEP)*", priority = 33)
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

	@ParserEvent(map = "exp : (!|~) exp", priority = 34)
	public Object not(Parser parser) {
		setCounter(16);
		if (parser.getTokens().get(0).getText().equals("!"))
			return new SyntaxTree.Not((ValueBase) parser.getTokens().get(1).getObject());
		else
			return new SyntaxTree.BitwiseNot((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : (((STATIC )?VAR )?set exp|(STATIC )?vard( exp)?|exp DOT set exp) SEP", priority = 35)
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
					boolean staticParameterExists = false;
					for (String string : SyntaxTree.staticParameters) {
						if (string.startsWith("#C" + finalName + "#" + parser.getTokens().get(2).getObject())) {
							staticParameterExists = true;
							break;
						}
					}
					if (staticParameterExists) {
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
			if (SyntaxTree.getClassesParameters().containsKey(parser.getTokens().get(0).getObject())){
				syntaxError("a class with this variable name was exist: " + parser.getTokens().get(0).getObject());
			}
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(), value, true, true).setStatic(isStatic);
		} else if (parser.getTokens().get(0).getName().equals("VAR")) {
			if (SyntaxTree.getClassesParameters().containsKey(parser.getTokens().get(1).getObject())){
				syntaxError("a class with this variable name was exist: " + parser.getTokens().get(1).getObject());
			}
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(1).getObject(),
					(ValueBase) parser.getTokens().get(2).getObject(), true, true).setStatic(isStatic);
		}
		return new SyntaxTree.SetVariable((String) parser.getTokens().get(0).getObject(),
				(ValueBase) parser.getTokens().get(1).getObject(), false, true);
	}

	@ParserEvent(map = "program : RET (exp )?SEP", priority = 36)
	public Object _return(Parser parser) {
		if (parser.getTokens().size() == 2) {
			return new SyntaxTree.Return(new SyntaxTree.Null());
		}
		return new SyntaxTree.Return((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : BR SEP", priority = 37)
	public Object _break(Parser parser) {
		return new SyntaxTree.Break();
	}

	@ParserEvent(map = "program : CON SEP", priority = 38)
	public Object _continue(Parser parser) {
		return new SyntaxTree.Continue();
	}

	@ParserEvent(map = "program : FOR OP_PAREN program exp( SEP)* program( CL_PAREN)? (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", priority = 39)
	public Object _for(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		parser.remove("CL_PAREN");
		parser.remove("OP_PAREN");
		return new SyntaxTree.For((ValueBase) parser.getTokens().get(2).getObject(), (ProgramBase) parser.getTokens().get(3).getObject(), (ProgramBase) parser.getTokens().get(1).getObject(), parser.getTokens().get(5).getName().equals("program")? (ProgramBase)parser.getTokens().get(5).getObject():new SyntaxTree.Programs());
	}

	@ParserEvent(map = "program : FOR (OP_PAREN )?in exp( CL_PAREN)? OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", priority = 40)
	public Object foreach(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		parser.remove("CL_PAREN");
		parser.remove("OP_PAREN");
		ValueBase instance = (ValueBase) parser.getTokens().get(2).getObject();
		return new SyntaxTree.While(new SyntaxTree.Boolean(true), new SyntaxTree.Programs(
				new SyntaxTree.If(new SyntaxTree.Not(new SyntaxTree.CallFunction("hasNext").fromInstance(instance)
						.setAddInstanceName(true)), new SyntaxTree.Break()),
				new SyntaxTree.SetVariable((String) parser.getTokens().get(1).getObject(),
						new SyntaxTree.CallFunction("next").fromInstance(instance).setAddInstanceName(true)).setIsDeclaration(true),
				parser.getTokens().get(4).getName().equals("program")? (ProgramBase) parser.getTokens().get(4).getObject():new SyntaxTree.Programs()));
	}

	@ParserEvent(map = "program : program (SEP )*(program ?)+", priority = 41)
	public Object programs(Parser parser) {
		try {
			if (parser.getParent().getTokens().get(parser.getParent().getListIndex() - 2).getName().equals("FOR")) {
				return null;
			}
		} catch (Exception ignored) {}
		parser.remove("SEP");
		ProgramBase[] programs = new ProgramBase[parser.getTokens().size()];
		for (int i = 0; i < programs.length; i++) {
			programs[i] = (ProgramBase) parser.getTokens().get(i).getObject();
		}
		return new SyntaxTree.Programs(programs);
	}

	@ParserEvent(map = "program : WH exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", priority = 42)
	public Object _while(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		return new SyntaxTree.While((ValueBase)parser.getTokens().get(1).getObject(), parser.getTokens().get(3).getName().equals("program")? (ProgramBase)parser.getTokens().get(3).getObject():new SyntaxTree.Programs());
	}

	@ParserEvent(map = "program : IF exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET( ELSE IF exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET)*( ELSE( SEP)* OP_BRACKET( SEP)* (program )?CL_BRACKET)? SEP", priority = 43)
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

	@ParserEvent(map = "program : (STATIC )?fn CL_PAREN (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", priority = 44)
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

	@ParserEvent(map = "exp : (OP_PAREN CL_PAREN ARROW|lambda) OP_BRACKET (SEP )*(program )?CL_BRACKET", priority = 45)
	public Object lambda(Parser parser) {
		setCounter(16);
		parser.remove("SEP");
		if (parser.getTokens().size() == 6 || parser.getTokens().size() == 5)
			return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(4).getName().equals("program")? (ProgramBase) parser.getTokens().get(4).getObject():new SyntaxTree.Programs()));
		String[] args = new String[((ArrayList<String>)parser.getTokens().get(0).getObject()).size()];
		args = ((ArrayList<String>)parser.getTokens().get(0).getObject()).toArray(args);
		return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(2).getName().equals("program")? (ProgramBase) parser.getTokens().get(2).getObject():new SyntaxTree.Programs(), args));
	}

	@ParserEvent(map = "program : class (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET", priority = 46)
	public Object createClass1(Parser parser) {
		setCounter(33);
		parser.remove("SEP");
		ProgramBase program =  parser.getTokens().get(2).getObject() instanceof ProgramBase? (ProgramBase) parser.getTokens().get(2).getObject() : new SyntaxTree.Programs();
		ArrayList<String> parents = SyntaxTree.getClassesParents().get((String) parser.getTokens().get(0).getObject());
		if (parents == null) parents = new ArrayList<>();
		for (String parent : parents) {
			if (!parentIsNotInitedInProgram(program, parent)) {
				SyntaxTree.InitParentClass initParentClass = new SyntaxTree.InitParentClass(parent, new SyntaxTree.CreateInstance(parent));
				initParentClass.getSetter().setIsDeclaration(true).setUseInstanceName(true);
				initParentClass.getSetter().getData().setInstanceName(initParentClass.getData().getInstanceName());
				program = new SyntaxTree.Programs(initParentClass, program);
			}
		}
		return new SyntaxTree.CreateClass((String) parser.getTokens().get(0).getObject(), parents, program);
	}

	private boolean parentIsNotInitedInProgram(ProgramBase program, String parentName) {
		if (program instanceof SyntaxTree.InitParentClass && ((SyntaxTree.InitParentClass) program).getClassName().equals(parentName)) {
			return true;
		} else if (program instanceof SyntaxTree.Programs) {
			boolean res = false;
			for (ProgramBase program1 : ((SyntaxTree.Programs) program).getPrograms()) {
				res |= parentIsNotInitedInProgram(program1, parentName);
			}
			return res;
		} else if (program instanceof SyntaxTree.Function) {
			return parentIsNotInitedInProgram(((SyntaxTree.Function) program).getProgram(), parentName);
		}
		return false;
	}

	@ParserEvent(map = "program : exp SEP", priority = 47)
	public Object executeValue(Parser parser) {
		try {
			if (parser.getParent().getTokens().get(parser.getParent().getListIndex() - 3).getName().equals("FOR")) {
				return null;
			}
		} catch (Exception ignored) {}
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
		result.on("program (SEP )*(program ?)+", "program", this::programs);
		if (result.getTokens().size() == 0) {
			if (CustomCompileStep.used) {
				CustomCompileStep.run(new SyntaxTree.Programs());
			}
			return ;
		}
		if (result.getTokens().size() != 1) {
			syntaxError(result);
			return ;
		}
		if (result.getTokens().get(0).getName().equals("program")) {
			if (!Targets.isWeb) {
				if (serializeFileName != null) {
					SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
					serializer.serialize(serializeFileName, (ProgramBase) result.getTokens().get(0).getObject());
				}
			}
			if (Targets.isWeb && Targets.awaitedInput) {
				SyntaxTree.deleteNativeFunction("input", "input", 0);
				SyntaxTree.deleteNativeFunction(".", "runJS_str", 1);
				SyntaxTree.deleteNativeFunction(".", "runJS_int", 1);
				SyntaxTree.declareNativeFunction("input", "input", 0);
				SyntaxTree.declareNativeFunction(".", "runJS_str", 1);
				SyntaxTree.declareNativeFunction(".", "runJS_int", 1);
				SyntaxTree.CallFunction input = new SyntaxTree.CallFunction("input");
				input.findFunction();
				SyntaxTree.CallFunction getRes = new SyntaxTree.CallFunction("runJS_str", new SyntaxTree.Text("__COMPILER_INPUT"));
				getRes.findFunction();
				SyntaxTree.CallFunction resExists = new SyntaxTree.CallFunction("runJS_int", new SyntaxTree.Text("try{__COMPILER_INPUT!=null}catch(e){false}"));
				resExists.findFunction();
				SyntaxTree.CallFunction reset = new SyntaxTree.CallFunction("runJS_int", new SyntaxTree.Text("__COMPILER_INPUT = null"));
				reset.findFunction();
				SyntaxTree.deleteNativeFunction("input", "input", 0);
				SyntaxTree.deleteNativeFunction(".", "runJS_str", 1);
				SyntaxTree.deleteNativeFunction(".", "runJS_int", 1);
				result.getTokens().get(0).setObject(new SyntaxTree.Programs(
						new SyntaxTree.Function("input", new SyntaxTree.Programs(new SyntaxTree.ExecuteValue(input), new SyntaxTree.While(new SyntaxTree.Not(resExists), new SyntaxTree.Programs()), new SyntaxTree.SetVariable("tmp", getRes), new SyntaxTree.ExecuteValue(reset), new SyntaxTree.Return(new SyntaxTree.Variable("tmp")))),
						(ProgramBase) result.getTokens().get(0).getObject()
				));
			} else {
				SyntaxTree.declareNativeFunction("input", "input", 0);
			}
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
					new SyntaxTree.CreateClass("%Range",
						new SyntaxTree.SetVariable("from", new SyntaxTree.Null()).setIsDeclaration(true),
							new SyntaxTree.SetVariable("to", new SyntaxTree.Null()).setIsDeclaration(true),
							new SyntaxTree.SetVariable("i", new SyntaxTree.Null()).setIsDeclaration(true),
						new SyntaxTree.Function("<init>", new SyntaxTree.Programs(new SyntaxTree.SetVariable("from",
								new SyntaxTree.Variable("a")).fromInstance(new SyntaxTree.This()).setAddInstanceName(true),
								new SyntaxTree.SetVariable("to", new SyntaxTree.Variable("b"))
										.fromInstance(new SyntaxTree.This()).setAddInstanceName(true),
								new SyntaxTree.SetVariable("i", new SyntaxTree.Variable("a"))
										.fromInstance(new SyntaxTree.This()).setAddInstanceName(true)), "a", "b"),
						new SyntaxTree.Function("next", new SyntaxTree.If(new SyntaxTree.GreaterThanOrEqual(new SyntaxTree.Variable("to"), new SyntaxTree.Variable("from")), new SyntaxTree.Return(new SyntaxTree.Increase(new SyntaxTree.Variable("i"), true))).addElse(new SyntaxTree.Return(new SyntaxTree.Decrease(new SyntaxTree.Variable("i"), true)))),
						new SyntaxTree.Function("hasNext", new SyntaxTree.If(new SyntaxTree.GreaterThanOrEqual(new SyntaxTree.Variable("to"), new SyntaxTree.Variable("from")), new SyntaxTree.Return(new SyntaxTree.LesserThanOrEqual(new SyntaxTree.Variable("i"), new SyntaxTree.Variable("to")))).addElse(new SyntaxTree.Return(new SyntaxTree.GreaterThanOrEqual(new SyntaxTree.Variable("i"), new SyntaxTree.Variable("to")))))
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
			}
			SyntaxTree.deleteNativeFunction("input", "input", 0);
		} else {
			syntaxError(result);
		}
	}

	private int count(String find, String in) {
		int count = 0;
		int lastIndex = 0;
		while (lastIndex != -1) {
			lastIndex = in.indexOf(find,lastIndex);
			if (lastIndex != -1) {
				count ++;
				lastIndex += find.length();
			}
		}
		return count;
	}

	private void syntaxError(Parser result) {
		boolean foundError = false;
		String map = lexResult.getMap();
		if (count("OP_BRACKET", map) != count("CL_BRACKET", map)) {
			syntaxError("The number of open brackets('{') and close brackets('}') do not match");
			int openedOnes = 0;
			Token lastBracket = null;
			for (Token token : lexResult.getTokens()) {
				if (token.getName().equals("OP_BRACKET")) {
					openedOnes++;
					lastBracket = token;
				} else if (token.getName().equals("CL_BRACKET")) {
					openedOnes--;
					lastBracket = token;
				}
				if (openedOnes == -1) {
					if (lastBracket != null) {
						syntaxError("\tthere is an extra '}' in line: " + lastBracket.getLine());
						break;
					}
				}
			}
			if (openedOnes > 0) {
				if (lastBracket != null) syntaxError("\tthe bracket in line " + lastBracket.getLine() + " was not closed");
			}
			foundError = true;
		}
		if (count("OP_PAREN", map) != count("CL_PAREN", map)) {
			syntaxError("The number of open parentheses('(') and close parentheses(')') do not match");
			int openedOnes = 0;
			Token lastParen = null;
			for (Token token : lexResult.getTokens()) {
				if (token.getName().equals("OP_PAREN")) {
					openedOnes++;
					lastParen = token;
				} else if (token.getName().equals("CL_PAREN")) {
					openedOnes--;
					lastParen = token;
				}
				if (openedOnes == -1) {
					if (lastParen != null) {
						syntaxError("\tthere is an extra ')' in line: " + lastParen.getLine());
						break;
					}
				}
			}
			if (openedOnes > 0) {
				if (lastParen != null) syntaxError("\tthe parentheses in line " + lastParen.getLine() + " was not closed");
			}
			foundError = true;
		}
		if (count("OP_SQ_BRACKET", map) != count("CL_SQ_BRACKET", map)) {
			syntaxError("The number of open square brackets('[') and close parentheses(']') do not match");
			int openedOnes = 0;
			Token lastParen = null;
			for (Token token : lexResult.getTokens()) {
				if (token.getName().equals("OP_SQ_BRACKET")) {
					openedOnes++;
					lastParen = token;
				} else if (token.getName().equals("CL_SQ_BRACKET")) {
					openedOnes--;
					lastParen = token;
				}
				if (openedOnes == -1) {
					if (lastParen != null) {
						syntaxError("\tthere is an extra ']' in line: " + lastParen.getLine());
						break;
					}
				}
			}
			if (openedOnes > 0) {
				if (lastParen != null) syntaxError("\tthe square bracket in line " + lastParen.getLine() + " was not closed");
			}
			foundError = true;
		}
		for (Token token : result.getTokens()) {
			if ("FOR".equals(token.getName())) {
				syntaxError("there is an incomplete 'for' statement in line " + token.getLine());
				foundError = true;
				for (int i = 0; i < lexResult.getTokens().size(); i++) {
					if (lexResult.getTokens().get(i) == token) {
						boolean isOldStyled = true;
						for (int j = 1; j < 6; j++) {
							if (lexResult.getTokens().get(i + j).getName().equals("IN")) {
								isOldStyled = false;
								break;
							}
						}
						if (!isOldStyled && lexResult.getTokens().get(i + 1).getName().equals("VAR")) {
							syntaxError("perhaps it's because there is an extra 'var' after 'for' (it must be like: for i in 1...10)");
						} else if (isOldStyled && !lexResult.getTokens().get(i + 1).getName().equals("OP_PAREN")) {
							syntaxError("perhaps it's because there is no '(' after 'for'");
						}
					}
				}
				break;
			}
		}
		if (!foundError) System.out.println("ERROR: There is a syntax error in the code\nHere is the parsed version of it:\n" + result);
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
