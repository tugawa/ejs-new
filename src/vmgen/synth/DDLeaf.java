/*
   DDLeaf.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
*/
package vmgen.synth;

import vmgen.RuleSet;
import vmgen.RuleSet.Rule;

public class DDLeaf extends DDNode {
	RuleSet.Rule rule;

	public DDLeaf(RuleSet.Rule rule) {
		this.rule = rule;
	}

	@Override
	public String toString() {
		return "{" + rule.action.split("\n")[0] + "}";
	}

	@Override
	public String code() {
		return label + ": {"+ rule.action + "\n}\n";
	}

	public boolean mergable(DDNode that) {
		return this == that;
	}
}
