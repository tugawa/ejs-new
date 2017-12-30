import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// import estree.Node;
// import estree.EsTreeNormalizer;
//import iast.*;

import antlr.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;


public class Main {

    static class Info {
        String inputFileName;   // .js
        String outputFileName;  // .sbc

        boolean optPrintESTree = false;
        boolean optPrintIAST = false;
        boolean optPrintAnalyzer = false;
        boolean optPrintLowLevelCode = false;
        boolean optHelp = false;
        boolean optOmitArguments = false;
        boolean optOmitFrame = false;

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
                    case "--llcode":
                    	info.optPrintLowLevelCode = true;
                    	break;
                    case "--help":
                        info.optHelp = true;
                        break;
                    case "-o":
                        info.outputFileName = args[++i];
                        break;
                    case "-omit-arguments":
                        info.optOmitArguments = true;
                        break;
                    case "-omit-frame":
                        info.optOmitFrame = true;
                        break;
                    }
                } else {
                    info.inputFileName = args[i];
                }
            }
            if (info.inputFileName == null) {
                info.optHelp = true;
            } else if (info.outputFileName == null) {
                int pos = info.inputFileName.lastIndexOf(".");
                if (pos != -1) {
                    info.outputFileName = info.inputFileName.substring(0, pos) + ".sbc";
                } else {
                    info.outputFileName = info.inputFileName + ".sbc";
                }
            }
            return info;
        }
    }

    void writeBCodeToSBCFile(List<BCode> bcodes, String filename) {
        try {
            File file = new File(filename);
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            for (BCode bc : bcodes) {
                pw.println(bc.toString());
            }
            pw.close();
        } catch(IOException e) {
            System.out.println(e);
        }
    }


    void run(String[] args) {

        // Parse command line option.
        Info info = Info.parseOption(args);
        if (info.optHelp && info.inputFileName == null) {
            // TODO print how to use ...
            return;
        }

        // Parse JavaScript File
        ANTLRInputStream antlrInStream;
        try {
            InputStream inStream;
            inStream = new FileInputStream(info.inputFileName);
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
        ASTGenerator astgen = new ASTGenerator();
        ast_node.Node ast = astgen.visit(tree);
        if (info.optPrintESTree) {
            System.out.println(ast.getEsTree());
        }

        // normalize ESTree.
        new ESTreeNormalizer().normalize(ast);
//        if (info.optPrintESTree) {
//            System.out.println(ast.getEsTree());
//        }

        // convert ESTree into iAST.
        IASTGenerator iastgen = new IASTGenerator();
        IASTNode iast = iastgen.gen(ast);
        if (info.optPrintIAST) {
            new IASTPrinter().print(iast);
        }

        // iAST level optimisation
        if (info.optOmitArguments || info.optOmitFrame) {
            // iAST newargs analyzer
            NewargsAnalyzer analyzer = new NewargsAnalyzer(info.optOmitFrame);
            analyzer.analyze(iast);
            if (info.optPrintAnalyzer) {
                new IASTPrinter().print(iast);
            }
        }

        // convert iAST into low level code.
        CodeGenerator codegen = new CodeGenerator();
        BCBuilder bcBuilder = codegen.compile((IASTProgram) iast);

        if (info.optPrintLowLevelCode) {
        	bcBuilder.assignAddress();
        	System.out.print(bcBuilder);
        }
        
        // macro instruction expansion
        bcBuilder.expandMacro();
        
        // resolve jump destinations
    	bcBuilder.assignAddress();
        List<BCode> bcodes = bcBuilder.build();

        writeBCodeToSBCFile(bcodes, info.outputFileName);
    }

    public static void main(String[] args) throws IOException {
        new Main().run(args);
    }
}
