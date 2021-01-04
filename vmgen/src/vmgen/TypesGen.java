/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import vmgen.type.TypeDefinition;
import vmgen.type.VMDataType;
import vmgen.type.VMRepType;

public class TypesGen {
    String definePT() {
        StringBuilder sb = new StringBuilder();
        for (VMRepType.PT pt: VMRepType.allPT()) {
            sb.append(String.format("#define %s %d\n", pt.getValueName(), pt.getValue()));
            sb.append(String.format("#define %s ((PTag) {%s})\n", pt.getName(), pt.getValueName()));
        }
        return sb.toString();
    }

    String defineHT() {
        StringBuilder sb = new StringBuilder();
        for (VMRepType.HT ht: VMRepType.allHT()) {
            sb.append(String.format("#define %s %d\n", ht.getValueName(), ht.getValue()));
            sb.append(String.format("#define %s ((HTag) {%s})\n", ht.getName(), ht.getCelltypeName()));
        }
        return sb.toString();
    }

    @SuppressWarnings("serial")
    static class ClassifiedVMRepTypes extends HashMap<VMRepType.PT, Set<VMRepType>> {
        public ClassifiedVMRepTypes(Collection<VMRepType> rts) {
            for (VMRepType rt: rts) {
                VMRepType.PT pt = rt.getPT();
                Set<VMRepType> set = this.get(pt);
                if (set == null) {
                    set = new HashSet<VMRepType>();
                    this.put(pt, set);
                }
                set.add(rt);
            }
        }
    }

