package vmdlc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class ControlFlowGraphNode implements Iterable<SyntaxTree>{
    public static ControlFlowGraphNode enter = new ControlFlowGraphNode(Collections.emptySet(), Collections.emptySet());
    public static ControlFlowGraphNode exit = new ControlFlowGraphNode(Collections.emptySet(), Collections.emptySet());

    private Collection<ControlFlowGraphNode> next = new HashSet<>();
    private Collection<ControlFlowGraphNode> prev = new HashSet<>();
    private List<SyntaxTree> statementList = new ArrayList<>();
    private Collection<String> validVars;
    private Collection<String> jsTypeVars;

    private Collection<String> initialized = null;

    public ControlFlowGraphNode(Collection<String> validVars, Collection<String> jsTypeVars){
        this.validVars = validVars;
        this.jsTypeVars = jsTypeVars;
    }
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
    public void setValidVars(Collection<String> c){
        this.validVars = c;
    }
    public void setJSTypeVars(Collection<String> c){
        this.jsTypeVars = c;
    }
    public void setInitialized(Collection<String> c){
        this.initialized = c;
    }
    public boolean isSetInitialized(){
        return initialized != null;
    }
    public Collection<String> getInitialized(){
        return initialized;
    }
    public Collection<String> getJSTypeVars(){
        return jsTypeVars;
    }
    public Collection<String> selectValid(Collection<String> c){
        Collection<String> ret = new HashSet<>();
        for(String s : c){
            if(validVars.contains(s)){
                ret.add(s);
            }
        }
        return ret;
    }
    @Override
    public Iterator<SyntaxTree> iterator(){
        return statementList.iterator();
    }
}
