import java.io.File;
import java.io.IOException;
import java.util.Set;

public class CompilationEngine {
    private final JackTokenizer tokenizer;
    private final VMWriter vmWriter;
    private final SymbolTable symbolTable;

    private String className;
    private int whileCounter = 0;
    private int ifCounter = 0;

    public CompilationEngine(File inputFile, File outputFile) throws Exception {
        tokenizer = new JackTokenizer(inputFile);
        vmWriter = new VMWriter(outputFile);
        symbolTable = new SymbolTable();

        className = "";

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }

    public void compileClass() throws Exception {
        eat("class");
        className = tokenizer.identifier();
        eatIdentifier();
        eat("{");

        while (tokenIs("static") || tokenIs("field")) {
            compileClassVarDec();
        }

        while (tokenIs("constructor") || tokenIs("function") || tokenIs("method")) {
            compileSubroutine();
        }

        eat("}");
        vmWriter.close();
    }

    private void compileClassVarDec() throws Exception {
        String kindStr = tokenizer.token();
        Kind kind = kindStr.equals("static") ? Kind.STATIC : Kind.FIELD;
        eat(kindStr);

        String type = compileType();
        String name = tokenizer.identifier();
        eatIdentifier();
        symbolTable.define(name, type, kind);

        while (tokenIs(",")) {
            eat(",");
            name = tokenizer.identifier();
            eatIdentifier();
            symbolTable.define(name, type, kind);
        }

        eat(";");
    }

