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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vmdlc.ErrorPrinter;
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
    static Map<String, AstBaseType> definedTypes = new HashMap<>();
    static Map<String, AstMappingType> definedMappingTypes = new HashMap<>();
    static Map<VMDataType, JSValueVMType> vmtToType = new HashMap<>();
    static Map<AstBaseType, Set<AstType>> childrenMap = new HashMap<>();
    static Set<AstProductType> builtinFunctionTypes = new HashSet<>(2);
    static AstBaseType defineType(String name) {
        return defineType(name, null);
    }
    static AstBaseType defineType(String name, AstBaseType parent) {
        AstBaseType t = new AstBaseType(name, parent);
        definedTypes.put(name, t);
        putChild(parent, t);
        return t;
    }
    static void defineJSValueType(String name, AstBaseType parent) {
        AstBaseType t = new JSValueType(name, parent);
        definedTypes.put(name, t);
        putChild(parent, t);
    }
    private static void putChild(AstBaseType parent, AstBaseType child){
        Set<AstType> children = childrenMap.get(parent);
        if(children == null){
            children = new HashSet<>();
        }
        children.add(child);
        childrenMap.put(parent, children);
        if(!(parent instanceof JSValueType)) return;
        JSValueType jparent = (JSValueType)parent;
        if(jparent.parent != null){
            putChild(jparent.parent, child);
        }
    }
    static void defineJSValueVMType(String name, JSValueType parent, VMDataType vmt) {
        JSValueVMType t = new JSValueVMType(name, parent, vmt);
        definedTypes.put(name, t);
        vmtToType.put(vmt,  t);
        putChild(parent, AstType.get(vmt));
    }
    public static void defineRequireGCPushPopType(String name) {
        AstBaseType t = new AstBaseType(name, null);
        t.setRequireGCPushPop(true);
        definedTypes.put(name, t);
    }
    public static AstMappingType defineMappingType(String name) {
        AstMappingType type = new AstMappingType(name);
        definedMappingTypes.put(name, type);
        return type;
    }
    public static void addAlias(String typeName, AstBaseType realType, String cTypeName){
        definedTypes.put(typeName, new AstAliasType(typeName, realType, cTypeName));
    }
    public static AstType get(String name) {
        if(name.endsWith("[]")){
            return new AstArrayType(get(name.substring(0, name.length()-2)));
        }
        if(name.equals("Args")) return ARGS;
        AstBaseType type = definedTypes.get(name);
        if(type != null) return type;
        return definedMappingTypes.get(name);
    }
    public static JSValueVMType get(VMDataType vmt) {
        return vmtToType.get(vmt);
    }
    public static Set<AstType> getChildren(AstType parent){
        if(!(parent instanceof AstBaseType)) return Collections.emptySet();
        return childrenMap.get((AstBaseType)parent);
    }
    public Set<VMDataType> getVMDataTypes(){
        if(this instanceof JSValueVMType){
            JSValueVMType vmType = (JSValueVMType) this;
            Set<VMDataType> result = new HashSet<>(1);
            result.add(vmType.vmt);
            return result;
        }
        if(!(this instanceof JSValueType)){
            return null;
        }
        Set<AstType> children = getChildren(this);
        Set<VMDataType> result = new HashSet<>();
        for(AstType child : children){
            result.addAll(child.getVMDataTypes());
        }
        return result;
    }
    public Set<AstType> getDetailedTypes(){
        if(this instanceof JSValueType){
            Set<VMDataType> vmdts = getVMDataTypes();
            Set<AstType> astTyped = new HashSet<>(vmdts.size());
            for(VMDataType vmdt : vmdts){
                astTyped.add(get(vmdt));
            }
            return astTyped;
        }
        Set<AstType> newSet = new HashSet<>(1);
        newSet.add(this);
        return newSet;
    }

    public static final Args ARGS;
    static final AstProductType BUILTINFUNCTION_TYPE;

    public static boolean isBuiltinFunctionType(AstType type){
        return BUILTINFUNCTION_TYPE.equals(type);
    }
    public String getCName(){
        if(this instanceof AstAliasType){
            return ((AstAliasType)this).getCTypeName();
        }
        if(this instanceof JSValueType){
            return "JSValue";
        }
        if(this.name.equals("cdouble")){
            return "double";
        }
        if(this.name.equals("cstring")){
            return "char*";
        }
        return name;
    }
    static {
        defineType("Top");
        defineType("void");
        defineRequireGCPushPopType("HeapObject");
        defineType("cint");
        defineType("cdouble").setCCodeName("double");
        defineType("cstring").setCCodeName("char*");
        defineType("ConstantDisplacement");
        defineType("InstructionDisplacement");
        defineType("Subscript");
        defineJSValueType("JSValue", null);
        JSValueType jsValType = (JSValueType) AstType.get("JSValue");
        defineJSValueType("Number", jsValType);
        JSValueType jsNumType = (JSValueType) AstType.get("Number");
        defineJSValueVMType("String", jsValType, VMDataType.get("string"));
        defineJSValueVMType("Fixnum", jsNumType, VMDataType.get("fixnum"));
        defineJSValueVMType("Flonum", jsNumType, VMDataType.get("flonum"));
        defineJSValueVMType("Special", jsValType, VMDataType.get("special"));
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
        AstType cintType = AstType.get("cint");
        ARGS = new Args();
        //BUILTINFUNCTION_TYPE = new AstProductType(new AstPairType(Arrays.asList(new AstType[]{ cintType, cintType, new AstArrayType(jsValType) })), jsValType);
        BUILTINFUNCTION_TYPE = new AstProductType(new AstPairType(Arrays.asList(new AstType[]{ cintType, cintType, ARGS })), jsValType);
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
        } else if(node.is(Symbol.unique("TypeArray"))) {
            return new AstArrayType(nodeToType(node.get(Symbol.unique("type"))));
        }else if (node.is(Symbol.unique("JSValueTypeName")) ||
                node.is(Symbol.unique("Ctype"))){
            return AstType.get(node.toText());
        }else if (node.is(Symbol.unique("UserTypeName"))) {
            AstType type = AstType.get(node.toText());
            if(type == null){
                ErrorPrinter.error("Unkwon type: "+node.toText(), node);
            }
            return type;
        } else if (node.is(Symbol.unique("TopTypeName")))
            return AstType.get("Top");
        else if (node.is(Symbol.unique("VoidTypeName")))
            return AstType.get("void");
        throw new Error("Unknown type: "+node.toText());
    }

    public boolean isSuperOrEqual(AstType t) {
        if(this == get("Top")) return true; /* The Top type is the top of type hierarchy (EVEN ARRAY TYPE) */
        if(t == AstType.BOT) return true;
        if(!(t instanceof AstBaseType) || !(this instanceof AstBaseType)){
            return t.equals(this);
        }
        AstBaseType thisType;
        if(this instanceof AstAliasType) thisType = ((AstAliasType)this).realType;
        else thisType = (AstBaseType)this;
        AstBaseType type;
        if(t instanceof AstAliasType) type = ((AstAliasType)t).realType;
        else type = (AstBaseType)t;
        while(type != null){
            if (type == thisType) return true;
            type = type.parent;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null) return false;
        if(!(obj instanceof AstType)) return false;
        return this==obj;
    }

    public String getCCodeName(){
        return toString();
    }

    public static class AstBaseType extends AstType {
        AstBaseType parent;
        int depth;
        boolean requireGCPushPopFlag;
        String cCodeName;

        private AstBaseType(String _name) {
            name = _name;
            requireGCPushPopFlag = false;
            cCodeName = _name;
        }
        private AstBaseType(String _name, AstBaseType _parent) {
            name = _name;
            requireGCPushPopFlag = false;
            cCodeName = _name;
            parent = _parent;
            depth = 0;
            for (AstBaseType t = parent; t != null; t = t.parent)
                depth++;
        }
        public String toString() {
            return name;
        }
        public boolean isRequiredGCPushPop(){
            return requireGCPushPopFlag;
        }
        public void setRequireGCPushPop(boolean flag){
            requireGCPushPopFlag = flag;
        }
        public void setCCodeName(String name){
            cCodeName = name;
        }
        @Override
        public String getCCodeName(){
            return cCodeName;
        }
    }
    public AstType lub(AstType that) {
        if (!(this instanceof AstBaseType) || !(that instanceof AstBaseType)) {
            throw new Error("InternalError: AstType lub type error "+this.toString()+", "+that.toString());
        }
        AstBaseType a = (AstBaseType)that;
        AstBaseType b = (AstBaseType)this;
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
    }

    // Use the fact that JSValueType forms a tree rather than a lattice
    public AstType glb(AstType that) {
        if (!(this instanceof AstBaseType) || !(that instanceof AstBaseType)) {
            throw new Error("InternalError: AstType glb type error: "+this+" vs "+that);
        }
        AstBaseType a = (AstBaseType)this;
        AstBaseType b = (AstBaseType)that;
        if (a == BOT)
            return BOT;
        if (b == BOT)
            return BOT;
        while (a.depth > b.depth)
            a = a.parent;
        while (a.depth < b.depth)
            b = b.parent;
        if (a != b)
            return AstBaseType.BOT;
        else if (a == this)
            return that;
        else if (b == that)
            return this;
        throw new Error("InternalError: glb is wrong algorithm!");
    }

    public static final AstBaseType BOT = new AstBaseType("$bot");
    public static class JSValueType extends AstBaseType {
        public static JSValueType get(String name) {
            return (JSValueType) AstType.get(name);
        }
        private JSValueType(String name, AstBaseType parent) {
            super(name, parent);
            setRequireGCPushPop(true);
        }
        @Override
        public String getCCodeName(){
            return "JSValue";
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
        List<AstType> types;
        public AstPairType(List<AstType> _types) {
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
        public List<AstType> getTypes() {
            return types;
        }
        public int size() {
            return types.size();
        }
        @Override
        public int hashCode(){
            return types.hashCode();
        }
        @Override
        public boolean equals(Object obj){
            if(obj == null) return false;
            if(!(obj instanceof AstPairType)) return false;
            return types.equals(((AstPairType)obj).types);
        }
    }

    public static class AstProductType extends AstType {
        AstType domain;
        AstType range;
        AstType[] domainArray;
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
        public int parameterSize(){
            if(domain instanceof AstBaseType)
                return 1;
            if(domain instanceof AstPairType)
                return ((AstPairType)domain).getTypes().size();
            throw new Error("Domain type of AstProductType is illigal state: "+domain.toString());
        }
        public AstType[] getDomainAsArray(){
            if(domainArray != null) return domainArray;
            if(domain instanceof AstPairType){
                domainArray = ((AstPairType)domain).getTypes().toArray(new AstType[0]);
            }else if(domain instanceof AstBaseType){
                domainArray = new AstType[]{ domain };
            }
            return domainArray;
        }
        @Override
        public int hashCode(){
            return domain.hashCode()*range.hashCode();
        }
        @Override
        public boolean equals(Object obj){
            if(obj == null) return false;
            if(!(obj instanceof AstProductType)) return false;
            return (domain.equals(((AstProductType)obj).domain)
                 && range.equals(((AstProductType)obj).range));
        }
    }

    public static class AstAliasType extends AstBaseType{
        private AstBaseType realType;
        private String cTypeName;

        private AstAliasType(String name, AstBaseType parent) {
            super(name, parent);
        }

        public AstAliasType(String name, AstBaseType _realType, String _cTypeName){
            super(name, _realType.parent);
            realType = _realType;
            cTypeName = _cTypeName;
        }

        public AstBaseType getRealType(){
            return realType;
        }

        public String getCTypeName(){
            return cTypeName;
        }

        @Override
        public String getCCodeName(){
            return getCTypeName();
        }
    }

    public static class AstArrayType extends AstType{
        AstType elementType;
        public AstArrayType(AstType _elementType){
            elementType = _elementType;
        }
        @Override
        public String toString(){
            return elementType.toString()+"[]";
        }
        public AstType getElementType(){
            return elementType;
        }
        @Override
        public int hashCode(){
            return elementType.hashCode();
        }
        @Override
        public boolean equals(Object obj){
            if(obj == null) return false;
            if(!(obj instanceof AstArrayType)) return false;
            return elementType.equals(((AstArrayType)obj).elementType);
        }
        @Override
        public String getCCodeName(){
            return elementType.getCCodeName()+"*";
        }
    }

    public static class Args extends AstArrayType{
        public Args(){
            super(get("JSValue"));
        }
        @Override
        public String toString(){
            return "Args";
        }
        @Override
        public String getCCodeName(){
            return "JSValue[]";
        }
    }

    public static class AstMappingType extends AstType{
        Set<Field> members;
        private AstMappingType(String _name){
            name = _name;
            members = new HashSet<>();
        }
        @Override
        public String toString(){
            return name;
        }
        public AstType getFieldType(String name){
            for(Field field : members){
                if(field.name.equals(name)) return field.type;
            }
            return null;
        }
        public Set<String> getFieldAnnotations(String name){
            for(Field field : members){
                if(field.name.equals(name)) return field.annotations;
            }
            return null;
        }
        public void addField(Set<String> annotations, String name, AstType type){
            members.add(new Field(annotations, name, type));
        }
        @Override
        public int hashCode(){
            return name.hashCode();
        }
        @Override
        public boolean equals(Object obj){
            if(obj == null) return false;
            if(!(obj instanceof AstMappingType)) return false;
            AstMappingType that = (AstMappingType)obj;
            return (name.equals(that.name) && members.equals(that.members));
        }

        private class Field{
            private Set<String> annotations;
            private String name;
            private AstType type;
            private Field(Set<String> _annotations, String _name, AstType _type){
                annotations = _annotations;
                name = _name;
                type = _type;
            }
            @Override
            public int hashCode(){
                return name.hashCode();
            }
            @Override
            public boolean equals(Object obj){
                if(obj == null) return false;
                if(!(obj instanceof Field)) return false;
                Field that = (Field)obj;
                return (name.equals(that.name)
                     && annotations.equals(that.annotations)
                     && type.equals(type));
            }
        }
    }
}

