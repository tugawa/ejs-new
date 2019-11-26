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

import java.util.Set;

public class SyntaxTree extends Tree<SyntaxTree> {
    TypeMapSet dict;
    ExprTypeSet exprTypeSet;
    Set<String> rematchVarSet;

    public SyntaxTree() {
        super();
        exprTypeSet = null;
        rematchVarSet = null;
    }

    public SyntaxTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
        super(tag, source, pos, len, size > 0 ? new SyntaxTree[size] : null, value);
        exprTypeSet = null;
        rematchVarSet = null;
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
}