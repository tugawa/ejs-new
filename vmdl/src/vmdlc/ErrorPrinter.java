package vmdlc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

//import java.util.StringUtils;

public class ErrorPrinter{
    private static List<String> code = null;

    public static void error(String message, SyntaxTree node){
        StringBuilder builder = new StringBuilder();
        int line = node.getLineNum();
        long column = node.getSourcePosition();
        int textLength = node.toText().length();
        for(int i=0; i<line-1; i++) column -= code.get(i).length() + 1;
        builder.append("[error] ");
        builder.append(message);
        builder.append(" (at line "+line+":"+column+")\n\n");
        if(code != null){
            builder.append(code.get(line-1));
            builder.append('\n');
            for(int i=0; i<column; i++) builder.append(' ');
            for(int i=0; i<textLength; i++) builder.append('^');
        }
        builder.append('\n');
        System.err.print(builder.toString());
        System.exit(-1);
    }

    public static void setSource(String path){
        try{
            code = Files.readAllLines(Paths.get(path));
        }catch(IOException e){
            code = null;
        }
    }
}