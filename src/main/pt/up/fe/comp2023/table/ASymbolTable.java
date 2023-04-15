package pt.up.fe.comp2023.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class ASymbolTable implements SymbolTable {
    private String className;
    private String superName;
    private List<String> importedClasses;
    private Map<String, Symbol> fields;
    private Map<String, SymbolTableMethod> methods;

    public ASymbolTable() {
        className = "";
        superName = "java.lang.Object";
        importedClasses = new ArrayList<String>();
        fields = new HashMap<String, Symbol>();
        methods = new HashMap<String, SymbolTableMethod>();
    }

    @Override
    public List<String> getImports() {
        return importedClasses;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superName;
    }

    @Override
    public List<Symbol> getFields() {
        return List.copyOf(fields.values());
    }

    @Override
    public List<String> getMethods() {
        return List.copyOf(methods.keySet());
    }

    @Override
    public Type getReturnType(String s) {
        return methods.get(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return methods.get(s).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return methods.get(s).getLocalVariables();
    }

    @Override
    public String toString() {
        String res = "";
        for (String s : importedClasses) {
            res += "[import] " + s + "\n";
        }
        res += "[class] " + className;
        if (superName != "") res += " [extends] " + superName + "\n";
        for (String s : fields.keySet()) {
            res += "  [field] (" + fields.get(s).getType().getName() + ") " + s + "\n";
        }
        for (SymbolTableMethod method : methods.values()) {
            res += method.toString();
        }
        return res;
    }

    public void addImport(String path) {
        importedClasses.add(path);
    }

    public void setClassName(String name) {
        className = name;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    public void addField(String name, Symbol symbol) {
        fields.put(name, symbol);
    }

    public void addMethod(SymbolTableMethod method) {
        methods.put(method.getName(), method);
    }

    public Type getFieldType(String name) {
        if (fields.containsKey(name))
            return fields.get(name).getType();
        return null;
    }

    public Type getVariableType(String name, String methodName) {
        Type type = getFieldType(name);
        if (type == null) {
            if (methods.containsKey(name))
                return methods.get(methodName).getFieldType(name);
        }
        return type;
    }

    public SymbolTableMethod getMethod(String name) {
        if (methods.containsKey(name))
            return methods.get(name);
        return null;
    }
}
