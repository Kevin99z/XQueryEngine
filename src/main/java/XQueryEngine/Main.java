package XQueryEngine;
import XQueryEngine.XPathParser.XPathLexer;
import XQueryEngine.XPathParser.XPathParser;
import XQueryEngine.XPathParser.XPathVisitorImpl;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;


public class Main {
    public static void main(String[] args) {
        String inputFile = args[0], outputFile = args[1];

        try {
            ArrayList<Node> nodes = executeQuery(inputFile);
            writeNodesToXml(nodes, outputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static ArrayList<Node> executeQuery(String query) throws IOException {
        CharStream lexerInput = CharStreams.fromFileName(query);
        XPathLexer lexer = new XPathLexer(lexerInput);
        XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
        XPathParser.ApContext ap = parser.ap();
        XPathVisitorImpl visitor = new XPathVisitorImpl(true);
        ArrayList<Node> nodes = visitor.visit(ap);
        // sort nodes by document order
        nodes.sort((n1, n2) -> {
            int c = n1.compareDocumentPosition(n2);
            if ((c & Node.DOCUMENT_POSITION_FOLLOWING) != 0) {
                return -1;
            } else if ((c & Node.DOCUMENT_POSITION_PRECEDING) != 0) {
                return 1;
            } else {
                return 0;
            }
        });
        return nodes;
    }


    public static void writeNodesToXml(List<Node> nodes, String outputFile) {
        try {
            // Create TransformerFactory and Transformer to write to a file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Set some output properties for pretty print
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");

            // skip xml header
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringBuilder output = new StringBuilder("<result>\n");

            for (Node node : nodes) {
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(node), new StreamResult(writer));
                output.append(writer);
                output.append('\n');
            }
            output.append("</result>");

            // Write the result to a file
            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                fileWriter.write(output.toString());
            }

            System.out.println("XML file created successfully at " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}