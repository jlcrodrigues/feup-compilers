package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirUtils {

    public static String getCode(Symbol symbol){
        return symbol.getName()+"."+ getOllirType(symbol.getType());
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
            case "&&" -> "&&.bool ";
            case "||" -> "||.bool ";
            default -> null;
        };
    }

    public static String getTypeOperator(JmmNode node){
        return switch (node.get("op")) {
            case "+", "-", "*", "/" -> ".i32 ";
            case "&&" -> ".bool ";
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

}
