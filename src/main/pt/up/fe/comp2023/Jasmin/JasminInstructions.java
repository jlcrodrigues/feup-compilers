package pt.up.fe.comp2023.Jasmin;

public class JasminInstructions {
    public static String astore(int index) {
        return "astore" + (index < 4 ? "_": " ") + index;
    }

    public static String istore(int index) {
        return "istore" + (index < 4 ? "_": " ") + index;
    }

    public static String generateInt(String value) {
        int intValue = Integer.parseInt(value);
        if (intValue == -1) {
            return "iconst_m1";
        } else if (intValue >= -1 && intValue <= 5) {
            return "iconst_" + intValue;
        } else if (intValue >= -128 && intValue <= 127) {
            return "bipush " + intValue;
        } else if (intValue >= -32768 && intValue <= 32767) {
            return "sipush " + intValue;
        } else {
            return ldc(value);
        }
    }

    public static String ldc(String value) {
        return "ldc " + value;
    }

    public static String generateBoolean(String value) {
        return value.equals("true") ? "iconst_1" : "iconst_0";
    }

    public static String aload(int virtualReg) {
        return "aload" + (virtualReg < 4 ? "_": " ") + virtualReg;
    }

    public static String iload(int virtualReg) {
        return "iload" + (virtualReg < 4 ? "_": " ") + virtualReg;
    }

    public static String newInstruction(String className) {
        return "new " + className;
    }

    public static String dup() {
        return "dup";
    }

}