    String minimumRepresentation(Collection<VMRepType> dts, Collection<VMRepType> among) {
        if (!among.containsAll(dts))
            throw new Error("Internal error");

        if (dts.containsAll(among))
            return "1";

        ClassifiedVMRepTypes targetMap = new ClassifiedVMRepTypes(dts);
        ClassifiedVMRepTypes amongMap = new ClassifiedVMRepTypes(among);

        Collection<VMRepType.PT> unique = new HashSet<VMRepType.PT>();
        Collection<VMRepType.PT> common = new HashSet<VMRepType.PT>();
        Collection<VMRepType.HT> hts = new ArrayList<VMRepType.HT>();
        for (VMRepType.PT pt: targetMap.keySet()) {
            Set<VMRepType> targetSet = targetMap.get(pt);
            Set<VMRepType> amongSet = amongMap.get(pt);
            if (targetSet.containsAll(amongSet))
                unique.add(pt);
            else {
                common.add(pt);
                for (VMRepType rt: targetSet)
                    hts.add(rt.getHT());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(((0");
        for (VMRepType.PT pt : common)
            sb.append(" || ")
            .append(String.format("is_ptag((x), %s)", pt.getName()));
        sb.append(") && (0");
        for (VMRepType.HT ht : hts)
            sb.append(" || ")
            .append(String.format("is_htag((x), %s)", ht.getName()));
        sb.append("))");
        for (VMRepType.PT pt : unique)
            sb.append(" || ")
            .append(String.format("is_ptag((x), %s)", pt.getName()));
        sb.append(")");

        return sb.toString();
    }

    String defineTypePredicates() {
        StringBuilder sb = new StringBuilder();

        sb.append("/* VM-DataTypes */\n");
        for (VMDataType dt: VMDataType.all()) {
            sb.append("#define is_").append(dt.getName()).append("(x) ");
            if (dt.getVMRepTypes().isEmpty())
                sb.append("0  /* not used */\n");
            else
                sb.append(minimumRepresentation(dt.getVMRepTypes(), VMRepType.all()))
                .append("\n");
        }

        sb.append("/* VM-RepTypes */\n");
        for (VMDataType dt: VMDataType.all()) {
            for (VMRepType rt: dt.getVMRepTypes()) {
                Set<VMRepType> rtSingleton = new HashSet<VMRepType>(1);
                rtSingleton.add(rt);
                sb.append("#define is_").append(rt.getName()).append("(x) ");
                sb.append(minimumRepresentation(rtSingleton, VMRepType.all()));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    void appendDataTypeFamilyPredicates(StringBuilder sb, String name, String[] dtNames) {
        Set<VMRepType> rts = new HashSet<VMRepType>();
        for (String dtName: dtNames)
            rts.addAll(VMDataType.get(dtName).getVMRepTypes());
        sb.append("#define is_").append(name).append("(x) ")
        .append(minimumRepresentation(rts, VMRepType.all()))
        .append("\n");
    }

    void appendHasHTagPredicate(StringBuilder sb) {
        Set<VMRepType> rts = new HashSet<VMRepType>();
        for (VMRepType rt: VMRepType.all())
            if (rt.hasHT())
                rts.add(rt);
        sb.append("#define has_htag(x) ")
        .append(minimumRepresentation(rts, VMRepType.all()))
        .append("\n");
    }

    String defineDTFamilyPredicates() {
        StringBuilder sb = new StringBuilder();
        appendDataTypeFamilyPredicates(sb,  "object", new String[] {
                "simple_object",
                "array",
                "function",
                "builtin",
                "iterator",
                "regexp",
                "string_object",
                "number_object",
                "boolean_object"
        });
        appendDataTypeFamilyPredicates(sb, "number", new String[]{
                "fixnum",
                "flonum"
        });
        appendHasHTagPredicate(sb);
        return sb.toString();
    }

    String defineVMRepType_LIST() {
        StringBuilder sb = new StringBuilder();
        sb.append("#define VMRepType_LIST \\\n");
        for (VMRepType rt: VMRepType.all()) {
            if (rt.getStruct() == null)
                continue;
            sb.append(String.format("VMRepType(%s, %s, %s) \\\n",
                      rt.getName(), rt.getPT().getName(), rt.getStruct()));
        }
        sb.append("\n");
        return sb.toString();
    }
    
    /* not used */
    String defineNeed() {
        StringBuilder sb = new StringBuilder();
        sb.append("/* VM-DataTypes */\n");
        for (VMDataType dt: VMDataType.all())
            if (!dt.getVMRepTypes().isEmpty())
                sb.append("#define need_").append(dt.getName()).append(" 1\n");
        sb.append("/* customised types */\n");
        for (VMDataType dt: VMDataType.all()) {
            for (VMRepType rt: dt.getVMRepTypes())
                sb.append("#define need_"+rt.getName()+" 1\n");
            if (dt.getVMRepTypes().size() > 1)
                sb.append("#define customised_"+dt.getName()+" 1\n");
        }
        return sb.toString();
    }

    /* not used */
    String defineTagOperations() {
        StringBuilder sb = new StringBuilder();

        /* leaf types */
        sb.append("/* leaf types */\n");
        for (VMRepType rt: VMRepType.all()) {
            String ptName = rt.getPT().getName();
            String cast = rt.getStruct() == null ? "" : ("("+rt.getStruct()+" *)");
            sb.append("#define put_"+rt.getName()+"_tag(p) ")
            .append("(put_tag((p), "+ptName+"))\n");
            sb.append("#define remove_"+rt.getName()+"_tag(p) ")
            .append("("+cast+"remove_tag((p), "+ptName+"))\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws FileNotFoundException {
        if (args.length == 1)
            TypeDefinition.load(args[0]);
        else
            TypeDefinition.load("datatype/new.dtdef"); // debug
        TypesGen tg = new TypesGen();
        System.out.println(tg.definePT());
        System.out.println(tg.defineHT());
        System.out.println(tg.defineTypePredicates());
        System.out.println(tg.defineDTFamilyPredicates());
 //       System.out.println(tg.defineTagOperations());
 //       System.out.println(tg.defineNeed());
        System.out.println(tg.defineVMRepType_LIST());
        System.out.println(TypeDefinition.getQuoted());
    }
}
