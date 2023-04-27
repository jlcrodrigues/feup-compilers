package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Void, StringBuilder> {

    public final StringBuilder ollirCode;
    public final SymbolTable symbolTable;
    private int count;

    public OllirGenerator(SymbolTable symbolTable) {
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
        this.count = 0;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Class", this::dealWithClass);
        addVisit("MainMethod", this::dealWithMainMethod);
        addVisit("InstanceMethod", this::dealWithInstanceMethod);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("NewObject", this::dealWithNewObject);
        addVisit("ExpressionStatement", this::dealWithExpressionStatement);
        addVisit("MethodCall", this::dealWithMethodCall);
        addVisit("ChainMethods", this::dealWithChainMethods);
        addVisit("BinaryOp", this::dealWithBinaryOp);
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

        if (OllirUtils.isField(node, symbolTable) != null) {
            ollirCode.append(OllirUtils.putField(node.get("id"),rhs,type));
            return null;
        }

        ollirCode.append("\t");
        ollirCode.append(lhs).append(".").append(type);
        ollirCode.append(" :=.").append(type).append(" ");
        ollirCode.append(rhs).append(".").append(type);
        ollirCode.append(";\n");

        return null;
    }

    private StringBuilder dealWithNewObject(JmmNode node, Void arg) {
        Symbol fieldSymbol = OllirUtils.isField(node.getJmmParent(), symbolTable);
        if (fieldSymbol != null || node.getJmmParent().getKind().equals("ChainMethods") || node.getJmmParent().getKind().equals("Assignment")){
            var type = node.get("id");
            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(".").append(type).append(" ");
            ollirCode.append(":=.").append(type).append(" ");
            ollirCode.append("new(").append(node.get("id")).append(").").append(type).append(";\n");
            ollirCode.append(OllirUtils.invokeSpecial(temp,type));
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
        Symbol symbol = OllirUtils.getSymbol(node.getJmmChild(0).getJmmChild(0),symbolTable);
        String methodType = "";
        String type = "";
        StringBuilder params = new StringBuilder();

        if (symbol != null || node.getJmmChild(0).getJmmChild(0).getKind().equals("NewObject")){

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
                else if (child.getKind().equals("Literal")){
                    type = OllirUtils.getLiteralType(child.get("value"));
                }
                else if (child.getKind().equals("MethodCall"))
                    type = OllirUtils.getOllirType(symbolTable.getReturnType(child.getJmmChild(0).get("id")));
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
        JmmNode src = node.getJmmChild(0);

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

    private StringBuilder dealWithBinaryOp(JmmNode node, Void arg) {

        StringBuilder lhs = visit(node.getJmmChild(0));
        StringBuilder rhs = visit(node.getJmmChild(1));

        String op = OllirUtils.getOllirOperator(node);
        String typeOp = OllirUtils.getTypeOperator(node);

        if (node.getJmmParent().getKind().equals("BinaryOp")
            || node.getJmmParent().getKind().equals("ReturnObject")
            || OllirUtils.isField(node.getJmmParent(),symbolTable) != null){

            String temp = createTemp();
            ollirCode.append("\t").append(temp).append(typeOp).append(":=").append(typeOp);
            ollirCode.append(lhs).append(typeOp).append(op).append(rhs).append(typeOp).append(";\n");

            return new StringBuilder(temp);
        }

        return new StringBuilder(lhs + typeOp + op + rhs);
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
                /*String type = OllirUtils.getOllirType(paramSymbol.getType());
                String temp = createTemp();
                ollirCode.append("\t").append(temp).append(".").append(type);
                ollirCode.append(" :=.").append(type).append(" ");
                ollirCode.append("$").append(index).append(".").append(node.get("id")).append(".").append(type).append(";\n");
                return new StringBuilder(temp);*/
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
                    return new StringBuilder(temp);
                }
            }
            else
                return new StringBuilder(node.get("id"));
        }
        return null;
    }

    private String createTemp() {
        return "t" + count++;
    }

    private StringBuilder joinParamsWithTypes(JmmNode node, SymbolTable symbolTable){
        StringBuilder params = new StringBuilder();
        var values = node.getChildren();
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
