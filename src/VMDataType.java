import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VMDataType implements GlobalConstantOptions {
	static Map<String, VMDataType> definedVMDataTypes = new HashMap<String, VMDataType>();

	static void defineVMDataType(String name) {
		definedVMDataTypes.put(name, new VMDataType(name));
	}

	static VMDataType get(String name) {
		return get(name, false);
	}
	
	static VMDataType get(String name, boolean permitNull) {
		VMDataType dt = definedVMDataTypes.get(name);
		if (dt == null && !permitNull)
				throw new Error("unknown data type; "+ name);
		return dt;
	}

	private VMDataType(String name) {
		this.name = name;
		reptypes = new ArrayList<VMRepType>();
	}

	static {
		if (DEBUG_WITH_SMALL) {
			defineVMDataType("string");
			defineVMDataType("fixnum");
			defineVMDataType("array");
		} else {
			defineVMDataType("string");
			defineVMDataType("fixnum");
			defineVMDataType("flonum");
			defineVMDataType("special"); 
			defineVMDataType("simple_object");
			defineVMDataType("array");
			defineVMDataType("function");
			defineVMDataType("builtin");
			defineVMDataType("iterator");
			defineVMDataType("regexp");
			defineVMDataType("string_object");
			defineVMDataType("number_object");
			defineVMDataType("boolean_object");
		}
	}
	
	static List<VMDataType> all() {
		return new ArrayList<VMDataType>(definedVMDataTypes.values());
	}

	/*
	 * utilities
	 */

	static Stream<VMRepType> typeRepresentationStreamOf(Collection<VMDataType> dts) {
		return typeRepresentationStreamOf(dts.stream());
	}

	static Stream<VMRepType> typeRepresentationStreamOf(Stream<VMDataType> dts) {
		return dts.flatMap(dt -> dt.getRepresentations().stream());
	}

	static Set<PT> uniquePT(Set<VMRepType> trs, Collection<VMDataType> among) {
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
	List<VMRepType> reptypes;

	String getName() {
		return name;
	}

	Set<VMRepType> getRepresentations() {
		return new HashSet<VMRepType>(reptypes);
	}

	void addVMRepType(VMRepType r) {
		reptypes.add(r);
	}

	void setDataStructure(String struct) {
		this.struct = struct;
	}

	@Override
	public String toString() {
		String s = name + " =";
		for (VMRepType r : reptypes) {
			s += " " + r;
		}
		return s;
	}
}
