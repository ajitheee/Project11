import java.io.*;

public class VMWriter {
    private final BufferedWriter writer;

    public VMWriter(File file) throws IOException {
        writer = new BufferedWriter(new FileWriter(file));
    }

    public void writePush(String segment, int index) throws IOException {
        writer.write("push " + segment + " " + index);
        writer.newLine();
    }

    public void writePop(String segment, int index) throws IOException {
        writer.write("pop " + segment + " " + index);
        writer.newLine();
    }

    public void writeArithmetic(String command) throws IOException {
        writer.write(command);
        writer.newLine();
    }

    public void writeLabel(String label) throws IOException {
        writer.write("label " + label);
        writer.newLine();
    }

    public void writeGoto(String label) throws IOException {
        writer.write("goto " + label);
        writer.newLine();
    }

    public void writeIf(String label) throws IOException {
        writer.write("if-goto " + label);
        writer.newLine();
    }

    public void writeCall(String name, int nArgs) throws IOException {
        writer.write("call " + name + " " + nArgs);
        writer.newLine();
    }

    public void writeFunction(String name, int nLocals) throws IOException {
        writer.write("function " + name + " " + nLocals);
        writer.newLine();
    }

    public void writeReturn() throws IOException {
        writer.write("return");
        writer.newLine();
    }

    public void close() throws IOException {
        writer.close();
    }
}