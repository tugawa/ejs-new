/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import dispatch.DecisionDiagram.HTNode;
import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.PTNode;
import dispatch.DecisionDiagram.TagPairNode;
import dispatch.DecisionDiagram.TagPairNode.TagPair;
import type.VMRepType;
import type.VMRepType.HT;
import type.VMRepType.PT;
import vmdlc.Option;

class CodeGenerateVisitor extends NodeVisitor<Void> {
    static boolean USE_GOTO = true;
    static boolean PAD_CASES = true;
    static boolean USE_DEFAULT = false;  // add default by the same strategy as -old (exclusive to PAD_CASES)
    static boolean DEBUG_COMMENT = true;

    static class Macro {
        int nextLabel = 0;

        String getPTCode(String var) {
            return "GET_PTAG("+var+")";
        }
        String getHTCode(String var) {
            return "GET_HTAG("+var+")";
        }
        String composeTagPairCode(String... vars) {
            return "TAG_PAIR("+vars[0]+", "+vars[1]+")";
        }
        String composeTagPairLiteral(String... vars) {
            return "TAG_PAIR("+vars[0]+", "+vars[1]+")";
        }
        String getLabel() {
            return String.format("L%d", nextLabel++);
        }
    }

    Option option;
    StringBuffer sb = new StringBuffer();
    Macro tagMacro;
    String[] varNames;
    Map<Node, String> gotoLabels = new TreeMap<Node, String>();
    Map<Node, Set<String>> typeLabels;
    String labelPrefix;

    public CodeGenerateVisitor(String[] varNames, Macro tagMacro, Option option, Map<Node, Set<String>> typeLabels, String labelPrefix) {
        this.varNames = varNames;
        this.tagMacro = tagMacro;
        this.typeLabels = typeLabels;
        this.option = option;
        USE_GOTO = option.getOption(Option.AvailableOptions.GEN_USE_GOTO, USE_GOTO);
        PAD_CASES = option.getOption(Option.AvailableOptions.GEN_PAD_CASES, PAD_CASES);
        USE_DEFAULT = option.getOption(Option.AvailableOptions.GEN_USE_DEFAULT, USE_DEFAULT);
        DEBUG_COMMENT = option.getOption(Option.AvailableOptions.GEN_DEBUG_COMMENT, DEBUG_COMMENT);
        this.labelPrefix = labelPrefix;
    }

    @Override
    public String toString() {
        if (option.getOption(Option.AvailableOptions.GEN_MAGIC_COMMENT, false)) {
            return sb.toString() +
                    "/* Local Variables: */\n" +
                    "/* mode: c */\n" +
                    "/* c-basic-offset: 4 */\n" +
                    "/* End: */\n";
        } else
            return sb.toString();
    }

    boolean processSharedNode(Node node) {
        if (USE_GOTO) {
            String label = gotoLabels.get(node);
            if (label != null) {
                sb.append("goto ").append(label).append(";\n");
                return true;
            }
            label = tagMacro.getLabel();
            gotoLabels.put(node, label);
            sb.append(label).append(":");
        }
        return false;
    }

    private void addTypeLabels(Node node) {
        if (typeLabels != null) {
            Set<String> labels = typeLabels.get(node);
            if (labels == null)
                return;
            sb.append("\n");
            for (String label: labels)
                sb.append("TL").append(labelPrefix).append(label).append(":\n");
        }
    }

