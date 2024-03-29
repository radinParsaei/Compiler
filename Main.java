import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;

public class Main extends CompilerMain {
	public static void main(String[] args) {
		ArgParser argParser = new ArgParser(args);
		String fileName;
		boolean hotReload = false;
		try {
			fileName = argParser.getArgs().get(0);
		} catch (IndexOutOfBoundsException e) {
			fileName = "";
		}
		if (argParser.getItems().contains("hotreload") || argParser.getItems().contains("r")) {
			if (!fileName.equals("")) {
				hotReload = true;
				if (argParser.getItems().contains("shell")) {
					System.err.println("ERROR: shell and hot reload Interference");
					hotReload = false;
				}
			} else {
				System.err.println("when hot reload enabled, the file name must be entered [hot reload disabled]");
			}
		}
		if (fileName.endsWith(".ser")) {
			SyntaxTreeSerializer serializer = new SyntaxTreeSerializer();
			serializer.deserialize(fileName).eval();
			System.exit(0);
		}
		if (fileName.endsWith(".xml")) {
			XMLToSyntaxTree xmlToSyntaxTree = new XMLToSyntaxTree();
			File file = new File(fileName);
			Scanner scanner = null;
			try {
				scanner = new Scanner(file);
			} catch (FileNotFoundException e) {
				System.err.println("Can't open the file");
				return;
			}
			scanner.useDelimiter("\\Z");
			try {
				xmlToSyntaxTree.xmlToProgram(scanner.next()).eval();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
		String outputFileName = argParser.getValues().get("output");
		String classFileName = argParser.getValues().get("class-output");
		String xmlFileName = argParser.getValues().get("xml-output");
		String serializeFileName = argParser.getValues().get("serialize");
		if (serializeFileName != null && !serializeFileName.endsWith(".ser")) {
			serializeFileName += ".ser";
		}
		if (serializeFileName != null && !serializeFileName.endsWith(".xml")) {
			serializeFileName += ".xml";
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
		if (xmlFileName != null && xmlFileName.equals(outputFileName) && !argParser.getItems().contains("y")) {
			System.err.print("xml output and output files are the same file. continue? [y/n] ");
			Scanner scanner = new Scanner(System.in);
			if (!scanner.next().equals("y")) {
				System.out.println();
				System.exit(0);
			}
			System.out.println();
		}
		Compiler compiler = new Compiler(fileName, argParser.getItems().contains("shell"), outputFileName, (classFileName != null? classFileName.split("\\.")[0]:null), serializeFileName, xmlFileName);
		do {
			if (argParser.getItems().contains("lex")) {
				System.out.println(lex(compiler));
			} else {
				if (argParser.getItems().contains("shell") || hotReload) {
					SyntaxTree.getFunctions().clear();
					SyntaxTree.getVariables().clear();
					SyntaxTree.getClassesParameters().clear();
					SyntaxTree.CreateLambda.setCounter(0);
					SyntaxTree.resetNameSpaces();
				}
				compile(compiler);
			}
			if (hotReload) {
				long lastModified, previousLastModified;
				File file = new File(fileName);
				lastModified = file.lastModified();
				previousLastModified = lastModified;
				while (lastModified == previousLastModified) {
					lastModified = file.lastModified();
				}
				System.out.println("\n" + fileName + " rewritten...\n");
			}
		} while (argParser.getItems().contains("shell") || hotReload);
	}
}
