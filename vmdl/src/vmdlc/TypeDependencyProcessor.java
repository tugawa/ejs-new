package vmdlc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import type.AstType;
import type.VMDataType;

public class TypeDependencyProcessor {
    private static Map<String, FunctionTypeDependency> dependencyMap = new HashMap<>();

    private static Set<List<VMDataType>> astsToVmds(List<AstType> src){
        int size = src.size();
        if(size == 0){
            Set<List<VMDataType>> newSet = new HashSet<>(1);
            newSet.add(Collections.emptyList());
            return newSet;
        }
        Set<List<VMDataType>> tailSet = astsToVmds(src.subList(1, size));
        Set<VMDataType> heads;
        if(src.get(0) != null)
            heads = src.get(0).getVMDataTypes();
        else
            heads = null;
        if(heads == null){
            heads = new HashSet<>(1);
            heads.add(null);
        }
        Set<List<VMDataType>> newSet = new HashSet<>(tailSet.size()*heads.size());
        if(tailSet.isEmpty()){
            for(VMDataType head : heads){
                List<VMDataType> newList = new ArrayList<>(1);
                newList.add(head);
                newSet.add(newList);
            }
        }
        for(VMDataType head : heads){
            for(List<VMDataType> tail : tailSet){
                List<VMDataType> newList = new ArrayList<>(tail.size()+1);
                newList.add(head);
                newList.addAll(tail);
                newSet.add(newList);
            }
        }
        return newSet;
    }
    public static void addDependency(String fromFunctionName, List<AstType> fromTypes, String toFunctionName, List<AstType> toTypes){
        FunctionTypeDependency fromFunction = dependencyMap.get(fromFunctionName);
        if(fromFunction == null){
            fromFunction = new FunctionTypeDependency(fromFunctionName);
            dependencyMap.put(fromFunctionName, fromFunction);
        }
        Set<List<VMDataType>> fromTypess = astsToVmds(fromTypes);
        Set<List<VMDataType>> toTypess = astsToVmds(toTypes);
        for(List<VMDataType> fromVMDataTypes : fromTypess){
            for(List<VMDataType> toVMDataTypes : toTypess){
                fromFunction.addDependency(fromVMDataTypes, toFunctionName, toVMDataTypes);
            }
        }
    }

    public static void write(FileWriter writer) throws IOException{
        StringBuilder builder = new StringBuilder();
        for(FunctionTypeDependency ftd : dependencyMap.values()){
            builder.append(ftd.getDependencyCode());
        }
        writer.write(builder.toString());
    }

