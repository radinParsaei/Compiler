import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextChecker extends StringCheckerBase {
    int size = 0;

    @Override
    public boolean check(String data) {
        Pattern pattern = Pattern.compile("\"(.*?(\\\\\")*)*(\")|'(.*?(\\\\')*)*(')");
        Matcher matcher = pattern.matcher(data);
        if (!matcher.find()) {
            return false;
        }
        String matched = matcher.group(0);
        if (data.startsWith(matched)) {
            size = matched.length();
            return (matched.endsWith("\"") || matched.endsWith("'")) &&
                    !(matched.endsWith("\\\"") || matched.endsWith("\\'"));
        }
        return false;
    }

    @Override
    public String getText(String data) {
        return data.substring(0, size);
    }
}
