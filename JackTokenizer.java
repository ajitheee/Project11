import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class JackTokenizer {
    private static final Set<String> KEYWORDS = Set.of(
            "class", "constructor", "function", "method", "field", "static",
            "var", "int", "char", "boolean", "void", "true", "false", "null",
            "this", "let", "do", "if", "else", "while", "return"
    );

    private static final Set<Character> SYMBOLS = Set.of(
            '{', '}', '(', ')', '[', ']', '.', ',', ';',
            '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'
    );

    private final List<String> tokens = new ArrayList<>();
    private int currentIndex = -1;
    private String currentToken;

    public JackTokenizer(File file) throws IOException {
        String content = Files.readString(file.toPath());
        content = removeComments(content);
        tokenize(content);
    }

    private String removeComments(String input) {
        input = input.replaceAll("(?s)/\\*.*?\\*/", "");
        input = input.replaceAll("//.*", "");
        return input;
    }

    private void tokenize(String input) {
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (SYMBOLS.contains(c)) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            if (c == '"') {
                int j = i + 1;
                while (j < input.length() && input.charAt(j) != '"') {
                    j++;
                }
                tokens.add(input.substring(i, j + 1));
                i = j + 1;
                continue;
            }

            int j = i;
            while (j < input.length()
                    && !Character.isWhitespace(input.charAt(j))
                    && !SYMBOLS.contains(input.charAt(j))) {
                j++;
            }
            tokens.add(input.substring(i, j));
            i = j;
        }
    }

    public boolean hasMoreTokens() {
        return currentIndex + 1 < tokens.size();
    }

    public void advance() {
        currentIndex++;
        currentToken = tokens.get(currentIndex);
    }

    public TokenType tokenType() {
        if (KEYWORDS.contains(currentToken)) return TokenType.KEYWORD;
        if (currentToken.length() == 1 && SYMBOLS.contains(currentToken.charAt(0))) return TokenType.SYMBOL;
        if (currentToken.matches("\\d+")) return TokenType.INT_CONST;
        if (currentToken.startsWith("\"")) return TokenType.STRING_CONST;
        return TokenType.IDENTIFIER;
    }

    public String keyword() { return currentToken; }
    public char symbol() { return currentToken.charAt(0); }
    public String identifier() { return currentToken; }
    public int intVal() { return Integer.parseInt(currentToken); }
    public String stringVal() { return currentToken.substring(1, currentToken.length() - 1); }
    public String token() { return currentToken; }

    public List<String> getAllTokens() {
        return tokens;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}