    public static void write(String fileName) throws IOException{
        try{
            FileWriter writer = new FileWriter(new File(fileName), false);
            write(writer);
            writer.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static List<AstType> stringToAstTypes(String target){
        String[] strings = target.split(",");
        List<AstType> types = new ArrayList<>(strings.length);
        for(String typeName : strings){
            if(!typeName.equals("null"))
                types.add(AstType.get(VMDataType.get(typeName.trim())));
            else
                types.add(null);
        }
        return types;
    }

    public static void load(Scanner sc){
        if(!dependencyMap.isEmpty()){
            System.err.println("[Warning] TypeDependencyProcessor overwrites existing data.");
            dependencyMap = new HashMap<>();
        }
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            if(line == "") continue;
            String[] record = line.split("->");
            if(record.length != 2){
                throw new Error("Type dependency file is broken: "+line);
            }
            String[] from = record[0].trim().split(" ");
            String[] to = record[1].trim().split(" ");
            if(from.length != 2 || to.length != 2){
                throw new Error("Type dependency file is broken: "+line);
            }
            addDependency(from[0].trim(), stringToAstTypes(from[1]), to[0].trim(), stringToAstTypes(to[1]));
        }
    }
    
    public static void load(String file) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileInputStream(file));
        try {
            load(sc);
        } finally {
            sc.close();
        }
    }

    private static final OperandSpecifications.OperandSpecificationRecord.Behaviour ACCEPT =
    OperandSpecifications.OperandSpecificationRecord.Behaviour.ACCEPT;

    public static OperandSpecifications getExpandSpecifications(OperandSpecifications original){
        OperandSpecifications result = original.clone();
        Map<String, Set<VMDataType[]>> needTypessMap = original.getAllAccpetSpecifications();
        for(Entry<String, Set<VMDataType[]>> entry : needTypessMap.entrySet()){
            String functionName = entry.getKey();
            FunctionTypeDependency ftd = dependencyMap.get(functionName);
            if(ftd == null) continue;
            Set<VMDataType[]> typess = entry.getValue();
            for(VMDataType[] types : typess){
                ftd.addNeedTypes(Arrays.asList(types), dependencyMap);
            }
        }
        for(FunctionTypeDependency ftd : dependencyMap.values()){
            String functionName = ftd.functionName;
            Set<List<VMDataType>> needTypess = ftd.getNeedTypess();
            for(List<VMDataType> needTypes : needTypess){
                String[] typeNames = new String[needTypes.size()];
                int length = needTypes.size();
                int excludeNullSize = 0;
                for(int i=0; i<length ; i++){
                    VMDataType t = needTypes.get(i);
                    if(t == null){
                        typeNames[i] = "null";
                        continue;
                    }
                    typeNames[i] = t.toString();
                    excludeNullSize++;
                }
                VMDataType[] excludeNullTypes = new VMDataType[excludeNullSize];
                int j = 0;
                for(int i=0; i<length; i++){
                    if(typeNames[i].equals("null")) continue;
                    excludeNullTypes[j++] = VMDataType.get(typeNames[i]);
                }
                if(!result.hasName(functionName)){
                    String[] excludeNullTypeNames = new String[excludeNullSize];
                    for(int i=0; i<excludeNullSize; i++){
                        excludeNullTypeNames[i] = excludeNullTypes[i].toString();
                    }
                    result.insertRecord(functionName, excludeNullTypeNames, ACCEPT);
                }
                if(result.findSpecificationRecord(functionName, excludeNullTypes).behaviour != ACCEPT){
                    result.insertRecord(functionName, typeNames, ACCEPT);
                }
            }
        }
        return result;
    }

    private static class FunctionTypeDependency{
        String functionName;
        Set<List<VMDataType>> needTypess;
        Map<List<VMDataType>, Set<Entry<String, List<VMDataType>>>> needFunctionTypeMap;

        private FunctionTypeDependency(String _functionName){
            functionName = _functionName;
            needTypess = new HashSet<>();
            needFunctionTypeMap = new HashMap<>();
        }

        public void addNeedTypes(List<VMDataType> types, Map<String, FunctionTypeDependency> dependencyMap){
            if(needTypess.contains(types)) return;
            needTypess.add(types);
            //call addNeedTypes that this function need
            Set<Entry<String, List<VMDataType>>> needFunctionTypes = needFunctionTypeMap.get(types);
            if(needFunctionTypes == null) return;
            for(Entry<String, List<VMDataType>> entry : needFunctionTypes){
                FunctionTypeDependency ftd = dependencyMap.get(entry.getKey());
                if(ftd == null){
                    ftd =  new FunctionTypeDependency(entry.getKey());
                    dependencyMap.put(entry.getKey(), ftd);
                }
                ftd.addNeedTypes(entry.getValue(), dependencyMap);
            }
        }

        public void addDependency(List<VMDataType> trigger, String needFunctionName, List<VMDataType> needTypes){
            Set<Entry<String, List<VMDataType>>> needSet = needFunctionTypeMap.get(trigger);
            if(needSet == null){
                needSet = new HashSet<>();
                needFunctionTypeMap.put(trigger, needSet);
            }
            needSet.add(new SimpleEntry<String, List<VMDataType>>(needFunctionName, needTypes));
        }

        private void commaSepalateHelper(List<VMDataType> target, StringBuilder builder){
            int size = target.size();
            for(int i=0; i<size; i++){
                VMDataType t = target.get(i);
                String typeName;
                if(t != null) typeName = t.toString();
                else typeName = "null";
                builder.append(typeName);
                if(i<size-1){
                    builder.append(',');
                }
            }
        }

        public String getDependencyCode(){
            StringBuilder builder = new StringBuilder();
            for(Entry<List<VMDataType>, Set<Entry<String, List<VMDataType>>>> entry : needFunctionTypeMap.entrySet()){
                for(Entry<String, List<VMDataType>> needsEntry : entry.getValue()){
                    builder.append(functionName);
                    builder.append(" ");
                    commaSepalateHelper(entry.getKey(), builder);
                    builder.append(" -> ");
                    builder.append(needsEntry.getKey());
                    builder.append(" ");
                    commaSepalateHelper(needsEntry.getValue(), builder);
                    builder.append('\n');
                }
            }
            return builder.toString();
        }

        public Set<List<VMDataType>> getNeedTypess(){
            return needTypess;
        }

        @Override
        public String toString(){
            return needFunctionTypeMap.toString();
        }
    }
}