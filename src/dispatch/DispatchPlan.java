package dispatch;

import java.util.ArrayList;
import java.util.List;

public class DispatchPlan {
    ArrayList<DecisionDiagram.DispatchCriterion> plan = new ArrayList<DecisionDiagram.DispatchCriterion>();

    // create empty DispatchPlan
    DispatchPlan() {}
    
    // create standard DispatchPlan
    DispatchPlan(int nrands, boolean useTagPair) {
        if (useTagPair)
            addTagPair();
        for (int i = 0; i < nrands; i++)
            addPT(i);
        for (int i = 0; i < nrands; i++)
            addHT(i);
    }
    
    void addTagPair() {
        plan.add(new DecisionDiagram.TagPairDispatch());
    }
    
    void addPT(int n) {
        plan.add(new DecisionDiagram.PTDispatch(n));
    }
    
    void addHT(int n) {
        plan.add(new DecisionDiagram.HTDispatch(n));
    }
    
    List<DecisionDiagram.DispatchCriterion> getPlan() {
        return plan;
    }
}

