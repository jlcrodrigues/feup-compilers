package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp2023.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));

        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }

        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, parser.getDefaultRule(),config);

        // Check if there are parsing errors
        long errorCount = parserResult.getReports().stream()
                .filter(report -> report.getType() == ReportType.ERROR)
                .peek(report -> System.out.println("Error report: " + report))
                .count();

        System.out.println("Number of error reports: " + errorCount);

        if (errorCount > 0) return;


        System.out.println(parserResult.getRootNode().toTree());

        JmmSemanticsResult result = new AJmmAnalysis().semanticAnalysis(parserResult);

        /*
        System.out.println("Symbol Table:");
        System.out.println(result.getSymbolTable());
        System.out.println(result.getSymbolTable().print());
        */

        var optimizer = new JmmOptimizer();

        OllirResult ollirResult = optimizer.toOllir(result);

        System.out.println(ollirResult.getOllirCode());
        // ... add remaining stages
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
