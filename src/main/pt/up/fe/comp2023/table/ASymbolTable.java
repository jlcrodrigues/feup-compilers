package pt.up.fe.comp2023.table;

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
        superName = "";
        importedClasses = List.of();
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

    public void addImport(String path) {
        importedClasses.add(path);
    }

    public void setClassName(String name) {
        className = name;
    }

    public void setSuper(String superName) {
        this.superName = superName;
    }

    public void addField(String name, String type) {
        // TODO fix isArray
        fields.put(name, new Symbol(new Type(type, false), name));
    }

    public void addMethod(SymbolTableMethod method) {
        methods.put(method.getName(), method);
    }

}
