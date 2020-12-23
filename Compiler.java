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
		if (!Targets.isWeb) {
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
		} else {
			return "";
		}
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
		//lambda arrow
		lexer.add("ARROW", "->");
		//operators
		lexer.add("COMP", "!==|===|!=|<=|>=|==|<|>");
		lexer.add("SET", "=");
		lexer.add("E", "\\*\\*");//exponentiation
		lexer.add("OP1", "\\*|\\/|%");
		lexer.add("OP2", "\\-|\\+");
		lexer.add("OP3", "\\|\\||\\||and|&&|&|or");
		//while (keyword)
		lexer.add("WH", "while ");
		lexer.add("FOR", "for ");
		//exclamation
		lexer.add("!", "!");
		//if (keyword)
		lexer.add("IF", "if ");
		//else (keyword)
		lexer.add("ELSE", "else");
		//class (keyword)
		lexer.add("CLASS", "class ");
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
	}

	public void parse(Parser parser) {
		if (!Targets.isWeb) {
			if (getCounter() == 0) {
				parser.on("SEP SEP", "SEP", (parser1) -> null);
				parser.setSaveTexts(true);
			} else if (getCounter() == 14) {
				for (int i = 0; i < parser.getTokens().size(); i++) {
					if (parser.getTokens().get(i).getName().equals("OP2")
							&& (i == 0 || (!parser.getTokens().get(i - 1).getName().equals("exp")
							&& !parser.getTokens().get(i - 1).getName().equals("CL_PAREN")))) {
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

	@ParserEvent(map = "class : CLASS ID", priority = 6)
	public Object createClass(Parser parser) {
		return parser.getTokens().get(1).getText();
	}

	@ParserEvent(map = "fn : (FN ID|INIT) OP_PAREN( vard| ID)?", priority = 7)
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

	@ParserEvent(map = "fn : fn COMMA (ID|vard)", priority = 8)
	public Object function2(Parser parser) {
		ArrayList<String> stringArrayList = (ArrayList<String>) parser.getTokens().get(0).getObject();
		if (parser.getTokens().get(2).getName().equals("ID")) stringArrayList.add(parser.getTokens().get(2).getText());
		else stringArrayList.add((String) parser.getTokens().get(2).getObject());
		return stringArrayList;
	}

	@ParserEvent(map = "exp : ID ! OP_PAREN (exp )?(COMMA exp )*CL_PAREN", priority = 9)
	public Object callFunctionFromPointer(Parser parser) {
		ArrayList<ValueBase> arrayList = new ArrayList<>();
		for (Token token : parser.getTokens()) {
			if (token.getName().equals("exp")) {
				arrayList.add((ValueBase) token.getObject());
			}
		}
		ValueBase[] args = new ValueBase[arrayList.size()];
		args = arrayList.toArray(args);
		return new SyntaxTree.CallFunctionFromPointer(new SyntaxTree.Variable(parser.getTokens().get(0).getText()), args);
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
		setCounter(8);
		parser.remove("DOT");
		parser.setSaveTexts(true);
		if (parser.getTokens().size() == 2) {
			return new SyntaxTree.Variable(parser.getTokens().get(1).getText()).fromInstance((ValueBase) parser.getTokens().get(0).getObject())
					.setAddInstanceName(true);
		}
		return new SyntaxTree.Variable(parser.getTokens().get(0).getText());
	}

	@ParserEvent(map = "exp : exp E exp", priority = 15)
	public Object exponentiation(Parser parser) {
		setCounter(8);
		return new SyntaxTree.Pow((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP1 exp", priority = 16)
	public Object multiplyAndDivideAndRemainder(Parser parser) {
		setCounter(8);
		if (parser.getTokens().get(1).getText().equals("*")) {
			return new SyntaxTree.Mul((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		if (parser.getTokens().get(1).getText().equals("%")) {
			return new SyntaxTree.Mod((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Div((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp OP2 exp", priority = 17)
	public Object additionAndSubtraction(Parser parser) {
		setCounter(8);
		if (parser.getTokens().get(1).getText().equals("+")) {
			return new SyntaxTree.Add((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
		}
		return new SyntaxTree.Sub((ValueBase)parser.getTokens().get(0).getObject(), (ValueBase)parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "exp : exp COMP exp", priority = 18)
	public Object comparison(Parser parser) {
		setCounter(8);
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

	@ParserEvent(map = "exp : exp OP3 exp", priority = 19)
	public Object bitwiseAnd(Parser parser) {
		setCounter(8);
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

	@ParserEvent(map = "exp : new OP_PAREN ((exp COMMA )*exp )?CL_PAREN", priority = 20)
	public Object createInstance1(Parser parser) {
		setCounter(8);
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

	@ParserEvent(map = "exp : OP_PAREN exp CL_PAREN", priority = 21)
	public Object parentheses(Parser parser) {
		setCounter(8);
		return parser.getTokens().get(1).getObject();
	}

	@ParserEvent(map = "program : inc exp( SEP)?", priority = 22)
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
			case "**":
				return new SyntaxTree.SetVariable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText(), new SyntaxTree.Pow(new SyntaxTree
						.Variable(((Parser) parser.getTokens().get(0).getObject()).getTokens().get(0).getText()), (ValueBase) parser.getTokens().get(1).getObject()));
			default:
				syntaxError("Use +=, -=, *=, /=, %=, **=, &=, |=");
				if (!Targets.isWeb) System.exit(1);
		}
		return null;
	}

	@ParserEvent(map = "exp : (exp DOT )?fnc ((exp COMMA )*exp )?CL_PAREN", priority = 23)
	public Object functionCall2(Parser parser) {
		setCounter(8);
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
		if (parser.getTokens().get(0).getName().equals("exp"))
			res =  new SyntaxTree.CallFunction((String) parser.getTokens().get(2).getObject(), args)
					.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
		else
			res =  new SyntaxTree.CallFunction((String) parser.getTokens().get(0).getObject(), args);
		if (parser.getTokens().get(0).getName().equals("exp")) {
			res.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
		}
		return res;
	}

	@ParserEvent(map = "program : ((VAR )?set exp|vard( exp)?|exp DOT set exp) SEP", priority = 24)
	public Object setVariable(Parser parser) {
		if (parser.getTokens().get(0).getName().equals("exp")) {
			return new SyntaxTree.SetVariable((String) parser.getTokens().get(2).getObject()
					, (ValueBase) parser.getTokens().get(3).getObject())
					.fromInstance((ValueBase) parser.getTokens().get(0).getObject()).setAddInstanceName(true);
		}
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

	@ParserEvent(map = "program : PRINT exp SEP", priority = 25)
	public Object print(Parser parser) {
		return new SyntaxTree.Print((ValueBase)parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : RET (exp )?SEP", priority = 26)
	public Object _return(Parser parser) {
		if (parser.getTokens().size() == 2) {
			return new SyntaxTree.Return(new SyntaxTree.Null());
		}
		return new SyntaxTree.Return((ValueBase) parser.getTokens().get(1).getObject());
	}

	@ParserEvent(map = "program : BR SEP", priority = 27)
	public Object _break(Parser parser) {
		return new SyntaxTree.Break();
	}

	@ParserEvent(map = "program : CON SEP", priority = 28)
	public Object _continue(Parser parser) {
		return new SyntaxTree.Continue();
	}

	@ParserEvent(map = "program : program (SEP )?(program ?)+", priority = 29)
	public Object programs(Parser parser) {
		parser.remove("SEP");
		ProgramBase[] programs = new ProgramBase[parser.getTokens().size()];
		for (int i = 0; i < programs.length; i++) {
			programs[i] = (ProgramBase) parser.getTokens().get(i).getObject();
		}
		return new SyntaxTree.Programs(programs);
	}

	@ParserEvent(map = "program : WH exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 30)
	public Object _while(Parser parser) {
		setCounter(29);
		parser.remove("SEP");
		return new SyntaxTree.While((ValueBase)parser.getTokens().get(1).getObject(), parser.getTokens().get(3).getName().equals("program")? (ProgramBase)parser.getTokens().get(3).getObject():new SyntaxTree.Programs());
	}

	@ParserEvent(map = "program : FOR (OP_PAREN )?program exp SEP program (CL_PAREN )?(SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 31)
	public Object _for(Parser parser) {
		setCounter(29);
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

	@ParserEvent(map = "program : IF exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET( ELSE IF exp (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET)*( ELSE( SEP)? OP_BRACKET( SEP)? (program )?CL_BRACKET)? SEP", priority = 32)
	public Object _if(Parser parser) {
		setCounter(29);
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

	@ParserEvent(map = "program : fn CL_PAREN (SEP )?OP_BRACKET (SEP )?(program )?CL_BRACKET SEP", priority = 33)
	public Object funcDeclaration(Parser parser) {
		setCounter(29);
		parser.remove("SEP");
		ArrayList<String> stringArrayList = (ArrayList<String>)parser.getTokens().get(0).getObject();
		String functionName = stringArrayList.get(0);
		stringArrayList.remove(0);
		String[] args = new String[stringArrayList.size()];
		for (int i = 0; i < stringArrayList.size(); i++) {
			args[i] = stringArrayList.get(i);
		}
		return new SyntaxTree.Function(functionName,
				parser.getTokens().get(3).getName().equals("program")?(ProgramBase)parser.getTokens().get(3).getObject():new SyntaxTree.Programs(), args);
	}

	@ParserEvent(map = "exp : (OP_PAREN CL_PAREN ARROW|lambda) OP_BRACKET (SEP )?(program )?CL_BRACKET", priority = 34)
	public Object lambda(Parser parser) {
		setCounter(8);
		parser.remove("SEP");
		if (parser.getTokens().size() == 6 || parser.getTokens().size() == 5)
			return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(4).getName().equals("program")? (ProgramBase) parser.getTokens().get(4).getObject():new SyntaxTree.Programs()));
		String[] args = new String[((ArrayList<String>)parser.getTokens().get(0).getObject()).size()];
		args = ((ArrayList<String>)parser.getTokens().get(0).getObject()).toArray(args);
		return new SyntaxTree.Lambda(new SyntaxTree.CreateLambda(parser.getTokens().get(2).getName().equals("program")? (ProgramBase) parser.getTokens().get(2).getObject():new SyntaxTree.Programs(), args));
	}

	@ParserEvent(map = "program : class (SEP )?OP_BRACKET (SEP )?program CL_BRACKET", priority = 35)
	public Object createClass1(Parser parser) {
		setCounter(29);
		parser.remove("SEP");
		return new SyntaxTree.CreateClass((String) parser.getTokens().get(0).getObject(), (ProgramBase) parser.getTokens().get(2).getObject());
	}

	@ParserEvent(map = "program : exp SEP", priority = 36)
	public Object executeValue(Parser parser) {
		setCounter(29);
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
			if (!Targets.isWeb) {
				if (serializeFileName != null) {
					SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
					serializer.serialize(serializeFileName, (ProgramBase) result.getTokens().get(0).getObject());
				}
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
			if (CustomCompileStep.used) {
				compiled |= CustomCompileStep.run((ProgramBase)result.getTokens().get(0).getObject());
			}
			if (!compiled) {
				((ProgramBase)result.getTokens().get(0).getObject()).eval();
				if (Targets.isWeb) System.out.println("\nCode Running Done");
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
