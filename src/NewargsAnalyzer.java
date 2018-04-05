/*
   NewargsAnalyzer.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoya Nonaka, 2017-18
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewargsAnalyzer extends IASTBaseVisitor {
    public boolean optOmitFrame;
    public boolean useArguments;
    public boolean useFunction;

    public NewargsAnalyzer(boolean optOmitFrame) {
        this.optOmitFrame = optOmitFrame;
        useArguments = false;
        useFunction = false;
    }

    public void analyze(IASTNode node) {
        node.accept(this);
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optOmitFrame);
        node.program.body.accept(analyzer);
        node.program.needArguments = false;
        node.program.needFrame = false;
        return null;
    }
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        this.useFunction = true;
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optOmitFrame);
        node.body.accept(analyzer);
        boolean useArg = analyzer.useArguments;
        boolean useFunc = analyzer.useFunction;
        boolean hasLocals = !node.locals.isEmpty();
        if (this.optOmitFrame) {
            node.needArguments = useArg || useFunc;
            node.needFrame = node.needArguments || hasLocals;
        } else {
            node.needArguments = useArg || useFunc || hasLocals;
            node.needFrame = node.needArguments;
        }
        return null;
    }
    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        if (node.id != null) {
            if (node.id.equals("arguments"))
                useArguments = true;
        }
        return null;
    }
}
