import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import type.*;
import dispatch.*;

class TestRuleSet {

    public static void main(String[] argv) throws IOException {
        TypeDefinition.load("default.def");
        
        VMDataType[] dataTypes1 = {
            VMDataType.get("fixnum"),
            VMDataType.get("flonum")
        };

        RuleSet.OperandDataTypes odt1 = new RuleSet.OperandDataTypes(dataTypes1);

        VMDataType[] dataTypes2 = {
            VMDataType.get("flonum"),
            VMDataType.get("fixnum")
        };

        RuleSet.OperandDataTypes odt2 = new RuleSet.OperandDataTypes(dataTypes2);

        RuleSet.OperandDataTypes[] odtsA = { odt1, odt2 };
        String actionA = "A";

        RuleSet.Rule ruleA = new RuleSet.Rule(actionA, odtsA);
        Set<RuleSet.Rule> rules1 = new HashSet<RuleSet.Rule>();
        rules1.add(ruleA);

        String[] dvars1 = { "v1", "v2" };

        RuleSet ruleSet = new RuleSet(dvars1, rules1);

        DispatchPlan dp = new DispatchPlan(2, false);

        DispatchProcessor dispatchProcessor = new DispatchProcessor();

        String s = dispatchProcessor.translate(ruleSet, dp);
        System.out.println(s);

    }
}
