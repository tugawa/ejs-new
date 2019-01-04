package type;

import java.util.Set;

import type.AstType.JSValueType;

public abstract class VMDataTypeVecSet {
    protected String[] varNames;

    public String[] getVarNames() {
        return varNames;
    }
    
    protected int lookup(String varName) {
        for (int i = 0; i < varNames.length; i++)
            if (varNames[i].equals(varName))
                return i;
        throw new Error("var not found");
    }

    protected VMDataTypeVecSet(String[] varNames) {
        this.varNames = varNames;
    }

    /*
    public static class ImplSet extends VMDataTypeVecSet {
        Set<VMDataType[]> dtsSet;
        public ImplSet(String[] varNames, Set<VMDataType[]> dtsSet) {
            super(varNames);
            this.dtsSet = dtsSet;
        }
        public JSValueType getMostSpecificType(String vn) {
            TODO
        }
    }
    
    public static class ImplTypeVec extends VMDataTypeVecSet {
        AstType[] typeVec;
        public ImplTypeVec(String[] varNames, AstType[] typeVec) {
            super(varNames);
            this.typeVec = typeVec;
        }
        public JSValueType getMostSpecificType(String vn) {
            TODO
        }
    }
    */

    public abstract AstType getMostSpecificType(String vn);
}
