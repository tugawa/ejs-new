package vmgen;

public abstract class Synthesiser implements GlobalConstantOptions {
	public abstract String synthesise(RuleSet plan);

	String getPTCode(String dispatchVar) {
		return "get_tag("+dispatchVar+")";
	}

	protected String getPTCode(String[] dispatchVars) {
		return getPTCode(dispatchVars[0]);
	}

	String getHTCode(String dispatchVar) {
		return "gc_obj_header_type((void*) clear_tag("+dispatchVar+"))";
	}

	protected String getHTCode(String[] dispatchVars) {
		return getHTCode(dispatchVars[0]);
	}

	String composeTagPairCode(String... ptcode) {
		return "TAG_PAIR("+ptcode[0]+", "+ptcode[1]+")";
	}

	protected String getTagPairCode(String[] dispatchVars) {
		return composeTagPairCode(getPTCode(dispatchVars[0]), getPTCode(dispatchVars[1]));
	}
}
