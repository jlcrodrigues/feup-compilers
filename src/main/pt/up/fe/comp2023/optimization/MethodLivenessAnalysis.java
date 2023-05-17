package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;

import java.util.HashSet;
import java.util.Set;


public class MethodLivenessAnalysis {
    private Method method;
    private final Set<String>[] def;
    private final Set<String>[] use;
    private final Set<String>[] liveIn;
    private final Set<String>[] liveOut;

    @SuppressWarnings("unchecked")
    public MethodLivenessAnalysis(Method method) {
        this.method = method;
        def = new Set[method.getInstructions().size()];
        use = new Set[method.getInstructions().size()];
        liveIn = new Set[method.getInstructions().size()];
        liveOut = new Set[method.getInstructions().size()];
        analyze();
    }

    private void analyze() {
        method.buildCFG();

        for (Instruction instruction : method.getInstructions()) {
            use[instruction.getId() - 1] = new HashSet<>();
            def[instruction.getId() - 1] = new HashSet<>();
            liveIn[instruction.getId() - 1] = new HashSet<>();
            liveOut[instruction.getId() - 1] = new HashSet<>();
            fillSets(instruction);
        }

        boolean changed;
        do  {
            changed = false;
            for (Instruction instruction : method.getInstructions()) {
                if (liveIn[instruction.getId() - 1] == null) {
                    liveIn[instruction.getId() - 1] = new HashSet<>();
                }
                if (liveOut[instruction.getId() - 1] == null) {
                    liveOut[instruction.getId() - 1] = new HashSet<>();
                }

                if (updateLiveIn(instruction))
                        changed = true;
                if (updateLiveOut(instruction))
                    changed = true;
            }
        } while (changed);
    }

    private void fillSets(Instruction instruction) {
        InstructionType type = instruction.getInstType();

        switch (type) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                Element dest = assignInstruction.getDest();
                if (dest instanceof Operand) {
                    Operand op = (Operand) dest;
                    def[instruction.getId() - 1].add(op.getName());
                }
                fillSets(assignInstruction.getRhs());
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instruction;
                for (Element operand: callInstruction.getListOfOperands()) {
                    addUseElement(operand, instruction.getId());
                }
                break;
            case BRANCH:
                CondBranchInstruction condBranchInstruction = (CondBranchInstruction) instruction;
                for (Element operand: condBranchInstruction.getOperands()) {
                    addUseElement(operand, instruction.getId());
                }
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.getOperand() != null) {
                    addUseElement(returnInstruction.getOperand(), instruction.getId());
                }
                break;
            case PUTFIELD:
                break;
            case GETFIELD:
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                addUseElement(unaryOpInstruction.getOperand(), instruction.getId());
                break;
            case BINARYOPER:
                BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;
                for (Element operand: binaryOpInstruction.getOperands()) {
                    addUseElement(operand, instruction.getId());
                }
                break;
            case NOPER:
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                addUseElement(singleOpInstruction.getSingleOperand(), instruction.getId());
                break;
        }
    }

    private void addUseElement(Element element, int id) {
        if (element.isLiteral())
            return;
        if (element instanceof Operand) {
            Operand op = (Operand) element;
            use[id - 1].add(op.getName());
        }
    }

    /**
     * Update liveIn set <br>
     * liveIn(n) = use(n) U (liveOut(n) - def(n))
     */
    private boolean updateLiveIn(Instruction instruction) {
        boolean changed = liveIn[instruction.getId() - 1].addAll(use[instruction.getId() - 1]);

        // liveOut - def
        Set<String> temp = new HashSet<>(liveOut[instruction.getId() - 1]);
        temp.removeAll(def[instruction.getId() - 1]);

        return changed || liveIn[instruction.getId() - 1].addAll(temp);
    }

    /**
     * Update liveOut set <br>
     * liveOut(n) = U liveIn(s) for all s in successors(n)
     */
    private boolean updateLiveOut(Instruction instruction) {
        boolean changed = false;
        for (Node successor: instruction.getSuccessors()) {
            if (successor.getNodeType() == NodeType.END)
                continue;
            changed = changed || liveOut[instruction.getId() - 1].addAll(liveIn[successor.getId() - 1]);
        }
        return changed;
    }
}
