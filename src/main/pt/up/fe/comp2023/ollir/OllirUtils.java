package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirUtils {

    public static String getCode(Symbol symbol){
        return symbol.getName()+"."+ getOllirType(symbol.getType());
    }

    public static String getLiteralType(String value){
        if (value.matches("\\d+")){
            return "i32";
        }
        else if (value.equals("false") || value.equals("true")){
            return "bool";
        }
        return null;
    }

    public static String getOllirType(Type type) {
        String ollirType = type.isArray() ? "array." : "";
        if (type.getName().equals("boolean")) {
            return ollirType + "bool";
        }
        if (type.getName().equals("int")) {
            return ollirType + "i32";
        }
        if (type.getName().equals("void")) {
            return ollirType + "V";
        }
        return ollirType + type.getName();
    }

    public static String getOllirOperator(JmmNode node){
        return switch (node.get("op")) {
            case "+" -> "+.i32 ";
            case "-" -> "-.i32 ";
            case "*" -> "*.i32 ";
            case "/" -> "/.i32 ";
            case "<" -> "<.bool ";
            case "<=" -> "<=.bool ";
            case ">" -> ">.bool ";
            case ">=" -> ">=.bool ";
            case "&&" -> "&&.bool ";
            case "||" -> "||.bool ";
            default -> null;
        };
    }

    public static String getTypeOperator(JmmNode node){
        return switch (node.get("op")) {
            case "+", "-", "*", "/" -> ".i32 ";
            case "&&", "||", "<", "<=", ">", ">=" -> ".bool ";
            default -> null;
        };
    }

    public static String getTypeOperands(JmmNode node){
        return switch (node.get("op")) {
            case "+", "-", "*", "/", "<", "<=", ">", ">=" -> ".i32 ";
            case "&&", "||" -> ".bool ";
            default -> null;
        };
    }

    public static String getParentMethod(JmmNode node) {
        while(!node.getKind().equals("InstanceMethod") && !node.getKind().equals("MainMethod"))
            node = node.getJmmParent();

        if(node.getKind().equals("InstanceMethod"))
            return node.get("id");

        return "main";
    }

    public static String getParentType(JmmNode node, SymbolTable symbolTable){
        var childNode = node;
        node = node.getJmmParent();
        while (!node.getKind().equals("MethodCall") && !node.getKind().equals("Assignment"))
            node = node.getJmmParent();

        if (node.getKind().equals("Assignment"))
            return getOllirType(getSymbol(node,symbolTable).getType());

        return getParameterType(node, childNode, symbolTable);
    }

    public static String getParameterType(JmmNode parentNode, JmmNode childNode, SymbolTable symbolTable){
        var params = symbolTable.getParameters(parentNode.getJmmChild(0).get("id"));
        var index = parentNode.getChildren().indexOf(childNode);

        return getOllirType(params.get(index-1).getType());
    }

    public static Symbol isField(JmmNode node, SymbolTable symbolTable){
        for (Symbol symbol : symbolTable.getFields()) {
            if (symbol.getName().equals(node.get("id"))){
                return symbol;
            }
        }
        return null;
    }

    public static Symbol isParam(JmmNode node, SymbolTable symbolTable, String parentMethod) {
        for (Symbol symbol : symbolTable.getParameters(parentMethod)) {
            if (symbol.getName().equals(node.get("id"))){
                return symbol;
            }
        }
        return null;
    }

    public static Symbol isLocal(JmmNode node, SymbolTable symbolTable, String parentMethod) {
        for (Symbol symbol : symbolTable.getLocalVariables(parentMethod)){
            if (symbol.getName().equals(node.get("id"))) {
                return symbol;
            }
        }
        return null;
    }

    public static Symbol isImport(JmmNode node, SymbolTable symbolTable){
        for (String imp : symbolTable.getImports())
            if (node.get("id").equals(imp))
                return new Symbol(new Type("", false),imp);
        return null;
    }

    public static String invokeSpecial(String variable, String type){
        return "\tinvokespecial("+ variable + "." + type + ",\"<init>\").V;\n";
    }

    public static String putField(String field, StringBuilder rhs, String type){
        return "\tputfield(this, " + field + "." + type + ", " + rhs + "." + type +").V;\n";
    }

    public static String getField(JmmNode node, String type) {
        return "getfield(this, " + node.get("id") + "." + type + ")." + type + ";\n";
    }

    public static Symbol getSymbol(JmmNode node, SymbolTable symbolTable) {
        if (node.getKind().equals("Literal")){
            var type = "";
            if (node.get("value").equals("false") || node.get("value").equals("true"))
                type = "bool";
            else
                type = node.get("value");
            return new Symbol(new Type(type,false),node.get("value"));
        }
        String parentMethod = getParentMethod(node);
        Symbol localSymbol = isLocal(node, symbolTable, parentMethod);
        if (localSymbol != null) {
            return localSymbol;
        }
        Symbol paramSymbol = isParam(node, symbolTable, parentMethod);
        if (paramSymbol != null) {
            return paramSymbol;
        }

        return isField(node, symbolTable);
    }

}
