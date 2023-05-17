package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.*;

import java.util.ArrayList;

public class RegisterAllocator {
    OllirResult ollirResult;

    public RegisterAllocator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        analyze();
    }

    private void analyze() {
        ClassUnit classUnit = ollirResult.getOllirClass();
        classUnit.buildCFGs();

        for (Method method: classUnit.getMethods()) {
            if (!method.getMethodName().equals("reg"))
                continue;
            MethodLivenessAnalysis methodLivenessAnalysis = new MethodLivenessAnalysis(method);
            ArrayList<LivenessNode> instructionNodes = methodLivenessAnalysis.analyze();
            InterferenceGraph interferenceGraph = new InterferenceGraph(instructionNodes);
        }
    }
}
