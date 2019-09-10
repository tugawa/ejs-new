/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import dispatch.DecisionDiagram.HTNode;
import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.PTNode;
import dispatch.DecisionDiagram.TagPairNode;
import dispatch.DecisionDiagram.TagPairNode.TagPair;
import dispatch.LLRuleSet.LLRule;
import type.VMRepType;
import type.VMRepType.HT;
import type.VMRepType.PT;

public class SearchVisitor extends NodeVisitor<LLRule> {

    VMRepType[] rts;

    SearchVisitor(VMRepType[] rts) {
        this.rts = rts;
    }

    @Override
    LLRule visitLeaf(Leaf node) {
        return node.getRule();
    }

    @Override
    LLRule visitTagPairNode(TagPairNode node) {
        TagPair tag = new TagPair(rts[0].getPT(), rts[1].getPT());
        Node next = node.getChild(tag);
        return next.accept(this);
    }

    @Override
    LLRule visitPTNode(PTNode node) {
        PT tag = rts[node.opIndex].getPT();
        Node next = node.getChild(tag);
        return next.accept(this);
    }

    @Override
    LLRule visitHTNode(HTNode node) {
        if (node.isNoHT())
            return node.getChild().accept(this);
        HT tag = rts[node.opIndex].getHT();
        Node next = node.getChild(tag);
        return next.accept(this);
    }
}
