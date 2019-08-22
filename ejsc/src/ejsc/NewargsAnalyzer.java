/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.util.HashSet;
import java.util.Set;

public class NewargsAnalyzer extends IASTBaseVisitor {
    Main.Info.OptLocals optLocals;
    public boolean useArguments;
    public boolean useFunction;
    public Set<String> variables;
    public Set<String> freeVariables;

    public NewargsAnalyzer(Main.Info.OptLocals optLocals) {
        this.optLocals = optLocals;
        useArguments = false;
        useFunction = false;
        variables = new HashSet<String>();
        freeVariables = new HashSet<String>();
    }

    public void analyze(IASTNode node) {
        node.accept(this);
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        for (IASTFunctionExpression func: node.programs) {
            NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optLocals);
            func.body.accept(analyzer);
            func.needArguments = false;
            func.needFrame = false;
        }
        return null;
    }
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        node.innerUsedLocals = new HashSet<String>();
        this.useFunction = true;
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optLocals);
        node.body.accept(analyzer);
        boolean useArg = analyzer.useArguments;
        boolean useFunc = analyzer.useFunction;
        boolean hasLocals = !node.locals.isEmpty();

        for (String var : analyzer.variables) {
            if (node.params.contains(var) || node.locals.contains(var))
                continue;
            this.freeVariables.add(var);
        }
        for (String var : analyzer.freeVariables) {
            if (node.params.contains(var) || node.locals.contains(var))
                node.innerUsedLocals.add(var);
            else
                this.freeVariables.add(var);
        }
        if (optLocals == Main.Info.OptLocals.G1) {
            node.innerUsedLocals.addAll(node.params);
            node.innerUsedLocals.addAll(node.locals);
        } else if (optLocals == Main.Info.OptLocals.PROSYM) {
            node.innerUsedLocals.addAll(node.locals);
        }

        switch (this.optLocals) {
        case NONE:
            throw new Error("NewargsAnalyzer is called with optLocals == NONE");
        case PROSYM:
            node.needArguments = useArg || useFunc;
            node.needFrame = node.needArguments || hasLocals;
            break;
        case G1:
            node.needArguments = useArg;
            node.needFrame = true;
            break;
        case G3:
            node.needArguments = useArg;
            node.needFrame = node.needArguments || !node.innerUsedLocals.isEmpty();
            break;
        }
        return null;
    }
    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        if (node.id != null) {
            if (node.id.equals("arguments")) {
                useArguments = true;
            } else {
                variables.add(node.id);
            }
        }
        return null;
    }
}
