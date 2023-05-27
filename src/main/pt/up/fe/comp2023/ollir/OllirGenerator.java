package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Void, StringBuilder> {

    public final StringBuilder ollirCode;
    public final SymbolTable symbolTable;
    private int tempCount;
    private int auxIfLabel;
    private int auxWhileLabel;
    private HashMap<String,String> tempTypes;

    public OllirGenerator(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
        this.tempCount = 0;
        this.auxIfLabel = 0;
        this.auxWhileLabel = 0;
        this.tempTypes = new HashMap<>();
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("InstanceMethod", this::dealWithInstanceMethod);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("ArrayAssignment", this::dealWithArrayAssignment);
        addVisit("NewArray", this::dealWithNewArray);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("MemberAccessLength", this::dealWithArrayLength);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("ExpressionStatement", this::dealWithExpressionStatement);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("ChainMethods", this::dealWithChainMethods);
        addVisit("While",this::dealWithWhile);
        addVisit("If", this::dealWithIf);
        addVisit("Condition", this::dealWithCondition);
        addVisit("Block", this::dealWithBlock);
        addVisit("Else", this::dealWithElse);
        addVisit("Parentheses", this::dealWithParentheses);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Negate", this::dealWithNegate);
        addVisit("Literal", this::dealWithLeafNode);
        addVisit("Variable", this::dealWithLeafNode);
    }

    private StringBuilder dealWithProgram(JmmNode node, Void arg) {
        for (var importString : symbolTable.getImports()) {
            ollirCode.append("import ").append(importString).append(";\n");
        }
        ollirCode.append("\n");
        for (var child : node.getChildren()) {
            if (!child.getKind().equals("Import"))
                visit(child);
        }
        return null;
    }

    private StringBuilder dealWithClass(JmmNode node, Void arg) {
        String superClass = symbolTable.getSuper();

        ollirCode.append(symbolTable.getClassName());
        if (!Objects.equals(superClass, "")
                && !Objects.equals(superClass, "java.lang.Object"))
            ollirCode.append(" extends ").append(superClass);

        ollirCode.append("{\n\n");

        List<Symbol> fields = symbolTable.getFields();
        if (!fields.isEmpty()){
            for (Symbol field : fields) {
                ollirCode.append(".field private ");
                ollirCode.append(OllirUtils.getCode(field));
                ollirCode.append(";\n");
            }
        }

        ollirCode.append("\n.construct ").append(symbolTable.getClassName()).append("().V{\n");
        ollirCode.append("\tinvokespecial(this, \"<init>\").V;\n}\n");

        for (var child : node.getChildren()) {
            if (!child.getKind().equals("Extends") && !child.getKind().equals("Var")) {
                visit(child);
            }
        }

        ollirCode.append("}\n");

        return null;
    }

    private StringBuilder dealWithMainMethod(JmmNode node, Void arg) {

        String returnType = OllirUtils.getOllirType(symbolTable.getReturnType("main"));
        String paramCode = symbolTable.getParameters("main")
                .stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append("\n.method public static main(");
        ollirCode.append(paramCode).append(").");
        ollirCode.append(returnType);
        ollirCode.append("{\n");

        for (JmmNode child : node.getChildren()) {
            if (!child.getKind().equals("Var")){
                visit(child);
            }
        }

        ollirCode.append("\n\tret.V;\n");
        ollirCode.append("}\n\n");

        return null;
    }

    private StringBuilder dealWithInstanceMethod(JmmNode node, Void arg) {

        String returnType = OllirUtils.getOllirType(symbolTable.getReturnType(node.get("id")));
        String paramCode = symbolTable.getParameters(node.get("id"))
                .stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        ollirCode.append("\n.method public ").append(node.get("id"));
        ollirCode.append("(").append(paramCode).append(").");
        ollirCode.append(returnType).append("{\n");

        for (JmmNode child : node.getChildren()) {
            if (!child.getKind().equals("ReturnType") && !child.getKind().equals("Var")
                    && !child.getKind().equals("ReturnObject") && !child.getKind().equals("ArgumentObject"))
                visit(child);
        }

        StringBuilder returnObject = visit(node.getJmmChild(node.getNumChildren()-1).getJmmChild(0));

        ollirCode.append("\n\tret.").append(returnType).append(" ");
        ollirCode.append(returnObject).append(".").append(returnType);
        ollirCode.append(";\n}\n");

        return null;
    }

    private StringBuilder dealWithAssignment(JmmNode node, Void arg) {

        String lhs = node.get("id");
        StringBuilder rhs = visit(node.getJmmChild(0));
        String type = OllirUtils.getOllirType(OllirUtils.getSymbol(node,symbolTable).getType());

        if (OllirUtils.isLocal(node, symbolTable, OllirUtils.getParentMethod(node)) == null
            && OllirUtils.isParam(node, symbolTable, OllirUtils.getParentMethod(node)) == null
            && OllirUtils.isField(node,symbolTable) != null) {
            ollirCode.append(OllirUtils.putField(node.get("id"),rhs,type));
            return null;
        }

        ollirCode.append("\t");
        ollirCode.append(lhs).append(".").append(type);
        ollirCode.append(" :=.").append(type).append(" ");
        ollirCode.append(rhs);
        if (!node.getJmmChild(0).getKind().equals("BinaryOp"))
            ollirCode.append(".").append(type);
        ollirCode.append(";\n");

        return null;
    }

    private StringBuilder dealWithArrayAssignment(JmmNode node, Void arg){
        StringBuilder arrayAccess = visit(node.getJmmChild(0));
        StringBuilder rhs = visit(node.getJmmChild(1));
        String temp = createTemp();
        ollirCode.append("\t").append(temp).append(".i32 ");
        ollirCode.append(":=.i32 ").append(arrayAccess).append(".i32").append(";\n");
        ollirCode.append("\t").append(node.get("id")).append("[").append(temp).append(".i32").append("].i32 ");
        ollirCode.append(":=.i32 ").append(rhs).append(".i32;\n");

        return null;
    }

    private StringBuilder dealWithNewArray(JmmNode node, Void arg){
        var rhs = visit(node.getJmmChild(0));
        String temp = createTemp();
        ollirCode.append("\t").append(temp).append(".i32").append(" ");
        ollirCode.append(":=.i32").append(" ");
        ollirCode.append(rhs).append(".i32").append(";\n");

        return new StringBuilder("new(array, " + temp + ".i32)");
    }

    private StringBuilder dealWithArrayAccess(JmmNode node, Void arg){
        String temp1 = createTemp();
        StringBuilder variable = visit(node.getJmmChild(0));
        StringBuilder arrayAccess = visit(node.getJmmChild(1));
        ollirCode.append("\t").append(temp1).append(".i32 ");
        ollirCode.append(":=.i32 ").append(arrayAccess).append(".i32").append(";\n");
        String temp2 = createTemp();
        ollirCode.append("\t").append(temp2).append(".i32 :=.i32 ").append(variable).append("[").append(temp1).append(".i32").append("].i32;\n");
        return new StringBuilder(temp2);
    }

    private StringBuilder dealWithArrayLength(JmmNode node, Void arg){
        String temp = createTemp();
        StringBuilder variable = visit(node.getJmmChild(0));
        ollirCode.append("\t").append(temp).append(".i32 :=.i32 ");
        ollirCode.append("arraylength(").append(variable).append(".array.i32).i32;\n");
        return new StringBuilder(temp);
    }

    private StringBuilder dealWithNewObject(JmmNode node, Void arg) {
        Symbol fieldSymbol = OllirUtils.isField(node.getJmmParent(), symbolTable);
        if (fieldSymbol != null || node.getJmmParent().getKind().equals("ChainMethods")
                || node.getJmmParent().getKind().equals("Assignment") || node.getJmmParent().getKind().equals("MethodCall")
                || node.getJmmParent().getKind().equals("Parentheses")){
            var type = node.get("id");
            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(".").append(type).append(" ");
            ollirCode.append(":=.").append(type).append(" ");
            ollirCode.append("new(").append(node.get("id")).append(").").append(type).append(";\n");
            ollirCode.append(OllirUtils.invokeSpecial(temp,type));
            tempTypes.put(temp,type);
            return new StringBuilder(temp);
        }
        return new StringBuilder("new("+node.get("id")+")");
    }

    private StringBuilder dealWithExpressionStatement(JmmNode node, Void arg){
        ollirCode.append("\n\t").append(visit(node.getJmmChild(0)));
        return null;
    }

    private StringBuilder dealWithMethodCall(JmmNode node, Void arg) {

        StringBuilder methodInvokeString = visit(node.getJmmChild(0));
        StringBuilder result = new StringBuilder();
        var grandChild = node.getJmmChild(0).getJmmChild(0).getKind().equals("Parentheses") ? node.getJmmChild(0).getJmmChild(0).getJmmChild(0):node.getJmmChild(0).getJmmChild(0);
        Symbol symbol = OllirUtils.getSymbol(grandChild,symbolTable);
        String methodType = "";
        String type = "";
        StringBuilder params = new StringBuilder();

        if (symbol != null || grandChild.getKind().equals("NewObject")){

            if (symbolTable.getMethods().contains(node.getJmmChild(0).get("id"))){
                methodType = OllirUtils.getOllirType(symbolTable.getReturnType(node.getJmmChild(0).get("id")));
                params = joinParamsWithTypes(node,symbolTable);
            }
            if (params.isEmpty())
                result.append("invokevirtual(").append(methodInvokeString).append(")");
            else
                result.append("invokevirtual(").append(methodInvokeString).append(", ").append(params).append(")");
        }
        else{
            var values = node.getChildren();
            values.remove(0);
            for (var child : values) {
                if (child.getKind().equals("Variable")) {
                    symbol = OllirUtils.getSymbol(child, symbolTable);
                    if (symbol != null)
                        type = OllirUtils.getOllirType(symbol.getType());
                }
                else if (child.getKind().equals("Literal"))
                    type = OllirUtils.getLiteralType(child.get("value"));
                else if (child.getKind().equals("MethodCall"))
                    type = OllirUtils.getOllirType(symbolTable.getReturnType(child.getJmmChild(0).get("id")));
                else if (child.getKind().equals("NewObject"))
                    type = child.get("id");
                else if (child.getKind().equals("BinaryOp"))
                    type = tempTypes.get(visit(child).toString()).substring(1);
                else if (child.getKind().equals("Negate"))
                    type = "bool";
                else if (child.getKind().equals("ArrayAccess"))
                    type = "i32";
                else
                    type = "V";

                params.append(visit(child)).append(".").append(type).append(",");
            }
            if (params.length() > 0 && params.charAt(params.length() - 1) == ',') {
                params.deleteCharAt(params.length() - 1);
                result.append("invokestatic(").append(methodInvokeString).append(", ").append(params).append(")");
            }
            else
                result.append("invokestatic(").append(methodInvokeString).append(")");
        }

        if(!node.getJmmParent().getKind().equals("ExpressionStatement") && !node.getJmmParent().getKind().equals("Assignment")){
            if (methodType.equals(""))
                methodType = OllirUtils.getParentType(node,symbolTable);
            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(".").append(methodType).append(" :=").append(".").append(methodType);
            ollirCode.append(" ").append(result).append(".").append(methodType).append(";\n");
            tempTypes.put(temp,type);
            return new StringBuilder(temp);
        }
        if(node.getJmmParent().getKind().equals("ExpressionStatement")){
            if (methodType.isEmpty())
                result.append(".V;\n");
            else
                result.append(".").append(methodType).append(";\n");
        }

        return result;
    }

    private StringBuilder dealWithChainMethods(JmmNode node, Void arg) {
        String type = "";
        JmmNode src = node.getJmmChild(0).getKind().equals("Parentheses") ? node.getJmmChild(0).getJmmChild(0) : node.getJmmChild(0);

        if (src.getKind().equals("Variable") && OllirUtils.isImport(src,symbolTable) == null){
            type = OllirUtils.getOllirType(OllirUtils.getSymbol(src,symbolTable).getType());
        }
        else if (src.getKind().equals("NewObject"))
            type = src.get("id");

        StringBuilder variable = visit(src);
        String methodInvokeString;
        if (Objects.equals(type, "")) {
            methodInvokeString = variable.toString() + ", " + "\"" + node.get("id") + "\"";
        } else
            methodInvokeString = variable.toString() + "." + type + ", " + "\"" + node.get("id") + "\"";

        return new StringBuilder(methodInvokeString);
    }

    private StringBuilder dealWithWhile(JmmNode node, Void arg) {
        StringBuilder condition = visit(node.getJmmChild(0));
        int currentLabel = ++auxWhileLabel;

        ollirCode.append("\nif (").append(condition).append(")").append(" goto While").append(currentLabel).append(";\n");
        ollirCode.append("goto EndWhile").append(currentLabel).append(";\n");
        ollirCode.append("While").append(currentLabel).append(":\n");

        visit(node.getJmmChild(1));

        condition = visit(node.getJmmChild(0));
        ollirCode.append("\nif (").append(condition).append(")").append(" goto While").append(currentLabel).append(";\n");
        ollirCode.append("EndWhile").append(currentLabel).append(":\n");
        return null;
    }

    private StringBuilder dealWithIf(JmmNode node, Void arg) {
        List<JmmNode> children = node.getChildren();
        int currentLabel = ++auxIfLabel;

        children.remove(0);
        ollirCode.append("\nif (").append(visit(node.getJmmChild(0))).append(")");

        if (children.get(children.size()-1).getKind().equals("Else")) {
            ollirCode.append(" goto Then").append(currentLabel).append(";\n");
            ollirCode.append("goto Else").append(currentLabel).append(";\n");
            ollirCode.append("Then").append(currentLabel).append(":\n");
            visit(node.getJmmChild(1));
            ollirCode.append("goto EndIf").append(currentLabel).append(";\n");
            visit(node.getJmmChild(2));
        }
        else {
            ollirCode.append(" goto EndIf").append(currentLabel).append(";\n");
            visit(node.getJmmChild(1));
        }

        ollirCode.append("EndIf").append(currentLabel).append(":\n");
        return null;
    }

    private StringBuilder dealWithCondition(JmmNode node, Void arg) {
        StringBuilder condition = visit(node.getJmmChild(0));

        if (!node.getJmmChild(0).getKind().equals("BinaryOp")){
            String type = OllirUtils.getOllirType(OllirUtils.getSymbol(node.getJmmChild(0),symbolTable).getType());
            return new StringBuilder(condition+"."+type);
        }

        return new StringBuilder(condition);
    }

    private StringBuilder dealWithElse(JmmNode node, Void arg) {
        List<JmmNode> children = node.getChildren();

        ollirCode.append("Else").append(auxIfLabel).append(":\n");
        for (var child : children){
            visit(child);
        }
        return null;
    }

    private StringBuilder dealWithBlock(JmmNode node, Void arg){
        for (var child : node.getChildren())
            visit(child);
        return null;
    }

    private StringBuilder dealWithParentheses(JmmNode node, Void arg) {
        return visit(node.getJmmChild(0));
    }

    private StringBuilder dealWithBinaryOp(JmmNode node, Void arg) {

        StringBuilder lhs = visit(node.getJmmChild(0));
        StringBuilder rhs = visit(node.getJmmChild(1));

        String op = OllirUtils.getOllirOperator(node);
        String typeOp = OllirUtils.getTypeOperator(node);
        String typeOperands = OllirUtils.getTypeOperands(node);

        if ((!node.getJmmParent().getKind().equals("Assignment")
            && !node.getJmmParent().getKind().equals("Condition"))
            || OllirUtils.isField(node.getJmmParent(),symbolTable) != null){

            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(typeOp).append(":=").append(typeOp);
            ollirCode.append(lhs).append(typeOperands);
            ollirCode.append(op);
            ollirCode.append(rhs).append(typeOperands).append(";\n");
            tempTypes.put(temp,typeOp);
            return new StringBuilder(temp);
        }

        return new StringBuilder(lhs + typeOperands + op + rhs + typeOperands);
    }

    private StringBuilder dealWithNegate(JmmNode node, Void arg) {
        var child = visit(node.getJmmChild(0));
        if ((!node.getJmmParent().getKind().equals("Assignment")
                && !node.getJmmParent().getKind().equals("Condition"))
                || OllirUtils.isField(node.getJmmParent(),symbolTable) != null){

            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(".bool :=.bool !.bool ").append(child).append(".bool;\n");
            tempTypes.put(temp,"bool");
            return new StringBuilder(temp);
        }
        return new StringBuilder("!.bool " + child);
    }

    private StringBuilder dealWithLeafNode(JmmNode node, Void arg) {
        if (Objects.equals(node.getKind(), "Literal")){
            return switch (node.get("value")) {
                case "true" -> new StringBuilder("1");
                case "false" -> new StringBuilder("0");
                default -> new StringBuilder(node.get("value"));
            };
        }

        Symbol fieldSymbol = OllirUtils.isField(node, symbolTable);
        String parentMethod = OllirUtils.getParentMethod(node);
        Symbol paramSymbol = OllirUtils.isParam(node, symbolTable, parentMethod);
        Symbol localSymbol = OllirUtils.isLocal(node, symbolTable, parentMethod);

        if (Objects.equals(node.getKind(), "Variable")){
            if (localSymbol != null){
                return new StringBuilder(node.get("id"));
            }
            else if (paramSymbol != null){
                int index = symbolTable.getParameters(parentMethod).indexOf(paramSymbol)+1;
                return new StringBuilder("$" + index + "." + node.get("id"));
            }
            else if (fieldSymbol != null){
                String type = OllirUtils.getOllirType(fieldSymbol.getType());
                if (node.getJmmParent().getKind().equals("Assignment"))
                    return new StringBuilder("getfield(this, " + node.get("id") + "." + type + ")");
                else{
                    String temp = createTemp();
                    ollirCode.append("\t").append(temp).append(".").append(type);
                    ollirCode.append(" :=.").append(type).append(" ");
                    ollirCode.append(OllirUtils.getField(node,type));
                    tempTypes.put(temp,type);
                    return new StringBuilder(temp);
                }
            }
            else
                return new StringBuilder(node.get("id"));
        }
        return null;
    }

    private String createTemp() {
        return "t" + tempCount++;
    }

    private StringBuilder joinParamsWithTypes(JmmNode node, SymbolTable symbolTable){
        StringBuilder params = new StringBuilder();
        List<JmmNode> values = node.getChildren();
        values.remove(0);
        Iterator<JmmNode> iter1 = values.iterator();
        Iterator<Symbol> iter2 = symbolTable.getParameters(node.getJmmChild(0).get("id")).iterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            params.append(visit(iter1.next()).toString());
            params.append(".");
            params.append(OllirUtils.getOllirType(iter2.next().getType()));
            params.append(",");
        }
        if (params.length() > 0 && params.charAt(params.length() - 1) == ',') {
            params.deleteCharAt(params.length() - 1);
        }
        return params;
    }
}
