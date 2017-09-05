import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TypesGen {
	String definePT() {
		Set<PT> processed = new HashSet<PT>();
		List<PT> pts = new ArrayList<PT>();
		for (VMRepType rt: VMRepType.all()) {
			PT pt = rt.getPT();
			if (!processed.contains(pt)) {
				pts.add(pt);
				processed.add(pt);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (PT pt: pts) {
			sb.append(String.format("#define %s %d\n", pt.name, pt.value));
			sb.append(String.format("#define %s_MASK 0x%x\n", pt.name, (1 << pt.bits) - 1));
		}
		return sb.toString();
	}

	String defineHT() {
		/* Need to produce HT definition regardless of whether the type is used or not
		 * because GC uses HT.
		 */
		List<HT> pts = VMDataType.typeRepresentationStreamOf(VMDataType.all())
						.filter(tr -> tr.hasHT())
						.map(tr -> tr.getHT())
						.distinct()
						.collect(Collectors.toList());
		pts.sort((h1, h2) -> h1.value - h2.value);
		return pts.stream()
			.map(ht -> "#define " + ht.name +" "+ ht.value + "\n")
			.collect(Collectors.joining());
	}

	boolean hasUniquePT(PT pt, Collection<VMDataType> target, Collection<VMDataType> among) {
		return VMDataType.typeRepresentationStreamOf(among)
				.filter(tr -> tr.getPT() == pt)
				.allMatch(tr -> VMDataType.typeRepresentationOf(target).contains(tr));
	}

	String minimumRepresentation(Collection<VMRepType> dts, Collection<VMRepType> among) {
		if (!among.containsAll(dts))
			throw new Error("Internal error");
		
		if (among.size() == 1)
			return "1";

		Collection<PT> unique = new HashSet<PT>();
		Collection<PT> common = new HashSet<PT>();
		Collection<HT> hts = new ArrayList<HT>();
		for (VMRepType rt: dts) {
			if (rt.hasUniquePT(among))
				unique.add(rt.getPT());
			else {
				common.add(rt.getPT());
				hts.add(rt.getHT());
			}
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("(((0");
		for (PT pt : common) {
//			int mask = (1 << pt.bits) - 1;
//			sb.append(" || (((x) & "+mask+") == "+pt.value+")");
			sb.append(" || (((x) & "+pt.name+"_MASK) == "+pt.name+")");
		}
		sb.append(") && (0");
		for (HT ht : hts)
			sb.append(" || (obj_header_tag(x) == "+ht.name+")");
		sb.append("))");
		for (PT pt : unique) {
//			int mask = (1 << pt.bits) - 1;
//			sb.append(" || (((x) & "+mask+") == "+pt.value+")");
			int mask = (1 << pt.bits) - 1;
			sb.append(" || (((x) & "+pt.name+"_MASK) == "+pt.name+")");
		}
		sb.append(")");
		
		return sb.toString();
	}

	String defineTypePredicates() {
		StringBuilder sb = new StringBuilder();

		sb.append("/* VM-DataTypes */\n");
		for (VMDataType dt: VMDataType.all()) {
			sb.append("#define is_").append(dt.name).append("(x) ");
			if (dt.getRepresentations().isEmpty())
				sb.append("0  /* not used */\n");
			else
				sb.append(minimumRepresentation(dt.getRepresentations(), VMRepType.all()))
				  .append("\n");
		}

		sb.append("/* VM-RepTypes */\n");
		for (VMDataType dt: VMDataType.all()) {
			for (VMRepType rt: dt.getRepresentations()) {
				Set<VMRepType> rtSingleton = new HashSet<VMRepType>(1);
				rtSingleton.add(rt);
				sb.append("#define is_").append(rt.name).append("(x) ");
				sb.append(minimumRepresentation(rtSingleton, dt.getRepresentations()));
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	void appendDataTypeFamilyPredicates(StringBuffer sb, String name, String[] dtNames) {
		Set<VMRepType> rts = new HashSet<VMRepType>();
		for (String dtName: dtNames)
			rts.addAll(VMDataType.get(dtName).getRepresentations());
		sb.append("#define is_").append(name).append("(x) ")
		  .append(minimumRepresentation(rts, VMRepType.all()))
		  .append("\n");
	}

	String defineDTFamilyPredicates() {
		StringBuffer sb = new StringBuffer();
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
		return sb.toString();
	}

	String defineMisc() {
		StringBuffer sb = new StringBuffer();

		sb.append("/* uniqueness of PTAG */\n");
		Map<PT, Long> ptCount = VMRepType.all().stream()
			.map(rt -> rt.getPT())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		Set<PT> uniqPT = ptCount.keySet().stream().filter(pt -> ptCount.get(pt) == 1).collect(Collectors.toSet());
		Set<PT> commonPT = ptCount.keySet().stream().filter(pt -> ptCount.get(pt) > 1).collect(Collectors.toSet());
		if (uniqPT.size() < commonPT.size()) {
			sb.append("#define has_unique_ptag(p) (")
			  .append(uniqPT.stream()
					      .map(pt -> ("(((p) & "+pt.name+"_MASK) == "+pt.name+")"))
					      .collect(Collectors.joining("||")))
			  .append(")\n");
			sb.append("#define has_common_ptag(p) (!has_unique_ptag(p))\n");
		} else {
			sb.append("#define has_common_ptag(p) (")
			  .append(commonPT.stream()
					      .map(pt -> ("(((p) & "+pt.name+"_MASK) == "+pt.name+")"))
					      .collect(Collectors.joining("||")))
			  .append(")\n");
			sb.append("#define has_unique_ptag(p) (!has_common_ptag(p))\n");
		}

		return sb.toString();
	}

	String defineNeed() {
		StringBuilder sb = new StringBuilder();
		sb.append("/* VM-DataTypes */\n");
		for (VMDataType dt: VMDataType.all())
			if (!dt.getRepresentations().isEmpty())
				sb.append("#define need_").append(dt.name).append(" 1\n");
		sb.append("/* customised types */\n");
		for (VMDataType dt: VMDataType.all()) {
			for (VMRepType rt: dt.getRepresentations())
				sb.append("#define need_"+rt.name+" 1\n");
			if (dt.getRepresentations().size() > 1)
				sb.append("#define customised_"+dt.name+" 1\n");
		}
		return sb.toString();
	}
	
	String defineTagOperations() {
		StringBuilder sb = new StringBuilder();

		/* leaf types */
		sb.append("/* leaf types */\n");
		for (VMRepType rt: VMRepType.all()) {
			String ptName = rt.getPT().name;
			String cast = rt.getStruct() == null ? "" : ("("+rt.getStruct()+" *)");
			sb.append("#define put_"+rt.name+"_tag(p) ")
			  .append("(put_tag((p), "+ptName+"))\n");
			sb.append("#define remove_"+rt.name+"_tag(p) ")
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
		System.out.println(tg.defineMisc());
		System.out.println(tg.defineTagOperations());
		System.out.println(tg.defineNeed());
		System.out.println(TypeDefinition.quoted);
	}
}
