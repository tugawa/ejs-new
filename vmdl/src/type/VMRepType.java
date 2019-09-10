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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class VMRepType implements Comparable<VMRepType> {
    public static class HT implements Comparable<HT> {
        static Map<String, HT> internTable = new HashMap<String, HT>();

        static HT create(String name, int value) {
            HT ht = internTable.get(name);
            if (ht != null) {
                if (ht.value != value)
                    throw new Error("HT "+name+" is defined twice inconsistently");
                return ht;
            }
            ht = new HT(name, value);
            internTable.put(name, ht);
            return ht;
        }

        private HT(String name, int value) {
            this.name = name;
            this.value = value;
            this.defineOrder = internTable.size();
        }

        private String name;
        private int value;
        private int defineOrder;

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return String.format("%s", getName());
        }

        @Override
        public int compareTo(HT that) {
            return this.defineOrder - that.defineOrder;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class PT implements Comparable<PT> {
        static Map<String, PT> internTable = new HashMap<String, PT>();

        static PT get(String name, int value, int bits) {
            /* TODO: duplicate check */

            PT pt = internTable.get(name);
            if (pt != null) {
                if (pt.value != value || pt.bits != bits)
                    throw new Error("PT "+name+" is defined twice inconsistently");
                return pt;
            }
            pt = new PT(name, value, bits);
            internTable.put(name, pt);
            return pt;
        }

        private PT(String name, int value, int bits) {
            this.name = name;
            this.value = value;
            this.bits = bits;
            this.defineOrder = internTable.size();
        }

        private String name;
        private int value;
        private int bits;
        private int defineOrder;

        @Override
        public String toString() {
            return String.format("%s", name);
        }

        @Override
        public int compareTo(PT that) {
            return this.defineOrder - that.defineOrder;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int getBits() {
            return bits;
        }

        public void setBits(int bits) {
            this.bits = bits;
        }
    }

    static Map<String, VMRepType> definedVMRepType = new HashMap<String, VMRepType>();

    PT pt;
    HT ht;
    private String name;
    String struct;
    private int defineOrder;

    public static ArrayList<VMRepType> all() {
        ArrayList<VMRepType> lst = new ArrayList<VMRepType>(definedVMRepType.values());
        Collections.sort(lst);
        return lst;
    }

    public static ArrayList<PT> allPT() {
        ArrayList<PT> lst = new ArrayList<PT>(PT.internTable.values());
        Collections.sort(lst);
        return lst;
    }

    public static ArrayList<HT> allHT() {
        ArrayList<HT> lst = new ArrayList<HT>(HT.internTable.values());
        Collections.sort(lst);
        return lst;
    }

    static VMRepType get(String name, boolean permitNull) {
        VMRepType rt = definedVMRepType.get(name);
        if (rt == null && !permitNull)
            throw new Error("undefined VMRepType: "+name);
        return rt;
    }

    VMRepType(String name) {
        if (definedVMRepType.get(name) != null)
            throw new Error("VMRepType "+name+" redefined");
        this.defineOrder = definedVMRepType.size();
        this.name = name;
        definedVMRepType.put(name, this);
    }

    boolean initialised() {
        return pt != null;
    }

    void initialise(String ptName, int ptValue, int ptBits, String htName, int htValue, String struct) {
        if (initialised())
            throw new Error("VMRepType "+name+" has already been initialised");

        pt = PT.get(ptName, ptValue, ptBits);
        if (htName != null)
            ht = HT.create(htName, htValue);
        this.struct = struct;
    }

    boolean hasHT() {
        return ht != null;
    };

    public PT getPT() {
        return pt;
    }

    public HT getHT() {
        return ht;
    }

    public String getStruct() {
        return struct;
    }

    public String getName() {
        return name;
    }

    public boolean hasUniquePT(Collection<VMRepType> among) {
        for (VMRepType other: among) {
            if (this == other)
                continue;
            if (getPT() == other.getPT())
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        if (hasHT())
            return pt.toString() + "/" + ht.toString();
        else
            return pt.toString();
    }

    @Override
    public int compareTo(VMRepType that) {
        return this.defineOrder - that.defineOrder;
    }
}
