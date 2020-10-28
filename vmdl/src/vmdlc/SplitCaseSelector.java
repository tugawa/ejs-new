package vmdlc;

import java.util.HashSet;
import java.util.Set;

import type.AstType;
import type.TypeMap;
import type.VMDataType;

public class SplitCaseSelector {

  private Set<TypeMap> enableConds = new HashSet<>();

  public SplitCaseSelector(String insnName, String[] formalParams, OperandSpecifications spec){
    Set<VMDataType[]> primitiveEnableConds = spec.getAccept(insnName, formalParams).getTuples();
    enableConds = new HashSet<>(primitiveEnableConds.size()+1, 1.0f);
    for(VMDataType[] vec : primitiveEnableConds){
      TypeMap typeMap = new TypeMap();
      int length = vec.length;
      for(int i=0; i<length; i++){
        typeMap.add(formalParams[i], AstType.get(vec[i]));
      }
      enableConds.add(typeMap);
    }
  }

  public Set<Set<TypeMap>> select(Set<Set<TypeMap>> original){
    Set<Set<TypeMap>> selected = new HashSet<>();
    for(Set<TypeMap> typeMaps : original){
      Set<TypeMap> temp = new HashSet<>(typeMaps);
      temp.retainAll(enableConds);
      if(temp.isEmpty()) continue;
      selected.add(temp);
    }
    return selected;
  }
}