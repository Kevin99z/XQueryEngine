package XQueryEngine.XQueryParser;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class XQueryVisitorImpl extends XQueryBaseVisitor<ArrayList<Node>> {
    ArrayList<Node> curNodes = new ArrayList<>(); // The current nodes to visit
    HashMap<String, ArrayList<Node>> variables = new HashMap<>(); // The variables defined in the query
    boolean debug;

    public XQueryVisitorImpl(boolean debug) {
        this.debug = debug;
    }

    @Override
    public ArrayList<Node> visitXqComma(XQueryParser.XqCommaContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> xq1 = this.visit(ctx.xq(0));
        this.variables = currentVariables;
        ArrayList<Node> xq2 = this.visit(ctx.xq(1));
        ret.addAll(xq1);
        ret.addAll(xq2);
        return ret;
    }

    @Override
    public ArrayList<Node> visitXqAp(XQueryParser.XqApContext ctx) {
        return this.visit(ctx.ap());
    }

    @Override
    public ArrayList<Node> visitXqVar(XQueryParser.XqVarContext ctx) {
        ArrayList<Node> var = this.variables.get(ctx.Var().getText());
        if (var == null) {
            return new ArrayList<>();
        }
        return var;
    }

    @Override
    public ArrayList<Node> visitXqDblSlashRp(XQueryParser.XqDblSlashRpContext ctx) {
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> xq = this.visit(ctx.xq());
        for (Node node : xq) {
            ret.addAll(this.getDescendants(node));
        }
        this.curNodes = ret;
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitXqParens(XQueryParser.XqParensContext ctx) {
        return this.visit(ctx.xq());
    }

    private void visitXqFLWRHelper(XQueryParser.XqFLWRContext ctx, int index, ArrayList<Node> ret) {
        if(index == ctx.forClause().xq().size()) {
            if (ctx.letClause() != null) {
                this.visit(ctx.letClause());
            }
            if (ctx.whereClause() != null) {
                ArrayList<Node> where = this.visit(ctx.whereClause());
                if (where.size() > 0) {
                    ret.addAll(this.visit(ctx.returnClause()));
                }
            } else {
                ret.addAll(this.visit(ctx.returnClause()));
            }
        } else {
            String varName = ctx.forClause().Var(index).getText();
            ArrayList<Node> varValue = this.visit(ctx.forClause().xq(index));
            for(Node node: varValue) {
                this.variables.put(varName, new ArrayList<Node>(){{add(node);}});
                this.visitXqFLWRHelper(ctx, index + 1, ret);
            }
        }
    }
    @Override
    public ArrayList<Node> visitXqFLWR(XQueryParser.XqFLWRContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        // do a dfs over vars in for clause
        this.visitXqFLWRHelper(ctx, 0, ret);
        this.variables = currentVariables;
        return ret;
    }

    @Override
    public ArrayList<Node> visitXqSlashRp(XQueryParser.XqSlashRpContext ctx) {
        curNodes = this.visit(ctx.xq());
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitXqString(XQueryParser.XqStringContext ctx) {
        String text = ctx.STRING().getText();
        text = text.substring(1, text.length() - 1);
        ArrayList<Node> ret = new ArrayList<>();
        //create a text node and return
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            ret.add(doc.createTextNode(text));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitXqTag(XQueryParser.XqTagContext ctx) {
        ArrayList<Node> xq = this.visit(ctx.xq());
        String tagName = ctx.NAME(0).getText();
        ArrayList<Node> ret = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Node tag = doc.createElement(tagName);
            for(Node node: xq) {
                tag.appendChild(doc.importNode(node, true));
            }
            ret.add(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitXqLet(XQueryParser.XqLetContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        this.visit(ctx.letClause());
        ArrayList<Node> ret = this.visit(ctx.xq());
        this.variables = currentVariables;
        return ret;
    }

    @Override
    public ArrayList<Node> visitForClause(XQueryParser.ForClauseContext ctx) {
        return null;
    }

    @Override
    public ArrayList<Node> visitLetClause(XQueryParser.LetClauseContext ctx) {
        for(int i = 0; i < ctx.Var().size(); i++) {
            String varName = ctx.Var(i).getText();
            ArrayList<Node> varValue = this.visit(ctx.xq(i));
            this.variables.put(varName, varValue);
        }
        return null;
    }

    @Override
    public ArrayList<Node> visitWhereClause(XQueryParser.WhereClauseContext ctx) {
        return this.visit(ctx.cond());
    }

    @Override
    public ArrayList<Node> visitReturnClause(XQueryParser.ReturnClauseContext ctx) {
        return this.visit(ctx.xq());
    }

    @Override
    public ArrayList<Node> visitCondOr(XQueryParser.CondOrContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> cond1 = this.visit(ctx.cond(0));
        this.variables = currentVariables;
        ArrayList<Node> cond2 = this.visit(ctx.cond(1));
        if (cond1.size() > 0 || cond2.size() > 0) {
            ret.add(null); // specifies that the condition is true
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondAnd(XQueryParser.CondAndContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> cond1 = this.visit(ctx.cond(0));
        this.variables = currentVariables;
        ArrayList<Node> cond2 = this.visit(ctx.cond(1));
        if (cond1.size() > 0 && cond2.size() > 0) {
            ret.add(null); // specifies that the condition is true
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondEmpty(XQueryParser.CondEmptyContext ctx) {
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> xq = this.visit(ctx.xq());
        if (xq.size() == 0) {
            ret.add(null); // specifies that the condition is true
        }
        return ret;
    }

    private boolean visitCondSomeHelper(XQueryParser.CondSomeContext ctx, int index) {
        if(index == ctx.Var().size()) {
            return this.visit(ctx.cond()).size() > 0;
        } else {
            String varName = ctx.Var(index).getText();
            ArrayList<Node> varValue = this.visit(ctx.xq(index));
            for(Node node: varValue) {
                this.variables.put(varName, new ArrayList<Node>(){{add(node);}});
                if(this.visitCondSomeHelper(ctx, index + 1)) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public ArrayList<Node> visitCondSome(XQueryParser.CondSomeContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        boolean isTrue = this.visitCondSomeHelper(ctx, 0);
        this.variables = currentVariables;
        ArrayList<Node> ret = new ArrayList<>();
        if(isTrue) {
            ret.add(null);
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondIs(XQueryParser.CondIsContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> xq1 = this.visit(ctx.xq(0));
        this.variables = currentVariables;
        ArrayList<Node> xq2 = this.visit(ctx.xq(1));
        for(Node n1: xq1) {
            for(Node n2: xq2) {
                if(n1.isSameNode(n2)) {
                    ret.add(null);
                    return ret;
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondNot(XQueryParser.CondNotContext ctx) {
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> cond = this.visit(ctx.cond());
        if (cond.size() == 0) {
            ret.add(null); // specifies that the condition is true
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondEq(XQueryParser.CondEqContext ctx) {
        HashMap<String, ArrayList<Node>> currentVariables = new HashMap<>(this.variables);
        ArrayList<Node> ret = new ArrayList<>();
        ArrayList<Node> xq1 = this.visit(ctx.xq(0));
        this.variables = currentVariables;
        ArrayList<Node> xq2 = this.visit(ctx.xq(1));
        for(Node n1: xq1) {
            for(Node n2: xq2) {
                if(n1.isEqualNode(n2)) {
                    ret.add(null);
                    return ret;
                }
            }
        }
        return ret;
    }

    @Override
    public ArrayList<Node> visitCondParens(XQueryParser.CondParensContext ctx) {
        return this.visit(ctx.cond());
    }

    //copied from XPathVisitorImpl
    @Override
    public ArrayList<Node> visitFileName(XQueryParser.FileNameContext ctx) {
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
    public ArrayList<Node> visitApChild(XQueryParser.ApChildContext ctx) { // ap -> doc(fileName) / rp
        try {
            this.curNodes = this.visit(ctx.fileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitApDescendent(XQueryParser.ApDescendentContext ctx) { // ap -> doc(fileName) // rp
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
    public ArrayList<Node> visitRpChild(XQueryParser.RpChildContext ctx) { // rp -> rp1/rp2
        this.curNodes = this.visit(ctx.rp(0));
        return this.visit(ctx.rp(1));
    }

    @Override
    public ArrayList<Node> visitRpDescendant(XQueryParser.RpDescendantContext ctx) { // rp -> rp1 // rp2
        this.curNodes = this.visit(ctx.rp(0));
        ArrayList<Node> ret = new ArrayList<>();
        for (Node node : this.curNodes) {
            ret.addAll(this.getDescendants(node));
        }
        this.curNodes = ret;
        return this.visit(ctx.rp(1));
    }

    @Override
    public ArrayList<Node> visitRpAttName(XQueryParser.RpAttNameContext ctx) { // rp -> @attName
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
    public ArrayList<Node> visitParenthesizedRp(XQueryParser.ParenthesizedRpContext ctx) { // rp -> (rp)
        return this.visit(ctx.rp());
    }

    @Override
    public ArrayList<Node> visitWildcard(XQueryParser.WildcardContext ctx) { // rp -> *
        LinkedHashSet<Node> ret = new LinkedHashSet<>();
        for (Node node : this.curNodes) {
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                ret.add(childNodes.item(i));
            }
        }
        return new ArrayList<>(ret);
    }

    @Override
    public ArrayList<Node> visitParentNode(XQueryParser.ParentNodeContext ctx) { // rp -> ..
        LinkedHashSet<Node> ret = new LinkedHashSet<>();
        for (Node node : this.curNodes) {
            Node parent = node.getParentNode();
            if (parent != null) {
                ret.add(parent);
            }
        }
        return new ArrayList<>(ret);
    }

    @Override
    public ArrayList<Node> visitRpTagName(XQueryParser.RpTagNameContext ctx) { // rp -> tagName
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
    public ArrayList<Node> visitCurrentNode(XQueryParser.CurrentNodeContext ctx) { // rp -> .
        return new ArrayList<>(this.curNodes);
    }



    @Override
    public ArrayList<Node> visitRpConcatenate(XQueryParser.RpConcatenateContext ctx) { // rp -> rp1,rp2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> rp1 = this.visit(ctx.rp(0));
        this.curNodes = curNodesCopy;
        ArrayList<Node> rp2 = this.visit(ctx.rp(1));
        rp1.addAll(rp2);
        return rp1;
    }

    @Override
    public ArrayList<Node> visitText(XQueryParser.TextContext ctx) { // rp -> text()
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
    public ArrayList<Node> visitRpFilter(XQueryParser.RpFilterContext ctx) { // rp -> rp[f]
        this.curNodes = this.visit(ctx.rp());
        return this.visit(ctx.f());
    }

    @Override
    public ArrayList<Node> visitFilterIs(XQueryParser.FilterIsContext ctx) { // f -> rp1 is rp2
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
    public ArrayList<Node> visitFilterEqual(XQueryParser.FilterEqualContext ctx) { // f -> rp1 = rp2
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
    public ArrayList<Node> visitFilterNot(XQueryParser.FilterNotContext ctx) {
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
    public ArrayList<Node> visitFilterStringEqual(XQueryParser.FilterStringEqualContext ctx) { // f -> rp = StringConstant
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
    public ArrayList<Node> visitParenthesizedF(XQueryParser.ParenthesizedFContext ctx) { // f -> (f)
        return this.visit(ctx.f());
    }

    @Override
    public ArrayList<Node> visitFilterOr(XQueryParser.FilterOrContext ctx) { // f -> f1 or f2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> f1 = this.visit(ctx.f(0));
        this.curNodes = curNodesCopy;
        ArrayList<Node> f2 = this.visit(ctx.f(1));
        f1.addAll(f2);
        return f1;
    }

    @Override
    public ArrayList<Node> visitFilterAnd(XQueryParser.FilterAndContext ctx) { // f -> f1 and f2
        ArrayList<Node> curNodesCopy = new ArrayList<>(this.curNodes);
        ArrayList<Node> f1 = this.visit(ctx.f(0));
        this.curNodes = curNodesCopy;
        ArrayList<Node> f2 = this.visit(ctx.f(1));
        f1.retainAll(f2);
        return f1;
    }

    @Override
    public ArrayList<Node> visitFilterRp(XQueryParser.FilterRpContext ctx) { // f -> rp
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
