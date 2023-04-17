package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;

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
        generateMethodsDeclaration();
        return builder.toString();
    }

    private void generateClassDeclaration() {
        final String classAccessModifier = JasminUtils.getAccessModifier(classUnit.getClassAccessModifier());
        builder.append(".class ").append(classAccessModifier).append(" ")
                .append(classUnit.getClassName()).append("\n");
    }

    private void generateSuperDeclaration() {
        String superClass = classUnit.getSuperClass() == null ? "java/lang/Object" :
                JasminUtils.getFullClassName(classUnit.getSuperClass(),classUnit);
        builder.append(".super ").append(superClass).append("\n");
        builder.append("\n");
    }

    private void generateFieldsDeclaration() {
        classUnit.getFields().forEach(field -> {
            final String fieldAccessModifier = JasminUtils.getAccessModifier(field.getFieldAccessModifier());
            final String fieldType = JasminUtils.getFieldType(field.getFieldType(),true,classUnit);
            builder.append(".field ").append(fieldAccessModifier).append(" ");
            if (field.isStaticField()) builder.append("static ");
            if (field.isFinalField()) builder.append("final ");
            builder.append(field.getFieldName()).append(" ").append(fieldType).append("\n");
        });
        builder.append("\n");
    }

    private void generateMethodsDeclaration() {
        classUnit.getMethods().forEach(method -> {
            final String methodAccessModifier = JasminUtils.getAccessModifier(method.getMethodAccessModifier());
            final String methodReturnType = JasminUtils.getFieldType(method.getReturnType(),true,classUnit);
            final String methodName = method.isConstructMethod() ? "<init>" : method.getMethodName();

            builder.append(".method ").append(methodAccessModifier).append(" ");
            if (method.isStaticMethod()) builder.append("static ");
            if (method.isFinalMethod()) builder.append("final ");

            builder.append(methodName).append("(");
            method.getParams().forEach(parameter -> {
                final String parameterType = JasminUtils.getFieldType(parameter.getType(),true,classUnit);
                builder.append(parameterType);
            });
            builder.append(")");
            builder.append(methodReturnType).append("\n");
            builder.append(".limit stack 99\n");
            builder.append(".limit locals 99\n");
            generateMethodBody(method);
            builder.append(".end method\n\n");
        });
    }

    private void generateMethodBody(Method method) {
        method.getInstructions().forEach(instruction -> generateInstruction(instruction,method));
    }

    private void generateInstruction(Instruction instruction,Method method) {
        switch (instruction.getInstType()){
            case ASSIGN -> generateAssignInstruction((AssignInstruction) instruction,method);
            case CALL -> generateCallInstruction((CallInstruction) instruction,method);
            case GOTO -> generateGotoInstruction(instruction);
            case BRANCH -> generateBranchInstruction(instruction);
            case RETURN -> generateReturnInstruction((ReturnInstruction) instruction,method);
            case PUTFIELD -> generateFieldInstruction((FieldInstruction) instruction,method, true);
            case GETFIELD -> generateFieldInstruction((FieldInstruction) instruction,method, false);
            case UNARYOPER -> generateUnaryInstruction((UnaryOpInstruction) instruction,method);
            case BINARYOPER -> generateBinaryInstruction((BinaryOpInstruction) instruction,method);
            case NOPER -> generateNoperInstruction((SingleOpInstruction) instruction,method);
        }
    }

    private void generateNoperInstruction(SingleOpInstruction instruction,Method method) {
        generateLoadInstruction(instruction.getSingleOperand(),method);
    }

    private void generateBinaryInstruction(BinaryOpInstruction instruction, Method method) {
        generateLoadInstruction(instruction.getLeftOperand(),method);
        generateLoadInstruction(instruction.getRightOperand(),method);

        switch (instruction.getOperation().getOpType()){
            case ADD -> builder.append("\t").append("iadd").append("\n");
            case SUB -> builder.append("\t").append("isub").append("\n");
            case MUL -> builder.append("\t").append("imul").append("\n");
            case DIV -> builder.append("\t").append("idiv").append("\n");
        }
    }

    private void generateUnaryInstruction(UnaryOpInstruction instruction , Method method) {
        if (instruction.getOperation().getOpType() != OperationType.NOT ||instruction.getOperation().getOpType() != OperationType.NOTB)
            return;
        generateLoadInstruction(instruction.getOperand(),method);
        JasminInstructions.generateInt("-1");
        builder.append("\t").append("ixor").append("\n");
    }

    private void generateFieldInstruction(FieldInstruction instruction, Method method, boolean putfield){
        generateLoadInstruction(instruction.getFirstOperand(),method);
        if (putfield)
            generateLoadInstruction(((PutFieldInstruction)instruction).getThirdOperand(),method);
        String className = JasminUtils.getFieldType(instruction.getFirstOperand().getType(),false,classUnit);
        String fieldName = JasminUtils.getElementName(instruction.getSecondOperand());
        String fieldType = JasminUtils.getFieldType(instruction.getSecondOperand().getType(),true,classUnit);
        if (putfield)
            builder.append("\t").append("putfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append("\n");
        else
            builder.append("\t").append("getfield ").append(className).append("/").append(fieldName).append(" ").append(fieldType).append("\n");
    }

    private void generateReturnInstruction(ReturnInstruction instruction,Method method) {
        if (instruction.hasReturnValue()) {
            generateLoadInstruction(instruction.getOperand(),method);
        }
        switch (instruction.getElementType()){
            case BOOLEAN, INT32 -> builder.append("\t").append("ireturn").append("\n");
            case STRING, CLASS, OBJECTREF, ARRAYREF -> builder.append("\t").append("areturn").append("\n");
            case VOID -> builder.append("\treturn\n");
        }

    }

    private void generateBranchInstruction(Instruction instruction) {
        //TODO
    }

    private void generateGotoInstruction(Instruction instruction) {
        //TODO
    }

    private void generateCallInstruction(CallInstruction instruction, Method method) {
        switch (instruction.getInvocationType()){
            case invokevirtual -> generateVirtualCall(instruction,method);
            case invokestatic -> generateStaticCall(instruction,method);
            case invokespecial -> generateSpecialCall(instruction,method);
            case NEW -> generateNewCall(instruction,method);
        }
    }

    private void generateNewCall(CallInstruction instruction, Method method) {
        String className = JasminUtils.getFieldType(instruction.getFirstArg().getType(),false,classUnit);
        builder.append('\t').append(JasminInstructions.newInstruction(className)).append('\n');
        builder.append('\t').append(JasminInstructions.dup()).append('\n');
    }

    private void generateSpecialCall(CallInstruction instruction, Method method) {
        String className;
        boolean localConstructor = method.isConstructMethod() && JasminUtils.getElementName(instruction.getFirstArg()).equals("this");
        if (localConstructor) {
            className = classUnit.getSuperClass() == null ? "java/lang/Object" : JasminUtils.getFullClassName(classUnit.getSuperClass(),classUnit);
        } else {
            className = JasminUtils.getFieldType(instruction.getFirstArg().getType(),false,classUnit);
        }
        generateGeneralCall("invokespecial",className,instruction,method);
        if (localConstructor) {
            builder.append("\t").append("return").append("\n");
        }

    }

    private void generateStaticCall(CallInstruction instruction, Method method) {
        generateGeneralCall("invokestatic",JasminUtils.getElementName(instruction.getFirstArg()),instruction,method);
    }

    private void generateVirtualCall(CallInstruction instruction, Method method) {
        generateGeneralCall("invokevirtual",JasminUtils.getFieldType(instruction.getFirstArg().getType(),false,classUnit),instruction,method);
    }

    private void generateGeneralCall(String invoke,String className, CallInstruction instruction, Method method) {
        String methodName = JasminUtils.getElementName(instruction.getSecondArg());
        methodName = methodName.substring(1,methodName.length()-1);
        if(!invoke.equals("invokestatic")) {
            generateLoadInstruction(instruction.getFirstArg(),method);
        }
        for (Element elem : instruction.getListOfOperands()) {
            generateLoadInstruction(elem,method);
        }
        builder.append("\t").append(invoke).append(" ").append(className).append("/").append(methodName).append("(");
        for (Element elem : instruction.getListOfOperands()) {
            builder.append(JasminUtils.getFieldType(elem.getType(),true,classUnit));
        }
        builder.append(")").append(JasminUtils.getFieldType(instruction.getReturnType(),true,classUnit)).append("\n");
    }


    private void generateAssignInstruction(AssignInstruction instruction,Method method) {
        Descriptor descriptor = JasminUtils.getDescriptor(instruction.getDest(),method);
        ElementType destType = instruction.getTypeOfAssign().getTypeOfElement();
        if (descriptor == null) return;
        generateInstruction(instruction.getRhs(),method);
        String jasminInstruction = switch (destType) {
            case INT32,BOOLEAN -> JasminInstructions.istore(descriptor.getVirtualReg());
            case THIS,OBJECTREF,ARRAYREF,STRING,CLASS -> JasminInstructions.astore(descriptor.getVirtualReg());
            case VOID -> null;
        };
        builder.append("\t").append(jasminInstruction).append("\n");

    }

    private void generateLoadInstruction(Element elem,Method method) {
        if (elem.isLiteral()){
            String literal = ((LiteralElement) elem).getLiteral();
            builder.append("\t").append(JasminInstructions.generateInt(literal)).append("\n");
            return;
        } else if (elem.getType().getTypeOfElement() == ElementType.BOOLEAN &&
            (JasminUtils.getElementName(elem).equals("true") || JasminUtils.getElementName(elem).equals("false"))) {
            builder.append("\t").append(JasminInstructions.generateBoolean(JasminUtils.getElementName(elem))).append("\n");
            return;
        }

        Descriptor descriptor = JasminUtils.getDescriptor(elem,method);
        if (descriptor == null) return;
        ElementType elemType = elem.getType().getTypeOfElement();

        String jasminInstruction =  switch (elemType) {
            case THIS, ARRAYREF, CLASS, OBJECTREF, STRING -> JasminInstructions.aload(descriptor.getVirtualReg());
            case INT32, BOOLEAN -> JasminInstructions.iload(descriptor.getVirtualReg());
            case VOID -> null;
        };

        builder.append("\t").append(jasminInstruction).append("\n");
    }

}
