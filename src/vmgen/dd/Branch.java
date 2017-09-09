package vmgen.dd;

public abstract class Branch {
	public DDNode action;

	Branch(DDNode action) {
		this.action = action;
	}
	abstract public int size();
	public String code() {
		return code(false);
	}
	abstract public String code(boolean isDefaultCase);
}
