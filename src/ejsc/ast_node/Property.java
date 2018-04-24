/*
   Property.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return literalKey;
    }

    @Override
    public IIdentifier getIdentifierKey() {
        // TODO Auto-generated method stub
        return identifierKey;
    }

    @Override
    public IExpression getValue() {
        // TODO Auto-generated method stub
        return value;
    }

    @Override
    public String getKind() {
        // TODO Auto-generated method stub
        return kind;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitProperty(this);
    }
}
