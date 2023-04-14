package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.AccessModifiers;

public class JasminUtils {
    public static String getAccessModifier(AccessModifiers accessModifier) {
        return switch (accessModifier) {
            case PUBLIC, DEFAULT -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
        };
    }
}
