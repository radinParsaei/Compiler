public class SeperatorChecker extends StringCheckerBase {
	@Override
	public boolean check(String data) {
		return data.startsWith("\n") || data.startsWith(";");
	}

	@Override
	public String getText(String data) {
		int i = 0;
		while ((i < data.length()) && (data.charAt(i) == ';' || data.charAt(i) == '\n')) {
			i++;
		}
		return data.substring(0, i);
	}
}
