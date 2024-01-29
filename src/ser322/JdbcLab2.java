package ser322;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class JdbcLab2 {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("USAGE: java ser322.JdbcLab2 <deptNo>");
            System.exit(0);
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        InputSource source = new InputSource(new FileReader("xmlfiles/completeData.xml"));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(source);

        Element root = doc.getDocumentElement();
        root.normalize();

        NodeList products = (NodeList) xpath.evaluate("//products/product[@made_by="+ args[0] + "]/descrip"
                , root, XPathConstants.NODESET);

        for (int i=0; i < products.getLength(); i++) {
            Node productNode = (Node) products.item(i);
            Node productDescription = (Node)xpath.evaluate("text()", productNode, XPathConstants.NODE);
            System.out.println("Product (XPath): " + productDescription.getNodeValue());
        }
    }
}
