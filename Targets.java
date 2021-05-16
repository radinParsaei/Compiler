public class Targets {
    public static final boolean isWeb = false;
    public static final boolean systemPrint = true;
    public static final boolean customWhile = false;
    public static final boolean isInThread = false;
    public static final boolean useAnalyzer = false;

    public static void tokenizerError(int i, String line) {
        //empty! this function used just when targeting web(via teaVM)
    }

    public interface CustomWhileInterface {
        boolean run();
    }

    public static void _while(CustomWhileInterface customWhileInterface) {
        while (customWhileInterface.run());
    }

    public static void print(ValueBase value) {}
    public static void error(String value) {}
}