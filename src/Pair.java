
public class Pair<S, T> {
	S s;
	T t;
	
	Pair(S s, T t) {
		this.s = s;
		this.t = t;
	}
	
	S first() {
		return s;
	}
	
	T second() {
		return t;
	}
}
