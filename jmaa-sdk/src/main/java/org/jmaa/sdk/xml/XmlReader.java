package org.jmaa.sdk.xml;

import org.jmaa.sdk.exceptions.PlatformException;
import org.dom4j.DocumentFactory;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.Reader;
import java.net.URL;
import java.io.InputStream;

import java.io.File;

/**
 * Xml文档读取，可以定位元素行号
 */
public class XmlReader extends SAXReader {
    @Override
    protected SAXContentHandler createContentHandler(XMLReader reader) {
        return new XmlContentHandler(getDocumentFactory(), getDispatchHandler());
    }

    @Override
    public void setDocumentFactory(DocumentFactory documentFactory) {
        super.setDocumentFactory(documentFactory);
    }
    public XmlDocument readXml(File file) {
        try {
            return (XmlDocument) read(file);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(URL url) {
        try {
            return (XmlDocument) read(url);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(String systemId) {
        try {
            return (XmlDocument) read(systemId);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(InputStream in) {
        try {
            return (XmlDocument) read(in);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(Reader read) {
        try {
            return (XmlDocument) read(read);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(InputStream in, String systemId) {
        try {
            return (XmlDocument) read(in);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(Reader read, String systemId) {
        try {
            return (XmlDocument) read(read);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public XmlDocument readXml(InputSource in) {
        try {
            return (XmlDocument) read(in);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }
}
