package no.difi.asic;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static javax.xml.xpath.XPathConstants.NODESET;

public class DomUtils {

    private static final DocumentBuilderFactory documentBuilderFactory;
    private static final TransformerFactory transformerFactory;

    static {
        transformerFactory = TransformerFactory.newInstance();
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }

    private DomUtils() {
    }

    public static Document newEmptyXmlDocument() {
        try {
            return documentBuilderFactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create new Document. ", e);
        }
    }

    public static Stream<Node> allNodesBelow(Node node) {
        try {
            NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().evaluate(". | .//node() | .//@*", node, NODESET);
            return IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("allNodesBelow failed!", e);
        }
    }

    public static byte[] serializeToXml(Node root) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(root), new StreamResult(outputStream));
            return outputStream.toByteArray();
        } catch (TransformerException | IOException e) {
            throw new IllegalStateException("Unable to serialize XML", e);
        }
    }
}