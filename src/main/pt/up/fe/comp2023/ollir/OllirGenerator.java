package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Objects;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Void, StringBuilder> {

    public final StringBuilder ollirCode;
    public final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMainMethod);
    }

    private StringBuilder dealWithProgram(JmmNode node, Void arg) {
        for (var importString : symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }
        ollirCode.append("\n");
        for (var child : node.getChildren()) {
            if (!child.getKind().equals("Import"))
                visit(child);
        }
        return null;
    }

    private StringBuilder dealWithClass(JmmNode node, Void arg) {
        ollirCode.append(symbolTable.getClassName());

        if (symbolTable.getSuper() != null)
            ollirCode.append(" extends ").append(symbolTable.getSuper());

        ollirCode.append("{\n\n");

        var fields = symbolTable.getFields();
        if (!fields.isEmpty()){
            for (Symbol field : fields) {
                ollirCode.append(".field private ");
                ollirCode.append(OllirUtils.getCode(field));
                ollirCode.append(";\n");
            }
        }

        ollirCode.append("\n.construct ").append(symbolTable.getClassName()).append("().V{\n");
        ollirCode.append("invokespecial(this, \"<init>\").V;\n}\n");

        for (var child : node.getChildren()) {
            if (!child.getKind().equals("Extends") && !child.getKind().equals("Var")) {
                visit(child);
            }
        }

        ollirCode.append("}\n");

        return null;
    }

    private StringBuilder dealWithMainMethod(JmmNode node, Void arg) {

        ollirCode.append("\n.method public static main(");

        var params = symbolTable.getParameters("main");

        var paramCode = params.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));
        System.out.println(params.stream().toString());

        ollirCode.append(paramCode);
        ollirCode.append(").");
        ollirCode.append(OllirUtils.getOllirType(symbolTable.getReturnType("main")));
        ollirCode.append("{\n");

        for (JmmNode child : node.getChildren()) {
            visit(child);
        }
        ollirCode.append("ret.V;\n");
        ollirCode.append("}\n\n");

        return null;
    }

}
