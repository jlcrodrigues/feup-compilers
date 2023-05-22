package pt.up.fe.comp2023.Jasmin;

public class JasminInstructions {
    public static String astore(int index) {
        JasminGenerator.updateStack(-1);
        return "astore" + (index < 4 ? "_": " ") + index;
    }

    public static String istore(int index) {
        JasminGenerator.updateStack(-1);
        return "istore" + (index < 4 ? "_": " ") + index;
    }

    public static String iastore() {
        JasminGenerator.updateStack(-3);
        return "iastore";
    }

    public static String aastore() {
        JasminGenerator.updateStack(-3);
        return "aastore";
    }

    public static String generateInt(String value) {
        JasminGenerator.updateStack(1);
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
        JasminGenerator.updateStack(1);
        return "aload" + (virtualReg < 4 ? "_": " ") + virtualReg;
    }

    public static String iload(int virtualReg) {
        JasminGenerator.updateStack(1);
        return "iload" + (virtualReg < 4 ? "_": " ") + virtualReg;
    }

    public static String iaload() {
        JasminGenerator.updateStack(-1);
        return "iaload";
    }

    public static String aaload() {
        JasminGenerator.updateStack(-1);
        return "aaload";
    }

    public static String newInstruction(String className) {
        JasminGenerator.updateStack(1);
        return "new " + className;
    }

    public static String newarray(String type) {
        return "newarray " + type;
    }

    public static String arraylength() {
        return "arraylength";
    }

    public static String dup() {
        JasminGenerator.updateStack(1);
        return "dup";
    }

    public static String ifne(String label) {
        JasminGenerator.updateStack(-1);
        return "ifne " + label;
    }

    public static String ifeq(String label) {
        JasminGenerator.updateStack(-1);
        return "ifeq " + label;
    }

    public static String iflt(String label) {
        JasminGenerator.updateStack(-1);
        return "iflt " + label;
    }

    public static String ifgt(String label) {
        JasminGenerator.updateStack(-1);
        return "ifgt " + label;
    }

    public static String ifle(String label) {
        JasminGenerator.updateStack(-1);
        return "ifle " + label;
    }

    public static String ifge(String label) {
        JasminGenerator.updateStack(-1);
        return "ifge " + label;
    }

    public static String if_icmpeq(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmpeq " + label;
    }

    public static String if_icmpge(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmpge " + label;
    }

    public static String if_icmpgt(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmpgt " + label;
    }

    public static String if_icmple(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmple " + label;
    }

    public static String if_icmplt(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmplt " + label;
    }

    public static String if_icmpne(String label) {
        JasminGenerator.updateStack(-2);
        return "if_icmpne " + label;
    }

    public static String getfield(String className, String fieldName, String fieldType) {
        return "getfield " + className + "/" + fieldName + " " + fieldType;
    }

    public static String putfield(String className, String fieldName, String fieldType) {
        JasminGenerator.updateStack(-2);
        return "putfield " + className + "/" + fieldName + " " + fieldType;
    }

    public static String iand() {
        JasminGenerator.updateStack(-1);
        return "iand";
    }

    public static String ior() {
        JasminGenerator.updateStack(-1);
        return "ior";
    }

    public static String iadd() {
        JasminGenerator.updateStack(-1);
        return "iadd";
    }

    public static String isub() {
        JasminGenerator.updateStack(-1);
        return "isub";
    }

    public static String imul() {
        JasminGenerator.updateStack(-1);
        return "imul";
    }

    public static String idiv() {
        JasminGenerator.updateStack(-1);
        return "idiv";
    }

    public static String ixor() {
        JasminGenerator.updateStack(-1);
        return "ixor";
    }

    public static String ireturn() {
        JasminGenerator.updateStack(-1);
        return "ireturn";
    }

    public static String areturn() {
        JasminGenerator.updateStack(-1);
        return "areturn";
    }

    public static String iinc(int virtualReg, int value) {
        return "iinc " + virtualReg + " " + value;
    }

    public static String gotoInstruction(String label) {
        return "goto " + label;
    }

    public static String pop(){
        JasminGenerator.updateStack(-1);
        return "pop";
    }

}
