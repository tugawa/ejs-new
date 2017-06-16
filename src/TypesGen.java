import java.io.FileNotFoundException;
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
		List<PT> pts = DataType.typeRepresentationStreamOf(DataType.allUsed(true))
						.map(tr -> tr.getPT())
						.distinct()
						.collect(Collectors.toList());
		pts.sort((p1, p2) -> p1.value - p2.value);
		return pts.stream()
			.map(pt -> {
				StringBuffer sb = new StringBuffer();
				sb.append(String.format("#define %s %d\n", pt.name, pt.value));
				sb.append(String.format("#define %s_MASK 0x%x\n", pt.name, (1 << pt.bits) - 1));
				return sb;
			})
			.collect(Collectors.joining());
	}

	String defineHT() {
		/* Need to produce HT definition regardless of whether the type is used or not
		 * because GC uses HT.
		 */
		List<HT> pts = DataType.typeRepresentationStreamOf(DataType.allInSpec())
						.filter(tr -> tr.hasHT())
						.map(tr -> tr.getHT())
						.distinct()
						.collect(Collectors.toList());
		pts.sort((h1, h2) -> h1.value - h2.value);
		return pts.stream()
			.map(ht -> "#define " + ht.name +" "+ ht.value + "\n")
			.collect(Collectors.joining());
	}

	boolean hasUniquePT(PT pt, Collection<DataType> target, Collection<DataType> among) {
		return DataType.typeRepresentationStreamOf(among)
				.filter(tr -> tr.getPT() == pt)
				.allMatch(tr -> DataType.typeRepresentationOf(target).contains(tr));
	}

	String minimumRepresentation(Set<DataType> dts, Collection<DataType> among) {
		return "(" +
				DataType.typeRepresentationStreamOf(dts)
				.map(tr -> {
					PT pt = tr.getPT();
					if (hasUniquePT(pt, dts, among))
						return "(((x) & "+ pt.name +"_MASK) == "+ pt.name +")";
					else
						return "(((x) & "+ pt.name +"_MASK) == "+ pt.name +" && "
								+"obj_header_tag(x) == "+ tr.getHT().name +")";
					})
				.distinct()
				.collect(Collectors.joining(" || ")) +
			   ")";
	}

	String defineDTPredicates() {
		StringBuilder sb = new StringBuilder();

		sb.append("/* VM types */\n");
		for (DataType dt: DataType.allInSpec()) {
			sb.append("#define is_").append(dt.name).append("(x) ");
			if (dt.reprs.isEmpty())
				sb.append("0  /* not used */\n");
			else
				sb.append(minimumRepresentation(Stream.of(dt).collect(Collectors.toSet()), DataType.allLeaves()) +"\n");
		}

		sb.append("/* custom types */\n");
		for (DataType dt: DataType.allLeaves()) {
			if (dt.isVMType())
				continue;
			sb.append("#define is_").append(dt.name).append("(x) ");
			sb.append(minimumRepresentation(Stream.of(dt).collect(Collectors.toSet()), dt.parent.children));
			sb.append("\n");
		}
		return sb.toString();
	}

	String defineDTFamilyPredicates() {
		final Pair<String, Set<String>>[] families = new Pair[] {
			new Pair<String, Set<String>>("object",
					Stream.of(
							"simple_object",
							"array",
							"function",
							"builtin",
							"iterator",
							"regexp",
							"string_object",
							"number_object",
							"boolean_object").collect(Collectors.toSet())),
			new Pair<String, Set<String>>("number",
					Stream.of(
							"fixnum",
							"flonum").collect(Collectors.toSet()))
		};
		return Arrays.stream(families)
			.map(p -> "#define is_"+ p.first() +"(x) "+ minimumRepresentation(p.second().stream().map(s -> DataType.get(s)).collect(Collectors.toSet()), DataType.allLeaves()) + "\n")
			.collect(Collectors.joining());
	}

	String defineMisc() {
		StringBuffer sb = new StringBuffer();

		sb.append("/* uniqueness of PTAG */\n");
		Map<PT, Long> ptCount = DataType.allLeaves().stream()
			.map(dt -> dt.getRepresentation().getPT())
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
		sb.append("/* VM types */\n");
		DataType.allUsed(false).forEach(dt -> {
			sb.append("#define need_").append(dt.name).append(" 1\n");
		});
		sb.append("/* customised types */\n");
		DataType.allUsed(false).forEach(dt -> {
			if (!dt.isLeaf()) {
				if (dt.children.size() > 1)
					sb.append("#define customised_"+dt.name+" 1\n");
				dt.children.forEach(c -> {
					sb.append("#define need_"+c.name+" 1\n");
				});
			}
		});
		return sb.toString();
	}
	
	String defineTagOperations() {
		StringBuilder sb = new StringBuilder();

		/* leaf types */
		sb.append("/* leaf types */\n");
		for (DataType dt: DataType.allLeaves()) {
			String ptName = dt.getRepresentation().getPT().name;
			String cast = dt.struct == null ? "" : ("("+dt.struct+" *)");
			sb.append("#define put_"+dt.name+"_tag(p) ")
			  .append("(put_tag((p), "+ptName+"))\n");
			sb.append("#define remove_"+dt.name+"_tag(p) ")
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
		System.out.println(tg.defineDTPredicates());
		System.out.println(tg.defineDTFamilyPredicates());
		System.out.println(tg.defineMisc());
		System.out.println(tg.defineTagOperations());
		System.out.println(tg.defineNeed());
		System.out.println(TypeDefinition.quoted);
	}
}
