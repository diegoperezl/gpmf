package gpmf.gp.treeGenerator.nodes;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import gpmf.gp.treeGenerator.TreeElement;

public class ConditionExpression extends Node{

    private boolean isWhile;

    public ConditionExpression(String nodeType, int depth, Node parent, boolean isWhile, NodeTool nodeTool){
        this.setNodeType(nodeType);
        this.setDepth(depth);
        this.setParent(parent);
        this.setOffspring(0);
        this.setNodeTool(nodeTool);

        this.setIsWhile(isWhile);
        this.setOperator(null);
    }

    public void setIsWhile(boolean While) {
        this.isWhile = While;
    }

    public boolean getIsWhile() {
        return this.isWhile;
    }

    @Override
    public void expand() {
        if(this.getNodeType() == "ComparisonExpression"){
            comparisonExpressionExpand();
        }else if(this.getNodeType() == "MultiConditionExpression"){
            multiConditionExpressionExpand();
        }else if(this.getNodeType() == "NegationExpression"){
            negationExpressionExpand();
        }
    }

    public void comparisonExpressionExpand(){
        String operatorTypeSelection = this.selectComparisonOperatorExpression();
        this.setOperator(new Leaf(operatorTypeSelection, this.getDepth()+1, this, this.getNodeTool()));

        if(this.isWhile){
            Expression expression = new Expression("ConstantExpression",this.getDepth()+1,this, this.getNodeTool());
            expression.setOperator(new Leaf("result", this.getDepth()+2,this, this.getNodeTool()));
            this.setLeftNode(expression);
        }else{
            String expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth()+1);
            this.getNodeTool().addNodeNumber();
            this.setLeftNode(new Expression(expressionNodeTypeSelection, this.getDepth()+1,this, this.getNodeTool()));
            this.getLeftNode().expand();
            //this.setOffspring(this.getOffspring()+this.getLeftNode().getOffspring()+1);
        }