    private void compileSubroutine() throws Exception {
        symbolTable.startSubroutine();

        String subroutineType = tokenizer.token();
        eat(subroutineType);

        if (tokenizer.tokenType() == TokenType.KEYWORD || tokenizer.tokenType() == TokenType.IDENTIFIER) {
            advanceToken();
        } else {
            throw new RuntimeException("Expected return type");
        }

        String subroutineName = tokenizer.identifier();
        eatIdentifier();

        if (subroutineType.equals("method")) {
            symbolTable.define("this", className, Kind.ARG);
        }

        eat("(");
        compileParameterList();
        eat(")");

        eat("{");

        while (tokenIs("var")) {
            compileVarDec();
        }

        int numLocals = symbolTable.varCount(Kind.VAR);
        vmWriter.writeFunction(className + "." + subroutineName, numLocals);

        if (subroutineType.equals("constructor")) {
            int fieldCount = symbolTable.varCount(Kind.FIELD);
            vmWriter.writePush("constant", fieldCount);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        } else if (subroutineType.equals("method")) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);
        }

        compileStatements();
        eat("}");
    }

    private void compileParameterList() throws Exception {
        if (!tokenIs(")")) {
            String type = compileType();
            String name = tokenizer.identifier();
            eatIdentifier();
            symbolTable.define(name, type, Kind.ARG);

            while (tokenIs(",")) {
                eat(",");
                type = compileType();
                name = tokenizer.identifier();
                eatIdentifier();
                symbolTable.define(name, type, Kind.ARG);
            }
        }
    }

    private void compileVarDec() throws Exception {
        eat("var");

        String type = compileType();
        String name = tokenizer.identifier();
        eatIdentifier();
        symbolTable.define(name, type, Kind.VAR);

        while (tokenIs(",")) {
            eat(",");
            name = tokenizer.identifier();
            eatIdentifier();
            symbolTable.define(name, type, Kind.VAR);
        }

        eat(";");
    }

    private void compileStatements() throws Exception {
        while (tokenIs("let") || tokenIs("if") || tokenIs("while") || tokenIs("do") || tokenIs("return")) {
            switch (tokenizer.token()) {
                case "let" -> compileLet();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
            }
        }
    }

    private void compileDo() throws Exception {
        eat("do");
        compileSubroutineCall();
        eat(";");
        vmWriter.writePop("temp", 0);
    }

    private void compileLet() throws Exception {
        eat("let");

        String varName = tokenizer.identifier();
        eatIdentifier();

        boolean isArray = false;

        if (tokenIs("[")) {
            isArray = true;

            pushVar(varName);
            eat("[");
            compileExpression();
            eat("]");
            vmWriter.writeArithmetic("add");
        }

        eat("=");
        compileExpression();
        eat(";");

        if (isArray) {
            vmWriter.writePop("temp", 0);
            vmWriter.writePop("pointer", 1);
            vmWriter.writePush("temp", 0);
            vmWriter.writePop("that", 0);
        } else {
            popVar(varName);
        }
    }

    private void compileWhile() throws Exception {
        int idx = whileCounter++;
        String expLabel = "WHILE_EXP" + idx;
        String endLabel = "WHILE_END" + idx;

        eat("while");

        vmWriter.writeLabel(expLabel);

        eat("(");
        compileExpression();
        eat(")");

        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(endLabel);

        eat("{");
        compileStatements();
        eat("}");

        vmWriter.writeGoto(expLabel);
        vmWriter.writeLabel(endLabel);
    }

    private void compileReturn() throws Exception {
        eat("return");

        if (!tokenIs(";")) {
            compileExpression();
        } else {
            vmWriter.writePush("constant", 0);
        }

        eat(";");
        vmWriter.writeReturn();
    }

    private void compileIf() throws Exception {
        int idx = ifCounter++;
        String trueLabel = "IF_TRUE" + idx;
        String falseLabel = "IF_FALSE" + idx;
        String endLabel = "IF_END" + idx;

        eat("if");
        eat("(");
        compileExpression();
        eat(")");

        vmWriter.writeIf(trueLabel);
        vmWriter.writeGoto(falseLabel);
        vmWriter.writeLabel(trueLabel);

        eat("{");
        compileStatements();
        eat("}");

        if (tokenIs("else")) {
            vmWriter.writeGoto(endLabel);
            vmWriter.writeLabel(falseLabel);

            eat("else");
            eat("{");
            compileStatements();
            eat("}");

            vmWriter.writeLabel(endLabel);
        } else {
            vmWriter.writeLabel(falseLabel);
        }
    }

    private void compileExpression() throws Exception {
        compileTerm();

        while (isOp(tokenizer.token())) {
            String op = tokenizer.token();
            eat(op);
            compileTerm();
            writeOp(op);
        }
    }

    private void compileTerm() throws Exception {
        TokenType type = tokenizer.tokenType();
        String token = tokenizer.token();

        if (type == TokenType.INT_CONST) {
            vmWriter.writePush("constant", tokenizer.intVal());
            advanceToken();
        }
        else if (type == TokenType.STRING_CONST) {
            String s = tokenizer.stringVal();
            vmWriter.writePush("constant", s.length());
            vmWriter.writeCall("String.new", 1);
            for (int i = 0; i < s.length(); i++) {
                vmWriter.writePush("constant", (int) s.charAt(i));
                vmWriter.writeCall("String.appendChar", 2);
            }
            advanceToken();
        }
        else if (type == TokenType.KEYWORD) {
            switch (token) {
                case "true" -> {
                    vmWriter.writePush("constant", 0);
                    vmWriter.writeArithmetic("not");
                }
                case "false", "null" -> vmWriter.writePush("constant", 0);
                case "this" -> vmWriter.writePush("pointer", 0);
                default -> throw new RuntimeException("Unexpected keyword constant: " + token);
            }
            advanceToken();
        }
        else if (token.equals("(")) {
            eat("(");
            compileExpression();
            eat(")");
        }
        else if (token.equals("-") || token.equals("~")) {
            String unaryOp = token;
            eat(unaryOp);
            compileTerm();
            if (unaryOp.equals("-")) {
                vmWriter.writeArithmetic("neg");
            } else {
                vmWriter.writeArithmetic("not");
            }
        }
        else if (type == TokenType.IDENTIFIER) {
            String name = tokenizer.identifier();
            eatIdentifier();

            if (tokenIs("[")) {
                pushVar(name);
                eat("[");
                compileExpression();
                eat("]");
                vmWriter.writeArithmetic("add");
                vmWriter.writePop("pointer", 1);
                vmWriter.writePush("that", 0);
            }
            else if (tokenIs("(") || tokenIs(".")) {
                compileSubroutineCallRest(name);
            }
            else {
                pushVar(name);
            }
        }
        else {
            throw new RuntimeException("Unexpected term: " + token);
        }
    }

    private int compileExpressionList() throws Exception {
        int count = 0;

        if (!tokenIs(")")) {
            compileExpression();
            count++;

            while (tokenIs(",")) {
                eat(",");
                compileExpression();
                count++;
            }
        }

        return count;
    }

    private void compileSubroutineCall() throws Exception {
        String name = tokenizer.identifier();
        eatIdentifier();
        compileSubroutineCallRest(name);
    }

    private void compileSubroutineCallRest(String firstName) throws Exception {
        int nArgs = 0;
        String fullName;

        if (tokenIs(".")) {
            eat(".");

            String secondName = tokenizer.identifier();
            eatIdentifier();

            Kind kind = symbolTable.kindOf(firstName);
            if (kind != Kind.NONE) {
                pushVar(firstName);
                fullName = symbolTable.typeOf(firstName) + "." + secondName;
                nArgs = 1;
            } else {
                fullName = firstName + "." + secondName;
            }
        } else {
            vmWriter.writePush("pointer", 0);
            fullName = className + "." + firstName;
            nArgs = 1;
        }

        eat("(");
        nArgs += compileExpressionList();
        eat(")");

        vmWriter.writeCall(fullName, nArgs);
    }

    private void pushVar(String name) throws Exception {
        Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);

        if (kind == Kind.NONE) {
            throw new RuntimeException("Undefined variable: " + name);
        }

        vmWriter.writePush(kindToSegment(kind), index);
    }

    private void popVar(String name) throws Exception {
        Kind kind = symbolTable.kindOf(name);
        int index = symbolTable.indexOf(name);

        if (kind == Kind.NONE) {
            throw new RuntimeException("Undefined variable: " + name);
        }

        vmWriter.writePop(kindToSegment(kind), index);
    }

    private String kindToSegment(Kind kind) {
        return switch (kind) {
            case STATIC -> "static";
            case FIELD -> "this";
            case ARG -> "argument";
            case VAR -> "local";
            default -> throw new RuntimeException("Invalid kind");
        };
    }

    private String compileType() throws Exception {
        if (tokenizer.tokenType() == TokenType.KEYWORD || tokenizer.tokenType() == TokenType.IDENTIFIER) {
            String type = tokenizer.token();
            advanceToken();
            return type;
        }
        throw new RuntimeException("Expected type, got: " + tokenizer.token());
    }

    private void writeOp(String op) throws IOException {
        switch (op) {
            case "+" -> vmWriter.writeArithmetic("add");
            case "-" -> vmWriter.writeArithmetic("sub");
            case "=" -> vmWriter.writeArithmetic("eq");
            case ">" -> vmWriter.writeArithmetic("gt");
            case "<" -> vmWriter.writeArithmetic("lt");
            case "&" -> vmWriter.writeArithmetic("and");
            case "|" -> vmWriter.writeArithmetic("or");
            case "*" -> vmWriter.writeCall("Math.multiply", 2);
            case "/" -> vmWriter.writeCall("Math.divide", 2);
            default -> throw new RuntimeException("Unknown operator: " + op);
        }
    }

    private boolean isOp(String token) {
        return Set.of("+", "-", "*", "/", "&", "|", "<", ">", "=").contains(token);
    }

    private boolean tokenIs(String expected) {
        return tokenizer.token().equals(expected);
    }

    private void eat(String expected) throws Exception {
        if (!tokenizer.token().equals(expected)) {
            throw new RuntimeException("Expected '" + expected + "', got '" + tokenizer.token() + "'");
        }
        advanceToken();
    }

    private void eatIdentifier() throws Exception {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new RuntimeException("Expected identifier, got: " + tokenizer.token());
        }
        advanceToken();
    }

    private void advanceToken() throws Exception {
        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        }
    }
}