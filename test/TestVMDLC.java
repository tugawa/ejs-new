import java.io.IOException;

import nez.ParserGenerator;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
//import nez.parser.io.StringSource;
import nez.parser.io.FileSource;
import nez.ast.Source;
import nez.util.ConsoleUtils;

import vmdlc.DesugarVisitor;
import vmdlc.AlphaConvVisitor;
import vmdlc.AstToCVisitor;
import vmdlc.SyntaxTree;

import dispatch.*;
import type.*;

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
            Source source = new FileSource("vmdl/test2.inc2");
            SyntaxTree node = (SyntaxTree) parser.parse(source, new SyntaxTree());

            new DesugarVisitor().start(node);
            new AlphaConvVisitor().start(node, true);
            new AstToCVisitor().start(node);
            
            // ConsoleUtils.println(node);
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}
