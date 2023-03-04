package pt.up.fe.comp2023.table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class SymbolTableMethod {
    private String name;
    private Type returnType;
    private Map<String, Symbol> parameters;
    private Map<String, Symbol> localVariables;

    public SymbolTableMethod(String name) {
        this.name = name;
        this.returnType = null;
        this.parameters = new HashMap<String, Symbol>();
        this.localVariables = new HashMap<String, Symbol>();
    }

    public String getName() {
        return name;
    }

    public void setReturnType(Type type) {
        this.returnType = type;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return List.copyOf(parameters.values());
    }

    public List<Symbol> getLocalVariables() {
        return List.copyOf(localVariables.values());
    }

    public void addParameter(String name, Type type) {
        parameters.put(name, new Symbol(type, name));
    }

    public void addLocalVariable(String name, Type type) {
        localVariables.put(name, new Symbol(type, name));
    }
}
