package pt.up.fe.comp2023.optimization;

import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.*;

import java.util.List;
import java.util.Set;

public class LivenessAnalysis {
    OllirResult ollirResult;

    public LivenessAnalysis(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        analyze();
    }

    private void analyze() {
        ClassUnit classUnit = ollirResult.getOllirClass();
        classUnit.buildCFGs();

        for (Method method: classUnit.getMethods()) {
            MethodLivenessAnalysis methodLivenessAnalysis = new MethodLivenessAnalysis(method);
        }
    }
}
