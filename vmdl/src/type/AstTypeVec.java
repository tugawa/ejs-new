package type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import type.AstType.AstPairType;

public class AstTypeVec {
  private List<AstType> list;

  public AstTypeVec(){
    list = new ArrayList<>();
  }
  public AstTypeVec(int initialCapacity){
    list = new ArrayList<>(initialCapacity);
  }
  public AstTypeVec(List<AstType> list){
    this.list = list;
  }
  public AstTypeVec(AstType type){
    if(type instanceof AstPairType){
      list = ((AstPairType)type).getTypes();
    }else{
      list = new ArrayList<>(1);
      list.add(type);
    }
  }
  public void add(AstType type){
    list.add(type);
  }
  public List<AstType> getList(){
    return list;
  }
  public int size(){
    return list.size();
  }
  public boolean contains(AstType type){
    return list.contains(type);
  }
  public AstType get(int i){
    return list.get(i);
  }
  private Set<VMDataTypeVec> toVMDataTypeVecSetInner(Set<VMDataTypeVec> vmdtVecSet, int i){
    if(i>=list.size()) return vmdtVecSet;
    Set<VMDataType> vmdTypes = list.get(i).getVMDataTypes();
    if(vmdTypes == null){
      return toVMDataTypeVecSetInner(vmdtVecSet, i+1);
    }
    if(vmdtVecSet.isEmpty()){
      vmdtVecSet.add(new VMDataTypeVec(list.size()));
    }
    Set<VMDataTypeVec> newVmdtVecSet = new HashSet<>(vmdtVecSet.size() * vmdTypes.size());
    for(VMDataTypeVec vec : vmdtVecSet){
      for(VMDataType vmdType : vmdTypes){
        VMDataTypeVec newVec = new VMDataTypeVec(vec);
        newVec.add(vmdType);
        newVmdtVecSet.add(newVec);
      }
    }
    return toVMDataTypeVecSetInner(newVmdtVecSet, i+1);
  }
  public Set<VMDataTypeVec> toVMDataTypeVecSet(){
    Set<VMDataTypeVec> vmdtVecSet = new HashSet<>();
    return toVMDataTypeVecSetInner(vmdtVecSet, 0);
  }
  /* ATTENTION: toVMDataTypeVecSet method ignores Non-JSValueType */
  public static Set<VMDataTypeVec> toVMDataTypeVecSet(Set<AstTypeVec> astTypeVecSet){
    Set<VMDataTypeVec> vmdtVecSet = new HashSet<>();
    for(AstTypeVec atVec : astTypeVecSet){
      vmdtVecSet.addAll(atVec.toVMDataTypeVecSet());
    }
    return vmdtVecSet;
  }
  private static Set<AstTypeVec> toAstTypeVecSetInner(Set<AstTypeVec> astTypeVecSet, List<ExprTypeSet> exprTypeSetVec, int i){
    int size = exprTypeSetVec.size();
    if(i >= exprTypeSetVec.size()) return astTypeVecSet;
    Set<AstType> astTypeSet = exprTypeSetVec.get(i).getTypeSet();
    if(astTypeVecSet.isEmpty()){
      for(AstType t : astTypeSet){
        AstTypeVec vec = new AstTypeVec(size);
        vec.add(t);
        astTypeVecSet.add(vec);
      }
    }else{
      for(AstTypeVec vec : astTypeVecSet){
        for(AstType t : astTypeSet){
          vec.add(t);
        }
      }
    }
    return toAstTypeVecSetInner(astTypeVecSet, exprTypeSetVec, i+1);
  }
  public static Set<AstTypeVec> toAstTypeVecSet(List<ExprTypeSet> exprTypeSetVec){
    Set<AstTypeVec> astTypeVecSet = new HashSet<>();
    return toAstTypeVecSetInner(astTypeVecSet, exprTypeSetVec, 0);
  }
  @Override
  public String toString(){
    return list.toString();
  }
}