package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.specs.comp.ollir.InstructionType.*;

public class MethodLivenessAnalysis {
    private Method method;
    private Set<String>[] def;
    private Set<String>[] use;
    private Set<String>[] liveIn;
    private Set<String>[] liveOut;

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

        System.out.println("Method: " + method.getMethodName());
        scan(method.getBeginNode());
    }

    /**
     * Scan the method's CFG to compute the sets of definitions and uses.
     */
    private void scan(Node node) {
        if (node.getId() > 0) {
            Instruction instruction = method.getInstr(node.getId() - 1);
            scanDefined(instruction);
        }

        for (Node child: node.getSuccessors()) {
            scan(child);
        }
    }

    private void scanDefined(Instruction instruction) {
        if (instruction.getInstType() != ASSIGN) {
            def[instruction.getId() - 1] = new HashSet<>();
            return;
        }
        AssignInstruction assignInstruction = (AssignInstruction) instruction;
        Operand dest = (Operand) assignInstruction.getDest();
        def[instruction.getId() - 1] = new HashSet<>() {{
            add(dest.getName());
        }};
    }
}
