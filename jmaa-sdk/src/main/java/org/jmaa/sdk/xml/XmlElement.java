package org.jmaa.sdk.xml;

import org.jmaa.sdk.exceptions.ValueException;
import org.dom4j.Attribute;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.BackedList;
import org.dom4j.tree.DefaultElement;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

/**
 * An Element that is aware of it location (line number in) in the source
 * document
 */
public class XmlElement extends DefaultElement {

    private int lineNumber = -1;

    public XmlElement(QName qname) {
        super(qname);
    }

    public XmlElement(QName qname, int attributeCount) {
        super(qname, attributeCount);
    }

    public XmlElement(String name, Namespace namespace) {
        super(name, namespace);
    }

    public XmlElement(String name) {
        super(name);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public XmlElement element(String name) {
        for (Node node : contentList()) {
            if (node instanceof XmlElement) {
                XmlElement element = (XmlElement) node;
                if (name.equals(element.getName())) {
                    return element;
                }
            }
        }
        return null;
    }

    public XmlElement element(QName qName) {
        for (Node node : contentList()) {
            if (node instanceof XmlElement) {
                XmlElement element = (XmlElement) node;
                if (qName.equals(element.getQName())) {
                    return element;
                }
            }
        }
        return null;
    }

    public XmlElement element(String name, Namespace namespace) {
        return this.element(this.getDocumentFactory().createQName(name, namespace));
    }

    public List<XmlElement> xmlElements() {
        BackedList<XmlElement> answer = this.createResultList();
        for (Node node : contentList()) {
            if (node instanceof XmlElement) {
                answer.addLocal((XmlElement) node);
            }
        }
        return answer;
    }

    public List<XmlElement> xmlElements(String name) {
        BackedList<XmlElement> answer = this.createResultList();
        for (Node node : contentList()) {
            if (node instanceof XmlElement) {
                XmlElement element = (XmlElement) node;
                if (name.equals(element.getName())) {
                    answer.addLocal(element);
                }
            }
        }
        return answer;
    }

    public List<XmlElement> xmlElements(QName qName) {
        BackedList<XmlElement> answer = this.createResultList();
        for (Node node : contentList()) {
            if (node instanceof XmlElement) {
                XmlElement element = (XmlElement) node;
                if (qName.equals(element.getQName())) {
                    answer.addLocal(element);
                }
            }
        }
        return answer;
    }

    public List<XmlElement> xmlElements(String name, Namespace namespace) {
        return this.xmlElements(this.getDocumentFactory().createQName(name, namespace));
    }

    public Iterator<XmlElement> xmlElementIterator() {
        List<XmlElement> list = this.xmlElements();
        return list.iterator();
    }

    public Iterator<XmlElement> xmlElementIterator(String name) {
        List<XmlElement> list = this.xmlElements(name);
        return list.iterator();
    }

    public Iterator<XmlElement> xmlElementIterator(QName qName) {
        List<XmlElement> list = this.xmlElements(qName);
        return list.iterator();
    }

    public Iterator<XmlElement> xmlElementIterator(String name, Namespace ns) {
        return this.xmlElementIterator(this.getDocumentFactory().createQName(name, ns));
    }

    /**
     * 指定输出格式
     *
     * @param format
     * @return
     */
    public String asXML(OutputFormat format) {
        try {
            StringWriter out = new StringWriter();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(this);
            writer.flush();
            return out.toString();
        } catch (IOException var3) {
            throw new RuntimeException("IOException while generating textual representation: " + var3.getMessage());
        }
    }

    /**
     * 获取属性值，如果指定名称不存在，抛出 {@link ValueException}
     *
     * @param name
     * @return
     */
    public String getAttribute(String name) {
        Attribute attr = attribute(name);
        if (attr == null) {
            throw new ValueException(String.format("未指定%s属性", name));
        }
        return attr.getText();
    }

    /**
     * 获取属性值，如果指定名称不存在，返回 defaultValue
     *
     * @param name
     * @param defaultValue
     * @return
     */
    public String getAttributeOr(String name, String defaultValue) {
        Attribute attr = attribute(name);
        if (attr == null) {
            return defaultValue;
        }
        return attr.getText();
    }
}
