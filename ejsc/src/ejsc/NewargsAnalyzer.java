/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;

import java.util.HashMap;
import java.util.Map;

import ejsc.Main.Info.OptLocals;

public class NewargsAnalyzer extends IASTBaseVisitor {

    static public void execute(Main.Info.OptLocals optLocals, IASTProgram node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer(optLocals);
        analyzer.analyze(node);
    }

    Main.Info.OptLocals optLocals;
    public boolean useArguments;

    public NewargsAnalyzer(Main.Info.OptLocals optLocals) {
        this.optLocals = optLocals;
        useArguments = false;
    }

    private void analyze(IASTNode node) {
        node.accept(this);
    }

    private IASTNode.VarDecl lookup(IASTNode.FrameHolder scope, String name) {
        while (scope != null) {
            for (IASTNode.VarDecl decl: scope.getVarDecls())
                if (decl.getName().equals(name))
                    return decl;
            scope = scope.getOwnerFrame();
        }
        return IASTNode.VarDecl.createGlobalVarDecl(name);
    }

    private int relocateVariables(IASTNode.FrameHolder node, boolean needArguments) {
        switch (optLocals) {
        case NONE:
            for (IASTNode.VarDecl decl: node.getVarDecls())
                decl.markMayEscape();
            break;
        case PROSYM: case G1:
            throw new Error("PROSYM and G1 are no longer supported");
        case G3:
            break;
        }

        int frameIndex = 0;
        for (IASTNode.VarDecl decl: node.getVarDecls()) {
            if (needArguments && decl instanceof IASTNode.ParameterVarDecl) {
                IASTNode.ParameterVarDecl paramDecl = (IASTNode.ParameterVarDecl) decl;
                paramDecl.convertToArguments();
            } else if (decl.mayEscape())
                decl.convertToFrame(frameIndex++);
            else
                decl.convertToRegister();
        }
        return frameIndex;
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        for (IASTFunctionExpression func: node.programs) {
            NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optLocals);
            func.body.accept(analyzer);
            func.needArguments = false;
            func.frameSize = relocateVariables(func, false);
        }
        return null;
    }

    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optLocals);
        node.body.accept(analyzer);
        node.needArguments = (optLocals == OptLocals.NONE) ? true : analyzer.useArguments;
        node.frameSize = relocateVariables(node, node.needArguments);
        return null;
    }
    @Override
    public Object visitTryCatchStatement(IASTTryCatchStatement node) {
        node.body.accept(this);
        node.handler.accept(this);
        relocateVariables(node, false);
        return null;
    }
    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        assert(node.id != null);
        if (node.id != null) {
            if (node.id.equals("arguments"))
                useArguments = true;
            else {
                IASTNode.VarDecl decl = lookup(node.getOwnerFrame(), node.id);
                decl.markMayEscapeUnlessOwnerFunction(node.getOwnerFunction());
                node.setDeclaration(decl);
            }
        }
        return null;
    }
}
