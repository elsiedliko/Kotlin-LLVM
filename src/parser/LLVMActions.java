package parser;

import grammar.KotlinParserBaseListener;

public class LLVMActions extends KotlinParserBaseListener {

    enum VarType {STRING, INT, REAL}

    static class Value {
        String content;
        VarType type;

        Value(String content, VarType type) {
            this.content = content;
            this.type = type;
        }
    }

    //TODO IMPLEMENT SOURCE TREE WALKER
}
