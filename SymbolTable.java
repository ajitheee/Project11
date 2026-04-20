import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private static class Symbol {
        String type;
        Kind kind;
        int index;

        Symbol(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private final Map<String, Symbol> classTable = new HashMap<>();
    private final Map<String, Symbol> subroutineTable = new HashMap<>();

    private int staticCount = 0;
    private int fieldCount = 0;
    private int argCount = 0;
    private int varCount = 0;

    public void startSubroutine() {
        subroutineTable.clear();
        argCount = 0;
        varCount = 0;
    }

    public void define(String name, String type, Kind kind) {
        int index = varCount(kind);
        Symbol symbol = new Symbol(type, kind, index);

        if (kind == Kind.STATIC || kind == Kind.FIELD) {
            classTable.put(name, symbol);
        } else {
            subroutineTable.put(name, symbol);
        }

        switch (kind) {
            case STATIC -> staticCount++;
            case FIELD -> fieldCount++;
            case ARG -> argCount++;
            case VAR -> varCount++;
        }
    }

    public int varCount(Kind kind) {
        return switch (kind) {
            case STATIC -> staticCount;
            case FIELD -> fieldCount;
            case ARG -> argCount;
            case VAR -> varCount;
            default -> 0;
        };
    }

    public Kind kindOf(String name) {
        if (subroutineTable.containsKey(name)) return subroutineTable.get(name).kind;
        if (classTable.containsKey(name)) return classTable.get(name).kind;
        return Kind.NONE;
    }

    public String typeOf(String name) {
        if (subroutineTable.containsKey(name)) return subroutineTable.get(name).type;
        if (classTable.containsKey(name)) return classTable.get(name).type;
        return null;
    }

    public int indexOf(String name) {
        if (subroutineTable.containsKey(name)) return subroutineTable.get(name).index;
        if (classTable.containsKey(name)) return classTable.get(name).index;
        return -1;
    }
}