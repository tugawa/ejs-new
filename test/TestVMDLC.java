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

<<<<<<< HEAD
import vmdlc.DesugarVisitor;
import vmdlc.AlphaConvVisitor;
import vmdlc.AstToCVisitor;
import vmdlc.SyntaxTree;
import vmdlc.TypeCheckVisitor;
=======
>>>>>>> 800ed61cd9db609c66689a68799dfca333c728fa

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

            ParserGenerator pg = new ParserGenerator();
            Grammar grammar = pg.loadGrammar("ejsdsl.nez");
            //grammar.dump();
            Parser parser = grammar.newParser(ParserStrategy.newSafeStrategy());
            
            //Source source = new StringSource("externC constant cint aaa = \"-1\";");
            Source source = new FileSource("vmdl/test3.inc2");
            SyntaxTree node = (SyntaxTree) parser.parse(source, new SyntaxTree());
<<<<<<< HEAD
            
            new DesugarVisitor().start(node);
            new AlphaConvVisitor().start(node, true);
            // System.out.println(node);
            new TypeCheckVisitor().start(node);
            new AstToCVisitor().start(node);
            //ConsoleUtils.println(node);
=======
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
>>>>>>> 800ed61cd9db609c66689a68799dfca333c728fa
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}
