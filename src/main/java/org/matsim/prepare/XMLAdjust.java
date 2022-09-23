package org.matsim.prepare;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XMLAdjust {

    public static void main(String[] args) throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setExpandEntityReferences(false);

        Document doc = factory.newDocumentBuilder().parse(new File("scenarios/berlin-v5.5-1pct/input/berlin-v5.5-1pct.plans.xml"));

        removeAll(doc, Node.ELEMENT_NODE, "route");

        //removeAll(doc, Node.COMMENT_NODE, null);

        //doc.normalize();

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(new File("output.xml"));
        Source input = new DOMSource(doc);

        transformer.transform(input, output);
    }

    public static void removeAll(Node node, short nodeType, String name) {
        if (node.getNodeType() == nodeType && (name == null || node.getNodeName().equals(name))) {
            node.getParentNode().removeChild(node);
        } else {
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                removeAll(list.item(i), nodeType, name);
            }
        }
    }
}
