package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.optimization.LivenessAnalysis;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());

        ollirGenerator.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult,ollirGenerator.ollirCode.toString(), Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int numRegisters = Integer.parseInt(ollirResult.getConfig().get("registerAllocation"));
        LivenessAnalysis livenessAnalysis = new LivenessAnalysis(ollirResult);

        return ollirResult;
    }
}
