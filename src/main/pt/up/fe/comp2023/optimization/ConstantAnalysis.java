package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;

public class ConstantAnalysis extends AJmmVisitor<Boolean, Void> {
    private final JmmSemanticsResult semanticsResult;
    private final HashMap<String, String> constants;
    private boolean run; // keep running while there are changes

    public ConstantAnalysis(JmmSemanticsResult semanticsResult) {
        super();
        this.semanticsResult = semanticsResult;
        setDefaultVisit(this::defaultVisit);
        constants = new HashMap<>();
        run = true;
    }

    public JmmSemanticsResult analyze() {
        while (run) {
            run = false;
            visit(semanticsResult.getRootNode(), false);
        }
        return semanticsResult;
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Negate", this::dealWithNegate);
        addVisit("Variable", this::dealWithVariable);
    }

    protected Void defaultVisit(JmmNode node, Boolean inLoop) {
        visitAllChildren(node, inLoop || node.getKind().equals("While"));
        return null;
    }

    /**
     * Performs constant folding for binary operations.
     */
    private Void dealWithBinaryOp(JmmNode node, Boolean inLoop) {
        visitAllChildren(node, inLoop);
        JmmNode left = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        if (!left.getKind().equals("Literal") && !right.getKind().equals("Literal")) return null;
        String op = node.get("op");
        if (op.equals("||")) {
            if (left.getKind().equals("Literal") && left.get("value").equals("true"))
                switchNode(node, "true");
            else if (right.getKind().equals("Literal") && right.get("value").equals("true")) {
                switchNode(node, "true");
            }
        } else if (op.equals("&&")) {
            if (left.getKind().equals("Literal") && left.get("value").equals("false"))
                switchNode(node, "false");
            else if (right.getKind().equals("Literal") && right.get("value").equals("false")) {
                switchNode(node, "false");
            }
        }

        if (left.getKind().equals("Literal") && right.getKind().equals("Literal")) {
            if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=") || op.equals("==") || op.equals("!=")) {
                int leftValue = Integer.parseInt(left.get("value"));
                int rightValue = Integer.parseInt(right.get("value"));
                boolean result = performBooleanOperation(op, leftValue, rightValue);
                switchNode(node, Boolean.toString(result));
            }

            if (op.equals("+") || op.equals("-") || op.equals("*") || (op.equals("/"))) {
                    int leftValue = Integer.parseInt(left.get("value"));
                    int rightValue = Integer.parseInt(right.get("value"));
                    int result = performIntegerOperation(op, leftValue, rightValue);
                    switchNode(node, Integer.toString(result));
            }
        }
        return null;
    }

    /**
     * Performs constant folding for negation operations.
     */
    private Void dealWithNegate(JmmNode node, Boolean inLoop) {
        visitAllChildren(node, inLoop);
        JmmNode child = node.getChildren().get(0);
        if (child.getKind().equals("Literal")) {
            if (child.get("value").equals("true")) {
                switchNode(node, "false");
            } else if (child.get("value").equals("false")) {
                switchNode(node, "true");
            }
        }
        return null;
    }

    /**
     * Deals with variable assignments.
     * Constant assignments are added to the constant table.
     */
    private Void dealWithAssignment(JmmNode node, Boolean inLoop) {
        // if the visitor is inside a loop, not all constants can be changed
        // thus, it is necessary to visit after the variable is removed
        if (!inLoop)
            defaultVisit(node, false);
        if (node.getChildren().get(0).getKind().equals("Literal")) {
            constants.put(node.get("id"), node.getChildren().get(0).get("value"));
        }
        else if (constants.containsKey(node.get("id"))) {
            constants.remove(node.get("id"));
        }
        if (inLoop)
            defaultVisit(node, true);

        return null;
    }

    /**
     * Performs constant propagation.
     */
    private Void dealWithVariable(JmmNode node, Boolean inLoop) {
        if (constants.containsKey(node.get("id"))) {
            switchNode(node, constants.get(node.get("id")));
        }
        return null;
    }

    private void switchNode(JmmNode old, String value) {
        JmmNode parent = old.getJmmParent();
        JmmNode newNode = new JmmNodeImpl("Literal");
        newNode.put("value", value);
        parent.removeJmmChild(old);
        parent.add(newNode);
        run = true;
    }

    private boolean performBooleanOperation(String op, int leftValue, int rightValue) {
        switch (op) {
            case "<":
                return leftValue < rightValue;
            case ">":
                return leftValue > rightValue;
            case "<=":
                return leftValue <= rightValue;
            case ">=":
                return leftValue >= rightValue;
            case "==":
                return leftValue == rightValue;
            case "!=":
                return leftValue != rightValue;
            default:
                return false;
        }
    }

    private int performIntegerOperation(String op, int leftValue, int rightValue) {
        switch (op) {
            case "+":
                return leftValue + rightValue;
            case "-":
                return leftValue - rightValue;
            case "*":
                return leftValue * rightValue;
            case "/":
                return leftValue / rightValue;
            default:
                return 0;
        }
    }
}
