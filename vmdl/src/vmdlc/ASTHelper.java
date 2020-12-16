package vmdlc;

import nez.ast.Symbol;
import type.AstType;

public class ASTHelper {

    //*********************************
    // Types
    //*********************************

    public static SyntaxTree generateTopTypeName(){
        return new SyntaxTree(Symbol.unique("TopTypeName"), null, null, "Top");
    }
    public static SyntaxTree generateVoidTypeName(){
        return new SyntaxTree(Symbol.unique("VoidTypeName"), null, null, "void");
    }
    public static SyntaxTree generateCType(String name){
        return new SyntaxTree(Symbol.unique("CType"), null, null, name);
    }
    public static SyntaxTree generateJSValueType(String name){
        return new SyntaxTree(Symbol.unique("JSValueTypeName"), null, null, name);
    }
    public static SyntaxTree generateUserTypeName(String name){
        return new SyntaxTree(Symbol.unique("UserTypeName"), null, null, name);
    }
    public static SyntaxTree generateType(String name){
        AstType t = AstType.get(name);
        if(t == null){
            System.err.println("InternalWarning: Unexpected type name : "+name);
            return null;
        }
        if(name.equals("void")){
            return generateVoidTypeName();
        }
        if(name.equals("Top")){
            return generateTopTypeName();
        }
        if(t instanceof AstType.JSValueType){
            return generateJSValueType(name);
        }
        if(t instanceof AstType.AstBaseType){
            return generateCType(name);
        }
        // really?
        return generateUserTypeName(name);
    }
    public static SyntaxTree generateType(AstType t){
        return generateType(t.toString());
    }

    //*********************************
    // TypePatterns
    //*********************************

    // TypePattern

    public static SyntaxTree generateTypePattern(String type, String var){
        return new SyntaxTree(Symbol.unique("TypePattern"),
            new Symbol[]{Symbol.unique("type"), Symbol.unique("var")}, 
            new SyntaxTree[]{generateType(type), generateName(var)}, null);
    }

    // AndPattern

    public static SyntaxTree generateAndPattern(SyntaxTree pattern1, SyntaxTree pattern2){
        return new SyntaxTree(Symbol.unique("AndPattern"), null, new SyntaxTree[]{pattern1, pattern2}, null);
    }

    // OrPattern

    public static SyntaxTree generateOrPattern(SyntaxTree pattern1, SyntaxTree pattern2){
        return new SyntaxTree(Symbol.unique("OrPattern"), null, new SyntaxTree[]{pattern1, pattern2}, null);
    }

    //*********************************
    // Statements
    //*********************************

    // Declaration

    public static SyntaxTree generateDeclaration(SyntaxTree type, SyntaxTree var, SyntaxTree expr){
        return new SyntaxTree(Symbol.unique("Declaration"),
            new Symbol[]{Symbol.unique("type"), Symbol.unique("var"), Symbol.unique("expr")},
            new SyntaxTree[]{type, var, expr}, null);
    }
    public static SyntaxTree generateDeclaration(SyntaxTree type, String var, SyntaxTree expr){
        return generateDeclaration(type, generateName(var), expr);
    }
    public static SyntaxTree generateDeclaration(String type, String var, SyntaxTree expr){
        return generateDeclaration(generateType(type), generateName(var), expr);
    }
    public static SyntaxTree generateDeclaration(AstType type, String var, SyntaxTree expr){
        return generateDeclaration(generateType(type), generateName(var), expr);
    }
    public static SyntaxTree generateDeclaration(SyntaxTree type, SyntaxTree var){
        return new SyntaxTree(Symbol.unique("Declaration"),
            new Symbol[]{Symbol.unique("type"), Symbol.unique("var")},
            new SyntaxTree[]{type, var}, null);
    }

    //Assignment

    public static SyntaxTree generateAssignment(SyntaxTree left, SyntaxTree right){
        return new SyntaxTree(Symbol.unique("Assignment"),
            new Symbol[]{Symbol.unique("left"), Symbol.unique("right")},
            new SyntaxTree[]{left, right}, null);
    }


    // ExpressionStatement

    public static SyntaxTree generateExpressionStatement(SyntaxTree expr){
        return new SyntaxTree(Symbol.unique("ExpressionStatement"), null, new SyntaxTree[]{expr}, null);
    }

    // Block

    public static SyntaxTree generateBlock(SyntaxTree[] args){
        return new SyntaxTree(Symbol.unique("Block"), null, args, null);
    }

    //*********************************
    // Expressions
    //*********************************

    public static SyntaxTree generateFunctionCall(String name, SyntaxTree args){
        return new SyntaxTree(Symbol.unique("FunctionCall"),
            new Symbol[]{Symbol.unique("recv"), Symbol.unique("args")},
            new SyntaxTree[]{generateName(name), args}, null);
    }
    public static SyntaxTree generateFunctionCall(String name, SyntaxTree[] args){
        return new SyntaxTree(Symbol.unique("FunctionCall"),
            new Symbol[]{Symbol.unique("recv"), Symbol.unique("args")},
            new SyntaxTree[]{generateName(name), generateArgList(args)}, null);
    }

    //*********************************
    // Others
    //*********************************

    // Name

    public static SyntaxTree generateName(String name){
        return new SyntaxTree(Symbol.unique("Name"), null, null, name);
    }

    // ArgList

    public static SyntaxTree generateArgList(SyntaxTree[] args){
        return new SyntaxTree(Symbol.unique("ArgList"), null, args, null);
    }

    // Case

    public static SyntaxTree generateCase(SyntaxTree condition, SyntaxTree body){
        return new SyntaxTree(Symbol.unique("Case"),
            new Symbol[]{Symbol.unique("pattern"), Symbol.unique("body")}, 
            new SyntaxTree[]{condition, body}, null);
    }

    // Cases
    public static SyntaxTree generateCases(SyntaxTree[] cases){
        return new SyntaxTree(Symbol.unique("Cases"), null, cases, null);
    }

    /*
    public static SyntaxTree generate(){
        return new SyntaxTree(Symbol.unique(""), , , null);
    }
    */

    //*********************************
    // Specials
    //*********************************

    static SyntaxTree EMPTY_ARGLIST = generateArgList(null);
    static SyntaxTree BUILTIN_PROLOGUE = generateFunctionCall("builtin_prologue", EMPTY_ARGLIST);

    static {
        EMPTY_ARGLIST.setValue("()");
    }

}