    @Override
    Void visitLeaf(Leaf node) {
        if (processSharedNode(node))
            return null;
        addTypeLabels(node);
        sb.append("{");
        if (DEBUG_COMMENT) {
            sb.append(" //");
            for (VMRepType rt: node.getRule().getVMRepTypes())
                sb.append(" ").append(rt.getName());
            sb.append(" ").append(node);
        }
        sb.append('\n').append(node.getRule().getHLRule().action).append("}\n");
        return null;
    }
    @Override
    Void visitTagPairNode(TagPairNode node) {
        if (processSharedNode(node))
            return null;
        addTypeLabels(node);
        TreeMap<Node, TreeSet<TagPairNode.TagPair>> childToTags = node.getChildToTagsMap();
        sb.append("switch(").append(tagMacro.composeTagPairCode(varNames[0], varNames[1])).append("){");
        if (DEBUG_COMMENT)
            sb.append(" // "+node+"("+childToTags.size()+")");
        sb.append('\n');

        TreeSet<Integer> tagValues = new TreeSet<Integer>();
        int max = 0;
        for (TagPair tag: node.getEdges()) {
            int v = tag.getValue();
            tagValues.add(v);
            if (v > max)
                max = v;
        }
        Node defaultChild = null;
        int defaultChildCases = 0;
        for (Node child: childToTags.keySet()) {
            TreeSet<?> tags = childToTags.get(child);
            if (tags.size() > defaultChildCases) {
                defaultChild = child;
                defaultChildCases = tags.size();
            }
        }

        for (Node child: childToTags.keySet()) {
            for (TagPairNode.TagPair tag: childToTags.get(child)) {
                sb.append("case ").append(tagMacro.composeTagPairLiteral(tag.op1.getName(), tag.op2.getName())).append(":\n");
                if (PAD_CASES) {
                    for (int v = tag.getValue() - 1; v >= 0; v--) {
                        if (tagValues.contains(v))
                            break;
                        sb.append("case "+v+":\n");
                    }
                    if (tag.getValue() == max)
                        sb.append("default:\n");
                }
            }
            if (USE_DEFAULT) {
                if (child == defaultChild)
                    sb.append("default:\n");
            }
            child.accept(this);
            sb.append("break;\n");
        }
        sb.append("}");
        if (DEBUG_COMMENT)
            sb.append(" // "+node);
        sb.append('\n');
        return null;
    }
    @Override
    Void visitPTNode(PTNode node) {
        if (processSharedNode(node))
            return null;
        addTypeLabels(node);
        TreeMap<Node, TreeSet<PT>> childToTags = node.getChildToTagsMap();
        sb.append("switch(").append(tagMacro.getPTCode(varNames[node.getOpIndex()])).append("){");
        if (DEBUG_COMMENT)
            sb.append(" // "+node+"("+childToTags.size()+")");
        sb.append('\n');

        TreeSet<Integer> tagValues = new TreeSet<Integer>();
        int max = 0;
        for (PT tag: node.getEdges()) {
            int v = tag.getValue();
            tagValues.add(v);
            if (v > max)
                max = v;
        }
        Node defaultChild = null;
        int defaultChildCases = 0;
        for (Node child: childToTags.keySet()) {
            TreeSet<?> tags = childToTags.get(child);
            if (tags.size() > defaultChildCases) {
                defaultChild = child;
                defaultChildCases = tags.size();
            }
        }

        for (Node child: childToTags.keySet()) {
            for (PT tag: childToTags.get(child)) {
                sb.append("case "+tag.getName()+":\n");
                if (PAD_CASES) {
                    for (int v = tag.getValue() - 1; v >= 0; v--) {
                        if (tagValues.contains(v))
                            break;
                        sb.append("case "+v+":\n");
                    }
                    if (tag.getValue() == max)
                        sb.append("default:\n");
                }
            }
            if (USE_DEFAULT) {
                if (child == defaultChild)
                    sb.append("default:\n");
            }
            child.accept(this);
            sb.append("break;\n");
        }
        sb.append("}");
        if (DEBUG_COMMENT)
            sb.append(" // "+node);
        sb.append('\n');
        return null;
    }
    @Override
    Void visitHTNode(HTNode node) {
        if (processSharedNode(node))
            return null;
        addTypeLabels(node);
        if (node.isNoHT()) {
            node.getChild().accept(this);
            return null;
        }
        TreeMap<Node, TreeSet<HT>> childToTags = node.getChildToTagsMap();
        sb.append("switch(").append(tagMacro.getHTCode(varNames[node.getOpIndex()])).append("){");
        if (DEBUG_COMMENT)
            sb.append(" // "+node+"("+childToTags.size()+")");
        sb.append('\n');

        TreeSet<Integer> tagValues = new TreeSet<Integer>();
        int max = 0;
        for (HT tag: node.getEdges()) {
            int v = tag.getValue();
            tagValues.add(v);
            if (v > max)
                max = v;
        }
        Node defaultChild = null;
        int defaultChildCases = 0;
        for (Node child: childToTags.keySet()) {
            TreeSet<?> tags = childToTags.get(child);
            if (tags.size() > defaultChildCases) {
                defaultChild = child;
                defaultChildCases = tags.size();
            }
        }

        for (Node child: childToTags.keySet()) {
            for (HT tag: childToTags.get(child))  {
                sb.append("case "+tag.getName()+":\n");
                if (PAD_CASES) {
                    for (int v = tag.getValue() - 1; v >= 0; v--) {
                        if (tagValues.contains(v))
                            break;
                        sb.append("case "+v+":\n");
                    }
                    if (tag.getValue() == max)
                        sb.append("default:\n");
                }
            }
            if (USE_DEFAULT) {
                if (child == defaultChild)
                    sb.append("default:\n");
            }
            child.accept(this);
            sb.append("break;\n");
        }
        sb.append("}");
        if (DEBUG_COMMENT)
            sb.append("// "+node);
        sb.append('\n');
        return null;
    }
}