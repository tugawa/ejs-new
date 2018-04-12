/*
   Branch.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
*/
package vmgen.synth;

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
