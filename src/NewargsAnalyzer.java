import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NewargsAnalyzer extends IASTBaseVisitor {
    public boolean optOmitFrame;
    public boolean useArguments;
    public boolean useFunction;
    public Set<String> variables;
    public Set<String> freeVariables;

    public NewargsAnalyzer(boolean optOmitFrame) {
        this.optOmitFrame = optOmitFrame;
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

        if (this.optOmitFrame) {
            node.needArguments = useArg;
            node.needFrame = node.needArguments || !node.innerUsedLocals.isEmpty();
        } else {
            node.needArguments = useArg || useFunc || hasLocals;
            node.needFrame = node.needArguments;
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
