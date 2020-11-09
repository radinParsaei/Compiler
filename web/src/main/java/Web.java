public class Web {
    public static void parse(Parser parser, Compiler compiler) {
        parser.on("SEP SEP", "SEP", (parser1) -> null);
        for (compiler.setCounter(0); compiler.getCounter() < 29; compiler.increaseCounter()) {
            if (compiler.getCounter() == 0) parser.on("NUM", "exp", compiler::number);
            if (compiler.getCounter() == 1) parser.on("TXT", "exp", compiler::text);
            if (compiler.getCounter() == 2) parser.on("BOOL", "exp", compiler::booleanExpression);
            if (compiler.getCounter() == 3) parser.on("NULL", "exp", compiler::nullExpression);
            if (compiler.getCounter() == 4) parser.on("ID SET", "set", compiler::set);
            if (compiler.getCounter() == 5) parser.on("VAR ID", "vard", compiler::varDeclaration);
            if (compiler.getCounter() == 6) {
                parser.setSingleRunPerLocation(false);
                parser.on("FN ID OP_PAREN( vard| ID)?", "fn", compiler::function);
                parser.setSingleRunPerLocation(true);
            }
            if (compiler.getCounter() == 7) parser.on("fn COMMA (ID|vard)", "fn", compiler::function2);
            if (compiler.getCounter() == 8) parser.on("ID OP_PAREN", "fnc", compiler::functionCall);
            if (compiler.getCounter() == 9) {
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
                parser.on("ID", "exp", compiler::variable);
            }
            if (compiler.getCounter() == 10) parser.on("exp OP1 exp", "exp", compiler::multiplyAndDivideAndRemainder);
            if (compiler.getCounter() == 11) parser.on("exp OP2 exp", "exp", compiler::additionAndSubtraction);
            if (compiler.getCounter() == 12) parser.on("exp COMP exp", "exp", compiler::comparison);
            if (compiler.getCounter() == 13) parser.on("exp OP3 exp", "exp", compiler::bitwiseAnd);
            if (compiler.getCounter() == 14) parser.on("OP_PAREN exp CL_PAREN", "exp", compiler::parentheses);
            if (compiler.getCounter() == 15) {
                parser.on("((VAR )?set exp|vard( exp)?) SEP", "program", compiler::setVariable);
            }
            if (compiler.getCounter() == 16) {
                parser.setSingleRunPerLocation(false);
                parser.on("PRINT exp SEP", "program", compiler::print);
                parser.setSingleRunPerLocation(true);
            }
            if (compiler.getCounter() == 17) parser.on("RET (exp )?SEP", "program", compiler::_return);
            if (compiler.getCounter() == 18) parser.on("fnc exp", "fnc", compiler::functionCall2);
            if (compiler.getCounter() == 19) parser.on("fnc COMMA exp", "fnc", compiler::functionCall3);
            if (compiler.getCounter() == 20) parser.on("program (SEP )?(program ?)+", "program", compiler::programs);
            if (compiler.getCounter() == 21)
                parser.on("WH exp (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", "program", compiler::_while);
            if (compiler.getCounter() == 22)
                parser.on("IF exp (SEP )?OP_BRACKET (SEP )?program CL_BRACKET", "ifprogram", compiler::_if);
            if (compiler.getCounter() == 23) parser.on("ifprogram (SEP )?ELSE ifprogram", "ifprogram", compiler::elseif);
            if (compiler.getCounter() == 24)
                parser.on("ifprogram (SEP )?ELSE (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", "program", compiler::_else);
            if (compiler.getCounter() == 25) parser.on("ifprogram SEP", "program", compiler::ifToProgram);
            if (compiler.getCounter() == 26)
                parser.on("fn CL_PAREN (SEP )?OP_BRACKET (SEP )?program CL_BRACKET SEP", "program", compiler::funcDeclaration);
            if (compiler.getCounter() == 27) parser.on("fnc CL_PAREN", "exp", compiler::functionCall4);
            if (compiler.getCounter() == 28) parser.on("exp SEP", "program", compiler::executeValue);
        }
    }
}
