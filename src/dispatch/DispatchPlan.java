package dispatch;

import java.util.ArrayList;
import java.util.List;

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
    public DispatchPlan() {}
    
    // create standard DispatchPlan
    public DispatchPlan(int nrands, boolean useTagPair) {
        if (useTagPair)
            addTagPair();
        for (int i = 0; i < nrands; i++)
            addPT(i);
        for (int i = 0; i < nrands; i++)
            addHT(i);
    }
    
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

