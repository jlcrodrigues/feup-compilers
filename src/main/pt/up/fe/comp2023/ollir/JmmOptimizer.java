package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2023.optimization.RegisterAllocator;

import java.util.Collections;

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
            new RegisterAllocator(ollirResult, numRegisters);

        return ollirResult;
    }
}
