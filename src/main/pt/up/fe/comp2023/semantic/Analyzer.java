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
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("Condition", this::dealWithCondition);
        addVisit("ExpressionStatement", this::dealWithExpressionStatement);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
    }

    protected Void defaultVisit(JmmNode node, String method) {
        return visitAllChildren(node, method);
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
                if (!expressionVisitor.checkTypes(methodType, returnType)) {
                    analysis.addReport(child.getChildren().get(0), "Return type of method "
                            + node.get("id") + " is " + methodType + " but found " + returnType);
                }
            }
            else visit(child, node.get("id"));
        }
        return null;
    }

    private Void dealWithMainMethod(JmmNode node, String _method) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) continue;
            else visit(child, "main");
        }
        return null;
    }

    private Void dealWithCondition(JmmNode node, String method) {
        Type conditionType = expressionVisitor.visit(node.getChildren().get(0), method);
        if (conditionType == null) {
            //analysis.addReport(node.getChildren().get(0), "Condition can't be null");
            return null;
        }
        if (!(conditionType.getName().equals("boolean") || conditionType.getName().equals("import"))) {
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
        Type fieldType = analysis.getSymbolTable().getVariableType(node.get("id"), method);
        if (fieldType == null) {
            analysis.addReport(node.getChildren().get(0),
                    "Field " + node.get("id") + " not found");
            return null;
        }
        Type type = expressionVisitor.visit(node.getChildren().get(0), method);

        if (!expressionVisitor.checkTypes(fieldType, type)) {
            analysis.addReport(node.getChildren().get(0),
                    "Type of right side of assignment must be " + fieldType.getName()
                            + " but found " + (type == null ? "null" : type.getName()));
        }

        return null;
    }

    private Void dealWithArrayAssignment(JmmNode node, String method) {
        Type fieldType = analysis.getSymbolTable().getVariableType(node.get("id"), method);
        if (fieldType == null) {
            analysis.addReport(node.getChildren().get(0),
                    "Field " + node.get("id") + " not found");
            return null;
        }

        if (!fieldType.isArray() && !fieldType.getName().equals("import")) {
            analysis.addReport(node.getChildren().get(0),
                    "Array access over non-array: " + node.get("id"));
        }
        Type indexType = expressionVisitor.visit(node.getChildren().get(0), method);
        if (!(indexType.getName().equals("int") || indexType.getName().equals("import"))) {
            analysis.addReport(node.getChildren().get(0),
                    "Array index must be of type int but found " + indexType.getName());
        }
        if (indexType.isArray()) {
            analysis.addReport(node.getChildren().get(0),
                    "Array index can't be an array");
        }

        Type type = expressionVisitor.visit(node.getChildren().get(1), method);
        Type tempFieldType = new Type(fieldType.getName(), false);
        if (!expressionVisitor.checkTypes(tempFieldType, type)) {
            analysis.addReport(node.getChildren().get(0),
                    "Type of right side of assignment must be " + fieldType.getName() + " but found " + type.getName());
        }
        return null;
    }
}
