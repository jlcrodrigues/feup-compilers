package pt.up.fe.comp;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.SimpleParser;
import pt.up.fe.comp2023.semantic.AJmmAnalysis;
import pt.up.fe.comp2023.table.ASymbolTable;
import pt.up.fe.comp2023.table.SymbolTableGenerator;

public class MySemanticTest {
    private boolean showTree = true;

    /**
     * Using the parser to save some trouble instantiating the AST
     * @param code the code to parse
     * @return Jmm
     */
    private JmmSemanticsResult getResult(String code) {
        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", "");
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, parser.getDefaultRule(),config);
        if (showTree)
            System.out.println(parserResult.getRootNode().toTree());
        AJmmAnalysis analysis = new AJmmAnalysis();

        return analysis.semanticAnalysis(parserResult);
    }

    @Test
    public void testReturnType() {
        String code = "class Foo { public int foo() { return 1; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 0;
    }

    @Test
    public void testWrongReturnType() {
        String code = "class Foo { public int foo() { return true; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testIf() {
        String code = "class Foo { public int foo() { if (true) { } else { } return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 0;
    }

    @Test
    public void testIfWrong() {
        String code = "class Foo { public int foo() { if (2) { } else { } return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testAssignment() {
        String code = "class Foo { public int foo() { int a; a = 1; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 0;
    }

    @Test
    public void testAssignmentWrong() {
        String code = "class Foo { public int foo() { int a; a = true; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testArrayAccess() {
        // Array access is done over an array
        String code = "class Foo { public int foo() { int a; a[0] = 1; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testArrayIndex() {
        String code = "class Foo { public int foo() { int[] a; a[false] = 1; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testArrayOperations() {
        String code = "class Foo { public int foo() { int[] a; a + 2; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }

    @Test
    public void testOperationTypes() {
        String code = "class Foo { public int foo() {int a; 1 + false; 2 > false; 2 || false;  a + 2; false || true; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 3;
    }

    @Test
    public void testUnaryOp() {
        String code = "class Foo { public int foo() { int a; a++; false++; return 0; } }";

        JmmSemanticsResult result = getResult(code);
        assert result.getReports().size() == 1;
    }
}
