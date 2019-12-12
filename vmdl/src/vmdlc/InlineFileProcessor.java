package vmdlc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nez.ast.Symbol;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.parser.io.StringSource;
import type.AstType;
import type.ExprTypeSet;

public class InlineFileProcessor {
    private static Map<String, InlineExpansionInfo> inlineTable = new HashMap<>();

    public enum Keyword {
        FUNC, COND, EXPR;
    }

    public static String code(Keyword keyword, String text) {
        return "#" + keyword + " " + text;
    }

    public static void read(String path, Grammar expressionGrammar) {
        Parser parser = new Parser(expressionGrammar, "Expression", ParserStrategy.newSafeStrategy());
        try{
            String currentFunctionName = null;
            InlineExpansionInfo currentInfo = null;
            Set<List<AstType>> currentConditions = null;
            List<String> codes = Files.readAllLines(Paths.get(path));
            for(String code : codes){
                if(code.isEmpty()) continue;
                if(code.charAt(0) != '#') throw new RuntimeException("Inline expansion file is broken: "+code);
                String head = code.substring(1, 5).trim();
                String tail = code.substring(6).trim();
                switch(Keyword.valueOf(head)){
                    case FUNC:
                        if(currentFunctionName != null){
                            inlineTable.put(currentFunctionName, currentInfo);
                        }
                        String[] functionCodeBody = tail.split(" ");
                        if(functionCodeBody.length != 2){
                            throw new RuntimeException("Code is broken: "+code);
                        }

                        currentFunctionName = functionCodeBody[0];
                        currentInfo = new InlineExpansionInfo();
                        currentInfo.setArgStrings(Arrays.asList(functionCodeBody[1].split(",")));
                        currentConditions = new HashSet<>();
                    break;
                    case COND:
                        String[] conditionStrings = tail.split(",");
                        List<AstType> newCondition = new ArrayList<>(conditionStrings.length);
                        for(String typeName : conditionStrings){
                            newCondition.add(AstType.get(typeName));
                        }
                        if(currentConditions == null){
                            throw new RuntimeException("Illigal happen of COND");
                        }
                        currentConditions.add(newCondition);
                    break;
                    case EXPR:
                        if(currentInfo == null){
                            throw new RuntimeException("Illigal happen of EXPR");
                        }
                        //NOTE: have to write no conditions case
                        currentInfo.put(currentConditions, (SyntaxTree)parser.parse(new StringSource(tail), new SyntaxTree()));
                        currentConditions = new HashSet<>();
                    break;
                    default:
                        throw new RuntimeException("Illigal code happen: "+code);
                }
            }
            if(currentFunctionName != null){
                inlineTable.put(currentFunctionName, currentInfo);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private static SyntaxTree getReplacedTree(SyntaxTree original, List<String> argStrings, List<SyntaxTree> argNodes){
        SyntaxTree cloneTree = original.dup();
        if(original.is(Symbol.unique("Name"))){
            int index = argStrings.indexOf(original.toText());
            if(index != -1){
                cloneTree = argNodes.get(index).dup();
            }
        }else{
            int size = original.size();
            for(int i=0; i<size; i++){
                SyntaxTree subTree = cloneTree.get(i);
                cloneTree.set(i, getReplacedTree(subTree, argStrings, argNodes));
            }
        }
        return cloneTree;
    }

    private static Set<List<AstType>> typeSetListToListSet(List<ExprTypeSet> typesList){
        int size = typesList.size();
        if(size == 0) return new HashSet<>();
        Set<List<AstType>> newSet = typeSetListToListSet(typesList.subList(1, size));
        Set<AstType> typeSet = typesList.get(0).getTypeSet();
        for(AstType t : typeSet){
            List<AstType> elementList = new ArrayList<>();
            elementList.add(t);
            newSet.add(elementList);
        }
        return newSet;
    }

    public static SyntaxTree inlineExpansion(SyntaxTree original, List<ExprTypeSet> argTypes){
        String functionName = original.get(Symbol.unique("recv")).toText();
        InlineExpansionInfo targetInfo = inlineTable.get(functionName);
        if(targetInfo == null){
            throw new Error("The function is not inline expandable: "+functionName);
        }
        List<String> argStrings = targetInfo.getArgStrings();
        List<SyntaxTree> argNodes = Arrays.asList((SyntaxTree[])original.get(Symbol.unique("args")).getSubTree());
        Set<List<AstType>> needTypes = typeSetListToListSet(argTypes);
        SyntaxTree expandedTree = targetInfo.get(needTypes);
        if(expandedTree == null){
            return original;
        }
        SyntaxTree result = getReplacedTree(expandedTree, argStrings, argNodes);
        //TEST PRINT ***************
        /*
        System.err.println("---------");
        System.err.println("Expansion information");
        System.err.println("---------");
        System.err.println("ORIGINAL:"+original.toString());
        System.err.println("argTypes:"+argTypes.toString());
        System.err.println("needTypes:"+needTypes.toString());
        System.err.println("targetInfo:"+targetInfo.toString());
        System.err.println("expandedTree:"+expandedTree.toString());
        System.err.println("result:"+result.toString());
        System.err.println("---------");
        */
        return result;
    }

    public static boolean isInlineExpandable(String functionName){
        return inlineTable.containsKey(functionName);
    }
}