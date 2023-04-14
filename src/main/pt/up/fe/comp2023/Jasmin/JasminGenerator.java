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
        generateFieldsDeclaration();
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

    private void generateFieldsDeclaration() {
        classUnit.getFields().forEach(field -> {
            final String fieldAccessModifier = JasminUtils.getAccessModifier(field.getFieldAccessModifier());
            final String fieldType = JasminUtils.getFieldType(field.getFieldType());
            builder.append(".field ").append(fieldAccessModifier).append(" ");
            if (field.isStaticField()) builder.append("static ");
            if (field.isFinalField()) builder.append("final ");
            builder.append(field.getFieldName()).append(" ").append(fieldType).append("\n");
        });
        builder.append("\n");
    }
}
