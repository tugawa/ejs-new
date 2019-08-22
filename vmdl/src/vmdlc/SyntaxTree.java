package vmdlc;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

import type.AstType;
import type.TypeMap;
import vmdlc.SyntaxTree;

import java.util.HashSet;

public class SyntaxTree extends Tree<SyntaxTree> {
    TypeMap dict;
    AstType type;

    public SyntaxTree() {
        super();
        type = null;
    }

    public SyntaxTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
        super(tag, source, pos, len, size > 0 ? new SyntaxTree[size] : null, value);
        type = null;
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
        if (type == null) {
            sb.append(" []");
        } else {
            sb.append(" " + this.type);
        }
    }
    
    public void setType(AstType _type) {
        type = _type;
    }
    
    public void setTypeMap(TypeMap dict) {
        this.dict = dict;
    }
    
    public TypeMap getTypeMap() {
        return dict;
    }
}