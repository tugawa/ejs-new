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

class DataType implements GlobalConstantOptions {
	static Map<String, DataType> dataTypes = new HashMap<String, DataType>();

	static void defineDataType(String name) {
		dataTypes.put(name, new DataType(name, null));
	}

	static DataType add(String name, DataType parent) {
		if (dataTypes.get(name) != null)
			throw new Error("double definition: "+ name);
		DataType dt = new DataType(name, parent);
		dataTypes.put(name, dt);
		return dt;
	}

	static DataType get(String name) {
		DataType dt = dataTypes.get(name);
		if (dt == null)
				throw new Error("unknown data type; "+ name);
		return dt;
	}

	private DataType(String name, DataType parent) {
		this.name = name;
		this.parent = parent;
		this.children = new HashSet<DataType>();
		if (parent != null)
			parent.children.add(this);
		reprs = new HashSet<TypeRepresentation>();
	}

	static {
		if (DEBUG_WITH_SMALL) {
			Stream.of(
					"string",
					"fixnum",
					"simple_object",
					"array",
					"function")
				.forEach(name -> defineDataType(name));
		} else {
			Stream.of(
				"string",
				"fixnum",
				"flonum",
				"special", 
				"simple_object",
				"array",
				"function",
				"builtin",
				"iterator",
				"regexp",
				"string_object",
				"number_object",
				"boolean_object")
			.forEach(name -> defineDataType(name));
		}
	}
	
	static Collection<DataType> allInSpec() {
		return dataTypes.values().stream()
				.filter(dt -> dt.parent == null)
				.collect(Collectors.toSet());
	}

	static Collection<DataType> allUsed(boolean includeUserDef) {
		if (includeUserDef) {
			return dataTypes.values().stream()
					.filter(dt -> !dt.reprs.isEmpty())
					.collect(Collectors.toSet());
		} else {
			return dataTypes.values().stream()
					.filter(dt -> !dt.reprs.isEmpty())
					.filter(dt -> dt.parent == null)
					.collect(Collectors.toSet());
		}
	}

	static Collection<DataType> allLeaves() {
		return dataTypes.values().stream()
				.filter(dt -> !dt.reprs.isEmpty())
				.filter(dt -> dt.isLeaf())
				.collect(Collectors.toSet());
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

	static Set<PT> uniquePT(Set<TypeRepresentation> trs, Collection<DataType> among) {
		return trs.stream()
				.map(tr -> tr.getPT())
				.distinct()
				.filter(pt -> {
					return typeRepresentationStreamOf(among)
							.filter(tr -> tr.getPT() == pt)
							.allMatch(tr -> trs.contains(tr));
				})
				.collect(Collectors.toSet());
	}
	
	/*
	 * data type instance
	 */

	String name;
	String struct;
	DataType parent;
	Set<DataType> children;
	Set<TypeRepresentation> reprs;

	String getName() {
		return name;
	}

	Set<TypeRepresentation> getRepresentations() {
		return new HashSet<TypeRepresentation>(reprs);
	}

	void addRepresentation(TypeRepresentation r) {
		reprs.add(r);
		if (parent != null)
			parent.addRepresentation(r);
	}

	void setDataStructure(String struct) {
		this.struct = struct;
	}

	TypeRepresentation getRepresentation() {
		if (!isLeaf())
			throw new Error("not a leaf type");
		return reprs.iterator().next();
	}

	boolean isVMType() {
		return parent == null;
	}

	boolean isLeaf() {
		return children.size() == 0;
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
