package vmdlc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ControlFlowGraphPrinter {
    private static String getNodeName(ControlFlowGraphNode node){
        if(node == ControlFlowGraphNode.enter)
            return node+"(Enter)";
        if(node == ControlFlowGraphNode.exit)
            return node+"(Exit)";
        return node+"";
    }
    public static void print(ControlFlowGraphNode enter){
        Set<ControlFlowGraphNode> printed = new HashSet<>();
        Stack<ControlFlowGraphNode> stack = new Stack<>();

        System.out.println("Enter:"+ControlFlowGraphNode.enter);
        System.out.println("Exit :"+ControlFlowGraphNode.exit);
        System.out.println("------------");

        stack.push(enter);
        while(!stack.isEmpty()){
            ControlFlowGraphNode target = stack.pop();
            if(printed.contains(target)) continue;
            printed.add(target);
            Collection<ControlFlowGraphNode> nexts = target.getNext();
            for(ControlFlowGraphNode o : nexts){
                stack.push(o);
            }
            System.out.println("Name:"+getNodeName(target));
            System.out.println("Next:");
            for(ControlFlowGraphNode o : nexts){
                System.out.println(getNodeName(o));
            }
            Collection<ControlFlowGraphNode> prevs = target.getPrev();
            System.out.println("Prev:");
            for(ControlFlowGraphNode o : prevs){
                System.out.println(getNodeName(o));
            }
            System.out.println("Codes:");
            for(SyntaxTree stmt : target){
                System.out.println(stmt.toText());
            }
            System.out.println("------------");
        }
    }
}
