package org.jmaa.sdk.data.xml;

import java.nio.charset.Charset;
import java.util.Properties;

public class Configuration {
    private Charset charset;
    protected boolean nullableOnForEach;
    protected Properties variables = new Properties();

    public Configuration() {
        this(Charset.defaultCharset());
    }

    public Configuration(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isNullableOnForEach() {
        return nullableOnForEach;
    }

    public void setNullableOnForEach(boolean nullableOnForEach) {
        this.nullableOnForEach = nullableOnForEach;
    }

    public Properties getVariables() {
        return variables;
    }

    public void setVariables(Properties variables) {
        this.variables = variables;
    }
}
