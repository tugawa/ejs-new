package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ForInStatement extends Node implements IForInStatement {

    public enum InitType {
        VAR_DECL,
        PATTERN
    }
    InitType initType;

    IVariableDeclaration varDeclLeft;
    IPattern patternLeft;
    IExpression right;
    IStatement body;

    String label = null;

    public ForInStatement(IVariableDeclaration left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.VAR_DECL;
        this.varDeclLeft = left;
        this.right = right;
        this.body = body;
    }

    public ForInStatement(IPattern left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.PATTERN;
        this.patternLeft = left;
        this.right = right;
        this.body = body;
    }
    
    public InitType getInitType() {
    	return initType;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ForInStatement");
        if (initType == InitType.VAR_DECL) {
            jb.add(KEY_LEFT, varDeclLeft.getEsTree());
        } else if (initType == InitType.PATTERN) {
            jb.add(KEY_LEFT, patternLeft.getEsTree());
        }else {
            jb.addNull(KEY_LEFT);
        }
        jb.add(KEY_RIGHT, right.getEsTree());
        jb.add(KEY_BODY, body.getEsTree());

        return jb.build();
    }

    @Override
    public IVariableDeclaration getValDeclLeft() {
        return varDeclLeft;
    }

    @Override
    public IPattern getPatternLeft() {
        return patternLeft;
    }

    @Override
    public IExpression getRight() {
        return right;
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

	@Override
	public void setVarDeclLeft(IVariableDeclaration varDeclLeft) {
		this.varDeclLeft = varDeclLeft;
	}

	@Override
	public void setPatternLeft(IPattern patternLeft) {
		this.patternLeft = patternLeft;
	}

	@Override
	public void setRight(IExpression right) {
		this.right = right;
	}

	@Override
	public void setBody(IStatement body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitForInStatement(this);
	}

}
