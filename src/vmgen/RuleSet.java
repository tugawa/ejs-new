/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vmgen.type.VMDataType;


public class RuleSet implements GlobalConstantOptions {
    public static class Condition {
        public VMDataType[] dts;

        Condition(String tn1) {
            dts = new VMDataType[]{VMDataType.get(tn1)};
        }
        Condition(String tn1, String tn2) {
            dts = new VMDataType[]{VMDataType.get(tn1), VMDataType.get(tn2)};
        }
        Condition(VMDataType[] dts) {
            this.dts = dts;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(dts);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Condition)
                return Arrays.equals(dts, ((Condition) obj).dts);
            else
                return false;
        }
    };

    public static class Rule {
        public Set<Condition> condition;
        public String action;

        Rule(String action, Condition...  condition) {
            this.action = action;
            this.condition = new HashSet<Condition>();
            for (Condition c: condition)
                this.condition.add(c);
        }

        Rule(String action, List<Condition> condition) {
            this.action = action;
            this.condition = new HashSet<Condition>();
            for (Condition c: condition)
                this.condition.add(c);
        }

        Rule(String action, Set<Condition> condition) {
            this.action = action;
            this.condition = condition;
        }

        Rule filterConditions(Collection<Condition> remove) {
            Set<Condition> filteredCondition = new HashSet<RuleSet.Condition>();
            for (Condition c: condition)
                if (!remove.contains(c))
                    filteredCondition.add(c);
            return new Rule(action, filteredCondition);
        }

        public Set<Condition> getCondition() {
            return condition;
        }
    }

    private String[] dispatchVars;
    Set<Rule> rules;

    public Set<Rule> getRules() {
        return rules;
    }
    public int getArity() {
        return dispatchVars.length;
    }

    public RuleSet() {
        rules = new HashSet<Rule>();
        dispatchVars = new String[]{"v1", "v2"};

        /* generate dummy data that looks like add */
        rules.add(new Rule("fixfix", new Condition("fixnum", "fixnum")));
        rules.add(new Rule("fixflo", new Condition("fixnum", "flonum")));
        rules.add(new Rule("flofix", new Condition("flonum", "fixnum")));
        rules.add(new Rule("floflo", new Condition("flonum", "flonum")));
        rules.add(new Rule("strstr", new Condition("string", "string")));
        rules.add(new Rule("strflo", new Condition("string", "flonum")));
        rules.add(new Rule("flostr", new Condition("flonum", "string")));
        rules.add(new Rule("strspe", new Condition("string", "special")));
        rules.add(new Rule("spestr", new Condition("special", "string")));
        rules.add(new Rule("strfix", new Condition("string", "fixnum")));
        rules.add(new Rule("fixstr", new Condition("fixnum", "string")));
        rules.add(new Rule("to_primitive",
                new Condition("simple_object", "simple_object"),
                new Condition("simple_object", "array"),
                new Condition("array", "simple_object"),
                new Condition("array", "array")));
    }

    RuleSet(String[] dispatchVars, Set<Rule> rules) {
        this.dispatchVars = dispatchVars;
        this.rules = rules;
    }
    public String[] getDispatchVars() {
        return dispatchVars;
    }
    public void setDispatchVars(String[] dispatchVars) {
        this.dispatchVars = dispatchVars;
    }
}
