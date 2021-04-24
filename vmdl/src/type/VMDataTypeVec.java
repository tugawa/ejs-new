package type;

import java.util.ArrayList;
import java.util.List;

public class VMDataTypeVec {
  private List<VMDataType> list;

  public VMDataTypeVec(){
    list = new ArrayList<>();
  }
  public VMDataTypeVec(int initialCapacity){
    list = new ArrayList<>(initialCapacity);
  }
  public VMDataTypeVec(VMDataTypeVec c){
    list = new ArrayList<>(c.toList());
  }
  public VMDataTypeVec(List<VMDataType> c){
    list = c;
  }
  public List<String> toStringList(){
    List<String> ret = new ArrayList<>(list.size());
    for(VMDataType vmd : list){
      ret.add(vmd.toString());
    }
    return ret;
  }
  public List<VMDataType> toList(){
    return list;
  }
  public VMDataType[] toArray(){
    return list.toArray(new VMDataType[0]);
  }
  public void add(VMDataType t){
    list.add(t);
  }
  @Override
  public String toString(){
    return list.toString();
  }
}