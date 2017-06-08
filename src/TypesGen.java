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
		List<PT> pts = DataType.typeRepresentationStreamOf(DataType.allUsed())
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

	String minimumRepresentation(Set<DataType> dts) {
		return "(" +
				DataType.typeRepresentationStreamOf(dts)
				.map(tr -> {
					PT pt = tr.getPT();
					if (hasUniquePT(pt, dts, DataType.allUsed()))
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
		for (DataType dt: DataType.allInSpec()) {
			sb.append("#define is_").append(dt.name).append(" ");
			if (dt.reprs.isEmpty())
				sb.append("0  /* not used */\n");
			else
				sb.append(minimumRepresentation(Stream.of(dt).collect(Collectors.toSet())) +"\n");
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
			.map(p -> "#define is_"+ p.first() +"(x) "+ minimumRepresentation(p.second().stream().map(s -> DataType.get(s)).collect(Collectors.toSet())) + "\n")
			.collect(Collectors.joining());
	}

	String uniquenessPredicates() {
		Map<PT, Long> ptCount = DataType.typeRepresentationStreamOf(DataType.allUsed())
			.map(tr -> tr.getPT())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		String def = "";
		def += "#define has_unique_ptag(x) ("+
				ptCount.keySet().stream().filter(pt -> ptCount.get(pt) == 1).map(pt -> {
					return "(((x) & "+ pt.name + "_MASK) == " + pt.name + ")";
				})
				.collect(Collectors.joining(" || ")) +
				")";
		def += "\n";
		def += "#define has_common_ptag(x) ("+
				ptCount.keySet().stream().filter(pt -> ptCount.get(pt) > 1).map(pt -> {
					return "(((x) & "+ pt.name + "_MASK) == " + pt.name + ")";
				})
				.collect(Collectors.joining(" || ")) +
				")";
		def += "\n";
		return def;
	}

	String defineTagOperations() {
		String[][] typemap = new String[][] {
				{"simple_object", "Object"},
				{"array", "ArrayCell"},
				{"function", "FunctionCell"},
				{"builtin", "BuiltinCell"},
				{"iterator", "IteratorCell"},
				{"regexp", "RegexpCell"},
				{"flonum", null},
				{"string", null}
		};
		StringBuilder sb = new StringBuilder();
		for (String[] t: typemap) {
			String jsType = t[0];
			String cType = t[1];
			if (!DataType.get(jsType).reprs.isEmpty()) {
				String ptName = DataType.get(jsType).reprs.iterator().next().pt.name;
				sb.append("#define put_").append(jsType).append("_tag(p) ")
				  .append("(put_tag(p, ").append(ptName).append("))\n");
				if (cType != null)
					sb.append("#define remove_").append(jsType).append("_tag(p) ")
					  .append("((").append(cType).append(" *)remove_tag(p, ").append(ptName).append("))\n");
			}
		}
		if (!DataType.get("string_object").reprs.isEmpty()) {
			sb.append("#define put_boxed_tag(p) (put_tag(p, ")
			  .append(DataType.get("string_object").reprs.iterator().next().pt.name)
			  .append("))\n");
			sb.append("#define remove_boxed_tag(p) ((BoxedCell *)remove_tag(p, ")
			  .append(DataType.get("string_object").reprs.iterator().next().pt.name)
			  .append("))\n");
		}
		return sb.toString();
	}

	public static void main(String[] args) throws FileNotFoundException {
		if (args.length == 1)
			TypeDefinition.load(args[0]);
		else
			TypeDefinition.load("datatype/genericfloat.def"); // debug
		TypesGen tg = new TypesGen();
		System.out.println(tg.definePT());
		System.out.println(tg.defineHT());
		System.out.println(tg.defineDTPredicates());
		System.out.println(tg.defineDTFamilyPredicates());
		System.out.println(tg.uniquenessPredicates());
		System.out.println(tg.defineTagOperations());
		System.out.println(TypeDefinition.quoted);
	}
}
