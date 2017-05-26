
abstract class Synthesiser implements GlobalConstantOptions {
	abstract String synthesise(Plan plan);

	String getPTCode(String dispatchVar) {
		return "get_tag("+dispatchVar+")";
	}

	String getPTCode(String[] dispatchVars) {
		return getPTCode(dispatchVars[0]);
	}

	String getHTCode(String dispatchVar) {
		return "obj_header_tag("+dispatchVar+")";
	}

	String getHTCode(String[] dispatchVars) {
		return getHTCode(dispatchVars[0]);
	}

	String composeTagPairCode(String... ptcode) {
		return "TAG_PAIR("+ptcode[0]+", "+ptcode[1]+")";
	}

	String getTagPairCode(String[] dispatchVars) {
		return composeTagPairCode(getPTCode(dispatchVars[0]), getPTCode(dispatchVars[1]));
	}
}
