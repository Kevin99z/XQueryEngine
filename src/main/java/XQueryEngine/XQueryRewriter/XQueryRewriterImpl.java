package XQueryEngine.XQueryRewriter;


import java.util.*;

public class XQueryRewriterImpl extends XQuerySubBaseVisitor<String> {
    LinkedHashMap<String, Integer> components = new LinkedHashMap<>();
    HashMap<String, String> paths = new HashMap<>(); // maps a variable to its path

    ArrayList<String> eqLeft = new ArrayList<>();
    ArrayList<String> eqRight = new ArrayList<>();

    HashMap<String, String> eqOthers = new HashMap<>(); // the other equal expressions that does not belongs to join arguments

    public int buildComponents(XQuerySubParser.XqContext ctx) {
        int compIndex = 0;
        // we assume that all variables are defined before it is referenced. so the component index will not change after it is assigned.
        for (int i = 0; i < ctx.Var().size(); i++) {
            String var = ctx.Var(i).getText();
            String path = ctx.path(i).getText();
            paths.put(var, path);
            if (path.startsWith("$")) {
                String parent = ctx.path(i).getChild(0).getText();
                components.put(var, components.get(parent));
            } else {
                components.put(var, compIndex++);
            }
        }
        return compIndex;
    }

    public ArrayList<String> getComponentVars(int compIndex) {
        ArrayList<String> ret = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : components.entrySet()) {
            String var = entry.getKey();
            Integer comp = entry.getValue();
            if (comp.equals(compIndex)) {
                ret.add(var);
            }
        }
        return ret;
    }

    public String buildForClause(int compIndex) {
        ArrayList<String> vars = getComponentVars(compIndex);
        StringBuilder sb = new StringBuilder();
        sb.append("for ");
        ArrayList<String> localVars = new ArrayList<>();
        ArrayList<String> conds = new ArrayList<>();
        for (String var : vars) {
            localVars.add(var);
            sb.append(var).append(" in ").append(paths.get(var)).append(",\n");
            if (eqOthers.containsKey(var)) {
                conds.add(eqOthers.get(var));
            }
        }
        if (compIndex == 0) { // handles the constant where clauses in the first for clause
            for (String key : eqOthers.keySet()) {
                if (!key.startsWith("$")) {
                    conds.add(eqOthers.get(key));
                }
            }
        }
        if (conds.size() > 0) {
            sb.append("where ");
            for (int j = 0; j < conds.size(); j++) {
                sb.append(conds.get(j));
                if (j < conds.size() - 1) {
                    sb.append(" and ");
                }
            }
            sb.append("\n");
        }
        sb.append("\nreturn <tuple>\n");
        for( int i = 0; i < localVars.size(); i++ ) {
            String var = localVars.get(i);
            sb.append("<").append(var.substring(1)).append(">{").append(var).append("}</").append(var.substring(1)).append('>');
            if (i < localVars.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n</tuple>");
        return sb.toString();
    }
    @Override
    public String visitXq(XQuerySubParser.XqContext ctx) {
        int compNum = buildComponents(ctx);
        if (compNum == 1) {
            return ctx.getText();
        } else {
            this.visit(ctx.cond());
            this.visit(ctx.return_());
            StringBuilder sb = new StringBuilder();
            sb.append("for $tuple in ");
            for (int i = 0; i < compNum - 1; i++) {
                sb.append("join (\n");
            }
            for (int i = 0; i < compNum; i += 1) {
                sb.append(buildForClause(i)).append(",\n\n");
                if (i > 0) {
                    ArrayList<String> leftEqs = new ArrayList<>();
                    ArrayList<String> rightEqs = new ArrayList<>();
                    for (int j = 0; j < eqRight.size(); j++) {
                        if (components.get(eqRight.get(j)) == i) {
                            leftEqs.add(eqLeft.remove(j).substring(1));
                            rightEqs.add(eqRight.remove(j).substring(1));
                        }
                    }
                    sb.append("[").append(String.join(", ", leftEqs)).append("], [").append(String.join(", ", rightEqs)).append("]\n");
                    sb.append(")\n");
                }
            }
            sb.append("return ").append(this.visit(ctx.return_()));
            return sb.toString();
        }

    }

    @Override
    public String visitReturnVar(XQuerySubParser.ReturnVarContext ctx) {
        return "$tuple/" + ctx.Var().getText().substring(1) + "/*";
    }

    @Override
    public String visitReturnComma(XQuerySubParser.ReturnCommaContext ctx) {
        return this.visit(ctx.return_(0)) + ", " + this.visit(ctx.return_(1));
    }

    @Override
    public String visitReturnPath(XQuerySubParser.ReturnPathContext ctx) {
        String path = ctx.path().getText();
        if (path.startsWith("$")) {
            String var = ctx.path().getChild(0).getText();
            return "$tuple/" + var + "/*" + path.substring(var.length());
        } else {
            return ctx.path().getText();
        }
    }

    @Override
    public String visitReturnTag(XQuerySubParser.ReturnTagContext ctx) {
        return "<" + ctx.NAME(0) + ">{" + this.visit(ctx.return_()) + "}</" + ctx.NAME(1) + ">";
    }

    @Override
    public String visitCondAnd(XQuerySubParser.CondAndContext ctx) {
        this.visit(ctx.cond(0));
        this.visit(ctx.cond(1));
        return "";
    }

    @Override
    public String visitCondEq(XQuerySubParser.CondEqContext ctx) {
        String left = ctx.getChild(0).getText();
        String right = ctx.getChild(2).getText();
        Integer leftCompIdx = components.get(left);
        Integer rightCompIdx = components.get(right);
        if (leftCompIdx != null && rightCompIdx != null && !leftCompIdx.equals(rightCompIdx)) {
            if (leftCompIdx < rightCompIdx) {
                eqLeft.add(left);
                eqRight.add(right);
            } else {
                eqLeft.add(right);
                eqRight.add(left);
            }
        } else {
            if (leftCompIdx != null) {
                if (eqOthers.containsKey(left)) {
                    eqOthers.put(left, eqOthers.get(left) + " and " + ctx.getText());
                } else {
                    eqOthers.put(left, ctx.getText());
                }
            } else {
                if (eqOthers.containsKey(right)) {
                    eqOthers.put(right, eqOthers.get(right) + " and " + ctx.getText());
                } else {
                    eqOthers.put(right, ctx.getText());
                }
            }
        }
        return "";
    }


}
