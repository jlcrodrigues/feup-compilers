package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;

public class JasminUtils {
    public static String getAccessModifier(AccessModifiers accessModifier) {
        return switch (accessModifier) {
            case PUBLIC, DEFAULT -> "public";
            case PRIVATE -> "private";
            case PROTECTED -> "protected";
        };
    }

    public static String getFullClassName(String className, ClassUnit classUnit) {
        for (String importStr : classUnit.getImports()) {
            String[] importArr = importStr.split("\\.");
            String lastName = importArr.length == 0 ? importStr : importArr[importArr.length - 1];
            if (lastName.equals(className)) {
                return importStr.replace(".", "/");
            }
        }

        return className;
    }

    public static String getFieldType(Type type, boolean complete, ClassUnit classUnit) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> { //for now only 1 dimension arrays
                ArrayType arrayType = (ArrayType) type;
                yield "[" + getFieldType(arrayType.getElementType(),complete,classUnit);
            }
            case OBJECTREF,CLASS->{
                String className = JasminUtils.getFullClassName(((ClassType)type).getName(),classUnit);
                yield complete ? "L" + className + ";" : className;
            }
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            case THIS -> classUnit.getClassName();
        };
    }

    public static Descriptor getDescriptor(Element elem,Method method) {
        if (elem.isLiteral()){
            return null;
        }

        if(elem.getType().getTypeOfElement() == ElementType.THIS) {
            return method.getVarTable().get("this");
        }
        return method.getVarTable().get(((Operand)elem).getName());
    }

    public static String getElementName(Element elem) {
        if (elem.isLiteral())
            return ((LiteralElement) elem).getLiteral();
        return ((Operand) elem).getName();
    }
}
