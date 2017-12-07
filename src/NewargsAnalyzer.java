import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewargsAnalyzer extends IASTBaseVisitor {
    public boolean optOmitNewframe;
    public boolean useArguments;
    public boolean useFunction;

    public NewargsAnalyzer(boolean optOmitNewframe) {
        this.optOmitNewframe = optOmitNewframe;
        useArguments = false;
        useFunction = false;
    }
    
    public void analyze(IASTNode node) {
        node.accept(this);
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optOmitNewframe);
        node.program.body.accept(analyzer);
        node.program.needArguments = false;
        node.program.needNewframe = false;
        return null;
    }
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        this.useFunction = true;
        NewargsAnalyzer analyzer = new NewargsAnalyzer(this.optOmitNewframe);
        node.body.accept(analyzer);
        boolean useArg = analyzer.useArguments;
        boolean useFunc = analyzer.useFunction;
        boolean hasLocals = !node.locals.isEmpty();
        if (this.optOmitNewframe) {
            node.needArguments = useArg || useFunc;
            node.needNewframe = node.needArguments || hasLocals;
        } else {
            node.needArguments = useArg || useFunc || hasLocals;
            node.needNewframe = node.needArguments;
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
