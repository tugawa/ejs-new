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
    TypeMapSet headDict;
    TypeMapSet tailDict;
    ExprTypeSet exprTypeSet;
    Set<String> rematchVarSet;
    Set<SyntaxTree> expandedTreeCandidate;
    SyntaxTree expandedTree;
    boolean cannotExpandFlag;

    public static final SyntaxTree PHANTOM_NODE = new SyntaxTree(Symbol.unique("None"), null, null, null);

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
        if(headDict != null)
            t.headDict = headDict.clone();
        if(tailDict != null)
            t.tailDict = tailDict.clone();
        if(exprTypeSet != null)
            t.exprTypeSet = exprTypeSet.clone();
        if(rematchVarSet != null)
            t.rematchVarSet = new HashSet<>(rematchVarSet);
        if(expandedTreeCandidate != null){
            t.expandedTreeCandidate = new HashSet<>(expandedTreeCandidate.size());
            for(SyntaxTree tree : expandedTreeCandidate)
                t.expandedTreeCandidate.add(tree.dup());
        }
        if(expandedTree != null)
            t.expandedTree = expandedTree.dup();
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

    public void setHeadDict(TypeMapSet dict) {
        this.headDict = dict;
    }
    
    public TypeMapSet getHeadDict() {
        return headDict;
    }
    
    public void setTailDict(TypeMapSet dict) {
        this.tailDict = dict;
    }
    
    public TypeMapSet getTailDict() {
        return tailDict;
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

    public void clearExpandedTreeCandidateRecursive(){
        clearExpandedTreeCandidate();
        expandedTree = null;
        for(SyntaxTree chunk : this)
            chunk.clearExpandedTreeCandidateRecursive();
    }

    public void addExpandedTreeCandidate(SyntaxTree tree){
        if(expandedTreeCandidate == null)
            expandedTreeCandidate = new HashSet<>();
        if(expandedTreeCandidate.isEmpty())
            expandedTree = tree;
        expandedTreeCandidate.add(tree);
    }

    //Test Method
    public Set<SyntaxTree> getExpnadedTreeCandidates(){
        return expandedTreeCandidate;
    }

    public void setFailToExpansion(){
        cannotExpandFlag = true;
    }

    public SyntaxTree getExpandedTree(){
        if(expandedTreeCandidate == null || cannotExpandFlag){
            return null;
        }
        if(expandedTreeCandidate.size() == 1){
            if(expandedTree == null)
                throw new Error("Illigal expandedTreeCandidate state.");
            return expandedTree;
        }
        return null;
    }

    public boolean hasExpandedTree(){
        return getExpandedTree() != null;
    }

    @Override
    public int hashCode() {
        return toText().hashCode();
    }

    @Override
    public boolean equals(Object that){
        if(!(that instanceof SyntaxTree)) return false;
        SyntaxTree thatTree = (SyntaxTree) that;
        if(hashCode() != thatTree.hashCode()) return false;
        if(subTree == null) return toText().equals(thatTree.toText());
        Iterator<SyntaxTree> subTreeIterator = this.iterator();
        Iterator<SyntaxTree> thatSubTreeIterator = thatTree.iterator();
        while(subTreeIterator.hasNext() && thatSubTreeIterator.hasNext()){
            SyntaxTree subTree = subTreeIterator.next();
            SyntaxTree thatSubTree = thatSubTreeIterator.next();
            if(!subTree.equals(thatSubTree)) return false;
        }
        if(subTreeIterator.hasNext() || thatSubTreeIterator.hasNext()) return false;
        SyntaxTree expandedThis = getExpandedTree();
        SyntaxTree expandedThat = thatTree.getExpandedTree();
        if(expandedThis == null && expandedThat == null) return true;
        if(expandedThis != null && expandedThat != null) return expandedThis.equals(expandedThat);
        return false;
    }
}