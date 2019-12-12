package vmdlc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import nez.ast.Symbol;

public class ErrorPrinter{
    private static List<String> codes = null;

    private static String getErrorText(String message, int line, long column, int length){
        StringBuilder builder = new StringBuilder();
        builder.append("[error] ");
        builder.append(message);
        builder.append(" (at line "+line+":"+column+")\n\n");
        if(codes != null){
            String code = codes.get(line-1);
            int lineLength = code.length();
            builder.append(code);
            if(length+column-1 >= lineLength) builder.append(" ...");
            builder.append('\n');
            for(int i=0; i<column; i++) builder.append(' ');
            for(int i=0; i<length && i+column<lineLength; i++) builder.append('^');
        }
        builder.append('\n');
        return builder.toString();
    }

    public static void error(String message){
        StringBuilder builder = new StringBuilder();
        builder.append("[error] ");
        builder.append(message);
        System.err.print(builder.toString());
        System.exit(-1);
    }

    public static void error(String message, SyntaxTree node){
        int line = node.getLineNum();
        long column = node.getSourcePosition();
        int textLength = node.toText().length();
        for(int i=0; i<line-1; i++){
            column -= codes.get(i).length() + 1;
        }
        System.err.print(getErrorText(message, line, column, textLength));
        System.exit(-1);
    }

    public static void recursiveError(String message, SyntaxTree node){
        int textLength = 0;
        Symbol symbol = node.getTag();
        SyntaxTree n = node;
        for(; n.getTag().equals(symbol); n = n.get(0)){
            textLength += n.toText().length();
        }
        textLength += n.toText().length();
        int line = n.getLineNum();
        long column = n.getSourcePosition();
        for(int i=0; i<line-1; i++){
            column -= codes.get(i).length() + 1;
        }
        System.err.print(getErrorText(message, line, column, textLength));
        System.exit(-1);
    }

    public static void setSource(String path){
        try{
            codes = Files.readAllLines(Paths.get(path));
        }catch(IOException e){
            codes = null;
        }
    }
}