package type;


public enum FunctionAnnotation {

    vmInstruction(),
    needContext(),
    triggerGC(),
    tailCall(),
    noIncPC(),
    makeInline(),
    builtinFunction(),
    calledFromC();

    private FunctionAnnotation(){
    }
}