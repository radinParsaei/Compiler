public class Errors {
  private static String getFunctionName(String functionName) {
    String[] splitFunctionName = functionName.split(":");
    String name = splitFunctionName[0];
    StringBuilder res = new StringBuilder(name).append("(");
    int parameters = 0;
    if (splitFunctionName.length > 1) {
      boolean isFirst = true;
      for (String string : splitFunctionName[1].split(",")) {
        if (isFirst) {
          isFirst = false;
          continue;
        }
        parameters++;
        res.append(string).append(",");
      }
      res.replace(res.length() - 1, res.length(), "");
    }
    res.append(")").append(" (").append(name).append(" with ").append(parameters).append(" parameters)");
    return res.toString();
  }
  static void error(byte errorCode, String... args) {
    switch (errorCode) {
      case ErrorCodes.ERROR_VARIABLE_DOES_NOT_EXISTS:
        Compiler.syntaxError("Variable " + args[0] + " does not exists");
        break;
      case ErrorCodes.ERROR_FUNCTION_DOES_NOT_EXISTS:
        Compiler.syntaxError("Function " + args[0].split(":")[0] + " does not exists");
        break;
      case ErrorCodes.ERROR_FUNCTION_REDECLARATION:
        Compiler.syntaxError("Function " + getFunctionName(args[0]) + " already exists");
        break;
      case ErrorCodes.ERROR_ARGS_NOT_MATCH:
        Compiler.syntaxError("arguments not match for " + args[0].split(":")[0]);
        break;
      case ErrorCodes.ERROR_VARIABLE_REDECLARATION:
        Compiler.syntaxError("Variable " + args[0].split(":")[0] + " already exists");
        break;
      case ErrorCodes.ERROR_VARIABLE_NOT_DECLARED:
        Compiler.syntaxError("use of undeclared variable " + args[0]);
        break;
      case ErrorCodes.ERROR_TYPE:
        if (args.length != 0) Compiler.syntaxError("Type Error:\t" + args[0]);
        else Compiler.syntaxError("Type Error");
        break;
    }
    if (!Targets.isWeb) System.exit(1);
    else new SyntaxTree.Exit(null).eval();
  }
}
