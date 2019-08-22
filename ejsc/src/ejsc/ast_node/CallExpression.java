/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class CallExpression extends Node implements ICallExpression {

    IExpression callee;
    List<IExpression> arguments;

    boolean isTailCall = false;

    public CallExpression(IExpression callee, List<IExpression> arguments) {
        type = CALL_EXP;
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "CallExpression")
                .add(KEY_CALLEE, callee.getEsTree());
        if (arguments == null || arguments.size() == 0) {
            JsonArrayBuilder argumentsJb = Json.createArrayBuilder();
            jb.add(KEY_ARGUMENTS, argumentsJb);
        } else {
            JsonArrayBuilder argumentsJb = Json.createArrayBuilder();
            for (IExpression argument : arguments) {
                argumentsJb.add(argument.getEsTree());
            }
            jb.add(KEY_ARGUMENTS, argumentsJb);
        }

        return jb.build();
    }

    public void setTailCall() {
        this.isTailCall = true;
    }

    public boolean getTailCall() {
        return this.isTailCall;
    }

    @Override
    public IExpression getCallee() {
        return callee;
    }

    @Override
    public List<IExpression> getArguments() {
        return arguments;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitCallExpression(this);
    }

}
