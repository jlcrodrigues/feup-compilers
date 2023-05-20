package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private final StringBuilder builder;

    private int comparisonLabelsCounter = 0;

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
            generateMethodBody(method);
            builder.append(".end method\n\n");
        });
    }

    private void generateMethodBody(Method method) {
        builder.append(".limit locals ").append(getNumLocals(method)).append("\n");
        for (Instruction instruction : method.getInstructions()) {
            generateInstruction(instruction,method);
            if (instruction.getInstType() == InstructionType.CALL ){
                CallInstruction call = (CallInstruction) instruction;
                if (call.getReturnType().getTypeOfElement() != ElementType.VOID) {
                    builder.append("\t").append("pop").append("\n");
                }
            }
        }
    }

    private int getNumLocals(Method method) {
        if (method.getVarTable().isEmpty()) {
            return method.isStaticMethod() ? 0 : 1;
        }

        List<Descriptor> locals = new ArrayList<>(method.getVarTable().values());

        if (!method.isStaticMethod()) {
            locals.add(new Descriptor(0));
        }

        Set<Integer> distinctRegisters = new HashSet<>();
        for (Descriptor descriptor : locals) {
            distinctRegisters.add(descriptor.getVirtualReg());
        }

        return distinctRegisters.size();
    }


    private void generateInstruction(Instruction instruction,Method method) {
        //builder.append("\t").append(instruction.getInstType()).append("\n");
        generateInstructionLabels(instruction,method);
        switch (instruction.getInstType()){
            case ASSIGN -> generateAssignInstruction((AssignInstruction) instruction,method);
            case CALL -> generateCallInstruction((CallInstruction) instruction,method);
            case GOTO -> generateGotoInstruction((GotoInstruction) instruction,method);
            case BRANCH -> generateBranchInstruction((CondBranchInstruction) instruction,method);
            case RETURN -> generateReturnInstruction((ReturnInstruction) instruction,method);
            case PUTFIELD -> generateFieldInstruction((FieldInstruction) instruction,method, true);
            case GETFIELD -> generateFieldInstruction((FieldInstruction) instruction,method, false);
            case UNARYOPER -> generateUnaryInstruction((UnaryOpInstruction) instruction,method);
            case BINARYOPER -> generateBinaryInstruction((BinaryOpInstruction) instruction,method);
            case NOPER -> generateNoperInstruction((SingleOpInstruction) instruction,method);
        }
    }

    private void generateInstructionLabels(Instruction instruction, Method method){
        List<String> labels = method.getLabels(instruction);
        for (String label : labels) {
            builder.append("\t").append(label).append(":\n");
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
            case AND,ANDB -> builder.append("\t").append("iand").append("\n");
            case OR,ORB -> builder.append("\t").append("ior").append("\n");
            case LTH -> {
                String mainLabel = "LTH";
                String operation = JasminInstructions.if_icmplt(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
            case GTH -> {
                String mainLabel = "GTH";
                String operation = JasminInstructions.if_icmpgt(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
            case LTE -> {
                String mainLabel = "LTE";
                String operation = JasminInstructions.if_icmple(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
            case GTE -> {
                String mainLabel = "GTE";
                String operation = JasminInstructions.if_icmpge(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
            case EQ -> {
                String mainLabel = "EQ";
                String operation = JasminInstructions.if_icmpeq(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
            case NEQ-> {
                String mainLabel = "NE";
                String operation = JasminInstructions.if_icmpne(mainLabel+ "_" + comparisonLabelsCounter);
                generateComparisonInstruction(operation,mainLabel);
            }
        }
    }

    private void generateComparisonInstruction(String operation, String mainLabel) {
        builder.append("\t").append(operation).append("\n");
        builder.append("\t").append(JasminInstructions.generateInt("0")).append("\n");
        builder.append("\t").append(JasminInstructions.gotoInstruction(mainLabel +"_"+ comparisonLabelsCounter + "_end")).append("\n");
        builder.append("\t").append(mainLabel).append("_").append(comparisonLabelsCounter).append(":\n");
        builder.append("\t").append(JasminInstructions.generateInt("1")).append("\n");
        builder.append("\t").append(mainLabel).append("_").append(comparisonLabelsCounter).append("_end:\n");
        comparisonLabelsCounter++;
    }

    private void generateUnaryInstruction(UnaryOpInstruction instruction , Method method) {
        if (instruction.getOperation().getOpType() != OperationType.NOT && instruction.getOperation().getOpType() != OperationType.NOTB)
            return;
        generateLoadInstruction(instruction.getOperand(),method);
        builder.append("\t").append(JasminInstructions.generateInt("1")).append("\n");
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

    private void generateBranchInstruction(CondBranchInstruction instruction, Method method) {
        if (instruction instanceof SingleOpCondInstruction){
            Element operand = ((SingleOpCondInstruction) instruction).getCondition().getSingleOperand();
            generateSingleOpCondInstruction(operand,method,instruction.getLabel());
        }
        else if (instruction instanceof OpCondInstruction){
            generateOpCondInstruction(instruction,method);
        }
    }

    private void generateSingleOpCondInstruction(Element operand, Method method, String label) {
        generateLoadInstruction(operand,method);
        builder.append("\t").append(JasminInstructions.ifne(label)).append("\n");
    }

    private void generateOpCondInstruction(CondBranchInstruction instruction, Method method) {
        OpInstruction condition = ((OpCondInstruction) instruction).getCondition();
        OperationType opType = condition.getOperation().getOpType();
        if (condition instanceof BinaryOpInstruction){
            if (checkComparisonWithZero((BinaryOpInstruction) condition)){
                generateZeroComparisonInstruction(instruction,(BinaryOpInstruction) condition,method);
                return;
            }
            generateLoadInstruction(((BinaryOpInstruction) condition).getLeftOperand(),method);
            generateLoadInstruction(((BinaryOpInstruction) condition).getRightOperand(),method);
            switch(opType){
                case AND,ANDB -> builder.append("\t").append(JasminInstructions.iand()).append("\n").append("\t").append(JasminInstructions.ifne(instruction.getLabel())).append("\n");
                case OR,ORB -> builder.append("\t").append(JasminInstructions.ior()).append("\n").append("\t").append(JasminInstructions.ifne(instruction.getLabel())).append("\n");
                case LTH -> builder.append("\t").append(JasminInstructions.if_icmplt(instruction.getLabel())).append("\n");
                case GTH -> builder.append("\t").append(JasminInstructions.if_icmpgt(instruction.getLabel())).append("\n");
                case LTE -> builder.append("\t").append(JasminInstructions.if_icmple(instruction.getLabel())).append("\n");
                case GTE -> builder.append("\t").append(JasminInstructions.if_icmpge(instruction.getLabel())).append("\n");
                case EQ -> builder.append("\t").append(JasminInstructions.if_icmpeq(instruction.getLabel())).append("\n");
                case NEQ -> builder.append("\t").append(JasminInstructions.if_icmpne(instruction.getLabel())).append("\n");
            }
        }
        else if (condition instanceof UnaryOpInstruction){
            generateLoadInstruction(((UnaryOpInstruction) condition).getOperand(),method);
            switch(opType){
                case NOT,NOTB -> builder.append("\t").append(JasminInstructions.ifeq(instruction.getLabel())).append("\n");
            }
        }
    }

    private boolean checkComparisonWithZero(BinaryOpInstruction instruction) {
        if (instruction.getOperation().getOpType() != OperationType.LTH && instruction.getOperation().getOpType() != OperationType.GTH &&
                instruction.getOperation().getOpType() != OperationType.LTE && instruction.getOperation().getOpType() != OperationType.GTE &&
                instruction.getOperation().getOpType() != OperationType.EQ && instruction.getOperation().getOpType() != OperationType.NEQ)
            return false;
        return JasminUtils.getElementName(instruction.getLeftOperand()).equals("0") || JasminUtils.getElementName(instruction.getRightOperand()).equals("0");
    }

    private void generateZeroComparisonInstruction(CondBranchInstruction instruction, BinaryOpInstruction condition, Method method) {
        boolean leftIsZero = JasminUtils.getElementName(condition.getLeftOperand()).equals("0");
        if (leftIsZero)
            generateLoadInstruction(condition.getRightOperand(),method);
        else
            generateLoadInstruction(condition.getLeftOperand(),method);
        builder.append("\t");
        switch(condition.getOperation().getOpType()){
            case LTH -> builder.append(leftIsZero ? JasminInstructions.ifgt(instruction.getLabel()) : JasminInstructions.iflt(instruction.getLabel()));
            case GTH -> builder.append(leftIsZero ? JasminInstructions.iflt(instruction.getLabel()) : JasminInstructions.ifgt(instruction.getLabel()));
            case LTE -> builder.append(leftIsZero ? JasminInstructions.ifge(instruction.getLabel()) : JasminInstructions.ifle(instruction.getLabel()));
            case GTE -> builder.append(leftIsZero ? JasminInstructions.ifle(instruction.getLabel()) : JasminInstructions.ifge(instruction.getLabel()));
            case EQ -> builder.append(JasminInstructions.ifeq(instruction.getLabel()));
            case NEQ -> builder.append(JasminInstructions.ifne(instruction.getLabel()));
        }
        builder.append("\n");
    }

    private void generateGotoInstruction(GotoInstruction instruction, Method method) {
        while(method.getLabels().get(instruction.getLabel()).getInstType() == InstructionType.GOTO){
            instruction = (GotoInstruction) method.getLabels().get(instruction.getLabel());
        }
        builder.append("\t").append(JasminInstructions.gotoInstruction(instruction.getLabel())).append("\n");

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