        String expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth()+1);
        this.getNodeTool().addNodeNumber();
        this.setRightNode(new Expression(expressionNodeTypeSelection, this.getDepth()+1,this, this.getNodeTool()));
        this.getRightNode().expand();
        //this.setOffspring(this.getOffspring()+this.getRightNode().getOffspring()+1);

    }

    public void multiConditionExpressionExpand(){

        String operatorTypeSelection = this.selectMultiOperatorExpression();
        this.setOperator(new Leaf(operatorTypeSelection, this.getDepth()+1,this, this.getNodeTool()));

        String conditionNodeTypeSelection = this.getNodeTool().selectConditionExpression();
        this.getNodeTool().addNodeNumber();
        this.setLeftNode(new ConditionExpression(conditionNodeTypeSelection, this.getDepth()+1, this, this.isWhile, this.getNodeTool()));
        this.getLeftNode().expand();
        //this.setOffspring(this.getOffspring()+this.getLeftNode().getOffspring()+1);

        conditionNodeTypeSelection = this.getNodeTool().selectConditionExpression();
        this.getNodeTool().addNodeNumber();
        this.setRightNode(new ConditionExpression(conditionNodeTypeSelection, this.getDepth()+1, this, this.isWhile, this.getNodeTool()));
        this.getRightNode().expand();
        //this.setOffspring(this.getOffspring()+this.getRightNode().getOffspring()+1);
    }

    public void negationExpressionExpand(){
        this.setOperator(new Leaf("!", this.getDepth()+1,this, this.getNodeTool()));

        String conditionNodeTypeSelection = this.getNodeTool().selectConditionExpression();
        this.getNodeTool().addNodeNumber();
        this.setRightNode(new ConditionExpression(conditionNodeTypeSelection, this.getDepth()+1, this, this.isWhile, this.getNodeTool()));
        this.getRightNode().expand();
        //this.setOffspring(this.getOffspring()+this.getRightNode().getOffspring()+1);
    }

    @Override
    public void draw(Pane canvas, int xStart, int yStart, int xEnd, int yEnd, int[] silhouette) {
        for( int i = 0;i < silhouette.length;i++){
            silhouette[i] += 50;
        }
        if(silhouette[this.getDepth()]>xEnd)xEnd=silhouette[this.getDepth()];

        if(this.getLeftNode() != null) this.getLeftNode().draw(canvas, xEnd, yEnd, xEnd, yEnd+100, silhouette);
        if(this.getRightNode() != null) this.getRightNode().draw(canvas, xEnd, yEnd, xEnd, yEnd+100, silhouette);

        Line line = new Line(xStart, yStart, xEnd, yEnd);
        canvas.getChildren().add(line);
        Circle circle = new Circle(xStart,yStart, 30, Paint.valueOf("white"));
        circle.toBack();
        canvas.getChildren().add(circle);
        Text txt = new Text(xEnd-5,yEnd+15, this.getOperator().getValue());
        txt.toFront();
        canvas.getChildren().add(txt);

        if(canvas.getMinHeight()<yEnd+100)canvas.setMinHeight(yEnd+100);
        if(canvas.getMinWidth()<xEnd+100)canvas.setMinWidth(xEnd+100);
    }

    @Override
    public String getPrefix(String currentPrefix) {
        return "";
    }

    @Override
    public double eval() {
        double res = 1.0;
        if(this.getNodeType() == "ComparisonExpression") {
            if (this.getLeftNode() != null && this.getRightNode() != null)
                switch (this.getOperator().getValue()) {
                    case "<":
                        if (this.getLeftNode().eval() < this.getRightNode().eval()) res = 0.0;
                        break;
                    case ">":
                        if (this.getLeftNode().eval() > this.getRightNode().eval()) res = 0.0;
                        break;
                    case "<=":
                        if (this.getLeftNode().eval() <= this.getRightNode().eval()) res = 0.0;
                        break;
                    case ">=":
                        if (this.getLeftNode().eval() >= this.getRightNode().eval()) res = 0.0;
                        break;
                }
        }else if(this.getNodeType() == "MultiCOnditionExpression") {
            if (getLeftNode() != null && getRightNode() != null)
                switch (this.getOperator().getValue()) {
                    case "&&":
                        if (this.getLeftNode().eval() == 0.0 && this.getRightNode().eval() == 0.0) res = 0.0;
                        break;
                    case "||":
                        if (this.getLeftNode().eval() == 0.0 || this.getRightNode().eval() == 0.0) res = 0.0;
                        break;
                }
        }else if(this.getNodeType() == "NegationExpression")
            if(getRightNode() != null)
                if(this.getRightNode().eval() == 1.0) res = 0.0;

        return res;
    }

    @Override
    public TreeElement clone(NodeTool nodeTool) {
        Node aux = new ConditionExpression(this.getNodeType(), this.getDepth(), this.getParent(), this.isWhile, nodeTool);
        aux.setNodeNumber(this.getNodeNumber());
        ((ConditionExpression) aux).setOperator(new Leaf(this.getOperator().getValue(), this.getDepth()+1, this, nodeTool));
        if(this.getLeftNode() != null)
            aux.setLeftNode((Node) this.getLeftNode().clone(nodeTool));
        if(this.getRightNode() != null)
            aux.setRightNode((Node) this.getRightNode().clone(nodeTool));


        return aux;
    }

    @Override
    public void restructure(int depth) {
        this.setDepth(depth);
        this.setNodeNumber(this.getNodeTool().getCurrentNodeNumber());


        if(this.getLeftNode() != null && !this.isWhile){
            this.getNodeTool().addNodeNumber();
            this.getLeftNode().restructure(depth+1);
        }
        if(this.getRightNode() != null){
            this.getNodeTool().addNodeNumber();
            this.getRightNode().restructure(depth+1);
        }
    }

    @Override
    public void setNode(Node node, int nodeNumber) {
        boolean found = false;
        if(this.getLeftNode() != null && !found && !this.isWhile){
            if (this.getLeftNode().getNodeNumber() == nodeNumber) {
                this.setLeftNode((Node) node.clone(this.getNodeTool()));
                found = true;
            }else{
                this.getLeftNode().setNode(node, nodeNumber);
            }
        }
        if(this.getRightNode() != null && !found){
            if (this.getRightNode().getNodeNumber() == nodeNumber) {
                this.setRightNode((Node) node.clone(this.getNodeTool()));
                found = true;
            }else{
                this.getRightNode().setNode(node, nodeNumber);
            }
        }
    }

    @Override
    public Node getNode(int nodeNumber) {
        Node node = null;
        Node auxNode = null;
        boolean found = false;
        if(this.getNodeNumber() == nodeNumber) {
            node = this;
            found = true;
        }
        if(this.getLeftNode() != null && !found && !this.isWhile) {
            auxNode = this.getLeftNode().getNode(nodeNumber);
            if (auxNode != null) {
                node = auxNode;
                found = true;
            }
        }
        if (this.getRightNode() != null && !found) {
            auxNode = this.getRightNode().getNode(nodeNumber);
            if (auxNode != null) {
                node = auxNode;
                found = true;
            }
        }
        return node;
    }

    @Override
    public String getNodeClass() {
        return "ConditionExpression";
    }

    @Override
    public String toString() {
        String res ="";

        if(this.getNodeType() == "ComparisonExpression"){
            res += this.getLeftNode().toString() + " " + this.getOperator() + " " + this.getRightNode().toString();
        }else if(this.getNodeType() == "MultiConditionExpression"){
            res += this.getLeftNode().toString() + " " + this.getOperator() + " " + this.getRightNode().toString();
        }else{
            res += this.getOperator() + this.getRightNode().toString();
        }
        return res;
    }
}
