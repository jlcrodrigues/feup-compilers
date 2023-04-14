package pt.up.fe.comp2023.semantic;

import java.util.List;
import java.util.function.BiFunction;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.table.ASymbolTable;

public class Analyzer extends AJmmVisitor<String, Void> {
    private Analysis analysis;
    private ExpressionVisitor expressionVisitor;

    public Analyzer(ASymbolTable symbolTable) {
        super();
        analysis = new Analysis(symbolTable);
        expressionVisitor = new ExpressionVisitor(analysis);
        setDefaultVisit(this::defaultVisit);
    }

    public void analyze(JmmNode node) {
        dealWithProgram(node, null);
        //visit(node);
    }

    public List<Report> getReports() {
        return analysis.getReports();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("InstanceMethod", this::dealWithMethod);
        addVisit("MainMethod", this::dealWithMethod);
        addVisit("Condition", this::dealWithCondition);
        addVisit("ExpressionStatement", this::dealWithExpressionStatement);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    protected Void defaultVisit(JmmNode node, String method) {
        visitAllChildren(node, method);
        return null;
    }

    private Void dealWithProgram(JmmNode node, String method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) continue;
            else visit(child, method);
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, String method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Extends")) continue;
            else if (child.getKind().equals("Var")) continue;
            else visit(child, method);
        }
        return null;
    }

    private Void dealWithMethod(JmmNode node, String _method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) continue;
            if (child.getKind().equals("ReturnObject")) {
                Type returnType = expressionVisitor.visit(child.getChildren().get(0), node.get("id"));
                if (returnType == null) return null;
                Type methodType = analysis.getSymbolTable().getReturnType(node.get("id"));
                if (!returnType.equals(methodType)) {
                    analysis.addReport(child.getChildren().get(0), "Return type of method " + node.get("id") + " is " + methodType + " but found " + returnType);
                }
            }
            else visit(child, node.get("id"));
        }
        return null;
    }

    private Void dealWithCondition(JmmNode node, String method) {
        Type conditionType = expressionVisitor.visit(node.getChildren().get(0), method);
        if (!conditionType.getName().equals("boolean")) {
            analysis.addReport(node.getChildren().get(0),
                    "Condition must be of type boolean but found " + conditionType.getName());
        }
        return null;
    }

    private Void dealWithExpressionStatement(JmmNode node, String method) {
        expressionVisitor.visit(node.getChildren().get(0), method);
        return null;
    }

    private Void dealWithAssignment(JmmNode node, String method) {
        Type fieldType = analysis.getSymbolTable().getFieldType(node.get("id"));
        if (fieldType == null) {
            fieldType = analysis.getSymbolTable().getMethod(method).getFieldType(node.get("id"));
            if (fieldType == null) {
                analysis.addReport(node.getChildren().get(0),
                        "Field " + node.get("id") + " not found");
                return null;
            }
        }
        Type type = expressionVisitor.visit(node.getChildren().get(0), method);
        if (type.getName().equals("this")) {
            if (!fieldType.getName().equals(analysis.getSymbolTable().getClassName())
                    && !fieldType.getName().equals(analysis.getSymbolTable().getSuper())) {
                analysis.addReport(node.getChildren().get(0),
                        "Expected to find " + analysis.getSymbolTable().getClassName()
                                + " or " + analysis.getSymbolTable().getSuper() + " but found " +
                                fieldType.getName() + "in 'this assignment'");
            }
        }
        else if (!fieldType.equals(type)) {
            List<String> imports = analysis.getSymbolTable().getImports();
            if (imports.contains(type.getName()) && imports.contains(fieldType.getName())) {
                return null;
            }
            String superName = analysis.getSymbolTable().getSuper();
            String className = analysis.getSymbolTable().getClassName();
            if (superName.equals(fieldType.getName()) && className.equals(type.getName()))
                return null;
            analysis.addReport(node.getChildren().get(0),
                    "Type of right side of assignment must be " + fieldType.getName() + " but found " + type.getName());
        }
        return null;
    }

    private Void dealWithArrayAssignment(JmmNode node, String method) {
        Type fieldType = analysis.getSymbolTable().getFieldType(node.get("id"));
        if (fieldType == null) {
            fieldType = analysis.getSymbolTable().getMethod(method).getFieldType(node.get("id"));
            if (fieldType == null) {
                analysis.addReport(node.getChildren().get(0),
                        "Field " + node.get("id") + " not found");
                return null;
            }
        }
        if (!fieldType.isArray()) {
            analysis.addReport(node.getChildren().get(0),
                    "Array access over non-array: " + node.get("id"));
        }
        Type indexType = expressionVisitor.visit(node.getChildren().get(0), method);
        if (!indexType.getName().equals("int")) {
            analysis.addReport(node.getChildren().get(0),
                    "Array index must be of type int but found " + indexType.getName());
        }

        Type type = expressionVisitor.visit(node.getChildren().get(1), method);
        if (!fieldType.getName().equals(type.getName())) {
            analysis.addReport(node.getChildren().get(0),
                    "Type of right side of assignment must be " + fieldType.getName() + " but found " + type.getName());
        }
        return null;
    }
}
