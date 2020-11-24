package vmdlc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ControlFlowGraphNode implements Iterable<SyntaxTree>{
    public static ControlFlowGraphNode enter = new ControlFlowGraphNode();
    public static ControlFlowGraphNode exit = new ControlFlowGraphNode();

    private Collection<ControlFlowGraphNode> next = new HashSet<>();
    private Collection<ControlFlowGraphNode> prev = new HashSet<>();
    List<SyntaxTree> statementList = new ArrayList<>();

    public void makeEdgeTo(ControlFlowGraphNode node){
        this.addNext(node);
        node.addPrev(this);
    }
    public void addNext(ControlFlowGraphNode node){
        this.next.add(node);
    }
    public void addPrev(ControlFlowGraphNode node){
        this.prev.add(node);
    }
    public Collection<ControlFlowGraphNode> getNext(){
        return next;
    }
    public Collection<ControlFlowGraphNode> getPrev(){
        return prev;
    }
    public void addStatement(SyntaxTree node){
        statementList.add(node);
    }
    @Override
    public Iterator<SyntaxTree> iterator(){
        return statementList.iterator();
    }
}
