package vmdlc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import type.AstType;

public class InlineExpansionInfo {
    private List<String> argStrings;
    private Map<Set<List<AstType>>, SyntaxTree> inlineMap = new HashMap<>();

    public void setArgStrings(List<String> argStrings){
        this.argStrings = argStrings;
    }
    public void put(Set<List<AstType>> conditions, SyntaxTree exprNode){
        inlineMap.put(conditions, exprNode);
    }

    public SyntaxTree get(Set<List<AstType>> types){
        for(Entry<Set<List<AstType>>, SyntaxTree> entry : inlineMap.entrySet()){
            if(entry.getKey().containsAll(types)) return entry.getValue();
        }
        return null;
    }

    public List<String> getArgStrings(){
        return argStrings;
    }

    @Override
    public String toString(){
        return inlineMap.toString();
    }
}