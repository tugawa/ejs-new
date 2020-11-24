package vmdlc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ControlFlowGraphPrinter {
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
            System.out.println("Name:"+target);
            System.out.println("Next:");
            for(ControlFlowGraphNode o : nexts){
                System.out.println(o);
            }
            Collection<ControlFlowGraphNode> prevs = target.getPrev();
            System.out.println("Prev:");
            for(ControlFlowGraphNode o : prevs){
                System.out.println(o);
            }
            System.out.println("Codes:");
            for(SyntaxTree o : target){
                System.out.println(o.toString());
            }
            System.out.println("------------");
        }
    }
}
