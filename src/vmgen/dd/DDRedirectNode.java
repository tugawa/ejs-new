package vmgen.dd;

public class DDRedirectNode extends DDNode {
	public DDNode destination;

	public DDRedirectNode(DDNode destination) {
		this.destination = destination;
	}

	@Override
	public String toString() {
		return ">" + destination;
	}

	@Override
	public String code() {
		return "goto "+destination.label+";\n";
	}

	public boolean mergable(DDNode that_) {
		if (!(that_ instanceof DDRedirectNode)) return false;
		DDRedirectNode that = (DDRedirectNode) that_;
		return destination.mergable(that.destination);
	}
}