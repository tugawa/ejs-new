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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import specfile.SpecFile;

public class Main {

    static class Info {
        static final String DEFAULT_SPECFILE = "default.spec";
        List<String> inputFileNames = new LinkedList<String>();   // .js
        List<Integer> loggedInputFileIndices = new LinkedList<Integer>();   // .js
        String outputFileName;  // .sbc
        SpecFile spec = loadDefaultSpec();
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
        boolean optOutOBC = false;
        boolean optOutInsn32 = false;
        boolean optOutAlign32 = false;  // should be read from SpecFile
        OptLocals optLocals = OptLocals.NONE;

        static Info parseOption(String[] args) throws IOException {
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
                        break;
                    case "-fn":
                        info.baseFunctionNumber = Integer.parseInt(args[++i]);
                        break;
                    case "--out-obc":
                        info.optOutOBC = true;
                        break;
                    case "--out-bit32":
                        info.optOutAlign32 = true;
                        info.optOutInsn32 = true;
                        break;
                    case "--out-align32":
                        info.optOutAlign32 = true;
                        break;
                    case "--out-insn32":
                        info.optOutInsn32 = true;
                        break;
                    case "--spec":
                        info.spec = SpecFile.loadFromFile(args[++i]);
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

        static SpecFile loadDefaultSpec() {
            try {
                InputStream is = ClassLoader.getSystemResourceAsStream(DEFAULT_SPECFILE);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader r = new BufferedReader(isr);
                List<String> specContents = new ArrayList<String>();
                String line;
                while ((line = r.readLine()) != null)
                    specContents.add(line);
                return SpecFile.parse(specContents);
            } catch (IOException e) {
                e.printStackTrace();
                throw new Error("cannot find default specfile: "+DEFAULT_SPECFILE);
            }
        }
    }
    void run(String[] args) throws IOException {

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

            // convert ESTree into iAST.
            IASTGenerator iastgen = new IASTGenerator();
            IASTFunctionExpression iastFile = iastgen.gen(ast);
            iast.add(iastFile);
        }

        if (info.optPrintIAST) {
            new IASTPrinter().print(iast);
        }

        // resolve variables
        ResolveVariables.execute(info.optLocals, iast);
        if (info.optPrintAnalyzer) {
            new IASTPrinter().print(iast);
        }

        // convert iAST into low level code.
        BCBuilder bcBuilder = CodeGenerator.compile((IASTProgram) iast, info);

        bcBuilder.optimisation(info.optBc, info.optPrintOptimisation, info);

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
            OBCFileComposer obc = new OBCFileComposer(bcBuilder, info.baseFunctionNumber, info.spec, info.optOutInsn32, info.optOutAlign32);
            obc.output(info.outputFileName);
        } else {
            SBCFileComposer sbc = new SBCFileComposer(bcBuilder, info.baseFunctionNumber, info.spec, info.optOutInsn32, info.optOutAlign32);
            sbc.output(info.outputFileName);
        }
    }

    public static void main(String[] args) throws IOException {
        new Main().run(args);
    }
}

