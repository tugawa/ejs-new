package type;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    static Map<String, AstBaseType> definedTypes = new HashMap<String, AstBaseType>();
    static Map<VMDataType, JSValueVMType> vmtToType = new HashMap<VMDataType, JSValueVMType>();
    static void defineType(String name) {
        AstBaseType t = new AstBaseType(name);
        definedTypes.put(name, t);
    }
    static void defineJSValueType(String name, JSValueType parent) {
        AstBaseType t = new JSValueType(name, parent);
        definedTypes.put(name, t);
    }
    static void defineJSValueVMType(String name, JSValueType parent, VMDataType vmt) {
        JSValueVMType t = new JSValueVMType(name, parent, vmt);
        definedTypes.put(name, t);
        vmtToType.put(vmt,  t);
    }
    public static AstBaseType get(String name) {
        return definedTypes.get(name);
    }
    public static JSValueVMType get(VMDataType vmt) {
        return vmtToType.get(vmt);
    }
    static {
        defineType("Top");
        defineType("HeapObject");
        defineType("cint");
        defineType("cdouble");
        defineType("cstring");
        defineJSValueType("JSValue", null);
        JSValueType jsValType = (JSValueType) AstType.get("JSValue");
        defineJSValueType("Number", jsValType);
        JSValueType jsNumType = (JSValueType) AstType.get("JSValue");
        defineJSValueVMType("String", jsValType, VMDataType.get("string"));
        defineJSValueVMType("Fixnum", jsNumType, VMDataType.get("fixnum"));
        defineJSValueVMType("Flonum", jsNumType, VMDataType.get("flonum"));
        defineJSValueVMType("Special", jsValType, VMDataType.get("special"));
        JSValueType jsSpeType = (JSValueType) AstType.get("special");
        defineJSValueVMType("SimpleObject", jsValType, VMDataType.get("simple_object"));
        defineJSValueVMType("Array", jsValType, VMDataType.get("array"));
        defineJSValueVMType("Function", jsValType, VMDataType.get("function"));
        defineJSValueVMType("Builtin", jsValType, VMDataType.get("builtin"));
        defineJSValueVMType("SimpleIterator", jsValType, VMDataType.get("simple_iterator"));
        defineJSValueVMType("Regexp", jsValType, VMDataType.get("regexp"));
        defineJSValueVMType("StringObject", jsValType, VMDataType.get("string_object"));
        defineJSValueVMType("NumberObject", jsValType, VMDataType.get("number_object"));
        defineJSValueVMType("BooleanObject", jsValType, VMDataType.get("boolean_object"));
        defineJSValueType("Bool", jsSpeType);
    }

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
            return AstType.get(node.toText());
        }
        return null;
    }

    public static class AstBaseType extends AstType {
        private AstBaseType(String _name) {
            name = _name;
        }
        public String toString() {
            return name;
        }
    }

    public static class JSValueType extends AstBaseType {
        static final JSValueType BOT = new JSValueType("$bot", null);
        
        JSValueType parent;
        int depth;
        private JSValueType(String name, JSValueType parent) {
            super(name);
            this.parent = parent;
            depth = 0;
            for (JSValueType t = parent; t != null; t = t.parent)
                depth++;
        }
        
        public JSValueType lub(JSValueType a) {
            JSValueType b = this;
            if (a == BOT)
                return b;
            if (b == BOT)
                return a;
            while (a.depth > b.depth)
                a = a.parent;
            while (a.depth < b.depth)
                b = b.parent;
            while (a != b) {
                a = a.parent;
                b = b.parent;
            }
            return a;
            /*
            if (this.name.equals("Fixnum") || this.name.equals("Flonum") || this.name.equals("Number")) {
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
            */
        }

        // Use the fact that JSValueType forms a tree rather than a lattice
        public JSValueType glb(JSValueType that) {
            JSValueType a = this;
            JSValueType b = that;
            if (a == BOT)
                return BOT;
            if (b == BOT)
                return BOT;
            while (a.depth > b.depth)
                a = a.parent;
            while (a.depth < b.depth)
                b = b.parent;
            if (a != b)
                return JSValueType.BOT;
            else if (a == this)
                return that;
            else if (b == that)
                return this;
            throw new Error("wrong algorithm!");
            /*
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
            */
        }
    }

    public static class JSValueVMType extends JSValueType {
        VMDataType vmt;
        private JSValueVMType(String name, JSValueType parent, VMDataType vmt) {
            super(name, parent);
            this.vmt = vmt;
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

