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
    //test print
    /*
    System.err.println("Duplication table:");
    for(Duplication d : duplicationList)
      System.err.println(d.toString());
    */
    int length = duplicationList.size();
    for(int i=0; i<length;){
      //System.err.println(">number "+i+".");
      Duplication dup = duplicationList.get(i);
      int size = dup.getSetSize();
      if(size <= 1) break;
      Set<TypeMap> clip = new HashSet<>(1);
      Set<Set<TypeMap>> includes = dup.getIncludes();
      /*
      TypeMap typeMap = dup.getTypeMap();
      for(Set<TypeMap> e : includes){
        e.remove(typeMap);
      }
      clip.add(typeMap);
      */
      for(int j=i; j<length; j++, i++){
        Duplication nextDup = duplicationList.get(j);
        int nextDupSize = nextDup.getSetSize();
        if(nextDupSize != size) break;
        Set<Set<TypeMap>> nextDupIncludes = nextDup.getIncludes();
        if(!(includes.equals(nextDupIncludes))) break;
        TypeMap nextDupTypeMap = nextDup.getTypeMap();
        //System.err.println(">"+nextDupTypeMap.toString()+" is removed.");
        for(Set<TypeMap> e : nextDupIncludes){
          e.remove(nextDupTypeMap);
        }
        clip.add(nextDupTypeMap);
      }
      //System.err.println(">conds adds clip of "+clip.toString()+".");
      conds.add(clip);
    }
    Set<Set<TypeMap>> emptyRemovedConds = new HashSet<>(conds.size());
    for(Set<TypeMap> set : conds){
      if(set.isEmpty()) continue;
        emptyRemovedConds.add(set);
    }
    /*
    System.err.println("input:\n"+source.toString());
    System.err.println("result:\n"+emptyRemovedConds.toString());
    */
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