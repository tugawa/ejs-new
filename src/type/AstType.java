package type;


import java.util.ArrayList;
import type.VMDataType;
import vmdlc.SyntaxTree;
import nez.ast.Symbol;

import java.lang.Error;

/*
cint
cdouble
cstring
(cchar)
JSValue <- Subtyping にして，Union 型ではとりあえずやらない
    Primitive
        Number
            Fixnum
            Flonum
        String
        Special
        Bool
        Undef
        Null
    JSObject
        Array
HeapObject
*/

public class AstType {
    static AstType Bot = new AstBaseType("Bot");
    static AstType JSValue = new AstBaseType("JSValue");
    String name;

    public static AstType nodeToType(SyntaxTree node) {
        if (node.is(Symbol.unique("TypeProduct"))) {
            return new AstProductType(nodeToType(node.get(0)), nodeToType(node.get(1)));
        } else if (node.is(Symbol.unique("TypePair"))) {
            ArrayList<AstType> al = new ArrayList<AstType>();
            for (int i = 0; i < node.size(); i++) {
                al.add(nodeToType(node.get(i)));
            }
            return new AstPairType(al);
        } else if (node.is(Symbol.unique("TypeName")) ||
                    node.is(Symbol.unique("Ctype"))) {
            return new AstBaseType(node.toText());
        }
        return null;
    }

    public static class AstBaseType extends AstType {
        public AstBaseType(String _name) {
            name = _name;
        }
        public String toString() {
            return name;
        }
    }
    public AstType lub(AstType that) {
        if (this.name.equals(that.name)) {
            return this;
        } else if (this.name.equals("Fixnum") || this.name.equals("Flonum") || this.name.equals("Number")) {
            if (that.name.equals("Flonum") || that.name.equals("Fixnum")) {
                return new AstBaseType("Number");
            } else if (that.name.equals("Number")) {
                return that;
            } else {
                return JSValue;
            }
        } else {
            return JSValue;
        }
    }
    public AstType glb(VMDataType that) {
        if (this.name.equals("Bot") || that.name.equals("Bot")) {
            throw new Error("glb: Bot");
        }
        if (this.name.equals(that.name)) {
            return this;
        } else if (this.name.equals("JSValue")) {
            return that;
        } else if (that.name.equals("JSValue")) {
            return this;
        } else if (this.name.equals("Number")) {
            if (that.name.equals("Flonum") || that.name.equals("Fixnum")) {
                return that;
            } else {
                return Bot;
            }
        } else if (that.name.equals("Number")) {
            if (this.name.equals("Flonum") || this.name.equals("Fixnum")) {
                return this;
            } else {
                return Bot;
            }
        } else {
            return Bot;
        }
    }
        /*
JSValue
    Primitive
        Number
            Fixnum
            Flonum
        String
        Special
            Bool
            Undef
            Null
    JSObject
        Array
HeapObject
*/

    public static class AstPairType extends AstType {
        ArrayList<AstType> types;
        public AstPairType(ArrayList<AstType> _types) {
            types = _types;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("(");
            for (int i = 0; i < types.size()-1; i++) {
                sb.append(types.get(i).toString());
                sb.append(",");
            }
            if (types.size() != 0) {
                sb.append(types.get(types.size()-1).toString());
            }
            sb.append(")");
            return sb.toString();
        }
        public ArrayList<AstType> getTypes() {
            return types;
        }
    }

    public static class AstProductType extends AstType {
        AstType domain;
        AstType range;
        public AstProductType(AstType _domain, AstType _range) {
            domain = _domain;
            range = _range;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(domain.toString());
            sb.append("->");
            sb.append(range.toString());
            return sb.toString();
        }
        public AstType getDomain() {
            return domain;
        }
        public AstType getRange() {
            return range;
        }
    }
}

