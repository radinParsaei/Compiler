public class SeperatorChecker extends StringCheckerBase {
	@Override
	public boolean check(String data) {
		if (data.startsWith("\n") || data.startsWith(";")) {
			return true;
		}
		return false;
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
