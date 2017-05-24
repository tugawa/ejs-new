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
		List<PT> pts = DataType.typeRepresentationStreamOf(DataType.all())
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
		List<HT> pts = DataType.typeRepresentationStreamOf(DataType.all())
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
		return DataType.typeRepresentationStreamOf(dts)
			.map(tr -> {
				PT pt = tr.getPT();
				if (hasUniquePT(pt, dts, DataType.all()))
					return "(((x) & "+ pt.name +"_MASK) == "+ pt.name +")";
				else
					return "(((x) & "+ pt.name +"_MASK) == "+ pt.name +" && "
							+"obj_header_tag(x) == "+ tr.getHT().name +")";
				})
			.distinct()
			.collect(Collectors.joining(" || "));
	}

	String defineDTPredicates() {
		return DataType.all().stream()
			.map(dt -> "#define is_" + dt.name + "(x) "+ minimumRepresentation(Stream.of(dt).collect(Collectors.toSet())) +"\n")
			.collect(Collectors.joining());
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
		Map<PT, Long> ptCount = DataType.typeRepresentationStreamOf(DataType.all())
			.map(tr -> tr.getPT())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		String def = "";
		def += "#define has_unique_ptag(x) "+
				ptCount.keySet().stream().filter(pt -> ptCount.get(pt) == 1).map(pt -> {
					return "(((x) & "+ pt.name + "_MASK) == " + pt.name + ")";
				})
				.collect(Collectors.joining(" || "));
		def += "\n";
		def += "#define has_common_ptag(x) "+
				ptCount.keySet().stream().filter(pt -> ptCount.get(pt) > 1).map(pt -> {
					return "(((x) & "+ pt.name + "_MASK) == " + pt.name + ")";
				})
				.collect(Collectors.joining(" || "));
		def += "\n";
		return def;
	}

	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		if (args.length == 1)
			td.load(args[0]);
		else
			td.load("datatype/embstr.dtdef"); // debug
		TypesGen tg = new TypesGen();
		System.out.println(tg.definePT());
		System.out.println(tg.defineHT());
		System.out.println(tg.defineDTPredicates());
		System.out.println(tg.defineDTFamilyPredicates());
		System.out.println(tg.uniquenessPredicates());
	}
}
