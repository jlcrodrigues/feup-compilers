package pt.up.fe.comp2023.table;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class SymbolTableGenerator extends AJmmVisitor<Void, Void> {
    private ASymbolTable symbolTable;

    public SymbolTableGenerator() {
        symbolTable = new ASymbolTable();
    }

    public ASymbolTable getSymbolTable(JmmNode node) {
        visit(node);
        return symbolTable;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("Extends", this::dealWithExtends);
        addVisit("InstanceMethod", this::dealWithInstanceMethod);
        addVisit("MainMethod", this::dealWithMainMethod);
    }

    private Void dealWithProgram(JmmNode node, Void arg) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) {
                symbolTable.addImport(child.get("id"));
            } else {
                visit(child, null);
            }
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, Void arg) {
        symbolTable.setClassName(node.get("id"));
        for (JmmNode child : node.getChildren()) {
            visit(child, null);
        }
        return null;
    }

    private Void dealWithExtends(JmmNode node, Void arg) {
        symbolTable.setSuper(node.get("id"));
        return null;
    }

    private Void dealWithInstanceMethod(JmmNode node, Void arg) {
        SymbolTableMethod method = new SymbolTableMethod(node.get("id"));
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("ReturnType")) {
                method.setReturnType(child.getChildren().get(0).get("id"));
            } else if (child.getKind().equals("ArgumentObject")) {
                method.addParameter(child.get("id"), child.getChildren().get(0).get("type"));
            } else if (child.getKind().equals("Var")) {
                method.addLocalVariable(child.get("id"), child.getChildren().get(0).get("type"));
            }
        }
        symbolTable.addMethod(method);
        return null;
    }

    private Void dealWithMainMethod(JmmNode node, Void arg) {
        SymbolTableMethod method = new SymbolTableMethod("main");
        method.setReturnType("void");
        method.addParameter("args", "String[]");
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) {
                method.addLocalVariable(child.get("id"), child.getChildren().get(0).get("type"));
            }
        }
        symbolTable.addMethod(method);
        return null;
    }
}