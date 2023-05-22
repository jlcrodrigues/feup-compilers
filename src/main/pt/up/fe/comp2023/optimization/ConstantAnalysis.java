package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;

public class ConstantAnalysis extends AJmmVisitor<Void, Void> {
    private JmmSemanticsResult semanticsResult;
    private HashMap<String, Integer> constants;

    public ConstantAnalysis(JmmSemanticsResult semanticsResult) {
        super();
        this.semanticsResult = semanticsResult;
        setDefaultVisit(this::defaultVisit);
        constants = new HashMap<>();
    }

    public JmmSemanticsResult analyze() {
        visit(semanticsResult.getRootNode(), null);
        return semanticsResult;
    }

    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Assignment", this::dealWithAssignment);
    }

    protected Void defaultVisit(JmmNode node, Void arg) {
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Variable")) {
                visitVariable(node, null);
            }
            visit(child, arg);
        }
        return null;
    }

    private Void dealWithBinaryOp(JmmNode node, Void arg) {
        visitAllChildren(node, null);
        return null;
    }

    private Void dealWithAssignment(JmmNode node, Void arg) {
        if (node.getChildren().get(0).getKind().equals("Literal")) {
            constants.put(node.get("id"), Integer.parseInt(node.getChildren().get(0).get("value")));
        }
        defaultVisit(node, null);
        return null;
    }

    private Void visitVariable(JmmNode parent, Void arg) {
        JmmNode node = parent.getChildren().get(0);
        if (constants.containsKey(node.get("id"))) {
            JmmNode newNode = new JmmNodeImpl("Literal");
            newNode.put("value", constants.get(node.get("id")).toString());
            parent.removeJmmChild(node);
            parent.add(newNode);
        }
        return null;
    }
}
