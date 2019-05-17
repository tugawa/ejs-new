/*
   Main.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoya Nonaka, 2018
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
package ejsc;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import ejsc.antlr.*;
import ejsc.ast_node.Node;
import ejsc.antlr.ECMAScriptLexer;
import ejsc.antlr.ECMAScriptParser;


import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Arrays;


public class Main {

    static class Info {
        List<String> inputFileNames = new LinkedList<String>();   // .js
        List<Integer> loggedInputFileIndices = new LinkedList<Integer>();   // .js
        String outputFileName;  // .sbc
        String specFilePath;
        String insnsDefFile;    // instructions.def
        InsnsDef insnsdef;
        SISpecInfo sispecInfo;
        int baseFunctionNumber = 0;
        enum OptLocals {
            NONE,
            PROSYM,
            G1,
            G3;
        }

        boolean optPrintESTree = false;
        boolean optPrintIAST = false;
        boolean optPrintAnalyzer = false;
        boolean optPrintLowLevelCode = false;
        boolean optPrintOptimisation = false;
        boolean optHelp = false;
        boolean optRedunantInstructionElimination = false;
        boolean optConstantPropagation = false;
        boolean optCopyPropagation = false;
        boolean optRegisterAssignment = false;
        boolean optCommonConstantElimination = false;
        boolean optEnableLogging = false;
        boolean outCompactByteCode = false;
        boolean optSuperInstruction = false;
        boolean optCBCSuperInstruction = false;
        boolean optCBCRedunantInstructionElimination = false;
        boolean optCBCRegisterAssignment = false;
        boolean optOutOBC = false;
        OptLocals optLocals = OptLocals.NONE;

        static Info parseOption(String[] args) {
            Info info = new Info();
            for (int i = 0; i < args.length; i++) {
                if (args[i].charAt(0) == '-') {
                    switch (args[i]) {
                    case "--estree":
                        info.optPrintESTree = true;
                        break;
                    case "--iast":
                        info.optPrintIAST = true;
                        break;
                    case "--analyzer":
                        info.optPrintAnalyzer = true;
                        break;
                    case "--show-llcode":
                        info.optPrintLowLevelCode = true;
                        break;
                    case "--show-opt":
                        info.optPrintOptimisation = true;
                        break;
                    case "--help":
                        info.optHelp = true;
                        break;
                    case "-o":
                        info.outputFileName = args[++i];
                        break;
                    case "-omit-arguments":
                        throw new Error("obsolete option: -omit-arguments");
                    case "-opt-prosym":
                    case "-omit-frame":
                        info.optLocals = OptLocals.PROSYM;
                        break;
                    case "-opt-g1":
                        info.optLocals = OptLocals.G1;
                        break;
                    case "-opt-g3":
                        info.optLocals = OptLocals.G3;
                        break;
                    case "-opt-const":
                        info.optConstantPropagation = true;
                        break;
                    case "-opt-rie":
                        info.optRedunantInstructionElimination = true;
                        break;
                    case "-opt-cce":
                        info.optCommonConstantElimination = true;
                        break;
                    case "-opt-copy":
                        info.optCopyPropagation = true;
                        break;
                    case "-opt-reg":
                        info.optRegisterAssignment = true;
                        break;
                    case "-log":
                        i++;
                        if (i >= args.length) {
                            throw new Error("failed to parse arguments: -log");
                        }
                        info.loggedInputFileIndices.add(info.inputFileNames.size());
                        info.inputFileNames.add(args[i]);
                        info.optEnableLogging = true;
                        break;
                    case "-opt-si":
                        info.optSuperInstruction = true;
                        info.specFilePath = args[++i];
                        info.sispecInfo = new SISpecInfo(info.specFilePath);
                        break;
                    case "-out-cbc":
                        info.outCompactByteCode = true;
                        break;
                    case "-opt-cbc-sie":
                        info.optCBCSuperInstruction = true;
                        break;
                    case "-opt-cbc-rie":
                        info.optCBCRedunantInstructionElimination = true;
                        break;
                    case "-opt-cbc-reg":
                        info.optCBCRegisterAssignment = true;
                        break;
                    case "-fn":
                        info.baseFunctionNumber = Integer.parseInt(args[++i]);
                        break;
                    case "--out-obc":
                        info.optOutOBC = true;
                        info.insnsDefFile = args[++i];
                        info.insnsdef = new InsnsDef(info.insnsDefFile);
                        break;
                    default:
                        throw new Error("unknown option: "+args[i]);
                    }
                } else {
                    info.inputFileNames.add(args[i]);
                }
            }
            if (info.inputFileNames.size() == 0) {
                info.optHelp = true;
            } else if (info.outputFileName == null) {
                String firstInputFileName = info.inputFileNames.get(0);
                int pos = firstInputFileName.lastIndexOf(".");
                String ext = ".sbc";
                if(info.optOutOBC) ext = ".obc";
                if (pos != -1) {
                    info.outputFileName = firstInputFileName.substring(0, pos) + ext;
                } else {
                    info.outputFileName = firstInputFileName + ext;
                }
            }
            //System.out.println(info.outputFileName);
            return info;
        }

        static class InsnsDef {
            String COMMENTOUT = "//";
            static HashMap<String, InsnDef> table;
            class InsnDef {
                int index;
                String[] operand;
                InsnDef(int _index, String[] _operand) {
                    index = _index;
                    operand = _operand;
                }
            }
            InsnsDef(String file) {
                table = new HashMap<String, InsnDef>();
                parse(file);
            }

            void parse(String file) {
                Scanner scanner;
                try {
                    scanner = new Scanner(new FileInputStream(file));
                    int index = 0;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (line.startsWith(COMMENTOUT)) {
                            continue;
                        } else {
                            String[] insns = line.split("\\s+", 0);
                            table.put(insns[0], new InsnDef(index, insns));
                            index++;
                        }
                    }
                    scanner.close();
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                }
            }
        }
        static int getOpcodeIndex(String opcode) {
            if (InsnsDef.table.containsKey(opcode)) {
                return InsnsDef.table.get(opcode).index;
            } else {
                return -1;
            }
        }
        static int getInsnTableSize() {
            return InsnsDef.table.size();
        }

        static class SISpecInfo {
            static LinkedList<SISpec> sispecs;
            class SISpec {
                String insnName, op0, op1, op2, siName;
                SISpec(String insnName, String op0, String op1, String op2, String siName) {
                    this.insnName = insnName;
                    this.op0 = op0;
                    this.op1 = op1;
                    this.op2 = op2;
                    this.siName = siName;
                }
                public String toString() {
                    return insnName + "(" + op0 + "," + op1 + "," + op2 + ")";
                }
            }
            SISpecInfo(String file) {
                parse(file);
            }
            void parse(String sispecPath) {
                sispecs = new LinkedList<SISpec>();
                try {
                    Scanner sc = new Scanner(new FileInputStream(sispecPath));
                    String pattern = "(?<insn>[a-z]+) *\\( *(?<op0>[a-z_-]+) *, *(?<op1>[a-z_-]+) *, *(?<op2>[a-z_-]+) *\\) *: *(?<newInsn>\\w+)";
                    Pattern format = Pattern.compile(pattern);
                    while (sc.hasNextLine()) {
                        String sispec = sc.nextLine();
                        if (Pattern.matches("^//.*", sispec))
                            continue;
                        if (Pattern.matches("^ *$", sispec))
                            continue;
                        Matcher matcher = format.matcher(sispec);
                        if (!matcher.find())
                            throw new Error("Invalid superinstruction specificated");
                        sispecs.add(new SISpec(matcher.group("insn"),
                                matcher.group("op0"),
                                matcher.group("op1"),
                                matcher.group("op2"),
                                matcher.group("newInsn")));
                    }
                    sc.close();
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                }
            }
            static boolean containByInsnName(String insnName) {
                for (SISpec spec :sispecs) {
                    if (spec.insnName.equals(insnName))
                        return true;
                }
                return false;
            }
            static LinkedList<SISpec> getSISpecsByInsnName(String insnName) {
                LinkedList<SISpec> specs = new LinkedList<SISpec>();
                for (SISpec spec :sispecs) {
                    if (spec.insnName.equals(insnName))
                        specs.add(spec);
                }
                return specs;
            }
            static int getOpcodeIndex(String siName) {
                for (int i = 0; i < sispecs.size(); i++) {
                    SISpec spec = sispecs.get(i);
                    if (spec.siName.equals(siName))
                        return Main.Info.getInsnTableSize() + i;
                }
                return -1;
            }
            static SISpec getSISpecBySIName(String siName) {
                for (int i = 0; i < sispecs.size(); i++) {
                    SISpec spec = sispecs.get(i);
                    if (spec.siName.equals(siName))
                        return spec;
                }
                return null;
            }
            static public void printSISpec() {
                for (SISpec spec :sispecs)
                    System.out.println(spec.toString());
            }
        }
    }


    void writeBCodeToSBCFile(List<?> bcodes, String filename) {
        try {
            File file = new File(filename);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            for (Object bc : bcodes) {
                pw.println(bc.toString());
            }
            pw.close();
        } catch(IOException e) {
            System.out.println(e);
        }
    }

    void writeBCodeToOBCFile(List<BCode> bcodes, String filename, int basefn) {
        LiteralList ll = new LiteralList();
        try {
            File file = new File(filename);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            FileOutputStream fo = null;
            BufferedOutputStream bo = null;

            try {
                bo = new BufferedOutputStream(new FileOutputStream(file));
                int n = 0;

                /* nfunc */
                String tmpstr = bcodes.get(n++).toByteString();
                int nfunc = Integer.parseInt(tmpstr,16);
                char[] tmp = tmpstr.toCharArray();
                for(int i=0;i<2;i++)
                    bo.write((byte)Integer.parseInt(String.format("%c%c", tmp[i*2], tmp[i*2+1]),16));

                /* for each functions */
                for(int k=0;k<nfunc;k++) {

                    ll.init();

                    /* headers */
                    for(int j=0;j<4;j++) {
                        tmpstr = bcodes.get(n++).toByteString();
                        tmp = tmpstr.toCharArray();
                        for(int i=0;i<2;i++)
                            bo.write((byte)Integer.parseInt(String.format("%c%c", tmp[i*2], tmp[i*2+1]),16));
                    }
                    int ninsn = Integer.parseInt(tmpstr,16);

                    /* instructions */
                    for(int j=0;j<ninsn;j++) {
                        BCode bcode = bcodes.get(n++);
                        tmp = bcode.toByteString().toCharArray();
                        int opcode = Integer.parseInt(String.format("%c%c%c%c",tmp[0], tmp[1], tmp[2], tmp[3]),16);
                        if(tmp.length>16) {
                            int opStr_flag[] = new int[3];
                            for(int i=0;i<3;i++) opStr_flag[i] = Integer.parseInt(String.format("%c", tmp[16+i]),16);
                            int count = 0;
                            for(int i=0;i<3;i++) {
                                //System.out.println("length:" + tmp.length + "//count:" + count);
                                //System.out.println("flag[" + i + "]" + opStr_flag[i]);
                                if(opStr_flag[i]==0) continue;
                                int nchar = Integer.parseInt(String.format("%c%c%c%c",tmp[4+i*4], tmp[5+i*4], tmp[6+i*4], tmp[7+i*4]),16);
                                String str="";
                                //System.out.println(nchar + "//" +(19+count+nchar*2));
                                for(int h=0;h<nchar;h++) str += String.format("%c%c", tmp[19+count+h*2], tmp[19+count+h*2+1]);
                                if(opStr_flag[i]==2) {
                                    str += "00";
                                    //nchar++;
                                }
                                int index = ll.add(str);
                                //System.out.println(index);
                                if(index<0) {
                                    //nchar = 0;
                                    index = (index+1) * (-1);
                                }
                                //System.out.println("[" + index + "] : " + str);
                                count+=nchar*2;
                                /*char[] new_nchar = String.format("%04x", nchar).toCharArray();
                                for(int i=0;i<4;i++)
                                    tmp[8+i] = new_nchar[i];
                                 */
                                char[] ret = String.format("%04x", index & 0xffff).toCharArray();
                                for(int h=0;h<4;h++)
                                    tmp[4+i*4+h] = ret[h];
                            }
                        }
                        if(basefn != 0 && bcode instanceof IMakeclosure) {
                            int fn = Integer.parseInt(String.format("%c%c%c%c", tmp[8], tmp[9], tmp[10], tmp[11]),16);
                            //System.out.println("overwrite " + fn + " to " + (fn+basefn));
                            fn += basefn;
                            char[] newfn = String.format("%04x", fn).toCharArray();
                            for(int i=0;i<4;i++)
                                tmp[8+i] = newfn[i];
                        }

                        for(int i=0;i<8;i++) {
                            bo.write((byte)Integer.parseInt(String.format("%c%c", tmp[i*2], tmp[i*2+1]),16));
                        }
                    }

                    // Literals
                    for(int j=0;j<ll.list().size();j++) {
                        tmp = ll.list().get(j).toCharArray();
                        bo.write((byte)((tmp.length/2) >> 8));
                        bo.write((byte)((tmp.length/2) & 0xff));
                        for(int i=0;i<tmp.length/2;i++)
                            bo.write((byte)Integer.parseInt(String.format("%c%c", tmp[i*2], tmp[i*2+1]),16));
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                try {
                    bo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch(IOException e) {
            System.out.println(e);
        }
    }

    void run(String[] args) {

        // Parse command line option.
        Info info = Info.parseOption(args);
        if (info.optHelp && info.inputFileNames.size() == 0) {
            // TODO print how to use ...
            return;
        }


        IASTProgram iast = new IASTProgram();
        for (int i = 0; i < info.inputFileNames.size(); i++) {
            String fname = info.inputFileNames.get(i);
            // Parse JavaScript File
            ANTLRInputStream antlrInStream;
            try {
                InputStream inStream;
                inStream = new FileInputStream(fname);
                antlrInStream = new ANTLRInputStream(inStream);
            } catch (IOException e) {
                System.out.println(e);
                return;
            }
            ECMAScriptLexer lexer = new ECMAScriptLexer(antlrInStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ECMAScriptParser parser = new ECMAScriptParser(tokens);
            ParseTree tree = parser.program();

            // convert ANTLR's parse tree into ESTree.
            ASTGenerator astgen = new ASTGenerator(info.loggedInputFileIndices.contains(i));
            Node ast = astgen.visit(tree);

            if (info.optPrintESTree) {
                System.out.println(ast.getEsTree());
            }

            // normalize ESTree.
            new ESTreeNormalizer().normalize(ast);
            //            if (info.optPrintESTree) {
            //                System.out.println(ast.getEsTree());
            //            }

            // convert ESTree into iAST.
            IASTGenerator iastgen = new IASTGenerator();
            IASTFunctionExpression iastFile = iastgen.gen(ast);
            iast.add(iastFile);
        }

        if (info.optPrintIAST) {
            new IASTPrinter().print(iast);
        }

        // iAST level optimisation
        if (info.optLocals != Info.OptLocals.NONE) {
            // iAST newargs analyzer
            NewargsAnalyzer analyzer = new NewargsAnalyzer(info.optLocals);
            analyzer.analyze(iast);
            if (info.optPrintAnalyzer) {
                new IASTPrinter().print(iast);
            }
        }

        // convert iAST into low level code.
        CodeGenerator codegen = new CodeGenerator(info);
        BCBuilder bcBuilder = codegen.compile((IASTProgram) iast);
        bcBuilder.optimisation(info);

        if (info.outCompactByteCode || info.optSuperInstruction) {
            // convert byte code into compact byte code.
            CBCBuilder cbcBuilder = bcBuilder.convertBCode();

            if (info.outCompactByteCode) {
                cbcBuilder.optimisation(info);
                if (info.optPrintLowLevelCode) {
                    cbcBuilder.assignAddress();
                    System.out.print(cbcBuilder);
                }

                cbcBuilder.assignAddress();

                // macro instruction expansion
                cbcBuilder.expandMacro(info);

                // resolve jump destinations
                cbcBuilder.assignAddress();

                if (info.optPrintLowLevelCode) {
                    cbcBuilder.assignAddress();
                    System.out.print(cbcBuilder);
                }

                cbcBuilder.setJumpDist();

                List<CBCode> bcodes = cbcBuilder.build(info);

                writeBCodeToSBCFile(bcodes, info.outputFileName);
                return;
            }
            cbcBuilder.makeSuperInstruction(info);
            bcBuilder = cbcBuilder.convertBCode();
        }
        if (info.optPrintLowLevelCode) {
            bcBuilder.assignAddress();
            System.out.print(bcBuilder);
        }

        bcBuilder.assignAddress();

        // macro instruction expansion
        bcBuilder.expandMacro(info);

        // resolve jump destinations
        bcBuilder.assignAddress();

        // replace instructions for logging
        bcBuilder.replaceInstructionsForLogging();

        if (info.optPrintLowLevelCode) {
            bcBuilder.assignAddress();
            System.out.print(bcBuilder);
        }
        List<BCode> bcodes = bcBuilder.build();

        if (info.optOutOBC)
            writeBCodeToOBCFile(bcodes, info.outputFileName, info.baseFunctionNumber);
        else
            writeBCodeToSBCFile(bcodes, info.outputFileName);
    }

    public static void main(String[] args) throws IOException {
        new Main().run(args);
    }
}

class LiteralList {
    List<String> list;

    LiteralList(){
        list = new ArrayList<String>();
    }

    void init() {
        list.clear();
    }

    int add(String code) {
        int index;
        //System.out.print(code + ", loaded, ");
        if((index=lookup(code))==-1) {
            list.add(code);
            return list.size()-1;
        }
        return index * (-1) - 1;
    }

    int lookup(String code) {
        int i = 0;
        for(i=0;i<list.size();i++)
            if(list.get(i).equals(code)) return i;
        return -1;
    }

    int size() {
        return list.size();
    }

    List<String> list() {
        return list;
    }
}
