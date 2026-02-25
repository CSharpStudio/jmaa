package org.jmaa.sdk.util;

/**
 * @author Eric Liang
 */
public class TextBuilder {
    StringBuilder sb = new StringBuilder();

    public TextBuilder append(Object obj) {
        sb.append(obj);
        return this;
    }

    public TextBuilder appendLine(Object obj) {
        append(obj);
        return appendLine();
    }

    public TextBuilder appendLine() {
        sb.append("\r\n");
        return this;
    }

    public TextBuilder append(String str, Object... args) {
        if (str != null) {
            if (args.length > 0) {
                sb.append(String.format(str, args));
            } else {
                sb.append(str);
            }
        }
        return this;
    }

    public TextBuilder appendLine(String str, Object... args) {
        append(str, args);
        return appendLine();
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
