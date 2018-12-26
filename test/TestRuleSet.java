import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import type.*;
import dispatch.*;

class TestRuleSet {

    TestRuleSet() { };

    RuleSet.OperandDataTypes makeOdt(String s1, String s2) {
        VMDataType[] dt = {
            VMDataType.get(s1), VMDataType.get(s2)
        };
        RuleSet.OperandDataTypes odt = new RuleSet.OperandDataTypes(dt);
        return odt;
    }

    public static void main(String[] argv) throws IOException {
        TypeDefinition.load("default.def");
        TestRuleSet trs = new TestRuleSet();

        RuleSet.OperandDataTypes[] odtsA = {
            trs.makeOdt("fixnum", "flonum"),
            trs.makeOdt("flonum", "fixnum")
        };
        String actionA = "A";
        RuleSet.Rule ruleA = new RuleSet.Rule(actionA, odtsA);

        RuleSet.OperandDataTypes[] odtsB = {
            trs.makeOdt("fixnum", "string"),
            trs.makeOdt("string", "fixnum")
        };
        String actionB = "B";
        RuleSet.Rule ruleB = new RuleSet.Rule(actionB, odtsB);

        Set<RuleSet.Rule> rules = new HashSet<RuleSet.Rule>();
        rules.add(ruleA);
        rules.add(ruleB);

        String[] dvars = { "v1", "v2" };

        RuleSet ruleSet = new RuleSet(dvars, rules);

        DispatchPlan dp = new DispatchPlan(2, false);

        DispatchProcessor dispatchProcessor = new DispatchProcessor();

        String s = dispatchProcessor.translate(ruleSet, dp);
        System.out.println(s);

    }
}
