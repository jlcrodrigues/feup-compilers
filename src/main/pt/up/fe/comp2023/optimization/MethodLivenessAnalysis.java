package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;

import java.util.ArrayList;

public class MethodLivenessAnalysis {
    private Method method;
    private final ArrayList<LivenessNode> interferenceGraph = new ArrayList<>();

    public MethodLivenessAnalysis(Method method) {
        this.method = method;
        for (Instruction instruction : method.getInstructions()) {
            interferenceGraph.add(new LivenessNode(instruction.getId()));
        }
    }

    public ArrayList<LivenessNode> analyze() {
        method.buildCFG();

        // Fill all use and def sets
        for (Instruction instruction : method.getInstructions()) {
            fillSets(instruction);
        }

        // Fill liveIn and liveOut sets
        boolean changed;
        do  {
            changed = false;
            for (Instruction instruction : method.getInstructions()) {
                if (updateLiveIn(instruction))
                        changed = true;
                if (updateLiveOut(instruction))
                    changed = true;
            }
        } while (changed);
        return interferenceGraph;
    }

    private boolean updateLiveIn(Instruction instruction) {
        return interferenceGraph.get(instruction.getId() - 1).updateLiveIn(instruction);
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
            LivenessNode successorNode = interferenceGraph.get(successor.getId() - 1);
            changed = changed || interferenceGraph.get(instruction.getId() - 1).addOut(successorNode.getLiveIn());
        }
        return changed;
    }

    private void fillSets(Instruction instruction) {
        InstructionType type = instruction.getInstType();

        switch (type) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                Element dest = assignInstruction.getDest();
                if (dest instanceof Operand) {
                    Operand op = (Operand) dest;
                    interferenceGraph.get(instruction.getId() - 1).addDef(op.getName());
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
            interferenceGraph.get(id - 1).addUse(op.getName());
        }
    }
}
