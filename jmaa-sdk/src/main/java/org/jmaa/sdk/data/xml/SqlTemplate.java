package org.jmaa.sdk.data.xml;

import org.jmaa.sdk.data.SqlFormat;

public interface SqlTemplate {
    SqlFormat process(Object data);
}
