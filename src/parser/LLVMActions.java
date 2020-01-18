package parser;

import grammar.KotlinParser;
import grammar.KotlinParserBaseListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class LLVMActions extends KotlinParserBaseListener {

    private HashMap<String, Value> variables = new HashMap<>();
    private HashSet<String> globalNames = new HashSet<>();
    private HashSet<String> localNames = new HashSet<>();
    private Queue<String> infixExpr = new LinkedList<>();
    private boolean global = true;
    private String function = "";

    @Override
    public void enterFunctionDeclaration(KotlinParser.FunctionDeclarationContext ctx) {
        global = false;
        function = ctx.simpleIdentifier().getText();
        LLVMGenerator.function_start(function);
    }

    @Override
    public void exitFunctionBody(KotlinParser.FunctionBodyContext ctx) {
        global = true;
        LLVMGenerator.function_end();
        removeLocalVariables();
        localNames = new HashSet<>();
    }

    @Override
    public void exitCallSuffix(KotlinParser.CallSuffixContext ctx) {
        String text = ctx.getText();
        String trimmed = null;
        try {
            trimmed = text.substring(1, text.length() - 1);
        } catch (Exception ignored) {
        }
        if (trimmed != null && !trimmed.isEmpty() && variables.containsKey(trimmed)) {
            printVariable(trimmed);
        } else {
            LLVMGenerator.call(text);
        }
    }

    @Override
    public void exitVariableDeclaration(KotlinParser.VariableDeclarationContext ctx) {
        String id = ctx.simpleIdentifier().getText();
        Value value = new Value(VarType.valueOf(ctx.type().getText().toUpperCase()));
        declareVariable(id, value);
    }

    @Override
    public void enterPropertyDeclaration(KotlinParser.PropertyDeclarationContext ctx) {
        if (ctx.VAL() != null) {
            String value = ctx.expression().getText();
            boolean isMath = isMathOperation(value);
            String type = ctx.variableDeclaration().type().getText();
            String name = ctx.variableDeclaration().simpleIdentifier().getText();
            Value value1 = new Value(VarType.valueOf(type.toUpperCase()), value);
            declareVariable(name, value1);
            assignVariable(name, value1, ctx.getStart().getLine(), isMath);
        }
    }

    @Override
    public void enterIfExpression(KotlinParser.IfExpressionContext ctx) {
        LLVMGenerator.if_start();
    }

    @Override
    public void exitIfExpression(KotlinParser.IfExpressionContext ctx) {
        LLVMGenerator.if_end();
    }

    @Override
    public void exitKotlinFile(KotlinParser.KotlinFileContext ctx) {
        System.out.println(LLVMGenerator.generate());
    }


    private void declareVariable(String ID, Value value) {
        if (!variables.containsKey(ID)) {
            if (value.type != VarType.STRING) {
                variables.put(ID, value);
            }
            if (value.type == VarType.INT) {
                LLVMGenerator.declare_i32(ID, global);
            } else if (value.type == VarType.DOUBLE) {
                LLVMGenerator.declare_double(ID, global);
            }
        }
    }

    private void assignVariable(String ID, Value value, int line, boolean isMathExpr) {
        if (global) {
            globalNames.add(ID);
        } else if (!globalNames.contains(ID)) {
            localNames.add(ID);
        }
        if (value.type == VarType.INT) {
            LLVMGenerator.assign_i32(ID, getValue(value, isMathExpr), globalNames);
        } else if (value.type == VarType.DOUBLE) {
            LLVMGenerator.assign_double(ID, getValue(value, isMathExpr), globalNames);
        } else if (value.type == VarType.STRING) {
            assignString(ID, value, line);
        } else {
            error(line, "Assign error: " + ID);
        }
    }

    private String getValue(Value value, boolean isMathExpr) {
        if (isMathExpr) {
            return value.content;
        }
        if (MathUtils.isNumeric(value.content)) {
            return value.content;
        } else {
            if (value.type == VarType.DOUBLE) {
                LLVMGenerator.load_double(value.content, globalNames);
            } else {
                LLVMGenerator.load_i32(value.content, globalNames);
            }
            return "%" + (LLVMGenerator.reg - 1);
        }
    }

    private void assignString(String ID, Value value, int line) {
        if (!variables.containsKey(ID)) {
            LLVMGenerator.assign_string(ID, value.content, global, function);
            variables.put(ID, value);
        } else {
            error(line, ID + " is constant value.");
        }
    }

    private void printConstant(String text) {
        LLVMGenerator.print(text);
    }

    private void removeLocalVariables() {
        for (String id : localNames) {
            variables.remove(id);
        }
    }

    private void error(int line, String msg) {
        System.err.println("Error, line " + line + ", " + msg);
        System.exit(1);
    }

    private void printVariable(String ID) {
        if (variables.get(ID).type == VarType.INT) {
            LLVMGenerator.printf_i32(ID, globalNames);
        } else if (variables.get(ID).type == VarType.DOUBLE) {
            LLVMGenerator.printf_double(ID, globalNames);
        } else if (variables.get(ID).type == VarType.STRING) {
            LLVMGenerator.printf_string(ID, variables.get(ID).content.length(), globalNames, function);
        }
    }

    private boolean isMathOperation(String text) {
        return text.contains("+") || text.contains("-") || text.contains("*") || text.contains("/") || text.contains("%");
    }

    enum VarType {STRING, INT, DOUBLE}

    static class Value {
        String content;
        VarType type;

        Value(VarType type) {
            this.type = type;
        }

        Value(VarType type, String content) {
            this.content = content;
            this.type = type;
        }
    }
}
