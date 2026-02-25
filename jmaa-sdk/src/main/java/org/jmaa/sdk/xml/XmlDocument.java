package org.jmaa.sdk.xml;

import org.dom4j.Element;
import org.dom4j.tree.DefaultDocument;

public class XmlDocument extends DefaultDocument {
    public XmlElement getRootXmlElement() {
        return (XmlElement) getRootElement();
    }
}
