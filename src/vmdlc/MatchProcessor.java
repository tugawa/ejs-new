package vmdlc;

import nez.ast.Symbol;

public class MatchProcessor {
    MatchProcessor(SyntaxTree node) {
       /* TODO: all */

        SyntaxTree params = node.get(Symbol.unique("params"));
        String[] varName = new String[params.size()];
        for (int i = 0; i < params.size(); i++) {
            varName[i] = params.get(i).toText();
        }
    }   
    


}
