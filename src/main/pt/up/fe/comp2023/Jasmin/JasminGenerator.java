package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.ClassUnit;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private final StringBuilder builder;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.builder = new StringBuilder();
    }

    public String generate() {
        generateClassDeclaration();
        generateSuperDeclaration();
        return builder.toString();
    }

    private void generateClassDeclaration() {
        final String classAccessModifier = JasminUtils.getAccessModifier(classUnit.getClassAccessModifier());
        builder.append(".class ").append(classAccessModifier).append(" ")
                .append(classUnit.getClassName()).append("\n");
    }

    private void generateSuperDeclaration() {
        String superClass = classUnit.getSuperClass() == null ? "java/lang/Object" : classUnit.getSuperClass();
        builder.append(".super ").append(superClass).append("\n");
    }
}
