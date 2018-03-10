import java.util.LinkedList;
import java.util.List;

public class NewargsAnalyzer extends IASTBaseVisitor {
    public boolean optOmitFrame;
    public boolean useArguments;
    public boolean useFunction;
    public List<String> variables;
    public List<String> innerUseVariables;

    public NewargsAnalyzer(boolean optOmitFrame) {
        this.optOmitFrame = optOmitFrame;
        useArguments = false;
        useFunction = false;
        variables = new LinkedList<String>();
        innerUseVariables = new LinkedList<String>();
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
            if (this.innerUseVariables.contains(var))
                continue;
            this.innerUseVariables.add(var);
        }
        for (String var : analyzer.innerUseVariables) {
            if (node.params.contains(var) || node.locals.contains(var))
                node.innerUseLocals.add(var);
            else
                this.innerUseVariables.add(var);
        }

        if (this.optOmitFrame) {
            node.needArguments = useArg;
            node.needFrame = node.needArguments || !node.innerUseLocals.isEmpty();
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
