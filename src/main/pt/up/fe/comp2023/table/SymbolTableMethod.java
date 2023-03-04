package pt.up.fe.comp2023.table;

import java.util.Map;

public class SymbolTableMethod {
    private String name;
    private String returnType;
    private Map<String, String> parameters;
    private Map<String, String> localVariables;

    public SymbolTableMethod(String name) {
        this.name = name;
        this.returnType = "void";
        this.parameters = Map.of();
        this.localVariables = Map.of();
    }

    public String getName() {
        return name;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnType() {
        return returnType;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getLocalVariables() {
        return localVariables;
    }

    public void addParameter(String name, String type) {
        parameters.put(name, type);
    }

    public void addLocalVariable(String name, String type) {
        localVariables.put(name, type);
    }
}
