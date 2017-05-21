import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class PT {
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
	}

	String name;
	int value;
	int bits;
	
	@Override
	public String toString() {
		return String.format("%s(%d/%d)", name, value, bits);
	}
}

class HT {
	static Map<String, HT> internTable = new HashMap<String, HT>();
	
	static HT get(String name, int value) {
		/* TODO: duplicate check */
		
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
	}

	String name;
	int value;
	
	@Override
	public String toString() {
		return String.format("%s(%d)", name, value);
	}
}


class TypeRepresentation {
	PT pt;
	HT ht;
	
	TypeRepresentation(String ptName, int ptValue, int ptBits, String htName, int htValue) {
		pt = PT.get(ptName, ptValue, ptBits);
		if (htName != null)
			ht = HT.get(htName, htValue);
	}
	
	boolean hasHT() {
		return ht != null;
	};
	
	PT getPT() {
		return pt;
	}
	
	HT getHT() {
		return ht;
	}
	
	@Override
	public String toString() {
		if (hasHT())
			return pt.toString() + "/" + ht.toString();
		else
			return pt.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ht == null) ? 0 : ht.hashCode());
		result = prime * result + ((pt == null) ? 0 : pt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypeRepresentation other = (TypeRepresentation) obj;
		if (ht == null) {
			if (other.ht != null)
				return false;
		} else if (!ht.equals(other.ht))
			return false;
		if (pt == null) {
			if (other.pt != null)
				return false;
		} else if (!pt.equals(other.pt))
			return false;
		return true;
	}
}

class DataType {
	static Map<String, DataType> dataTypes = new HashMap<String, DataType>();
	
	static void defineDataType(String name) {
		dataTypes.put(name, new DataType(name));
	}
	
	static DataType get(String name) {
		DataType dt = dataTypes.get(name);
		if (dt == null)
			throw new Error("unknown data type; "+ name);
		return dt;
	}

	private DataType(String name) {
		this.name = name;
		reprs = new HashSet<TypeRepresentation>();
	}
	
	static {
		Stream.of(
			"string",
			"fixnum",
/*			"flonum",
			"special", */
			"simple_object",
			"array",
			"function"//,
/*			"builtin",
			"iterator",
			"regexp",
			"string_object",
			"number_object",
			"boolean_object" */)
		.forEach(name -> defineDataType(name));
	}
	
	static Collection<DataType> all() {
		return dataTypes.values();
	}

	/*
	 * utilities
	 */
	
	static Set<TypeRepresentation> typeRepresentationOf(Collection<DataType> dts) {
		return typeRepresentationStreamOf(dts).collect(Collectors.toSet());
	}
	
	static Set<TypeRepresentation> typeRepresentationOf(Stream<DataType> dts) {
		return typeRepresentationStreamOf(dts).collect(Collectors.toSet());
	}

	static Stream<TypeRepresentation> typeRepresentationStreamOf(Collection<DataType> dts) {
		return typeRepresentationStreamOf(dts.stream());
	}

	static Stream<TypeRepresentation> typeRepresentationStreamOf(Stream<DataType> dts) {
		return dts.flatMap(dt -> dt.getRepresentations().stream());
	}

	static Set<PT> uniquePT(Set<TypeRepresentation> trs) {
		return trs.stream()
				.map(tr -> tr.getPT())
				.distinct()
				.filter(pt -> {
					return typeRepresentationStreamOf(DataType.all())
							.filter(tr -> tr.getPT() == pt)
							.allMatch(tr -> trs.contains(tr));
				})
				.collect(Collectors.toSet());
	}
	
	/*
	 * data type instance
	 */
	
	String name;
	Set<TypeRepresentation> reprs;

	String getName() {
		return name;
	}
	
	Set<TypeRepresentation> getRepresentations() {
		return new HashSet<TypeRepresentation>(reprs);
	}
	
	void addRepresentation(TypeRepresentation r) {
		reprs.add(r);
	}
	
	@Override
	public String toString() {
		String s = name + " =";
		for (TypeRepresentation r : reprs) {
			s += " " + r;
		}
		return s;
	}
}
