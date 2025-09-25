package com.bizflow.bas.webmaker;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlBindingFinder {

    private static final String FM_NAMESPACE = "http://www.hyfinity.com/formmaker";

    /**
     * Finds mapping information in XML binding files
     *
     * @param xmlFile The XML binding file to parse
     * @return A list of mapping strings
     */
    public List<String> findMappings(File xmlFile)
            throws ParserConfigurationException, IOException, SAXException {

        System.out.println("Parsing bindings file: " + xmlFile.getName());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);

        // Find all element nodes (they contain the mapping info)
        NodeList elementNodes = document.getElementsByTagNameNS(FM_NAMESPACE, "element");
        if (elementNodes.getLength() == 0) {
            // Try with default namespace if no elements found with FM namespace
            System.out.println("No elements found with FM namespace, trying default namespace");
            elementNodes = document.getElementsByTagName("element");
        }

        System.out.println("Found " + elementNodes.getLength() + " element nodes in " + xmlFile.getName());
        List<String> mappings = new ArrayList<>();

        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element elementNode = (Element) elementNodes.item(i);
            String elementName = elementNode.getAttribute("name");

            // Extract various XPath information
            String transformXpath = getChildElementTextWithMixedNamespace(elementNode, "transform_xpath");
            String valueXpath = getChildElementTextWithMixedNamespace(elementNode, "value_xpath");
            String textXpath = getChildElementTextWithMixedNamespace(elementNode, "text_xpath");
            String repeatXpath = getChildElementTextWithMixedNamespace(elementNode, "repeat_xpath");
            String xformXpath = null;

            // Look for xform_xpath in the fm:action element
            NodeList actionNodes = elementNode.getElementsByTagNameNS(FM_NAMESPACE, "action");
            if (actionNodes.getLength() > 0) {
                Element actionNode = (Element) actionNodes.item(0);
                // Check for xform_xpath with or without namespace
                NodeList xformNodes = actionNode.getElementsByTagNameNS(FM_NAMESPACE, "xform_xpath");
                if (xformNodes.getLength() > 0) {
                    xformXpath = xformNodes.item(0).getTextContent();
                } else {
                    NodeList noNsXformNodes = actionNode.getElementsByTagName("xform_xpath");
                    if (noNsXformNodes.getLength() > 0) {
                        xformXpath = noNsXformNodes.item(0).getTextContent();
                    }
                }
            }

            // Check if any XPath contains "ProcessVariables"
            boolean containsProcessVariables =
                    (transformXpath != null && transformXpath.contains("ProcessVariables")) ||
                            (valueXpath != null && valueXpath.contains("ProcessVariables")) ||
                            (textXpath != null && textXpath.contains("ProcessVariables")) ||
                            (repeatXpath != null && repeatXpath.contains("ProcessVariables")) ||
                            (xformXpath != null && xformXpath.contains("ProcessVariables"));

            if (containsProcessVariables) {
                System.out.println("Processing element: " + elementName + " (contains ProcessVariables)");
            } else {
                System.out.println("Processing element: " + elementName);
            }

            // Build mapping information string
            StringBuilder mappingInfo = new StringBuilder();
            mappingInfo.append("Element: ").append(elementName).append("\n");

            if (xformXpath != null && !xformXpath.isEmpty()) {
                mappingInfo.append("  Form XPath: ").append(xformXpath).append("\n");
                if (xformXpath.contains("ProcessVariables")) {
                    System.out.println("  Form XPath contains ProcessVariables: " + xformXpath);
                }
            }

            if (transformXpath != null && !transformXpath.isEmpty()) {
                mappingInfo.append("  Transform XPath: ").append(transformXpath).append("\n");
                if (transformXpath.contains("ProcessVariables")) {
                    System.out.println("  Transform XPath contains ProcessVariables: " + transformXpath);
                }
            }

            if (valueXpath != null && !valueXpath.isEmpty()) {
                mappingInfo.append("  Value XPath: ").append(valueXpath).append("\n");
                if (valueXpath.contains("ProcessVariables")) {
                    System.out.println("  Value XPath contains ProcessVariables: " + valueXpath);
                }
            }

            if (textXpath != null && !textXpath.isEmpty()) {
                mappingInfo.append("  Text XPath: ").append(textXpath).append("\n");
                if (textXpath.contains("ProcessVariables")) {
                    System.out.println("  Text XPath contains ProcessVariables: " + textXpath);
                }
            }

            if (repeatXpath != null && !repeatXpath.isEmpty()) {
                mappingInfo.append("  Repeat XPath: ").append(repeatXpath).append("\n");
                if (repeatXpath.contains("ProcessVariables")) {
                    System.out.println("  Repeat XPath contains ProcessVariables: " + repeatXpath);
                }
            }

            if (mappingInfo.length() > 0) {
                mappings.add(mappingInfo.toString());
            }
        }

        System.out.println("Finished parsing " + xmlFile.getName() + ", found " + mappings.size() + " mappings");
        return mappings;
    }

    /**
     * Gets text content from a child element, handling mixed namespace scenarios
     */
    private String getChildElementTextWithMixedNamespace(Element parent, String localName) {
        // First try with FM namespace
        NodeList childNodes = parent.getElementsByTagNameNS(FM_NAMESPACE, localName);
        if (childNodes.getLength() > 0) {
            return childNodes.item(0).getTextContent();
        }

        // Then try without namespace
        childNodes = parent.getElementsByTagName(localName);
        if (childNodes.getLength() > 0) {
            return childNodes.item(0).getTextContent();
        }

        return null;
    }
}