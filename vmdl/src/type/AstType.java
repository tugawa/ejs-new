/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package type;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import type.VMDataType;
import vmdlc.SyntaxTree;
import nez.ast.Symbol;

import java.lang.Error;

/*
cint
cdouble
cstring
(cchar)
JSValue <- Subtyping
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
    static Map<JSValueType, Set<JSValueVMType>> childrenMap = new HashMap<>();
    static void defineType(String name) {
        AstBaseType t = new AstBaseType(name);
        definedTypes.put(name, t);
    }
    static void defineJSValueType(String name, JSValueType parent) {
        AstBaseType t = new JSValueType(name, parent);
        definedTypes.put(name, t);
    }
    private static void putChild(JSValueType parent, JSValueVMType vmt){
        Set<JSValueVMType> set = childrenMap.get(parent);
        if(set == null){
            set = new HashSet<>();
        }
        set.add(vmt);
        childrenMap.put(parent, set);
        if(parent.parent != null){
            putChild(parent.parent, vmt);
        }
    }
    static void defineJSValueVMType(String name, JSValueType parent, VMDataType vmt) {
        JSValueVMType t = new JSValueVMType(name, parent, vmt);
        definedTypes.put(name, t);
        vmtToType.put(vmt,  t);
        putChild(parent, AstType.get(vmt));
    }
    public static AstBaseType get(String name) {
        return definedTypes.get(name);
    }
    public static JSValueVMType get(VMDataType vmt) {
        return vmtToType.get(vmt);
    }
    public static Set<JSValueVMType> getChildren(JSValueType parent){
        return childrenMap.get(parent);
    }
    static {
        defineType("Top");
        defineType("void");
        defineType("HeapObject");
        defineType("cint");
        defineType("cdouble");
        defineType("cstring");
        defineType("Displacement");
        defineType("Subscript");
        defineJSValueType("JSValue", null);
        JSValueType jsValType = (JSValueType) AstType.get("JSValue");
        defineJSValueType("Number", jsValType);
        JSValueType jsNumType = (JSValueType) AstType.get("Number");
        defineJSValueVMType("String", jsValType, VMDataType.get("string"));
        defineJSValueVMType("Fixnum", jsNumType, VMDataType.get("fixnum"));
        defineJSValueVMType("Flonum", jsNumType, VMDataType.get("flonum"));
        defineJSValueVMType("Special", jsValType, VMDataType.get("special"));
        JSValueType jsSpeType = (JSValueType) AstType.get("Special");
        defineJSValueType("JSObject", jsValType);
        JSValueType jsObjType = (JSValueType) AstType.get("JSObject");
        defineJSValueVMType("SimpleObject", jsObjType, VMDataType.get("simple_object"));
        defineJSValueVMType("Array", jsObjType, VMDataType.get("array"));
        defineJSValueVMType("Function", jsObjType, VMDataType.get("function"));
        defineJSValueVMType("Builtin", jsObjType, VMDataType.get("builtin"));
        defineJSValueVMType("Iterator", jsObjType, VMDataType.get("iterator"));
        defineJSValueVMType("Regexp", jsObjType, VMDataType.get("regexp"));
        defineJSValueVMType("StringObject", jsObjType, VMDataType.get("string_object"));
        defineJSValueVMType("NumberObject", jsObjType, VMDataType.get("number_object"));
        defineJSValueVMType("BooleanObject", jsObjType, VMDataType.get("boolean_object"));
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
        } else if (node.is(Symbol.unique("JSValueTypeName")) ||
                node.is(Symbol.unique("Ctype"))) {
            return AstType.get(node.toText());
        } else if (node.is(Symbol.unique("TopTypeName")))
            return AstType.get("Top");
        else if (node.is(Symbol.unique("VoidTypeName")))
            return AstType.get("void");
        throw new Error("Unknown type: "+node.toText());
    }

    public static class AstBaseType extends AstType {
        private AstBaseType(String _name) {
            name = _name;
        }
        public String toString() {
            return name;
        }
    }
    public AstType lub(AstType that) {
        if (!(this instanceof AstBaseType) || !(that instanceof AstBaseType)) {
            throw new Error("AstType lub: type error");
        }
        AstBaseType a = (AstBaseType)that;
        AstBaseType b = (AstBaseType)this;
        if (a == BOT)
            return b;
        if (b == BOT)
            return a;
        if (!(a instanceof JSValueType) || !(b instanceof JSValueType)) {
            if (a == b) {
                return a;
            } else {
                throw new Error("AstBaseType lub: type error");
            }
        }
        JSValueType a2 = (JSValueType)a;
        JSValueType b2 = (JSValueType)b;
        while (a2.depth > b2.depth)
            a2 = a2.parent;
        while (a2.depth < b2.depth)
            b2 = b2.parent;
        while (a2 != b2) {
            a2 = a2.parent;
            b2 = b2.parent;
        }
        return a2;
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
    public AstType glb(AstType that) {
        if (!(this instanceof AstBaseType) || !(that instanceof AstBaseType)) {
            throw new Error("AstType glb: type error: "+this+" vs "+that);
        }
        AstBaseType a = (AstBaseType)this;
        AstBaseType b = (AstBaseType)that;
        if (a == BOT)
            return BOT;
        if (b == BOT)
            return BOT;
        if (!(a instanceof JSValueType) || !(b instanceof JSValueType)) {
            if (a == b) {
                return a;
            } else {
                throw new Error("AstBaseType glb: type error");
            }
        }

        JSValueType a2 = (JSValueType)a;
        JSValueType b2 = (JSValueType)b;
        while (a2.depth > b2.depth)
            a2 = a2.parent;
        while (a2.depth < b2.depth)
            b2 = b2.parent;
        if (a2 != b2)
            return AstBaseType.BOT;
        else if (a2 == this)
            return that;
        else if (b2 == that)
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

    public static final AstBaseType BOT = new AstBaseType("$bot");
    public static class JSValueType extends AstBaseType {
        public static JSValueType get(String name) {
            return (JSValueType) AstType.get(name);
        }

        JSValueType parent;
        int depth;
        private JSValueType(String name, JSValueType parent) {
            super(name);
            this.parent = parent;
            depth = 0;
            for (JSValueType t = parent; t != null; t = t.parent)
                depth++;
        }

        public boolean isSuperOrEqual(VMDataType dt) {
            JSValueType jsvt = JSValueType.get(dt);
            return isSuperOrEqual(jsvt);
        }

        public boolean isSuperOrEqual(JSValueType jsvt) {
            while (jsvt != null) {
                if (jsvt == this)
                    return true;
                jsvt = jsvt.parent;
            }
            return false;
        }
    }

    public static class JSValueVMType extends JSValueType {
        VMDataType vmt;
        private JSValueVMType(String name, JSValueType parent, VMDataType vmt) {
            super(name, parent);
            this.vmt = vmt;
        }
        public VMDataType getVMDataType() {
            return vmt;
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

