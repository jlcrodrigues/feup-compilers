package pt.up.fe.comp2023.table;

import java.util.List;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class ASymbolTable implements SymbolTable {
    private String className;
    private String superName;
    private List<String> importedClasses;
    private Map<String, SymbolTableMethod> methods;

    public ASymbolTable() {
        importedClasses = List.of();
        methods = Map.of();
    }

    @Override
    public List<String> getImports() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String s) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String s) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return null;
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

    public void addMethod(SymbolTableMethod method) {
        methods.put(method.getName(), method);
    }
}
