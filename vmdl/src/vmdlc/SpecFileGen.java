/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.io.FileOutputStream;
import java.io.IOException;

import specfile.InstructionDef;
import specfile.SpecFile;
import specfile.SuperinstructionSpec;

public class SpecFileGen {
    public static void main(String args[]) throws IOException {
        SpecFile spec = new SpecFile();
        String outFileName = null;
        String fingerprintFileName = null;
        int i = 0;
        while (i < args.length) {
            if (args[i].equals("--insndef")) {
                String fileName = args[++i];
                spec.setInstructionDef(InstructionDef.loadFromFile(fileName));
            } else if (args[i].equals("--sispec")) {
                String fileName = args[++i];
                spec.setSuperinstructionSpec(SuperinstructionSpec.loadFromFile(fileName));
            } else if (args[i].equals("-o"))
                outFileName = args[++i];
            else if (args[i].equals("--fingerprint"))
                fingerprintFileName = args[++i];
            else
                throw new Error("Unknown option: "+args[i]);
            i++;
        }
        if (outFileName == null)
            System.out.print(spec.unparse());
        else {
            try (FileOutputStream out = new FileOutputStream(outFileName)) {
                out.write(spec.unparse().getBytes());
            }
        }
        if (fingerprintFileName != null) {
            try (FileOutputStream out = new FileOutputStream(fingerprintFileName)) {
                String f = String.format("0x%02x", spec.getFingerprint());
                out.write(f.getBytes());
            }
        }
    }
}
