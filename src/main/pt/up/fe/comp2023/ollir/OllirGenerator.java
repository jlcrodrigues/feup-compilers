package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
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
        addVisit("InstanceMethod", this::dealWithInstanceMethod);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Variable", this::dealWithVariable);
        addVisit("Literal", this::dealWithLiteral);
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

        String superClass = symbolTable.getSuper();

        ollirCode.append(symbolTable.getClassName());
        if (!Objects.equals(superClass, ""))
            ollirCode.append(" extends ").append(superClass);

        ollirCode.append("{\n\n");

        List<Symbol> fields = symbolTable.getFields();
        if (!fields.isEmpty()){
            for (Symbol field : fields) {
                ollirCode.append(".field private ");
                ollirCode.append(OllirUtils.getCode(field));
                ollirCode.append(";\n");
            }
        }

        ollirCode.append("\n.construct ").append(symbolTable.getClassName()).append("().V{\n");
        ollirCode.append("\tinvokespecial(this, \"<init>\").V;\n}\n");

        for (var child : node.getChildren()) {
            if (!child.getKind().equals("Extends") && !child.getKind().equals("Var")) {
                visit(child);
            }
        }

        ollirCode.append("}\n");

        return null;
    }

    private StringBuilder dealWithMainMethod(JmmNode node, Void arg) {

        String returnType = OllirUtils.getOllirType(symbolTable.getReturnType("main"));
        String paramCode = symbolTable.getParameters("main")
                .stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append("\n.method public static main(");
        ollirCode.append(paramCode).append(").");
        ollirCode.append(returnType);
        ollirCode.append("{\n");

        for (JmmNode child : node.getChildren()) {
            visit(child);
        }

        ollirCode.append("\tret.V;\n");
        ollirCode.append("}\n\n");

        return null;
    }

    private StringBuilder dealWithInstanceMethod(JmmNode node, Void arg) {

        String returnType = OllirUtils.getOllirType(symbolTable.getReturnType(node.get("id")));
        String paramCode = symbolTable.getParameters(node.get("id"))
                .stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append(".method public ").append(node.get("id"));
        ollirCode.append("(").append(paramCode).append(").");
        ollirCode.append(returnType).append("{\n");

        for (JmmNode child : node.getChildren()) {
            if (!child.getKind().equals("ReturnType") && !child.getKind().equals("Var") && !child.getKind().equals("ReturnObject"))
                visit(child);
        }

        var returnObject = visit(node.getJmmChild(node.getNumChildren()-1).getJmmChild(0));

        ollirCode.append("\n\tret.").append(returnType).append(" ");
        ollirCode.append(returnObject).append(".").append(returnType);
        ollirCode.append(";\n}\n");

        return null;
    }

    private StringBuilder dealWithAssignment(JmmNode node, Void arg) {
        String parentMethod = OllirUtils.getParentMethod(node);
        String type = "";

        String lhs = node.get("id");
        StringBuilder rhs = visit(node.getChildren().get(0));

        for (Symbol symbol : symbolTable.getLocalVariables(parentMethod)) {
            if (symbol.getName().equals(node.get("id")))
                type = OllirUtils.getOllirType(symbol.getType());
        }

        ollirCode.append("\t");
        ollirCode.append(lhs).append(".").append(type);
        ollirCode.append(" :=.").append(type).append(" ");
        ollirCode.append(rhs).append(".").append(type);
        ollirCode.append(";\n");

        return null;
    }


    private StringBuilder dealWithBinaryOp(JmmNode node, Void arg) {
        String code = "";

        StringBuilder lhs = visit(node.getJmmChild(0));
        StringBuilder rhs = visit(node.getJmmChild(1));

        String op = OllirUtils.getOllirOperator(node);
        String typeOp = OllirUtils.getTypeOperator(node);

        code += lhs + typeOp + op + rhs;

        return new StringBuilder(code);
    }


    private StringBuilder dealWithLiteral(JmmNode node, Void arg) {
        return new StringBuilder(node.get("value"));
    }

    private StringBuilder dealWithVariable(JmmNode node, Void arg) {
        return new StringBuilder(node.get("id"));
    }

}
