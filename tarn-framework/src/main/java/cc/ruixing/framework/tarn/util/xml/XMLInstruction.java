package cc.ruixing.framework.tarn.util.xml;


import cc.ruixing.framework.tarn.util.FastStringBuilder;

public class XMLInstruction extends XMLNode {
    String instruction;

    public XMLInstruction(String instruction) {
        this.instruction = instruction;
    }

    public void toString(String prefix, FastStringBuilder sb) {
        sb.append(prefix).append("<?").append(this.instruction).append(" ?>");
    }

    public String getText() {
        return "";
    }

    public int getType() {
        return 4;
    }

    void repack() {
        this.instruction = new String(this.instruction.toCharArray());
    }

    public String getInstruction() {
        return this.instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}
