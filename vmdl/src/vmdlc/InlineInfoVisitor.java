package vmdlc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.TypeMap;
import type.TypeMapSetFull;
import type.AstType.AstPairType;
import type.AstType.AstProductType;
import vmdlc.InlineInfoVisitor.DefaultVisitor;

public class InlineInfoVisitor extends TreeVisitorMap<DefaultVisitor> {
    private static final InlineFileProcessor.Keyword FUNC = InlineFileProcessor.Keyword.FUNC;
    private static final InlineFileProcessor.Keyword COND = InlineFileProcessor.Keyword.COND;
    private static final InlineFileProcessor.Keyword EXPR = InlineFileProcessor.Keyword.EXPR;
    private StringBuilder builder;

    public InlineInfoVisitor() {
        init(InlineInfoVisitor.class, new DefaultVisitor());
    }

    public String start(Tree<?> node) {
        try {
            TypeMapSetFull dict = new TypeMapSetFull();
            builder = new StringBuilder();
            for (Tree<?> chunk : node) {
                visit(chunk, dict);
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    private final boolean visit(Tree<?> node, TypeMapSetFull dict) throws Exception {
        return find(node.getTag().toString()).accept(node, dict);
    }

    private final void setArgsName(Tree<?> node, List<String> names){
        find(node.getTag().toString()).argsName(node, names);
    }

    private final void print(String str){
        builder.append(str);
    }

    private final void newLine(){
        builder.append('\n');
    }

    private final void println(String str){
        print(str);
        newLine();
    }

    public class DefaultVisitor {
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            return false;
        }
        public void argsName(Tree<?> node, List<String> names){
            for (Tree<?> seq : node) {
                setArgsName(seq, names);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            AstType rawType = AstType.nodeToType((SyntaxTree)typeNode);
            if(!(rawType instanceof AstProductType)){
                ErrorPrinter.error("Function is not typed functional type", (SyntaxTree)typeNode);
            }
            AstProductType funType = (AstProductType)rawType;
            AstType domain = funType.getDomain();
            List<AstType> argTypes;
            if(domain instanceof AstPairType){
                argTypes = ((AstPairType)domain).getTypes();
            }else{
                argTypes = new ArrayList<>();
                argTypes.add(domain);
            }
            Tree<?> defNode = node.get(Symbol.unique("definition"));
            Tree<?> nameNode = defNode.get(Symbol.unique("name"));
            Tree<?> paramsNode = null;
            if(defNode.has(Symbol.unique("params"))){
                paramsNode = defNode.get(Symbol.unique("params"));
            }
            int paramSize = paramsNode.size();
            if(paramSize != argTypes.size()){
                ErrorPrinter.error("Argument size is not match", (SyntaxTree)paramsNode);
            }
            List<String> argNames = new ArrayList<>();
            for(Tree<?> paramNode : paramsNode){
                argNames.add(paramNode.toText());
            }
            argsName(defNode, argNames);
            Map<String, AstType> argsMap = new HashMap<>(paramSize);
            if(paramsNode != null && paramSize > 0){
                for(int i=0; i<paramSize; i++){
                    argsMap.put(argNames.get(i), argTypes.get(i));
                }
            }
            Set<TypeMap> newSet = dict.getTypeMapSet();
            if(newSet.isEmpty()){
                newSet.add(new TypeMap());
            }
            for(Entry<String, AstType> entry : argsMap.entrySet()){
                Set<TypeMap> addedSet = new HashSet<>();
                for(TypeMap oldMap : newSet){
                    addedSet.addAll(dict.getAddedSet(oldMap, entry.getKey(), entry.getValue()));
                }
                newSet = addedSet;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(nameNode.toText());
            builder.append(' ');
            for(int i=0; i<paramSize; i++){
                builder.append(argNames.get(i));
                if(i==paramSize-1) break;
                builder.append(',');
            }
            println(InlineFileProcessor.code(FUNC, builder.toString()));
            visit(defNode.get(Symbol.unique("body")), new TypeMapSetFull(newSet));
            return false;
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            for (Tree<?> seq : node) {
                boolean continueFlag = visit(seq, dict);
                if(!continueFlag) break;
            }
            return false;
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            MatchProcessor mp = new MatchProcessor((SyntaxTree)node);
            for (int i = 0; i < mp.size(); i++) {
                TypeMapSetFull dictCaseIn = (TypeMapSetFull)dict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                if (dictCaseIn.noInformationAbout(mp.getFormalParams())){
                    continue;
                }
                Tree<?> body = mp.getBodyAst(i);
                visit(body, dictCaseIn);
            }
            return false;
        }
    }

    public class Return extends DefaultVisitor {
        List<String> argNameList;
        private String getReturnExpressionString(Tree<?> node){
            String returnNodeText = node.toText();
            return returnNodeText.substring("return".length()).trim();
        }
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            for(TypeMap map : dict){
                StringBuilder typeCondition = new StringBuilder();
                int size = argNameList.size();
                boolean outputFlag = false;
                for(int i=0; i<size; i++){
                    String name = argNameList.get(i);
                    AstType type = map.get(name);
                    if(name == null) continue;
                    if(outputFlag)typeCondition.append(",");
                    typeCondition.append(type.toString());
                    outputFlag = true;
                }
                println(InlineFileProcessor.code(COND, typeCondition.toString()));
            }
            println(InlineFileProcessor.code(EXPR, getReturnExpressionString(node)));
            return false;
        }
        @Override
        public void argsName(Tree<?> node, List<String> names){
            argNameList = names;
        }
    }

    public class CTypeDef extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            return true;
        }
    }

    public class CObjectmapping extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            return true;
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            return true;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public boolean accept(Tree<?> node, TypeMapSetFull dict) throws Exception {
            return true;
        }
    }
}