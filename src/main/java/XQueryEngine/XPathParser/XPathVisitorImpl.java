package XQueryEngine.XPathParser;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class XPathVisitorImpl extends XPathBaseVisitor<ArrayList<Node>> {
    ArrayList<Node> curNodes = new ArrayList<>(); // The current nodes to visit
    boolean debug;
    public XPathVisitorImpl(boolean debug) {
        this.debug = debug;
    }
    @Override
    public ArrayList<Node> visitFileName(XPathParser.FileNameContext ctx) {
        // parse the xml file into dom tree
        String fileName = ctx.getText();
        fileName = fileName.substring(1, fileName.length() - 1);
        ArrayList<Node> ret = new ArrayList<>();

        try {
            File xmlFile = new File(fileName);
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();
            // convert Document into Node
            ret.add(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitApChild(XPathParser.ApChildContext ctx) { // ap -> doc(fileName) / rp
        try {
            this.curNodes = this.visit(ctx.fileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitApDescendent(XPathParser.ApDescendentContext ctx) { // ap -> doc(fileName) // rp
        try {
            this.curNodes = this.visit(ctx.fileName());
            Node parent = this.curNodes.get(0);
            this.curNodes = this.getDescendants(parent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.visit(ctx.rp());
    }

    private ArrayList<Node> getDescendants(Node parent) {
        LinkedHashSet<Node> ret = new LinkedHashSet<>();
        ret.add(parent);
        Node child = parent.getFirstChild();
        while (child != null) {
            ret.addAll(this.getDescendants(child));
            child = child.getNextSibling();
        }
        return new ArrayList<>(ret);
    }

    @Override
    public ArrayList<Node> visitRpChild(XPathParser.RpChildContext ctx) { // rp -> rp1/rp2
        this.curNodes = this.visit(ctx.rp(0));
        return this.visit(ctx.rp(1));
    }

    @Override
    public ArrayList<Node> visitRpDescendant(XPathParser.RpDescendantContext ctx) { // rp -> rp1 // rp2
        this.curNodes = this.visit(ctx.rp(0));
        ArrayList<Node> ret = new ArrayList<>();
        for (Node node : this.curNodes) {
            ret.addAll(this.getDescendants(node));
        }
        this.curNodes = ret;
        return this.visit(ctx.rp(1));
    }

    @Override
    public ArrayList<Node> visitRpAttName(XPathParser.RpAttNameContext ctx) { // rp -> @attName
        ArrayList<Node> ret = new ArrayList<>();
        String attName = ctx.attName().getText().substring(1);
        for (Node node : this.curNodes) {
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                Node att = attributes.getNamedItem(attName);
                if (att != null) {
                    ret.add(att);
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitParenthesizedRp(XPathParser.ParenthesizedRpContext ctx) { // rp -> (rp)
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitWildcard(XPathParser.WildcardContext ctx) { // rp -> *
        ArrayList<Node> ret = new ArrayList<>();
        for (Node node : this.curNodes) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                ret.add(childNodes.item(i));
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitParentNode(XPathParser.ParentNodeContext ctx) { // rp -> ..
        ArrayList<Node> ret = new ArrayList<>();
        for (Node node : this.curNodes) {
            Node parent = node.getParentNode();
            if (parent != null) {
                ret.add(parent);
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitRpTagName(XPathParser.RpTagNameContext ctx) { // rp -> tagName
        ArrayList<Node> ret = new ArrayList<>();
        String tagName = ctx.tagName().getText();
        for (Node node : this.curNodes) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeName().equals(tagName)) {
                    ret.add(child);
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCurrentNode(XPathParser.CurrentNodeContext ctx) { // rp -> .
        return new ArrayList<>(this.curNodes);
    }



    @Override
    public ArrayList<Node> visitRpConcatenate(XPathParser.RpConcatenateContext ctx) { // rp -> rp1,rp2
        ArrayList<Node> rp1 = this.visit(ctx.rp(0));
        ArrayList<Node> rp2 = this.visit(ctx.rp(1));
        rp1.addAll(rp2);
        return rp1;
    }

    @Override
    public ArrayList<Node> visitText(XPathParser.TextContext ctx) { // rp -> text()
        ArrayList<Node> ret = new ArrayList<>();
        for (Node node : this.curNodes) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    ret.add(child);
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitRpFilter(XPathParser.RpFilterContext ctx) { // rp -> rp[f]
        this.curNodes = this.visit(ctx.rp());
        return this.visit(ctx.f());
    }

    @Override
    public ArrayList<Node> visitFilterIs(XPathParser.FilterIsContext ctx) { // f -> rp1 is rp2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> ret = new ArrayList<>();
        for(Node node: curNodesCopy) {
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp1 = this.visit(ctx.rp(0));
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp2 = this.visit(ctx.rp(1));
            boolean isSame = false;
            for(Node n1: rp1) {
                for(Node n2: rp2) {
                    if(n1.isSameNode(n2)) {
                        ret.add(node);
                        isSame = true;
                        break;
                    }
                }
                if(isSame) {
                    break;
                }
            }
        }
        return ret;
    }
    @Override
    public ArrayList<Node> visitFilterEqual(XPathParser.FilterEqualContext ctx) { // f -> rp1 = rp2
        // make a copy of curNodes
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> ret = new ArrayList<>();
        for(Node node: curNodesCopy) {
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp1 = this.visit(ctx.rp(0));
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp2 = this.visit(ctx.rp(1));
            boolean isEqual = false;
            for(Node n1: rp1) {
                for(Node n2: rp2) {
                    if(n1.isEqualNode(n2)) {
                        ret.add(node);
                        isEqual = true;
                        break;
                    }
                }
                if(isEqual) {
                    break;
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitFilterNot(XPathParser.FilterNotContext ctx) {
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> f = this.visit(ctx.f());
        for(Node node: curNodesCopy) {
            if(!f.contains(node)) {
                ret.add(node);
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitFilterStringEqual(XPathParser.FilterStringEqualContext ctx) { // f -> rp = StringConstant
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        String stringConstant = ctx.STRING().getText();
        stringConstant = stringConstant.substring(1, stringConstant.length() - 1);
        for(Node node: curNodesCopy) {
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp = this.visit(ctx.rp());
            for(Node n: rp) {
                if(n.getNodeType() == Node.TEXT_NODE && n.getTextContent().equals(stringConstant)) {
                    ret.add(node);
                    break;
                }
            }
        }
        return ret;

    }

    @Override
    public ArrayList<Node> visitParenthesizedF(XPathParser.ParenthesizedFContext ctx) { // f -> (f)
        return this.visit(ctx.f());
    }

    @Override
    public ArrayList<Node> visitFilterOr(XPathParser.FilterOrContext ctx) { // f -> f1 or f2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> f1 = this.visit(ctx.f(0));
        this.curNodes = curNodesCopy;
        ArrayList<Node> f2 = this.visit(ctx.f(1));
        f1.addAll(f2);
        return f1;
    }

    @Override
    public ArrayList<Node> visitFilterAnd(XPathParser.FilterAndContext ctx) { // f -> f1 and f2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> f1 = this.visit(ctx.f(0));
        this.curNodes = curNodesCopy;
        ArrayList<Node> f2 = this.visit(ctx.f(1));
        f1.retainAll(f2);
        return f1;
    }

    @Override
    public ArrayList<Node> visitFilterRp(XPathParser.FilterRpContext ctx) { // f -> rp
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        for(Node node: curNodesCopy) {
            this.curNodes.clear();
            this.curNodes.add(node);
            ArrayList<Node> rp = this.visit(ctx.rp());
            if(rp.size() > 0) {
                ret.add(node);
            }
        }
        return ret;
    }


}
