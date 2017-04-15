import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import estree.EsTreeNormalizer;
import estree.Node;
import iast.*;

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
    EsTreeGenerator astgen = new EsTreeGenerator();
    Node ast = astgen.visit(tree);

    if (optPrintESTree) {
      System.out.println(ast.getEsTree());
    }
    
    new EsTreeNormalizer(ast);
    // System.out.println(ast.getEsTree());
    IASTGenerator iastgen = new IASTGenerator();
    IASTNode iast = iastgen.gen(ast);
    
    if (optPrintIAST) {
      new IASTPrinter().print(iast);
    }
    
		/*
    String jsFileName = null;
    String sbcFileName = null;
    boolean outAst = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-' && args[i].length() == 2) {
        char o = args[i].charAt(1);
        switch (o) {
          case 'o':
            sbcFileName = args[++i];
            break;
          case 'a':
            outAst = true;
            break;
        }
      } else {
        jsFileName = args[i];
      }
    }
    if (jsFileName == null) {
      System.out.println("Error");
      return;
    }
    if (sbcFileName == null) {
      int pos = jsFileName.lastIndexOf(".");
      if (pos != -1) {
        sbcFileName = jsFileName.substring(0, pos) + ".sbc";
      } else {
        sbcFileName = jsFileName + ".sbc";
      }
    }

    final InputStream inStream = new FileInputStream(jsFileName);
		ANTLRInputStream in = new ANTLRInputStream(inStream);
		ECMAScriptLexer lexer = new ECMAScriptLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ECMAScriptParser parser = new ECMAScriptParser(tokens);
		ParseTree tree = parser.program();
		EsTreeGenerator astgen = new EsTreeGenerator();
		Node ast = astgen.visit(tree);

    if (outAst) {
      System.out.println(ast.getEsTree());
      new EsTreeNormalizer(ast);
      System.out.println(ast.getEsTree());
      IASTGenerator iastgen = new IASTGenerator();
      IASTNode iast = iastgen.gen(ast);
      new IASTPrinter().print(iast);
      return;
    }*/

    /*
    System.out.println(">>> " + sbcFileName);
		EsTreeGenerator codeGen = new EsTreeGenerator();
		codeGen.compile(ast);
    String out = codeGen.getBytecode();
    try {
      File outFile = new File(sbcFileName);
      FileWriter writer = new FileWriter(outFile);
      writer.write(out);
      writer.close();
    } catch (IOException e) {
      System.out.println("Error: could not open output file \"" + sbcFileName + "\"");
      return;
    }*/
	}

}
