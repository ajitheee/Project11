import java.io.File;

public class JackCompiler {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java JackCompiler <file or directory>");
            return;
        }

        File input = new File(args[0]);

        try {
            if (input.isDirectory()) {
                File[] files = input.listFiles((dir, name) -> name.endsWith(".jack"));
                if (files != null) {
                    for (File file : files) {
                        compileFile(file);
                    }
                }
            } else if (input.getName().endsWith(".jack")) {
                compileFile(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void compileFile(File inputFile) throws Exception {
        String outName = inputFile.getAbsolutePath().replace(".jack", ".vm");
        CompilationEngine engine = new CompilationEngine(inputFile, new File(outName));
        engine.compileClass();
        System.out.println("Generated: " + outName);
    }
}