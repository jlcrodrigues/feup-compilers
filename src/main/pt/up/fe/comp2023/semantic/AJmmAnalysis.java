package pt.up.fe.comp2023.semantic;

import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2023.table.ASymbolTable;
import pt.up.fe.comp2023.table.SymbolTableGenerator;

public class AJmmAnalysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult jmmParserResult) {
        ASymbolTable symbolTable = new SymbolTableGenerator().getSymbolTable(jmmParserResult.getRootNode());
        Analyzer analyzer = new Analyzer(symbolTable);
        analyzer.analyze(jmmParserResult.getRootNode());

        return new JmmSemanticsResult(jmmParserResult.getRootNode(), symbolTable, analyzer.getReports(), jmmParserResult.getConfig());
    }
}
