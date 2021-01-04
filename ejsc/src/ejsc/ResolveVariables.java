/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;

import ejsc.Main.Info.OptLocals;

public class ResolveVariables extends IASTBaseVisitor {

    static public void execute(Main.Info.OptLocals optLocals, IASTProgram node) {
        ResolveVariables analyzer = new ResolveVariables(optLocals);
        analyzer.analyze(node);
    }

    Main.Info.OptLocals optLocals;
    public boolean useArguments;

    public ResolveVariables(Main.Info.OptLocals optLocals) {
        this.optLocals = optLocals;
        useArguments = false;
    }

    private void analyze(IASTNode node) {
        node.accept(this);
    }

    private IASTNode.VarDecl lookup(IASTNode.ScopeHolder scopeHolder, String name) {
        while (scopeHolder != null) {
            for (IASTNode.VarDecl decl: scopeHolder.getVarDecls())
                if (decl.getName().equals(name))
                    return decl;
            if (name.equals("arguments") && scopeHolder instanceof IASTFunctionExpression) {
                IASTFunctionExpression func = (IASTFunctionExpression) scopeHolder;
                if (!func.topLevel)
                    return func.createArgumentsArray();
            }
            scopeHolder = scopeHolder.getScope();
        }
        return IASTNode.VarDecl.createGlobalVarDecl(name);
    }

    private int relocateVariable(IASTNode.VarDecl decl, int frameIndex) {
        if (optLocals == OptLocals.NONE)
            decl.markMayEscape();

        if (decl.mayEscape())
            decl.convertToFrame(frameIndex++);
        else
            decl.convertToRegister();

        return frameIndex;
    }

    private int relocateVariables(IASTFunctionExpression node) {
        switch (optLocals) {
        case NONE:
            for (IASTNode.VarDecl decl: node.params)
                decl.markMayEscape();
            if (!node.topLevel)
                node.createArgumentsArray();
            break;
        case PROSYM: case G1:
            throw new Error("PROSYM and G1 are no longer supported");
        case G3:
            break;
        }

        int frameIndex = 0;
        if (node.needArguments()) {
            IASTNode.VarDecl decl = node.getArgumentsArray();
            decl.convertToFrame(frameIndex++);  // "arguments" sits in the first slot.
        }
        for (IASTNode.ParameterVarDecl decl: node.params)
            if (node.needArguments())
                decl.convertToArguments();
            else
                frameIndex = relocateVariable(decl, frameIndex);
        for (IASTNode.VarDecl decl: node.locals)
            frameIndex = relocateVariable(decl, frameIndex);
        return frameIndex;
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        for (IASTFunctionExpression func: node.programs) {
            ResolveVariables analyzer = new ResolveVariables(this.optLocals);
            func.body.accept(analyzer);
            func.needArguments = false;
            func.frameSize = relocateVariables(func);
        }
        return null;
    }

    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        ResolveVariables analyzer = new ResolveVariables(this.optLocals);
        node.body.accept(analyzer);
        node.needArguments = (optLocals == OptLocals.NONE) ? true : analyzer.useArguments;
        node.frameSize = relocateVariables(node);
        return null;
    }
    @Override
    public Object visitTryCatchStatement(IASTTryCatchStatement node) {
        node.body.accept(this);
        node.handler.accept(this);
        relocateVariable(node.var, 0);
        return null;
    }
    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        assert(node.id != null);
        IASTNode.VarDecl decl = lookup(node.getScope(), node.id);
        decl.markMayEscapeUnlessOwnerFunction(node.getOwnerFunction());
        node.setDeclaration(decl);
        /*
        if (node.id != null) {
            if (node.id.equals("arguments")) {
                IASTNode.VarDecl decl = node.getOwnerFunction().getArgumentsArrayDecl();
                decl.markMayEscapeUnlessOwnerFunction(node.getOwnerFunction());
                node.setDeclaration(decl);
                useArguments = true;
            } else {
                IASTNode.VarDecl decl = lookup(node.getScope(), node.id);
                decl.markMayEscapeUnlessOwnerFunction(node.getOwnerFunction());
                node.setDeclaration(decl);
            }
        }
         */
        return null;
    }
}
