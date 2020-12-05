/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

import type.AstType;
import type.ExprTypeSet;
import type.TypeMapSet;
import vmdlc.SyntaxTree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SyntaxTree extends Tree<SyntaxTree> {
    TypeMapSet dict;
    ExprTypeSet exprTypeSet;
    Set<String> rematchVarSet;
    Set<SyntaxTree> expandedTreeCandidate;
    SyntaxTree expandedTree;
    boolean cannotExpandFlag;

    public SyntaxTree() {
        super();
        exprTypeSet = null;
        rematchVarSet = null;
        expandedTreeCandidate = null;
        expandedTree = null;
        cannotExpandFlag = false;
    }

    public SyntaxTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
        super(tag, source, pos, len, size > 0 ? new SyntaxTree[size] : null, value);
        exprTypeSet = null;
        rematchVarSet = null;
        expandedTreeCandidate = null;
        expandedTree = null;
        cannotExpandFlag = false;
    }
    
    public SyntaxTree(Symbol tag, Symbol[] labels, SyntaxTree[] subTree, Object value){
        super(tag, null, 0, 0, subTree, value);
        if(labels == null){
            this.labels = (subTree != null) ? new Symbol[subTree.length] : EmptyLabels;
        }else{
            this.labels = labels;
        }
    }

    @Override
    public SyntaxTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
        return new SyntaxTree(tag, source, pos, len, objectsize, value);
    }

    @Override
    public SyntaxTree newInstance(Symbol tag, int size, Object value) {
        return new SyntaxTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
    }

    @Override
    public void link(int n, Symbol label, Object child) {
        this.set(n, label, (SyntaxTree) child);
    }

    @Override
    protected SyntaxTree dupImpl() {
        SyntaxTree t = new SyntaxTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
        t.exprTypeSet = exprTypeSet;
        t.rematchVarSet = rematchVarSet;
        t.expandedTreeCandidate = expandedTreeCandidate;
        t.expandedTree = expandedTree;
        t.cannotExpandFlag = cannotExpandFlag;
        return t;
    }

    public Tree<SyntaxTree>[] getSubTree() {
        return this.subTree;
    }

    public void replace(SyntaxTree node) {
        Tree<SyntaxTree>[] subTree = node.getSubTree();
        this.labels = (subTree != null) ? new Symbol[subTree.length] : EmptyLabels;

        if (subTree != null) {
            for (int i = 0; i < subTree.length; i++) {
                if (subTree[i] != null) {
                    this.subTree[i] = subTree[i].dup();
                    this.labels[i] = labels[i];
                }
            }
        }
    }

    @Override
    protected void appendExtraStringfied(StringBuilder sb) {
        if (exprTypeSet == null) {
            sb.append(" []");
        } else if (exprTypeSet.getTypeSet().isEmpty()){
            sb.append(" []");
        } else {
            sb.append(" [");
            for(AstType type : exprTypeSet.getTypeSet()){
                sb.append(type+",");
            }
            sb.append("]");
            /*
            sb.append(" " + this.type);
            */
        }
    }
    
    public void setExprTypeSet(ExprTypeSet _exprTypeSet) {
        exprTypeSet = _exprTypeSet;
    }
    
    public ExprTypeSet getExprTypeSet() {
        return exprTypeSet;
    }

    public void setTypeMapSet(TypeMapSet dict) {
        this.dict = dict;
    }
    
    public TypeMapSet getTypeMapSet() {
        return dict;
    }

    public void setRematchVarSet(Set<String> set){
        rematchVarSet = set;
    }

    public Set<String> getRematchVarSet(){
        return rematchVarSet;
    }

    public void clearExpandedTreeCandidate(){
        if(expandedTreeCandidate == null) return;
        expandedTreeCandidate.clear();
    }

    public void addExpandedTreeCandidate(SyntaxTree tree){
        if(expandedTreeCandidate == null){
            expandedTreeCandidate = new HashSet<>();
            expandedTree = tree;
        }
        expandedTreeCandidate.add(tree);
    }

    //Test Method
    public Set<SyntaxTree> getExpnadedTreeCandidates(){
        return expandedTreeCandidate;
    }

    public void setFailToExpansion(){
        cannotExpandFlag = true;
    }

    public SyntaxTree getExpanndedTree(){
        /*
        System.err.println("original:"+toString());
        if(expandedTreeCandidate!=null)System.err.println("candidates:"+expandedTreeCandidate.toString());
        else System.err.println("candidates:null");
        */
        if(expandedTreeCandidate == null || cannotExpandFlag){
            //System.err.println("No candidate, or cannnotExpandFalg");
            //System.err.println("cannnotExpandFalg?="+cannotExpandFlag);
            return null;
        }
        if(expandedTreeCandidate.size() == 1){
            //System.err.println("Success To Expand");
            return expandedTree;
        }
        //System.err.println("Fail To Expand due to multiple candidates");
        return null;
    }

    @Override
    public int hashCode() {
        return toText().hashCode();
    }

    @Override
    public boolean equals(Object that){
        if(!(that instanceof SyntaxTree)) return false;
        SyntaxTree thatTree = (SyntaxTree) that;
        if(subTree == null) return toText().equals(thatTree.toText());
        Iterator<SyntaxTree> subTreeIterator = this.iterator();
        Iterator<SyntaxTree> thatSubTreeIterator = thatTree.iterator();
        while(subTreeIterator.hasNext() && thatSubTreeIterator.hasNext()){
            SyntaxTree subTree = subTreeIterator.next();
            SyntaxTree thatSubTree = thatSubTreeIterator.next();
            if(!subTree.equals(thatSubTree)) return false;
        }
        return !(subTreeIterator.hasNext() || thatSubTreeIterator.hasNext());
    }
}