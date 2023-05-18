package pt.up.fe.comp2023.ollir;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.optimization.InterferenceGraph;
import pt.up.fe.comp2023.optimization.LivenessNode;
import pt.up.fe.comp2023.optimization.MethodLivenessAnalysis;

import java.util.ArrayList;
import java.util.HashMap;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());

        ollirGenerator.visit(semanticsResult.getRootNode());

        return new OllirResult(ollirGenerator.ollirCode.toString(), semanticsResult.getConfig());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int numRegisters = 0;
        String regNumber = ollirResult.getConfig().get("registerAllocation");
        if (regNumber != null)
            numRegisters = Integer.parseInt(regNumber);
        else
            return ollirResult;

        if (numRegisters != -1)
            allocate(ollirResult, numRegisters);

        return ollirResult;
    }

    private void allocate(OllirResult ollirResult, int maxRegisters) {
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
