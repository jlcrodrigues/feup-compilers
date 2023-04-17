package pt.up.fe.comp2023.Jasmin;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;

public class AJasminBackend implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit classUnit = ollirResult.getOllirClass();
        classUnit.show();
        String code = new JasminGenerator(classUnit).generate();
        System.out.println("Jasmin Code:");
        System.out.println(code);
        return new JasminResult(classUnit.getClassName(), code, new ArrayList<>());
    }
}
