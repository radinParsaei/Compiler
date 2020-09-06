import java.util.Scanner;

public class REPLReader {
	public static String readLine() {
		System.out.print("> ");
		return new Scanner(System.in).nextLine() + "\n";
	}
}
