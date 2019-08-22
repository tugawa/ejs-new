/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class Property extends Node implements IProperty {

    public enum KeyType {
        LITERAL, IDENTIFIER
    }

    KeyType keyType;

    public static final String KIND_INIT = "init";
    public static final String KIND_GET = "get";
    public static final String KIND_SET = "set";

    ILiteral literalKey;
    IIdentifier identifierKey;
    IExpression value;
    String kind;

    public Property(ILiteral key, IExpression value, String kind) {
        type = PROPERTY;
        keyType = KeyType.LITERAL;
        this.literalKey = key;
        this.value = value;
        this.kind = kind;
    }

    public Property(IIdentifier key, IExpression value, String kind) {
        type = PROPERTY;
        keyType = KeyType.IDENTIFIER;
        this.identifierKey = key;
        this.value = value;
        this.kind = kind;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "Property");
        if (keyType == KeyType.LITERAL) {
            jb.add(KEY_KEY, literalKey.getEsTree());
        } else if (keyType == KeyType.IDENTIFIER){
            jb.add(KEY_KEY, identifierKey.getEsTree());
        }

        jb.add(KEY_VALUE, value.getEsTree());
        jb.add(KEY_KIND, kind);

        return jb.build();
    }

    @Override
    public ILiteral getLiteralKey() {
        return literalKey;
    }

    @Override
    public IIdentifier getIdentifierKey() {
        return identifierKey;
    }

    @Override
    public IExpression getValue() {
        return value;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitProperty(this);
    }
}
