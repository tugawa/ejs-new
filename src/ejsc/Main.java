/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import ejsc.ast_node.Node;
import ejsc.antlr.ECMAScriptLexer;
import ejsc.antlr.ECMAScriptParser;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.HashMap;


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
        String  optBc = "";
        boolean optEnableLogging = false;
        boolean optSuperInstruction = false;
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


                    case "-O":
                        info.optLocals = OptLocals.G3;
                        info.optBc = "const:cce:copy:rie:dce:reg:rie:dce:reg";
                        break;

                    case "--bc-opt":
                        if (++i >= args.length)
                            throw new Error("--opt takes an argument. Available optimizations: const, rie, cce, copy, reg");
                        info.optBc = args[i];
                        break;
                    case "-opt-const":
                        info.optBc += ":const";
                        System.err.println("-opt-const is obsolete. Use \"--opt const\" option.");
                        break;
                    case "-opt-rie":
                        info.optBc += ":rie";
                        System.err.println("-opt-rie is obsolete. Use \"--opt rie\" option.");
                        break;
                    case "-opt-cce":
                        info.optBc += ":cce";
                        System.err.println("-opt-cce is obsolete. Use \"--opt cce\" option.");
                        break;
                    case "-opt-copy":
                        info.optBc += ":copy";
                        System.err.println("-opt-copy is obsolete. Use \"--opt copy\" option.");
                        break;
                    case "-opt-reg":
                        info.optBc += ":reg";
                        System.err.println("-opt-reg is obsolete. Use \"--opt reg\" option.");
                        break;

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
                if (sispecs != null)
                    for (SISpec spec :sispecs) {
                        if (spec.insnName.equals(insnName))
                            return true;
                    }
                return false;
            }
            static LinkedList<SISpec> getSISpecsByInsnName(String insnName) {
                LinkedList<SISpec> specs = new LinkedList<SISpec>();
                if (sispecs != null)
                    for (SISpec spec :sispecs) {
                        if (spec.insnName.equals(insnName))
                            specs.add(spec);
                    }
                return specs;
            }
            static int getOpcodeIndex(String siName) {
                if (sispecs != null)
                    for (int i = 0; i < sispecs.size(); i++) {
                        SISpec spec = sispecs.get(i);
                        if (spec.siName.equals(siName))
                            return Main.Info.getInsnTableSize() + i;
                    }
                return -1;
            }
            static SISpec getSISpecBySIName(String siName) {
                if (sispecs != null)
                    for (int i = 0; i < sispecs.size(); i++) {
                        SISpec spec = sispecs.get(i);
                        if (spec.siName.equals(siName))
                            return spec;
                    }
                return null;
            }
            static public void printSISpec() {
                if (sispecs != null)
                    for (SISpec spec :sispecs)
                        System.out.println(spec.toString());
            }
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
        bcBuilder.optimisation(info.optBc, info.optPrintOptimisation);

        bcBuilder.assignAddress();

        // macro instruction expansion
        bcBuilder.expandMacro(info);

        // resolve jump destinations
        bcBuilder.assignAddress();

        // replace instructions for logging
        bcBuilder.replaceInstructionsForLogging();

        bcBuilder.mergeTopLevel();

        if (info.optPrintLowLevelCode) {
            bcBuilder.assignAddress();
            System.out.print(bcBuilder);
        }

        bcBuilder.assignFunctionIndex(true);

        if (info.optOutOBC) {
            OBCFileComposer obc = new OBCFileComposer(bcBuilder, info.baseFunctionNumber);
            obc.output(info.outputFileName);
        } else {
            SBCFileComposer sbc = new SBCFileComposer(bcBuilder, info.baseFunctionNumber);
            sbc.output(info.outputFileName);
        }
    }

    public static void main(String[] args) throws IOException {
        new Main().run(args);
    }
}

