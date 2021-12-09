import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Targets {
    public static final boolean isWeb = false;
    public static final boolean systemPrint = true;
    public static final boolean customWhile = false;
    public static final boolean isInThread = false;
    public static boolean awaitedInput = false;

    public static void tokenizerError(int i, String line) {
        //empty! this function used just when targeting web(via teaVM)
    }

    public static String readFile(String fileName) {
        File file = new File(fileName);
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.err.println("Failed to open file: " + fileName);
            System.exit(1);
        }
        try {
            scanner.useDelimiter("\\Z");
            return scanner.next() + "\n";
        } catch (NoSuchElementException ignored) {
            return "";
        }
    }

    public static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
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