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
        addVisit("MemberAccessLength", this::dealWithMemberAccessLength);
        addVisit("NewArray", this::dealWithNewArray);
        addVisit("NewObject", this::dealWithNewObject);
    }

    protected Type defaultVisit(JmmNode node, String method) {
        return visit(node.getChildren().get(0), method);
    }

    private Type dealWithUnary(JmmNode node, String method) {
       Type type = visit(node.getChildren().get(0), method);
         if ((type.getName().equals("int") || type.getName().equals("import")) && !type.isArray()) {
              return new Type("int", false);
         }
         else {
             analysis.addReport(node, "Unary operator " + node.get("op") + " can only be applied to int");
             return null;
         }
    }

    private Type dealWithNegate(JmmNode node, String method) {
        Type type = visit(node.getChildren().get(0), method);
        if (type == null || !(type.equals("boolean") || type.equals("import"))) {
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
            if (!(left.getName().equals("boolean") || left.getName().equals("import"))
                    || !(right.getName().equals("boolean") || right.getName().equals("import"))) {
                analysis.addReport(node, "Operator " + op + " used with conflicting types " + left.getName() + " and "
                        + right.getName());
            }
        }
        else {
            if (!(left.getName().equals("int") || left.getName().equals("import"))  ||
                    !(right.getName().equals("int") || right.getName().equals("import"))) {
                analysis.addReport(node, "Operator " + op + " used with conflicting types " + left.getName() + " and "
                        + right.getName());
            }
            if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
                return new Type("boolean", false);
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
        else if (!array.isArray() && !array.getName().equals("import")) {
            analysis.addReport(node, "Array access used with a non-array " + array.getName());
            return null;
        }
        else if (!(index.getName().equals("int") || index.getName().equals("import")) || index.isArray()) {
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

    private Type dealWithMethodCall(JmmNode node, String methodName) {
        String calledMethodName = getMethodName(node);
        if (checkMethod(node, methodName))
            return new Type("import", false);

        SymbolTableMethod calledMethod = analysis.getSymbolTable().getMethod(calledMethodName);

        List<Symbol> args  = calledMethod.getParameters();
        Type returnType = analysis.getSymbolTable().getMethod(calledMethodName).getReturnType();
        if (node.getChildren().size() - 1 != args.size()) {
            analysis.addReport(node, "Method " + calledMethodName + " called with "
                    + (node.getChildren().size() - 1) + " arguments, expected " + args.size());
        }
        for (int i = 0; i < args.size() && i < node.getChildren().size() - 1; i++) {
            Type actualType = visit(node.getChildren().get(i + 1), methodName);
            Type expectedType = args.get(i).getType();
            if (!checkTypes(expectedType, actualType)) {
                analysis.addReport(node.getChildren().get(0),
                        "Expected to find " + expectedType.getName() + " but found " +
                                actualType.getName() + " in 'this assignment'");
            }
        }
        return returnType;
    }

    private boolean checkMethod(JmmNode node, String method)  {
        // class extends another class
        if (!analysis.getSymbolTable().getSuper().equals("java.lang.Object")) {
            return true;
        }

        // method is from imported class
        JmmNode firstChild = node.getChildren().get(0);
        if (firstChild.getKind().equals("ChainMethods")) {
            if (firstChild.getChildren().get(0).getKind().equals("Variable")) {
                String className = firstChild.getChildren().get(0).get("id");
                Type type = analysis.getSymbolTable().getVariableType(className, method);
                if (type != null) {
                    if (analysis.getSymbolTable().getImports().contains(type.getName()))
                        return true;
                }
                if (analysis.getSymbolTable().getImports().contains(className)) {
                    return true;
                }
            }
            else if (firstChild.getChildren().get(0).getKind().equals("Literal")) {
                if (firstChild.getChildren().get(0).get("value").equals("this")
                        && analysis.getSymbolTable().getMethod(method).isStatic()) {
                    analysis.addReport(node, "'this' cannot be used this in a static method");
                }
            }
        }

        // method is from same class
        String methodName = getMethodName(node);
        if (!analysis.getSymbolTable().getMethods().contains(methodName)) {
            analysis.addReport(node, "Method " + methodName + " not found");
            return true;
        }
        return false;
    }

    public boolean checkTypes(Type expected, Type actual) {
        if (actual == null) return false;
        if (expected.equals(actual)) return true;
        if (actual.getName().equals("import")) return true;

        // this can be used as an object
        if (actual.getName().equals("this")) {
            if (expected.getName().equals(analysis.getSymbolTable().getClassName())
                    || expected.getName().equals(analysis.getSymbolTable().getSuper())) {
                return true;
            }
        }

        String superName = analysis.getSymbolTable().getSuper();
        String className = analysis.getSymbolTable().getClassName();

        // extended class
        if (className.equals(actual.getName()) && superName.equals(expected.getName())) {
            return true;
        }

        // imported class may extend actual class
        List<String> imports = analysis.getSymbolTable().getImports();
        if (imports.contains(actual.getName())) {
            return true;
        }
        return false;
    }

    private String getMethodName(JmmNode node) {
        JmmNode firstChild = node.getChildren().get(0);
        if (firstChild.getKind().equals("Literal")) {
            if (node.get("value").equals("this")) return "this";
            else analysis.addReport(node, "Method " + node.get("id") + " called with invalid receiver");
        }
        else if (firstChild.getKind().equals("Variable")) {
            return firstChild.get("id");
        }
        else if (firstChild.getKind().equals("ChainMethods")) {
            return firstChild.get("id");
        }
        return null;
    }

    private Type dealWithMemberAccessLength(JmmNode node, String method) {
        visit(node.getChildren().get(0), method);
        return new Type("int", false);
    }

    private Type dealWithNewArray(JmmNode node, String method) {
        Type type = visit(node.getChildren().get(0), method);
        if (!type.getName().equals("int") || type.isArray()) {
            analysis.addReport(node, "Array size must be of type int");
            return null;
        }
        return new Type("int", true);
    }

    private Type dealWithNewObject(JmmNode node, String method) {
        return new Type(node.get("id"), false);
    }
}
