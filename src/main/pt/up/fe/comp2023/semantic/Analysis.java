package pt.up.fe.comp2023.semantic;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2023.table.ASymbolTable;

public class Analysis {
    private List<Report> reports;
    private ASymbolTable symbolTable;

    Analysis(List<Report> reports, ASymbolTable symbolTable) {
        this.reports = reports;
        this.symbolTable = symbolTable;
    }

    Analysis(ASymbolTable symbolTable) {
        this.reports = new ArrayList<Report>();
        this.symbolTable = symbolTable;
    }

    public List<Report> getReports() {
        return reports;
    }

    public ASymbolTable getSymbolTable() {
        return symbolTable;
    }

    public void addReport(JmmNode node, String message) {
        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                //TODO get line and col
                //Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                0, 0,
                message));
    }
}
