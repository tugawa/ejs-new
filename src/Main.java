import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// import estree.Node;
// import estree.EsTreeNormalizer;
//import iast.*;
 
import antlr.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class Main {
  
  public static void main(String[] args) throws IOException {
    
    String inFileName = null; // js
    String outFileName = null; // sbc
    
    boolean optPrintESTree = false;
    boolean optPrintIAST = false;
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
        if (args[i].equals("--estree")) {
          optPrintESTree = true;
        } else if (args[i].equals("--iast")) {
          optPrintIAST = true;
        }
      } else {
        inFileName = args[i];
      }
    }
    if (outFileName == null) {
      int pos = inFileName.lastIndexOf(".");
      if (pos != -1) {
        outFileName = inFileName.substring(0, pos) + ".sbc";
      } else {
        outFileName = inFileName + ".sbc";
      }
    }
    
    final InputStream inStream = new FileInputStream(inFileName);
    ANTLRInputStream in = new ANTLRInputStream(inStream);
    ECMAScriptLexer lexer = new ECMAScriptLexer(in);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ECMAScriptParser parser = new ECMAScriptParser(tokens);
    ParseTree tree = parser.program();
    
    ASTGenerator astgen = new ASTGenerator();
    ast_node.Node ast = astgen.visit(tree);
    if (optPrintESTree) {
        System.out.println(ast.getEsTree());
    }
    
    new ESTreeNormalizer().normalize(ast);
    if (optPrintESTree) {
        System.out.println(ast.getEsTree());
    }
    
    IASTGenerator iastgen = new IASTGenerator();
    IASTNode iast = iastgen.gen(ast);
    if (optPrintIAST) {
        new IASTPrinter().print(iast);
    }
    
    CodeGenerator codegen = new CodeGenerator();
    codegen.compile((IASTProgram) iast);
	}

}
