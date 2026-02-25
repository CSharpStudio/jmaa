package org.jmaa.sdk;

import org.jmaa.sdk.core.BaseModel;

/**
 * @author Eric Liang
 */
public class TransientModel extends BaseModel {
    public TransientModel() {
        isAuto = true;
        isAbstract = false;
        isTransient = true;
    }
}
