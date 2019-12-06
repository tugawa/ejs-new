package type;


public enum FunctionAnnotation {

    vmInstruction(),
    needContext(),
    triggerGC(),
    tailCall(),
    noIncPC();

    private FunctionAnnotation(){
    }
}