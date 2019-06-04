/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import vmgen.synth.SimpleSynthesiser;
import vmgen.type.TypeDefinition;

public class InsnGenSimple {
    public static void main(String[] args) throws FileNotFoundException {
        if (args.length != 3) {
            System.out.println("InsnGenSimple <type definition> <insn definition> <out dir>");
            System.exit(1);
        }
        String typeDefFile = args[0];
        String insnDefFile = args[1];
        String outDir = args[2];

        TypeDefinition.load(typeDefFile);

        ProcDefinition procDef = new ProcDefinition();
        ProcDefinition.InstDefinition insnDef = procDef.load(insnDefFile);

        System.out.println(insnDef.name);
        Synthesiser synth = new SimpleSynthesiser();
        StringBuilder sb = new StringBuilder();
        sb.append(insnDef.name + "_HEAD:\n");
        String[] dispatchVars = new String[insnDef.dispatchVars.length];
        for (int i = 0; i < insnDef.dispatchVars.length; i++) {
            String dv = insnDef.dispatchVars[i];
            dispatchVars[i] = dv.substring(1, dv.length());
        }
        RuleSet p = new RuleSet(dispatchVars, insnDef.rs.rules);
        sb.append(synth.synthesise(p, "none", new InsnGen.Option()));
        try {
            File file = new File(outDir + "/" + insnDef.name.substring(2).toLowerCase() + ".inc");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.print(sb.toString());
            pw.close();
        }catch(IOException e){
            System.out.println(e);
        }
    }
}
