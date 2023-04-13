package pt.up.fe.comp2023.semantic;

import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ExpressionVisitor extends AJmmVisitor<String, Type> {
    Analysis analysis;

    public ExpressionVisitor(Analysis analysis) {
        this.analysis = analysis;
    }

    public Type getType(JmmNode node) {
        return visit(node, null);
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    @Override
    protected void buildVisitor() {
        addVisit("UnaryOp", this::dealWithUnary);
        addVisit("Negate", this::dealWithNegate);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("Literal", this::dealWithLiteral);
        addVisit("Variable", this::dealWithVariable);
    }

    protected Void defaultVisit(JmmNode node, String method) {
        visitAllChildren(node, method);
        return null;
    }

    private Type dealWithUnary(JmmNode node, String method) {
       Type type = visit(node.getChildren().get(0), method);
         if (type.getName().equals("int") && !type.isArray()) {
              return new Type("int", false);
         }
         else {
             analysis.addReport(node, "Unary operator " + node.get("op") + " can only be applied to int");
             return null;
         }
    }

    private Type dealWithNegate(JmmNode node, String method) {
        Type type = visit(node.getChildren().get(0), method);
        if (!type.equals("boolean")) {
            return new Type("int", false);
        }
        else {
            analysis.addReport(node, "Negation operator ! can only be applied to bool");
            return null;
        }
    }

    private Type dealWithBinaryOp(JmmNode node, String method) {
        Type left = visit(node.getChildren().get(0), method);
        Type right = visit(node.getChildren().get(1), method);
        String op = node.get("op");
        if (left == null || right == null) {
            analysis.addReport(node, "Invalid operands for " + op + ".");
            return null;
        } else if (right.isArray() || left.isArray()) {
            analysis.addReport(node, "Operator " + op + " used with an array " + left.getName() + " and "
                    + right.getName());
            return null;
        }
        if (op.equals("||") || op.equals("&&")) {
            if (!left.getName().equals("boolean") || !right.getName().equals("boolean")) {
                analysis.addReport(node, "Operator " + op + " used with conflicting types " + left.getName() + " and "
                        + right.getName());
            }
        }
        else {
            if (!left.getName().equals("int") || !right.getName().equals("int")) {
                analysis.addReport(node, "Operator " + op + " used with conflicting types " + left.getName() + " and "
                        + right.getName());
            }
            return new Type("int", false);
        }
        return left;
    }

    private Type dealWithArrayAccess(JmmNode node, String method) {
        Type array = visit(node.getChildren().get(0), method);
        Type index = visit(node.getChildren().get(1), method);
        if (array == null || index == null) {
            analysis.addReport(node, "Invalid array access");
            return null;
        }
        else if (!array.isArray()) {
            analysis.addReport(node, "Array access used with a non-array " + array.getName());
            return null;
        }
        else if (!index.getName().equals("int")) {
            analysis.addReport(node, "Array access used with a non-int " + index.getName());
            return null;
        }
        return new Type(array.getName(), false);
    }

    private Type dealWithLiteral(JmmNode node, String method) {
        if (node.get("value").equals("true") || node.get("value").equals("false")) {
            return new Type("boolean", false);
        }
        else if (node.get("value").equals("this")) {
            return null;
        }
        else {
            return new Type("int", false);
        }
    }

    private Type dealWithVariable(JmmNode node, String method) {
        Type type = analysis.getSymbolTable().getFieldType(node.get("id"));
        if (type == null) {
            type = analysis.getSymbolTable().getMethod(method).getFieldType(node.get("id"));
            if (type == null) {
                analysis.addReport(node.getChildren().get(0),
                        "Field " + node.get("id") + " not found");
                return null;
            }
        }
        return type;
    }
}
