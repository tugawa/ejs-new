package vmdlc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import type.VMDataType;

public class InstructionCallCountInfo {
    private String insnName;
    private int totalCalled;
    private Map<VMDataType[], Integer> countMap = new HashMap<>();

    public InstructionCallCountInfo(String name, int callCount){
        insnName = name;
        totalCalled = callCount;
    }

    public void put(VMDataType[] vec, Integer number){
        if(countMap.get(vec) != null){
            ErrorPrinter.error("Illigal state happen in reading ICC file: Double operands specification");
        }
        countMap.put(vec, number);
    }

    public int getTotalCalled(){
        return totalCalled;
    }

    public int condSize(){
        return countMap.size();
    }

    public Set<Entry<VMDataType[], Integer>> entrySet(){
        return countMap.entrySet();
    }
}