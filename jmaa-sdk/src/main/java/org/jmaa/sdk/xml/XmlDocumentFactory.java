package org.jmaa.sdk.xml;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xml.sax.Locator;

public class XmlDocumentFactory extends DocumentFactory {

    private Locator locator;

    public XmlDocumentFactory() {
        super();
    }

    public void setLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public Document createDocument() {
        XmlDocument answer = new XmlDocument();
        answer.setDocumentFactory(this);
        return answer;
    }

    @Override
    public Element createElement(QName qname) {
        XmlElement element = new XmlElement(qname);
        if (locator != null) {
            element.setLineNumber(locator.getLineNumber());
        }
        return element;
    }
}
