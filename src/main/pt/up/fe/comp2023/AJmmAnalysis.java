package pt.up.fe.comp2023;

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
        SymbolTable symbolTable = new SymbolTableGenerator().getSymbolTable(jmmParserResult.getRootNode());
        return new JmmSemanticsResult(jmmParserResult, symbolTable, List.of());
    }
}
