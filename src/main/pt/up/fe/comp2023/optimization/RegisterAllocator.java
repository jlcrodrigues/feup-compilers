package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.*;

import java.util.ArrayList;
import java.util.HashMap;

public class RegisterAllocator {
    private OllirResult ollirResult;
    private int maxRegisters;

    public RegisterAllocator(OllirResult ollirResult, int maxRegisters) {
        this.ollirResult = ollirResult;
        allocate();
    }

    private void allocate() {
        ClassUnit classUnit = ollirResult.getOllirClass();
        for (Method method: classUnit.getMethods()) {
            method.buildCFG();

            MethodLivenessAnalysis methodLivenessAnalysis = new MethodLivenessAnalysis(method);
            ArrayList<LivenessNode> instructionNodes = methodLivenessAnalysis.analyze();

            InterferenceGraph interferenceGraph = new InterferenceGraph(instructionNodes);

            HashMap<String, Integer> colors =  interferenceGraph.color(maxRegisters);
            for (String var : method.getVarTable().keySet()) {
                if (colors.containsKey(var)) {
                    method.getVarTable().get(var).setVirtualReg(colors.get(var));
                }
            }
        }
    }
}
