package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private final StringBuilder builder;

    private final StringBuilder instructionsBuilder;

    private static int maxStack = 0;
    private static int currentStack = 0;

    private int comparisonLabelsCounter = 0;

    public  JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.builder = new StringBuilder();
        this.instructionsBuilder = new StringBuilder();
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
            generateMethodBody(method);
            builder.append(".end method\n\n");
        });
    }

    private void generateMethodBody(Method method) {
        instructionsBuilder.setLength(0);
        for (Instruction instruction : method.getInstructions()) {
            generateInstruction(instruction,method);
            if (instruction.getInstType() == InstructionType.CALL ){
                CallInstruction call = (CallInstruction) instruction;
                if (call.getReturnType().getTypeOfElement() != ElementType.VOID) {
                    instructionsBuilder.append("\t").append(JasminInstructions.pop()).append("\n");
                }
            }
        }
        builder.append(".limit stack ").append(maxStack).append("\n");
        builder.append(".limit locals ").append(getNumLocals(method)).append("\n");
        builder.append(instructionsBuilder);
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
            instructionsBuilder.append("\t").append(label).append(":\n");
        }
    }

    private void generateNoperInstruction(SingleOpInstruction instruction,Method method) {
        generateLoadInstruction(instruction.getSingleOperand(),method);
    }

    private void generateBinaryInstruction(BinaryOpInstruction instruction, Method method) {
        if (checkComparisonWithZero(instruction)){
            boolean leftIsZero = JasminUtils.getElementName(instruction.getLeftOperand()).equals("0");
            if (leftIsZero)
                generateLoadInstruction(instruction.getRightOperand(),method);
            else
                generateLoadInstruction(instruction.getLeftOperand(),method);

            switch (instruction.getOperation().getOpType()){
                case LTH -> {
                    String mainLabel = "LTH";
                    String operation = leftIsZero ? JasminInstructions.ifgt(mainLabel+ "_" + comparisonLabelsCounter) :
                            JasminInstructions.iflt(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
                case GTH -> {
                    String mainLabel = "GTH";
                    String operation = leftIsZero ? JasminInstructions.iflt(mainLabel+ "_" + comparisonLabelsCounter) :
                            JasminInstructions.ifgt(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
                case LTE -> {
                    String mainLabel = "LTE";
                    String operation = leftIsZero ? JasminInstructions.ifge(mainLabel+ "_" + comparisonLabelsCounter) :
                            JasminInstructions.ifle(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
                case GTE -> {
                    String mainLabel = "GTE";
                    String operation = leftIsZero ? JasminInstructions.ifle(mainLabel+ "_" + comparisonLabelsCounter) :
                            JasminInstructions.ifge(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
                case EQ -> {
                    String mainLabel = "EQ";
                    String operation = JasminInstructions.ifeq(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
                case NEQ-> {
                    String mainLabel = "NE";
                    String operation = JasminInstructions.ifne(mainLabel+ "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation,mainLabel);
                }
            }
        }
        else{
            generateLoadInstruction(instruction.getLeftOperand(),method);
            generateLoadInstruction(instruction.getRightOperand(),method);
            switch (instruction.getOperation().getOpType()) {
                case ADD -> instructionsBuilder.append("\t").append(JasminInstructions.iadd()).append("\n");
                case SUB -> instructionsBuilder.append("\t").append(JasminInstructions.isub()).append("\n");
                case MUL -> instructionsBuilder.append("\t").append(JasminInstructions.imul()).append("\n");
                case DIV -> instructionsBuilder.append("\t").append(JasminInstructions.idiv()).append("\n");
                case AND, ANDB -> instructionsBuilder.append("\t").append(JasminInstructions.iand()).append("\n");
                case OR, ORB -> instructionsBuilder.append("\t").append(JasminInstructions.ior()).append("\n");
                case LTH -> {
                    String mainLabel = "LTH";
                    String operation = JasminInstructions.if_icmplt(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
                case GTH -> {
                    String mainLabel = "GTH";
                    String operation = JasminInstructions.if_icmpgt(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
                case LTE -> {
                    String mainLabel = "LTE";
                    String operation = JasminInstructions.if_icmple(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
                case GTE -> {
                    String mainLabel = "GTE";
                    String operation = JasminInstructions.if_icmpge(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
                case EQ -> {
                    String mainLabel = "EQ";
                    String operation = JasminInstructions.if_icmpeq(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
                case NEQ -> {
                    String mainLabel = "NE";
                    String operation = JasminInstructions.if_icmpne(mainLabel + "_" + comparisonLabelsCounter);
                    generateComparisonInstruction(operation, mainLabel);
                }
            }
        }
    }

    private void generateComparisonInstruction(String operation, String mainLabel) {
        instructionsBuilder.append("\t").append(operation).append("\n");
        instructionsBuilder.append("\t").append(JasminInstructions.generateInt("0")).append("\n");
        instructionsBuilder.append("\t").append(JasminInstructions.gotoInstruction(mainLabel +"_"+ comparisonLabelsCounter + "_end")).append("\n");
        instructionsBuilder.append("\t").append(mainLabel).append("_").append(comparisonLabelsCounter).append(":\n");
        instructionsBuilder.append("\t").append(JasminInstructions.generateInt("1")).append("\n");
        instructionsBuilder.append("\t").append(mainLabel).append("_").append(comparisonLabelsCounter).append("_end:\n");
        comparisonLabelsCounter++;
    }

    private void generateUnaryInstruction(UnaryOpInstruction instruction , Method method) {
        if (instruction.getOperation().getOpType() != OperationType.NOT && instruction.getOperation().getOpType() != OperationType.NOTB)
            return;
        generateLoadInstruction(instruction.getOperand(),method);
        instructionsBuilder.append("\t").append(JasminInstructions.generateInt("1")).append("\n");
        instructionsBuilder.append("\t").append(JasminInstructions.ixor()).append("\n");
    }

    private void generateFieldInstruction(FieldInstruction instruction, Method method, boolean putfield){
        generateLoadInstruction(instruction.getFirstOperand(),method);
        if (putfield)
            generateLoadInstruction(((PutFieldInstruction)instruction).getThirdOperand(),method);
        String className = JasminUtils.getFieldType(instruction.getFirstOperand().getType(),false,classUnit);
        String fieldName = JasminUtils.getElementName(instruction.getSecondOperand());
        String fieldType = JasminUtils.getFieldType(instruction.getSecondOperand().getType(),true,classUnit);
        if (putfield)
            instructionsBuilder.append("\t").append(JasminInstructions.putfield(className,fieldName,fieldType)).append("\n");
        else
            instructionsBuilder.append("\t").append(JasminInstructions.getfield(className,fieldName,fieldType)).append("\n");
    }

    private void generateReturnInstruction(ReturnInstruction instruction,Method method) {
        if (instruction.hasReturnValue()) {
            generateLoadInstruction(instruction.getOperand(),method);
        }
        switch (instruction.getElementType()){
            case BOOLEAN, INT32 -> instructionsBuilder.append("\t").append(JasminInstructions.ireturn()).append("\n");
            case STRING, CLASS, OBJECTREF, ARRAYREF -> instructionsBuilder.append("\t").append(JasminInstructions.areturn()).append("\n");
            case VOID -> instructionsBuilder.append("\treturn\n");
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
        instructionsBuilder.append("\t").append(JasminInstructions.ifne(label)).append("\n");
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
                case AND,ANDB -> instructionsBuilder.append("\t").append(JasminInstructions.iand()).append("\n").append("\t").append(JasminInstructions.ifne(instruction.getLabel())).append("\n");
                case OR,ORB -> instructionsBuilder.append("\t").append(JasminInstructions.ior()).append("\n").append("\t").append(JasminInstructions.ifne(instruction.getLabel())).append("\n");
                case LTH -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmplt(instruction.getLabel())).append("\n");
                case GTH -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmpgt(instruction.getLabel())).append("\n");
                case LTE -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmple(instruction.getLabel())).append("\n");
                case GTE -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmpge(instruction.getLabel())).append("\n");
                case EQ -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmpeq(instruction.getLabel())).append("\n");
                case NEQ -> instructionsBuilder.append("\t").append(JasminInstructions.if_icmpne(instruction.getLabel())).append("\n");
            }
        }
        else if (condition instanceof UnaryOpInstruction){
            generateLoadInstruction(((UnaryOpInstruction) condition).getOperand(),method);
            switch(opType){
                case NOT,NOTB -> instructionsBuilder.append("\t").append(JasminInstructions.ifeq(instruction.getLabel())).append("\n");
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
        instructionsBuilder.append("\t");
        switch(condition.getOperation().getOpType()){
            case LTH -> instructionsBuilder.append(leftIsZero ? JasminInstructions.ifgt(instruction.getLabel()) : JasminInstructions.iflt(instruction.getLabel()));
            case GTH -> instructionsBuilder.append(leftIsZero ? JasminInstructions.iflt(instruction.getLabel()) : JasminInstructions.ifgt(instruction.getLabel()));
            case LTE -> instructionsBuilder.append(leftIsZero ? JasminInstructions.ifge(instruction.getLabel()) : JasminInstructions.ifle(instruction.getLabel()));
            case GTE -> instructionsBuilder.append(leftIsZero ? JasminInstructions.ifle(instruction.getLabel()) : JasminInstructions.ifge(instruction.getLabel()));
            case EQ -> instructionsBuilder.append(JasminInstructions.ifeq(instruction.getLabel()));
            case NEQ -> instructionsBuilder.append(JasminInstructions.ifne(instruction.getLabel()));
        }
        instructionsBuilder.append("\n");
    }

    private void generateGotoInstruction(GotoInstruction instruction, Method method) {
        while(method.getLabels().get(instruction.getLabel()).getInstType() == InstructionType.GOTO){
            instruction = (GotoInstruction) method.getLabels().get(instruction.getLabel());
        }
        instructionsBuilder.append("\t").append(JasminInstructions.gotoInstruction(instruction.getLabel())).append("\n");

    }

    private void generateCallInstruction(CallInstruction instruction, Method method) {
        switch (instruction.getInvocationType()){
            case invokevirtual -> generateVirtualCall(instruction,method);
            case invokestatic -> generateStaticCall(instruction,method);
            case invokespecial -> generateSpecialCall(instruction,method);
            case NEW -> generateNewCall(instruction,method);
            case arraylength -> generateArrayLengthCall(instruction,method);
        }
    }

    private void generateArrayLengthCall(CallInstruction instruction, Method method) {
        generateLoadInstruction(instruction.getFirstArg(),method);
        instructionsBuilder.append('\t').append(JasminInstructions.arraylength()).append('\n');
    }

    private void generateNewCall(CallInstruction instruction, Method method) {
        String className;
        if ( instruction.getReturnType().getTypeOfElement() == ElementType.ARRAYREF){
            className = "int";
            generateLoadInstruction(instruction.getListOfOperands().get(0),method);
            instructionsBuilder.append('\t').append(JasminInstructions.newarray(className)).append('\n');
        }
        else{
            className = JasminUtils.getFieldType(instruction.getFirstArg().getType(),false,classUnit);
            instructionsBuilder.append('\t').append(JasminInstructions.newInstruction(className)).append('\n');
            instructionsBuilder.append('\t').append(JasminInstructions.dup()).append('\n');
        }
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
        updateStack(-1);
        if (localConstructor) {
            instructionsBuilder.append("\t").append("return").append("\n");
        }

    }

    private void generateStaticCall(CallInstruction instruction, Method method) {
        generateGeneralCall("invokestatic",JasminUtils.getElementName(instruction.getFirstArg()),instruction,method);

    }

    private void generateVirtualCall(CallInstruction instruction, Method method) {
        generateGeneralCall("invokevirtual",JasminUtils.getFieldType(instruction.getFirstArg().getType(),false,classUnit),instruction,method);
        updateStack(-1);
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
        updateStack(-instruction.getListOfOperands().size());
        if(instruction.getReturnType().getTypeOfElement() != ElementType.VOID)
            updateStack(1);

        instructionsBuilder.append("\t").append(invoke).append(" ").append(className).append("/").append(methodName).append("(");
        for (Element elem : instruction.getListOfOperands()) {
            instructionsBuilder.append(JasminUtils.getFieldType(elem.getType(),true,classUnit));
        }
        instructionsBuilder.append(")").append(JasminUtils.getFieldType(instruction.getReturnType(),true,classUnit)).append("\n");
    }


    private void generateAssignInstruction(AssignInstruction instruction,Method method) {
        Descriptor descriptor = JasminUtils.getDescriptor(instruction.getDest(),method);
        ElementType destType = instruction.getTypeOfAssign().getTypeOfElement();

        if (instruction.getDest() instanceof ArrayOperand){
            generateArrayAssign(instruction,method,destType);
        }
        else{
            generateGeneralAssign(instruction,method,descriptor,destType);
        }

    }

    private void generateArrayAssign(AssignInstruction instruction, Method method,ElementType destType) {
        ArrayOperand arrayOperand = (ArrayOperand) instruction.getDest();

        Descriptor arrayDescriptor = method.getVarTable().get(arrayOperand.getName());
        instructionsBuilder.append("\t").append(JasminInstructions.aload(arrayDescriptor.getVirtualReg())).append("\n");

        Element idx = arrayOperand.getIndexOperands().get(0);
        generateLoadInstruction(idx,method);

        generateInstruction(instruction.getRhs(),method);

        String jasminInstruction = switch (destType) {
            case INT32, BOOLEAN -> JasminInstructions.iastore();
            case THIS, OBJECTREF, ARRAYREF, STRING, CLASS -> JasminInstructions.aastore();
            case VOID -> null;
        };

        instructionsBuilder.append("\t").append(jasminInstruction).append("\n");

    }
    private void generateGeneralAssign(AssignInstruction instruction, Method method, Descriptor descriptor, ElementType destType) {
        if (checkIncrementAndGenerate(instruction, method, descriptor)) {
            return;
        }
        if (descriptor == null) return;
        generateInstruction(instruction.getRhs(), method);
        String jasminInstruction = switch (destType) {
            case INT32, BOOLEAN -> JasminInstructions.istore(descriptor.getVirtualReg());
            case THIS, OBJECTREF, ARRAYREF, STRING, CLASS -> JasminInstructions.astore(descriptor.getVirtualReg());
            case VOID -> null;
        };
        instructionsBuilder.append("\t").append(jasminInstruction).append("\n");
    }

    private boolean checkIncrementAndGenerate(AssignInstruction instruction, Method method, Descriptor descriptor) {
        Instruction rhsInstruction = instruction.getRhs();
        if (!(rhsInstruction instanceof BinaryOpInstruction binaryOpInstruction)) return false;

        if (binaryOpInstruction.getOperation().getOpType() != OperationType.ADD &&
        binaryOpInstruction.getOperation().getOpType() != OperationType.SUB) return false;

        Descriptor varDescriptor;
        Element element;

        Element leftOperand = binaryOpInstruction.getLeftOperand();
        Element rightOperand = binaryOpInstruction.getRightOperand();
        Descriptor leftDescriptor = JasminUtils.getDescriptor(leftOperand,method);
        Descriptor rightDescriptor = JasminUtils.getDescriptor(rightOperand,method);

        if (leftDescriptor == null || leftDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
            if (rightDescriptor == null || rightDescriptor.getVirtualReg()!=descriptor.getVirtualReg()){
                return false;
            } else {
                varDescriptor = rightDescriptor;
                element = leftOperand;
            }
        } else {
            varDescriptor = leftDescriptor;
            element = rightOperand;
        }

        if (!element.isLiteral() || element.getType().getTypeOfElement() != ElementType.INT32) return false;

        int incrementValue = Integer.parseInt(JasminUtils.getElementName(element));

        if (binaryOpInstruction.getOperation().getOpType() == OperationType.SUB) incrementValue = -incrementValue;

        if (incrementValue < -128 || incrementValue > 127) return false;

        instructionsBuilder.append("\t").append(JasminInstructions.iinc(varDescriptor.getVirtualReg(),incrementValue)).append("\n");

        return true;

    }

    private void generateLoadInstruction(Element elem,Method method) {
        if (elem.isLiteral()){
            String literal = ((LiteralElement) elem).getLiteral();
            instructionsBuilder.append("\t").append(JasminInstructions.generateInt(literal)).append("\n");
            return;
        } else if (elem.getType().getTypeOfElement() == ElementType.BOOLEAN &&
            (JasminUtils.getElementName(elem).equals("true") || JasminUtils.getElementName(elem).equals("false"))) {
            instructionsBuilder.append("\t").append(JasminInstructions.generateBoolean(JasminUtils.getElementName(elem))).append("\n");
            return;
        }

        Descriptor descriptor = JasminUtils.getDescriptor(elem,method);
        if (descriptor == null) return;
        ElementType elemType = elem.getType().getTypeOfElement();

        if (elem instanceof ArrayOperand arrayOperand){
            generateArrayLoad(arrayOperand,elemType,method);
        }
        else{
            generateGeneralLoad(descriptor,elemType);
        }
    }

    private void generateArrayLoad(ArrayOperand arrayOperand, ElementType elemType, Method method) {
        Descriptor arrayDescriptor = method.getVarTable().get(arrayOperand.getName());
        if (arrayDescriptor == null) return;
        instructionsBuilder.append("\t").append(JasminInstructions.aload(arrayDescriptor.getVirtualReg())).append("\n");

        Element idx = arrayOperand.getIndexOperands().get(0);
        generateLoadInstruction(idx,method);

        String jasminInstruction = switch (elemType) {
            case INT32, BOOLEAN -> JasminInstructions.iaload();
            case THIS, OBJECTREF, ARRAYREF, STRING, CLASS -> JasminInstructions.aaload();
            case VOID -> null;
        };

        instructionsBuilder.append("\t").append(jasminInstruction).append("\n");
    }

    private void generateGeneralLoad(Descriptor descriptor, ElementType elemType) {
        String jasminInstruction = switch (elemType) {
            case INT32, BOOLEAN -> JasminInstructions.iload(descriptor.getVirtualReg());
            case THIS, OBJECTREF, ARRAYREF, STRING, CLASS -> JasminInstructions.aload(descriptor.getVirtualReg());
            case VOID -> null;
        };
        instructionsBuilder.append("\t").append(jasminInstruction).append("\n");
    }

    public static void updateStack(int change){
        currentStack += change;
        if (currentStack > maxStack) maxStack = currentStack;
    }

}