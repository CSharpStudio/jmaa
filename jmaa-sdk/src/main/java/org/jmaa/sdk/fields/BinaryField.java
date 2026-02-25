package org.jmaa.sdk.fields;

import org.jmaa.sdk.core.Constants;

/**
 * 二进制
 *
 * @author Eric Liang
 */
public class BinaryField extends BinaryBaseField<BinaryField> {
    public BinaryField() {
        type = Constants.BINARY;
        limit = 5;
    }
}
