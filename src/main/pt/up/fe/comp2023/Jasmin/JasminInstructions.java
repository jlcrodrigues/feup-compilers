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

    public static String ifne(String label) {
        return "ifne " + label;
    }

    public static String ifeq(String label) {
        return "ifeq " + label;
    }

    public static String iflt(String label) {
        return "iflt " + label;
    }

    public static String ifgt(String label) {
        return "ifgt " + label;
    }

    public static String ifle(String label) {
        return "ifle " + label;
    }

    public static String ifge(String label) {
        return "ifge " + label;
    }

    public static String if_icmpeq(String label) {
        return "if_icmpeq " + label;
    }

    public static String if_icmpge(String label) {
        return "if_icmpge " + label;
    }

    public static String if_icmpgt(String label) {
        return "if_icmpgt " + label;
    }

    public static String if_icmple(String label) {
        return "if_icmple " + label;
    }

    public static String if_icmplt(String label) {
        return "if_icmplt " + label;
    }

    public static String if_icmpne(String label) {
        return "if_icmpne " + label;
    }

    public static String iand() {
        return "iand";
    }

    public static String ior() {
        return "ior";
    }

    public static String iadd() {
        return "iadd";
    }

    public static String isub() {
        return "isub";
    }

    public static String imul() {
        return "imul";
    }

    public static String idiv() {
        return "idiv";
    }

    public static String ixor() {
        return "ixor";
    }

    public static String ireturn() {
        return "ireturn";
    }

    public static String areturn() {
        return "areturn";
    }

    public static String iinc(int virtualReg, int value) {
        return "iinc " + virtualReg + " " + value;
    }

    public static String gotoInstruction(String label) {
        return "goto " + label;
    }

}
