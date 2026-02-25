package org.jmaa.sdk.xml;

import org.dom4j.DocumentFactory;
import org.dom4j.ElementHandler;
import org.dom4j.io.SAXContentHandler;
import org.xml.sax.Locator;

public class XmlContentHandler extends SAXContentHandler {

    private Locator locator;

    // this is already in SAXContentHandler, but private
    private DocumentFactory documentFactory;

    public XmlContentHandler(DocumentFactory documentFactory, ElementHandler elementHandler) {
        super(documentFactory, elementHandler);
        this.documentFactory = documentFactory;
    }

    @Override
    public void setDocumentLocator(Locator documentLocator) {
        super.setDocumentLocator(documentLocator);
        this.locator = documentLocator;
        if (documentFactory instanceof XmlDocumentFactory) {
            ((XmlDocumentFactory) documentFactory).setLocator(documentLocator);
        }

    }

    public Locator getLocator() {
        return locator;
    }
}
