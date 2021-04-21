/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import type.AstType.JSValueType;
import type.AstType.JSValueVMType;

public abstract class VMDataTypeVecSet {
    static final boolean DEBUG = false;
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

    public static class BySet extends VMDataTypeVecSet {
        Set<VMDataType[]> dtsSet;
        public BySet(String[] varNames, Set<VMDataType[]> dtsSet) {
            super(varNames);
            this.dtsSet = dtsSet;
        }

        @Override
        public AstType getMostSpecificType(String vn) {
            return getMostSpecificTypeFromSet(dtsSet, vn);
        }

        @Override
        public Set<VMDataType[]> getTuples() {
            return dtsSet;
        }
    }

    public static class ByCommonTypes extends VMDataTypeVecSet {
        AstType[] types;
        public ByCommonTypes(String[] varNames, AstType[] types) {
            super(varNames);
            this.types = types;
        }

        @Override
        public AstType getMostSpecificType(String vn) {
            int index = lookup(vn);
            return types[index];
        }

        public ByCommonTypes intersection(VMDataTypeVecSet that) {
            Set<VMDataType[]> dtss = that.getTuples();
            AstType[] result = new AstType[types.length];
            Arrays.fill(result, AstType.BOT);
            NEXT_DTS: for (VMDataType[] dts: dtss) {
                for (int i = 0; i < types.length; i++) {
                    JSValueVMType t = JSValueVMType.get(dts[i]);
                    /*
                     * VMType is usually a subtype of a JSValueType.
                     * An exception is "Special", which has a subtype Bool.
                     */
                    if (t.glb(types[i]) == AstType.BOT)
                        continue NEXT_DTS;
                }
                for (int i = 0; i < types.length; i++) {
                    JSValueVMType t = JSValueVMType.get(dts[i]);
                    result[i] = result[i].lub(t);
                }
            }
            return new ByCommonTypes(varNames, result);
        }

        @Override
        public Set<VMDataType[]> getTuples() {
            int length = types.length;
            VMDataType[] vec = new VMDataType[length];
            for(int i=0; i<length; i++){
                if(types[i] instanceof JSValueVMType){
                    vec[i] = ((JSValueVMType)types[i]).getVMDataType();
                }else{
                    throw new Error("ByCommonTypes has not JSValueVMType element");
                }
            }
            HashSet<VMDataType[]> set = new HashSet<>();
            set.add(vec);
            return set;
        }
    }

    protected AstType getMostSpecificTypeFromSet(Set<VMDataType[]> dtss, String vn) {
        final JSValueType jsv = JSValueType.get("JSValue");
        int index = lookup(vn);
        AstType t = AstType.BOT;
        if (DEBUG) {
            System.out.println("===== Type candidates for "+vn+" =====");
            Set<VMDataType> dtSet = new HashSet<VMDataType>();
            for (VMDataType[] dts: dtss)
                dtSet.add(dts[index]);
            for (VMDataType dt: dtSet)
                System.out.println(dt);
            System.out.println("===== Type candidates end =====");                
        }           

        for (VMDataType[] dts: dtss) {
            JSValueVMType s = JSValueVMType.get(dts[index]);
            t = t.lub(s);
            if (t == jsv)
                break;
        }
        return t;
    }

    public abstract Set<VMDataType[]> getTuples();

    public abstract AstType getMostSpecificType(String vn);
}
