package com.bizflow.bas.webmaker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XmlElementFinder {

    /**
     * Finds elements in an XML file that match the condition <hy:target action="Query"
     *
     * @param xmlFile The XML file to parse
     * @return A list of matching elements as strings (including all child elements)
     */
    public List<String> findMatchingElements(File xmlFile)
            throws ParserConfigurationException, IOException, SAXException, TransformerException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Enable namespace support
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);

        // Define the namespace URI for "hy"
        String hyNamespaceURI = "http://www.hyfinity.com/xengine";

        // Find all hy:target elements
        NodeList targetElements = document.getElementsByTagNameNS(hyNamespaceURI, "target");

        List<String> matchingElements = new ArrayList<>();

        // Check each target element
        for (int i = 0; i < targetElements.getLength(); i++) {
            Element targetElement = (Element) targetElements.item(i);
            String action = targetElement.getAttribute("action");

            // Check if the action is "Query"
            if ("Query".equals(action)) {
                // Check if it has <hy:params><hy:param name="sql_statement"> child
                boolean hasSqlStatementParam = hasSqlStatementParam(targetElement, hyNamespaceURI);

                if (hasSqlStatementParam) {
                    // Convert the element to a string representation
                    String elementString = nodeToString(targetElement);
                    matchingElements.add(elementString);
                }
            }
        }

        return matchingElements;
    }

    /**
     * Checks if an element has a child structure of <hy:params><hy:param name="sql_statement">
     */
    private boolean hasSqlStatementParam(Element element, String hyNamespaceURI) {
        NodeList paramsNodeList = element.getElementsByTagNameNS(hyNamespaceURI, "params");

        for (int i = 0; i < paramsNodeList.getLength(); i++) {
            Element paramsElement = (Element) paramsNodeList.item(i);
            NodeList paramNodeList = paramsElement.getElementsByTagNameNS(hyNamespaceURI, "param");

            for (int j = 0; j < paramNodeList.getLength(); j++) {
                Element paramElement = (Element) paramNodeList.item(j);
                if ("sql_statement".equals(paramElement.getAttribute("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Converts a Node to its string representation.
     */
    private String nodeToString(Node node) throws TransformerException {
        StringWriter writer = new StringWriter();

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();

        // Set output properties to pretty-print the XML
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(node), new StreamResult(writer));

        return writer.toString();
    }
}