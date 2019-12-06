package type;

import java.util.HashMap;
import java.util.Map;

public class OperatorTypeChecker{
  private AstType[]   unaryOperatorTypeTable;
  private AstType[][] binaryOperatorTypeTable;
  protected static Map<AstType, Integer> numberMap = new HashMap<>();
  protected static final AstType T_CINT         = AstType.get("cint");
  protected static final AstType T_CDOUBLE      = AstType.get("cdouble");
  protected static final AstType T_SUBSCRIPT    = AstType.get("Subscript");
  protected static final AstType T_DISPLACEMENT = AstType.get("Displacement");
  private static final int CINT         = 0;
  private static final int CDOUBLE      = 1;
  private static final int SUBSCRIPT    = 2;
  private static final int DISPLACEMENT = 3;
  static{
    numberMap.put(T_CINT, CINT);
    numberMap.put(T_CDOUBLE, CDOUBLE);
    numberMap.put(T_SUBSCRIPT, SUBSCRIPT);
    numberMap.put(T_DISPLACEMENT, DISPLACEMENT);
  }
  private static final int typeSize =  numberMap.keySet().size();
  public static final OperatorTypeChecker PLUS = new OperatorTypeChecker().unaryOperator();
  public static final OperatorTypeChecker MINUS = PLUS;
  public static final OperatorTypeChecker COMPL = new OperatorTypeChecker().unaryOperator();
  public static final OperatorTypeChecker NOT = COMPL;
  public static final OperatorTypeChecker ADD = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker SUB = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker MUL = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker DIV = MUL;
  public static final OperatorTypeChecker MOD = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker OR = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker AND = OR;
  public static final OperatorTypeChecker BITWISE_OR = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker BITWISE_XOR = BITWISE_OR;
  public static final OperatorTypeChecker BITWISE_AND = BITWISE_OR;
  public static final OperatorTypeChecker EQUALS = new EqualsOperatorTypeChecker();
  public static final OperatorTypeChecker NOT_EQUALS = EQUALS;
  public static final OperatorTypeChecker LESSTHAN_EQUALS = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker GRATORTHAN_EQUALS = LESSTHAN_EQUALS;
  public static final OperatorTypeChecker LESSTHAN = LESSTHAN_EQUALS;
  public static final OperatorTypeChecker GRATORTHAN = LESSTHAN_EQUALS;
  public static final OperatorTypeChecker LEFT_SHIFT = new OperatorTypeChecker().binaryOperator();
  public static final OperatorTypeChecker RIGHT_SHIFT = LEFT_SHIFT;
  private OperatorTypeChecker unaryOperator(){
    unaryOperatorTypeTable = new AstType[typeSize];
    for(int i=0; i<typeSize; i++){
      unaryOperatorTypeTable[i] = null;
    }
    return this;
  }
  private OperatorTypeChecker binaryOperator(){
    binaryOperatorTypeTable = new AstType[typeSize][typeSize];
    for(int i=0; i<typeSize; i++){
      for(int j=0; j<typeSize; j++){
        binaryOperatorTypeTable[i][j] = null;
      }
    }
    return this;
  }
  private void add(int operand, AstType result){
    unaryOperatorTypeTable[operand] = result;
  }
  private void add(int loperand, int roperand, AstType result){
    binaryOperatorTypeTable[loperand][roperand] = result;
  }
  static{
    //********************
    // Unary operators
    //********************
    // PLUS, MINUS
    PLUS.add(CINT, T_CINT);
    PLUS.add(CDOUBLE, T_CDOUBLE);
    // COMPL, NOT
    COMPL.add(CINT, T_CINT);

    //********************
    // Binary operators
    //********************
    // ADD
    ADD.add(CINT, CINT, T_CINT);
    ADD.add(CINT, CDOUBLE, T_CDOUBLE);
    ADD.add(CDOUBLE, CINT, T_CDOUBLE);
    ADD.add(CDOUBLE, CDOUBLE, T_CDOUBLE);
    ADD.add(CINT, SUBSCRIPT, T_SUBSCRIPT);
    ADD.add(SUBSCRIPT, CINT, T_SUBSCRIPT);
    ADD.add(CINT, DISPLACEMENT, T_DISPLACEMENT);
    ADD.add(DISPLACEMENT, CINT, T_DISPLACEMENT);
    // SUB
    SUB.add(CINT, CINT, T_CINT);
    SUB.add(CINT, CDOUBLE, T_CDOUBLE);
    SUB.add(CDOUBLE, CINT, T_CDOUBLE);
    SUB.add(CDOUBLE, CDOUBLE, T_CDOUBLE);
    SUB.add(CINT, SUBSCRIPT, T_SUBSCRIPT);
    SUB.add(SUBSCRIPT, CINT, T_SUBSCRIPT);
    SUB.add(CINT, DISPLACEMENT, T_DISPLACEMENT);
    SUB.add(DISPLACEMENT, CINT, T_DISPLACEMENT);
    SUB.add(SUBSCRIPT, SUBSCRIPT, T_SUBSCRIPT);
    //MUL, DIV
    MUL.add(CINT, CINT, T_CINT);
    MUL.add(CINT, CDOUBLE, T_CDOUBLE);
    MUL.add(CDOUBLE, CINT, T_CDOUBLE);
    MUL.add(CDOUBLE, CDOUBLE, T_CDOUBLE);
    // MOD
    MOD.add(CINT, CINT, T_CINT);
    // OR, AND
    OR.add(CINT, CINT, T_CINT);
    // BITWISE_OR, BITWISE_XOR, BITWISE_AND
    BITWISE_OR.add(CINT, CINT, T_CINT);
    // LESSTHAN_EQUALS, GRATORTHAN_EQUALS, LESSTHAN, GRATORTHAN
    LESSTHAN_EQUALS.add(CINT, CINT, T_CINT);
    LESSTHAN_EQUALS.add(CDOUBLE, CDOUBLE, T_CINT);
    LESSTHAN_EQUALS.add(SUBSCRIPT, SUBSCRIPT, T_CINT);
    LESSTHAN_EQUALS.add(DISPLACEMENT, DISPLACEMENT, T_CINT);
    // LEFT_SHIFT, RIGHT_SHIFT
    LEFT_SHIFT.add(CINT, CINT, T_CINT);
    // NOTE: EQUALS and NOT_EQUALS defined in EqualsOperatorTypeChecker
  }
  public AstType typeOf(AstType type){
    Integer index = numberMap.get(type);
    if(index == null){
      //System.err.println("InternalWarning: Index out of range: "+type.toString());
      return null;
    }
    return unaryOperatorTypeTable[index];
  }
  public AstType typeOf(AstType ltype, AstType rtype){
    Integer lindex = numberMap.get(ltype);
    Integer rindex = numberMap.get(rtype);
    if(lindex == null || rindex == null){
      //System.err.println("InternalWarning: Index out of range: ("+ltype.toString()+", "+rtype.toString()+")");
      return null;
    }
    return binaryOperatorTypeTable[lindex][rindex];
  }
}

class EqualsOperatorTypeChecker extends OperatorTypeChecker{
  @Override
  public AstType typeOf(AstType ltype, AstType rtype){
    AstType result;
    if(ltype.isSuperOrEqual(rtype) || rtype.isSuperOrEqual(ltype)){
      result = AstType.get("cint");
    }else{
      result = null;
    }
    return result;
  }
}