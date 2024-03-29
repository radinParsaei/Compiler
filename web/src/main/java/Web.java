import java.math.BigDecimal;

public class Web {
    public static void parse(Parser parser, Compiler compiler) {
        parser.on("SEP SEP", "SEP", (parser1) -> null);
        parser.setSaveTexts(true);
        for (compiler.setCounter(0); compiler.getCounter() < 48; compiler.increaseCounter()) {
            if (compiler.getCounter() == 0) parser.on("IMPORT TXT SEP", "program", compiler::_import);
            if (compiler.getCounter() == 1) parser.on("NUM", "exp", compiler::number);
            if (compiler.getCounter() == 2) parser.on("TXT", "exp", compiler::text);
            if (compiler.getCounter() == 3) parser.on("BOOL", "exp", compiler::booleanExpression);
            if (compiler.getCounter() == 4) parser.on("NULL", "exp", compiler::nullExpression);
            if (compiler.getCounter() == 5) parser.on("ID SET", "set", compiler::set);
            if (compiler.getCounter() == 6) parser.on("VAR ID", "vard", compiler::varDeclaration);
            if (compiler.getCounter() == 7) parser.on("CLASS ID( EXT( ID( COMMA)?)+)?", "class", compiler::createClass);
            if (compiler.getCounter() == 8) parser.on("PAR DOT ID OP_PAREN", "parinit", compiler::initParent);
            if (compiler.getCounter() == 9) {
                parser.setSingleRunPerLocation(false);
                parser.on("(FN ID|INIT) OP_PAREN( vard| ID)?", "fn", compiler::function);
                parser.setSingleRunPerLocation(true);
            }
            if (compiler.getCounter() == 10) parser.on("fn COMMA (ID|vard)", "fn", compiler::function2);
            if (compiler.getCounter() == 11) parser.on("NEW ID", "new", compiler::createInstance);
            if (compiler.getCounter() == 12) parser.on("ID IN", "in", compiler::in);
            if (compiler.getCounter() == 13) parser.on("ID OP_PAREN", "fnc", compiler::functionCall);
            if (compiler.getCounter() == 14) parser.on("OP_PAREN ID( COMMA ID)* CL_PAREN ARROW", "lambda", compiler::lambdaArgs);
            if (compiler.getCounter() == 15) parser.on("ID (OP2|OP1|OP3|E) SET", "inc", compiler::increaseAndOthers);
            if (compiler.getCounter() == 16) parser.on("ID OP2 OP2|OP2 OP2 ID", "exp", compiler::incrementAndDecrement);
            if (compiler.getCounter() == 17) parser.on("PAR DOT ID", "exp", compiler::parent);
            if (compiler.getCounter() == 18) {
                parser.on("(exp DOT )?ID|exp DOT (ID|exp)", "exp", compiler::variable);
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
            }
            if (compiler.getCounter() == 19) parser.on("exp E exp(?! DOT)", "exp", compiler::exponentiation);
            if (compiler.getCounter() == 20) parser.on("exp OP1 exp(?! DOT)", "exp", compiler::multiplyAndDivideAndRemainder);
            if (compiler.getCounter() == 21) parser.on("exp OP2 exp(?! DOT)", "exp", compiler::additionAndSubtraction);
            if (compiler.getCounter() == 22) parser.on("exp COMP exp(?! DOT)", "exp", compiler::comparison);
            if (compiler.getCounter() == 23) parser.on("exp OP3 exp(?! DOT)", "exp", compiler::bitwiseAnd);
            if (compiler.getCounter() == 24) parser.on("parinit ((exp COMMA )*exp )?CL_PAREN SEP", "program", compiler::initParent1);
            if (compiler.getCounter() == 25) parser.on("new OP_PAREN ((exp COMMA )*exp )?CL_PAREN", "exp", compiler::createInstance1);
            if (compiler.getCounter() == 26) parser.on("exp (OP_SQ_BRACKET( SEP)* exp( SEP)* CL_SQ_BRACKET( SEP)* )+SET( SEP)* exp SEP", "program", compiler::setInArray);
            if (compiler.getCounter() == 27) parser.on("exp OP_SQ_BRACKET( SEP)* exp( SEP)* CL_SQ_BRACKET", "exp", compiler::getFromArray);
            if (compiler.getCounter() == 28) {
                parser.on("(exp DOT )?fnc ((exp COMMA )*exp )?CL_PAREN", "exp", compiler::functionCall2);
            }
            if (compiler.getCounter() == 29) parser.on("exp RANGE exp", "exp", compiler::range);
            if (compiler.getCounter() == 30) parser.on("OP_SQ_BRACKET( SEP)*( exp( SEP)* COMMA( SEP)*)*( exp)?( SEP)* CL_SQ_BRACKET", "exp", compiler::array);
            if (compiler.getCounter() == 31) parser.on("exp ! OP_PAREN (exp )?(COMMA exp )*CL_PAREN", "exp", compiler::callFunctionFromPointer);
            if (compiler.getCounter() == 32) parser.on("OP_PAREN exp CL_PAREN", "exp", compiler::parentheses);
            if (compiler.getCounter() == 33) parser.on("inc exp( SEP)*", "program", compiler::increaseAndOthers1);
            if (compiler.getCounter() == 34) {
                parser.on("(!|~) exp", "exp", compiler::not);
            }
            if (compiler.getCounter() == 35) {
                parser.on("(((STATIC )?VAR )?set exp|(STATIC )?vard( exp)?|exp DOT set exp) SEP", "program", compiler::setVariable);
            }
            if (compiler.getCounter() == 36) parser.on("RET (exp )?SEP", "program", compiler::_return);
            if (compiler.getCounter() == 37) parser.on("BR SEP", "program", compiler::_break);
            if (compiler.getCounter() == 38) parser.on("CON SEP", "program", compiler::_continue);
            if (compiler.getCounter() == 39)
                parser.on("FOR OP_PAREN program exp( SEP)* program( CL_PAREN)? (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", "program", compiler::_for);
            if (compiler.getCounter() == 40)
                parser.on("FOR (OP_PAREN )?in exp( CL_PAREN)? OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", "program", compiler::foreach);
            if (compiler.getCounter() == 41) parser.on("program (SEP )*(program ?)+", "program", compiler::programs);
            if (compiler.getCounter() == 42)
                parser.on("WH exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", "program", compiler::_while);
            if (compiler.getCounter() == 43)
                parser.on("IF exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET( ELSE IF exp (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET)*( ELSE( SEP)* OP_BRACKET( SEP)* (program )?CL_BRACKET)? SEP", "program", compiler::_if);
            if (compiler.getCounter() == 44)
                parser.on("(STATIC )?fn CL_PAREN (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET SEP", "program", compiler::funcDeclaration);
            if (compiler.getCounter() == 45) parser.on("(OP_PAREN CL_PAREN ARROW|lambda) OP_BRACKET (SEP )*(program )?CL_BRACKET", "exp", compiler::lambda);
            if (compiler.getCounter() == 46) parser.on("class (SEP )*OP_BRACKET (SEP )*(program )?CL_BRACKET", "program", compiler::createClass1);
            if (compiler.getCounter() == 47) parser.on("exp SEP", "program", compiler::executeValue);
        }
    }
}
