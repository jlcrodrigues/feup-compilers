package pt.up.fe.comp2023.semantic;

import java.util.List;
import java.util.function.BiFunction;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2023.table.ASymbolTable;

public class Analyzer extends AJmmVisitor<Void, Void> {
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
    }

    protected Void defaultVisit(JmmNode node, Void arg) {
        visitAllChildren(node, null);
        return null;
    }

    private Void dealWithProgram(JmmNode node, Void arg) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Import")) continue;
            else visit(child, null);
        }
        return null;
    }

    private Void dealWithClass(JmmNode node, Void arg) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Extends")) continue;
            else if (child.getKind().equals("Var")) continue;
            else visit(child, null);
        }
        return null;
    }

    private Void dealWithMethod(JmmNode node, Void arg) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Var")) continue;
            if (child.getKind().equals("ReturnObject")) {
                Type returnType = expressionVisitor.visit(child.getChildren().get(0), null);
                Type methodType = analysis.getSymbolTable().getReturnType(node.get("id"));
                if (!returnType.equals(methodType)) {
                    analysis.addReport(child.getChildren().get(0), "Return type of method " + node.get("id") + " is " + methodType + " but found " + returnType);
                }
            }
            else visit(child, null);
        }
        return null;
    }

}
