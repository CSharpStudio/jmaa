package org.jmaa.sdk.data.xml.tags;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;

public class XMLEntityResolver implements EntityResolver {
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return new InputSource(XMLEntityResolver.class.getResourceAsStream("script-1.0.dtd"));
    }
}
