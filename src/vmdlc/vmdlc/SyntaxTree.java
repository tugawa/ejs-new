package vmdlc;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;

import vmdlc.SyntaxTree;

public class SyntaxTree extends Tree<SyntaxTree> {
    protected NodeType type;

    public SyntaxTree() {
        super();
        type = null;
    }

    public SyntaxTree(Symbol tag, Source source, long pos, int len, int size, Object value, NodeType _type) {
        super(tag, source, pos, len, size > 0 ? new SyntaxTree[size] : null, value);
        type = _type;
    }

    @Override
    public SyntaxTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
        return new SyntaxTree(tag, source, pos, len, objectsize, value, null);
    }

    @Override
    public SyntaxTree newInstance(Symbol tag, int size, Object value) {
        return new SyntaxTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value, null);
    }

    @Override
    public void link(int n, Symbol label, Object child) {
        this.set(n, label, (SyntaxTree) child);
    }

    @Override
    protected SyntaxTree dupImpl() {
        SyntaxTree t = new SyntaxTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue(), this.getType());
        return t;
	}
	
	public NodeType getType() {
		return type;
	}

    public Tree<SyntaxTree>[] getSubTree() {
        return this.subTree;
    }

    public void replace(SyntaxTree node) {
        Tree<SyntaxTree>[] subTree = node.getSubTree();
        this.labels = (subTree != null) ? new Symbol[subTree.length] : EmptyLabels;
        // this.subTree = 

        if (subTree != null) {
			for (int i = 0; i < subTree.length; i++) {
				if (subTree[i] != null) {
					this.subTree[i] = subTree[i].dup();
					this.labels[i] = labels[i];
				}
			}
		}
    }
}