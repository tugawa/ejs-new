import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewargsAnalyzer extends IASTBaseVisitor {
    public boolean useArguments;

    public NewargsAnalyzer() {
        useArguments = false;
    }
    
    public void analyze(IASTNode node) {
        node.accept(this);
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer();
        node.program.body.accept(analyzer);
        node.program.needNewargs = false;
        return null;
    }
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        NewargsAnalyzer analyzer = new NewargsAnalyzer();
        node.body.accept(analyzer);
        useArguments = analyzer.useArguments;
        boolean hasLocals = !node.locals.isEmpty();
        node.needNewargs = useArguments || hasLocals;
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
