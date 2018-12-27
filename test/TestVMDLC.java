import java.io.IOException;

import nez.ParserGenerator;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
//import nez.parser.io.StringSource;
import nez.parser.io.FileSource;
import nez.ast.Source;
import nez.ast.SourceError;
import nez.util.ConsoleUtils;


import dispatch.*;
import type.*;
import vmdlc.AlphaConvVisitor;
import vmdlc.AstToCVisitor;
import vmdlc.DesugarVisitor;
import vmdlc.SyntaxTree;

public class TestVMDLC {
    public final static void main(String[] args) {
        try {
            TypeDefinition.load("default.def");

            /*
             * int w = 64; int n = 12; ejsdsl.SimpleTree t =
             * ejsdsl.parse("externC constant cint aaa = \"-1\";", w, n);
             * System.out.println(t);
             */

            ParserGenerator pg = new ParserGenerator();
            Grammar grammar = pg.loadGrammar("ejsdsl.nez");
            //grammar.dump();
            Parser parser = grammar.newParser(ParserStrategy.newSafeStrategy());
            
            //Source source = new StringSource("externC constant cint aaa = \"-1\";");
            Source source = new FileSource("vmdl/test3.inc2");
            SyntaxTree node = (SyntaxTree) parser.parse(source, new SyntaxTree());
            if (parser.hasErrors()) {
                for (SourceError e: parser.getErrors()) {
                    System.out.println(e);
                }
            }

            new DesugarVisitor().start(node);
            new AlphaConvVisitor().start(node, true);
            String program = new AstToCVisitor().start(node);
            System.out.println(program);
            
            // ConsoleUtils.println(node);
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}
