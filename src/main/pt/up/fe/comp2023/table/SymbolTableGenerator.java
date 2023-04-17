package pt.up.fe.comp2023.table;

import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
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
                List<JmmNode> gdChildren = child.getChildren();
                if (gdChildren.size() > 0)
                    symbolTable.addImport(gdChildren.get(gdChildren.size() - 1).get("id"));
                else symbolTable.addImport(child.get("id"));
            } else {
                visit(child, null);
            }
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, Void arg) {
        symbolTable.setClassName(node.get("id"));
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) {
                symbolTable.addField(
                        child.get("id"),
                        new Symbol(getType(child.getChildren().get(0)), child.get("id")));
            }
            else {
                visit(child, null);
            }
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
            if (child.getKind().equals("StaticMethod")) {
                method.setIsStatic(true);
            }
            else if (child.getKind().equals("ReturnType")) {
                method.setReturnType(getType(child.getChildren().get(0)));
            } else if (child.getKind().equals("ArgumentObject")) {
                method.addParameter(child.get("id"), getType(child.getChildren().get(0)));
            } else if (child.getKind().equals("Var")) {
                method.addLocalVariable(child.get("id"), getType(child.getChildren().get(0)));
            }
        }
        symbolTable.addMethod(method);
        return null;
    }

    private Void dealWithMainMethod(JmmNode node, Void arg) {
        SymbolTableMethod method = new SymbolTableMethod("main");
        method.setReturnType(new Type("void", false));
        method.addParameter("args", new Type("String", true));
        method.setIsStatic(true);
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) {
                method.addLocalVariable(child.get("id"), getType(child.getChildren().get(0)));
            }
        }
        symbolTable.addMethod(method);
        return null;
    }

    private Type getType(JmmNode node) {
        return new Type(node.get("id"), node.getChildren().size() > 0);
    }
}