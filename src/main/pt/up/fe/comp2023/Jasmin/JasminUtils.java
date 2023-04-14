package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.AccessModifiers;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.Type;

public class JasminUtils {
    public static String getAccessModifier(AccessModifiers accessModifier) {
        return switch (accessModifier) {
            case PUBLIC, DEFAULT -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
        };
    }

    public static String getFieldType(Type type){
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> "[I"; //only integer for now
            case OBJECTREF,CLASS-> "L" + ((ClassType)type).getName() + ";";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            default -> throw new IllegalStateException("Unexpected value: " + type.getTypeOfElement());
        };
    }
}
