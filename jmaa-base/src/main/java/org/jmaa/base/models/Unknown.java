package org.jmaa.base.models;

import org.jmaa.sdk.Model;
import org.jmaa.sdk.AbstractModel;

/**
 * Abstract model used as a substitute for relational fields with an unknown
 * model_relate.
 *
 * @author Eric Liang
 */
@Model.Meta(name = "_unknown", description = "Unknown")
public class Unknown extends AbstractModel {

}
