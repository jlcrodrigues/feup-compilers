package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

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
}
