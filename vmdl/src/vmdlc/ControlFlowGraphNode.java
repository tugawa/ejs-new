package vmdlc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


public class ControlFlowGraphNode implements Iterable<SyntaxTree>{
    public static ControlFlowGraphNode enter = new ControlFlowGraphNode(new HashSet<>(0), new HashSet<>(0));
    public static ControlFlowGraphNode exit = new ControlFlowGraphNode(new HashSet<>(0), new HashSet<>(0));

    private Collection<ControlFlowGraphNode> next = new HashSet<>();
    private Collection<ControlFlowGraphNode> prev = new HashSet<>();
    private List<SyntaxTree> statementList = new ArrayList<>();
    private Collection<String> headLocals;
    private Collection<String> tailLocals;
    private Collection<String> jsTypeVars;

    private Collection<String> initialized = null;

    public ControlFlowGraphNode(Collection<String> headLocals, Collection<String> jsTypeVars){
        this.headLocals = headLocals;
        this.tailLocals = headLocals;
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
    public void addLocals(String name){
        if(tailLocals == headLocals){
            tailLocals = new HashSet<>(headLocals);
        }
        tailLocals.add(name);
    }
    public void addJSTypeLocals(String name){
        addLocals(name);
        jsTypeVars.add(name);
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
    public Collection<String> selectValidAtHead(Collection<String> c){
        Collection<String> ret = new HashSet<>();
        for(String s : c){
            if(headLocals.contains(s)){
                ret.add(s);
            }
        }
        return ret;
    }
    public SyntaxTree getHeadStatement(){
        if(statementList.isEmpty()) return null;
        return statementList.get(0);
    }
    @Override
    public Iterator<SyntaxTree> iterator(){
        return statementList.iterator();
    }
}
