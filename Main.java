import java.util.Scanner;

public class Main extends CompilerMain {
	public static void main(String[] args) {
		ArgParser argParser = new ArgParser(args);
		String fileName;
		try {
			fileName = argParser.getArgs().get(0);
		} catch (IndexOutOfBoundsException e) {
			fileName = "";
		}
		if (fileName.endsWith(".ser")) {
			SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
			serializer.deserialize(fileName).eval();
			System.exit(0);
		}
		String outputFileName = argParser.getValues().get("output");
		String serializeFileName = argParser.getValues().get("serialize");
		if (serializeFileName != null && !serializeFileName.endsWith(".ser")) {
			serializeFileName += ".ser";
		}
		if (fileName.equals(outputFileName) && !argParser.getItems().contains("y")) {
			System.err.print("input and output files are the same file. continue? [y/n] ");
			Scanner scanner = new Scanner(System.in);
			if (!scanner.next().equals("y")) {
				System.out.println();
				System.exit(0);
			}
			System.out.println();
		}
		if (serializeFileName != null && serializeFileName.equals(outputFileName) && !argParser.getItems().contains("y")) {
			System.err.print("serialize and output files are the same file. continue? [y/n] ");
			Scanner scanner = new Scanner(System.in);
			if (!scanner.next().equals("y")) {
				System.out.println();
				System.exit(0);
			}
			System.out.println();
		}
		Compiler compiler = new Compiler(fileName, argParser.getItems().contains("shell"), outputFileName, serializeFileName);
		do {
			if (argParser.getItems().contains("lex")) {
				System.out.println(lex(compiler));
			} else {
				try {
					compile(compiler);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Please enter file name");
					System.exit(1);
				}
			}
		} while (argParser.getItems().contains("shell"));
	}
}
