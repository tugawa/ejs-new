package vmgen.dd;

import vmgen.GlobalConstantOptions;

public abstract class DDNode implements GlobalConstantOptions {
	static int nextLabel;
	static String prefix;
	
	public static void setPrefix(String prefix) {
		DDNode.prefix = prefix;
		nextLabel = 0;
	}

	static {
		setPrefix("");
	}

	String label;
	public boolean arranged;

	DDNode() {
		arranged = false;
		label = String.format("L%s%d", prefix, nextLabel++);
	}

	public boolean mergable(DDNode that) {
		return false;
	}
	
	abstract public String code();
}