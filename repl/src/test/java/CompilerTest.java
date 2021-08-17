import junit.framework.TestCase;

public class CompilerTest extends TestCase {

    public void testVariable() {
        Compiler compiler = new Compiler(null, false, null, null, null, null);
        compiler.setIsTestModeOn(true);
        Compiler.testCode = "var a = 10\n";
        CompilerMain.compile(compiler);
        assertTrue(((Boolean) ((SyntaxTree.Boolean) new SyntaxTree.Equals(new SyntaxTree.Variable("a"), new SyntaxTree.Number(10)).getData()).getData()));
    }

}