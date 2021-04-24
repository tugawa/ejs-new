package vmdlc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import type.TypeMap;

public class ExpandedCaseCondMaker {
  private static Set<Set<TypeMap>> cloneHashSet(Set<Set<TypeMap>> original){
    Set<Set<TypeMap>> cloned = new HashSet<>(original.size());
    for(Set<TypeMap> maps : original){
      Set<TypeMap> clonedMaps = new HashSet<>(maps.size());
      for(TypeMap map : maps){
        clonedMaps.add(map.clone());
      }
      cloned.add(clonedMaps);
    }
    return cloned;
  }

  public static Set<Set<TypeMap>> getExpandedCaseCond(Set<Set<TypeMap>> source){
    Set<Set<TypeMap>> conds = cloneHashSet(source);
    Map<TypeMap, Set<Set<TypeMap>>> includesMap = new HashMap<>();
    for(Set<TypeMap> maps : conds){
      for(TypeMap map : maps){
        Set<Set<TypeMap>> includes = includesMap.get(map);
        if(includes == null){
          includes = new HashSet<>();
          includesMap.put(map, includes);
        }
        includes.add(maps);
      }
    }
    List<Duplication> duplicationList = new ArrayList<>(includesMap.keySet().size());
    for(Entry<TypeMap, Set<Set<TypeMap>>> entry : includesMap.entrySet()){
      duplicationList.add(new Duplication(entry.getKey(), entry.getValue()));
    }
    Collections.sort(duplicationList);
    int length = duplicationList.size();
    for(int i=0; i<length;){
      Duplication dup = duplicationList.get(i);
      int size = dup.getSetSize();
      if(size <= 1) break;
      Set<TypeMap> clip = new HashSet<>(1);
      Set<Set<TypeMap>> includes = dup.getIncludes();
      for(int j=i; j<length; j++, i++){
        Duplication nextDup = duplicationList.get(j);
        int nextDupSize = nextDup.getSetSize();
        if(nextDupSize != size) break;
        Set<Set<TypeMap>> nextDupIncludes = nextDup.getIncludes();
        if(!(includes.equals(nextDupIncludes))) break;
        TypeMap nextDupTypeMap = nextDup.getTypeMap();
        for(Set<TypeMap> e : nextDupIncludes){
          e.remove(nextDupTypeMap);
        }
        clip.add(nextDupTypeMap);
      }
      conds.add(clip);
    }
    Set<Set<TypeMap>> emptyRemovedConds = new HashSet<>(conds.size());
    for(Set<TypeMap> set : conds){
      if(set.isEmpty()) continue;
        emptyRemovedConds.add(set);
    }
    return emptyRemovedConds;
  }

  static class Duplication implements Comparable<Duplication>{
    private TypeMap typeMap;
    private Set<Set<TypeMap>> includes;
    private int setSize = 0;

    public Duplication(TypeMap typeMap, Set<Set<TypeMap>> includes){
      this.typeMap = typeMap;
      this.includes = includes;
      if(includes != null)
        setSize = includes.size();
    }
    public int getSetSize(){
      return setSize;
    }
    public Set<Set<TypeMap>> getIncludes(){
      return includes;
    }
    public TypeMap getTypeMap(){
      return typeMap;
    }
    @Override
    public int compareTo(Duplication that) {
      return setSize - that.getSetSize();
    }
    @Override
    public String toString(){
      return typeMap.toString()+", "+setSize+", "+includes.toString();
    }
  }
}