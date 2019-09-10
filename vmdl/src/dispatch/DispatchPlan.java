/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import java.util.ArrayList;
import java.util.List;
import vmdlc.Option;

public class DispatchPlan {
    static abstract class DispatchCriterion {
        abstract public boolean available(int arity);
    }

    static class TagPairDispatch extends DispatchCriterion {
        @Override
        public boolean available(int arity) {
            return arity == 2;
        }
    }

    ArrayList<DispatchPlan.DispatchCriterion> plan = new ArrayList<DispatchPlan.DispatchCriterion>();

    // create empty DispatchPlan
    public DispatchPlan(Option option) {
        String layers = option.getOption(Option.AvailableOptions.CMP_TREE_LAYER, "p0:p1:h0:h1");
        for (String layer: layers.split(":")) {
            if (layer.equals("tp"))
                addTagPair();
            else {
                int opIndex = Integer.parseInt(layer.substring(1));
                if (layer.charAt(0) == 'p')
                    addPT(opIndex);
                else if (layer.charAt(0) == 'h')
                    addHT(opIndex);
                else
                    throw new Error();
            }
        }
    }

    // create standard DispatchPlan
    /*
    public DispatchPlan(int nrands, boolean useTagPair) {
        if (useTagPair)
            addTagPair();
        for (int i = 0; i < nrands; i++)
            addPT(i);
        for (int i = 0; i < nrands; i++)
            addHT(i);
    }
     */

    void addTagPair() {
        plan.add(new DispatchPlan.TagPairDispatch());
    }

    void addPT(int n) {
        plan.add(new DecisionDiagram.PTDispatch(n));
    }

    void addHT(int n) {
        plan.add(new DecisionDiagram.HTDispatch(n));
    }

    List<DispatchPlan.DispatchCriterion> getPlan() {
        return plan;
    }
}

