package vmgen.type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vmgen.GlobalConstantOptions;

public class VMDataType implements GlobalConstantOptions, Comparable<VMDataType> {
	static Map<String, VMDataType> definedVMDataTypes = new HashMap<String, VMDataType>();

	static void defineVMDataType(String name) {
		definedVMDataTypes.put(name, new VMDataType(name));
	}

	public static VMDataType get(String name) {
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
		this.defineOrder = definedVMDataTypes.size();
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
	
	public static List<VMDataType> all() {
		List<VMDataType> lst = new ArrayList<VMDataType>(definedVMDataTypes.values());
		Collections.sort(lst);
		return lst;
	}

	/*
	 * data type instance
	 */

	String name;
	String struct;
	ArrayList<VMRepType> reptypes;
	private int defineOrder;

	public String getName() {
		return name;
	}

	public ArrayList<VMRepType> getVMRepTypes() {
		return reptypes;
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

	@Override
	public int compareTo(VMDataType that) {
		return this.defineOrder - that.defineOrder;
	}
}
