package pt.up.fe.comp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.SimpleParser;
import pt.up.fe.comp2023.table.ASymbolTable;
import pt.up.fe.comp2023.table.SymbolTableGenerator;

public class MySymbolTableTest {
    private boolean showTree = true;

    /**
     * Using the parser to save some trouble instantiating the AST
     * @param code the code to parse
     * @return SymbolTable
     */
    private ASymbolTable getTable(String code) {
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

        SymbolTableGenerator symbolTableGenerator = new SymbolTableGenerator();
        return symbolTableGenerator.getSymbolTable(parserResult.getRootNode());
    }

    @Test
    public void testClass() {
        String code = "class Foo {}";

        ASymbolTable result = getTable(code);

        assert result.getClassName().equals("Foo");
    }

    @Test
    public void testImport() {
        String code = "import java.util.List; class Foo {}";

        ASymbolTable result = getTable(code);

        assert result.getImports().size() == 1;
        assert result.getImports().get(0).equals("java.util.List");
    }

    @Test
    public void testFields() {
        String code = "class Foo { int a; int[] b; }";

        ASymbolTable result = getTable(code);

        List<Symbol> expected = List.of(
            new Symbol(new Type("int", false), "a"),
            new Symbol(new Type("int", true), "b")
        );

        assert result.getFields().size() == expected.size();
        assertEquals(expected, result.getFields());
    }

    @Test
    public void testMethods() {
        String code = "class Foo { public int a() { return 0; } public int b() { return 0; }}";

        ASymbolTable result = getTable(code);

        List<String> expected = List.of("a", "b");

        assert result.getMethods().size() == expected.size();
        assertEquals(expected, result.getMethods());
    }

    @Test
    public void testMethodArgs() {
        String code = "class Foo { public int a(int b, int[] c) { return 0; }}";

        ASymbolTable result = getTable(code);

        List<Symbol> expected = List.of(
            new Symbol(new Type("int", false), "b"),
            new Symbol(new Type("int", true), "c")
        );

        assertEquals(expected, result.getParameters("a"));
    }

    @Test
    public void testMethodReturn() {
        String code = "class Foo { public int a() { return 0; }}";

        ASymbolTable result = getTable(code);

        Type expected = new Type("int", false);

        assertEquals(expected, result.getReturnType("a"));
    }

    @Test
    public void testMethodLocals() {
        String code = "class Foo { public int a() { int b; int[] c; return 0; }}";

        ASymbolTable result = getTable(code);

        List<Symbol> expected = List.of(
            new Symbol(new Type("int", false), "b"),
            new Symbol(new Type("int", true), "c")
        );

        assertEquals(expected, result.getLocalVariables("a"));
    }

    @Test
    public void testTypes() {
        String code = "class Foo { int a; int[] b; }";

        ASymbolTable result = getTable(code);

        List<Symbol> expected = List.of(
            new Symbol(new Type("int", false), "a"),
            new Symbol(new Type("int", true), "b")
        );

        assertEquals(expected, result.getFields());
    }
}
