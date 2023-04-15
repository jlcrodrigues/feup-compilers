package pt.up.fe.comp2023.semantic;

import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2023.table.SymbolTableMethod;

public class ExpressionVisitor extends AJmmVisitor<String, Type> {
    Analysis analysis;

    public ExpressionVisitor(Analysis analysis) {
        super();
        this.analysis = analysis;
        setDefaultVisit(this::defaultVisit);
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
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("ImportedMethodCall", this::dealWithMethodCall);
        addVisit("MemberAccessLength", this::dealWithMemberAccessLength);
        addVisit("NewArray", this::dealWithNewArray);
        addVisit("NewObject", this::dealWithNewObject);
    }

    protected Type defaultVisit(JmmNode node, String method) {
        return visit(node.getChildren().get(0));
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
        if (type == null || !type.equals("boolean")) {
            //analysis.addReport(node, "Negation operator ! can only be applied to bool");
            return null;
        }
        else {
            return new Type("boolean", false);
        }
    }

    private Type dealWithBinaryOp(JmmNode node, String method) {
        Type left = visit(node.getChildren().get(0), method);
        Type right = visit(node.getChildren().get(1), method);
        String op = node.get("op");
        if (left == null || right == null) {
            //analysis.addReport(node, "Invalid operands for " + op + ".");
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
            if (analysis.getSymbolTable().getMethod(method).isStatic()) {
                analysis.addReport(node, "'this' cannot be used this in a static method");
            }
            return new Type("this", false);
        }
        else {
            return new Type("int", false);
        }
    }

    private Type dealWithVariable(JmmNode node, String method) {
        Type type = analysis.getSymbolTable().getVariableType(node.get("id"), method);
        if (type == null) {
            analysis.addReport(node,
                    "Field " + node.get("id") + " not found");
        }
        return type;
    }

    private Type dealWithMethodCall(JmmNode node, String _method) {
        String methodName = node.get("id");
        if (checkMethod(node, _method))
            return null;

        SymbolTableMethod method = analysis.getSymbolTable().getMethod(methodName);

        List<Symbol> args  = method.getParameters();
        Type returnType = analysis.getSymbolTable().getMethod(node.get("id")).getReturnType();
        if (node.getChildren().size() - 1 != args.size()) {
            analysis.addReport(node, "Method " + methodName + " called with "
                    + (node.getChildren().size()) + " arguments, expected " + args.size());
        }
        for (int i = 0; i < args.size() && i < node.getChildren().size() - 1; i++) {
            Type actualType = visit(node.getChildren().get(i + 1), methodName);
            if (!args.get(i).getType().equals(actualType)) {
                analysis.addReport(node, "Method " + methodName
                        + " called with argument of type " + actualType.getName()
                        + ", expected " + args.get(i).getType().getName());
                return returnType;
            }
        }
        return returnType;
    }

    private boolean checkMethod(JmmNode node, String _method)  {
        String methodName = node.get("id");

        if (!analysis.getSymbolTable().getSuper().equals("java.lang.Object")) {
            return true;
        }

        Type methodType = analysis.getSymbolTable().getVariableType(methodName, _method);
        if (methodType != null) {
            List<String> imports = analysis.getSymbolTable().getImports();
            if (!imports.contains(methodType.getName())) {
                analysis.addReport(node, "Method " + methodName + " not found");
                return true;
            }
            return true;
        }
        List<JmmNode> chains = node.getChildren().get(0).getChildren();
        if (chains.size() > 0) {
            String chain = methodName;
            for (int i = 0; i < chains.size() - 1; i++) {
                if (!chains.get(i).hasAttribute("id")) {
                    analysis.addReport(node, "Method chain " + methodName + "malformed");
                    return true;
                }
                chain += "." + chains.get(i).get("id");
            }
            if (!analysis.getSymbolTable().getImports().contains(chain)) {
                analysis.addReport(node, "Method " + chain + " not found");
            }
            return true;
        }
        return false;
    }

    private Type dealWithMemberAccessLength(JmmNode node, String method) {
        visit(node.getChildren().get(0), method);
        return new Type("int", false);
    }

    private Type dealWithNewArray(JmmNode node, String method) {
        Type type = visit(node.getChildren().get(0), method);
        if (!type.getName().equals("int")) {
            analysis.addReport(node, "Array size must be of type int");
            return null;
        }
        return new Type("int", true);
    }

    private Type dealWithNewObject(JmmNode node, String method) {
        return new Type(node.get("id"), false);
    }
}
