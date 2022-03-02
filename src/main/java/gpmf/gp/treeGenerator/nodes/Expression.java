package gpmf.gp.treeGenerator.nodes;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import gpmf.gp.treeGenerator.TreeElement;

public class Expression extends Node{

    public Expression(String nodeType, int depth, Node parent, NodeTool nodeTool){
        this.setNodeType(nodeType);
        this.setDepth(depth);
        this.setParent(parent);
        this.setOffspring(0);
        this.setNodeTool(nodeTool);
    }

    @Override
    public void expand() {
        if(this.getNodeType() == "BinaryExpression"){
            binaryExpressionExpand();
        }else if(this.getNodeType() == "UnaryExpression"){
            unaryExpressionExpand();
        }else if(this.getNodeType() == "ConstantExpression"){
            constantExpressionExpand();
        }
    }

    public void binaryExpressionExpand(){
        String operatorTypeSelection = this.selectBinaryOperatorExpression();
        this.setOperator( new Leaf(operatorTypeSelection,this.getDepth()+1,this, this.getNodeTool()));

        String expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth()+1);
        this.getNodeTool().addNodeNumber();
        this.setLeftNode(new Expression(expressionNodeTypeSelection, this.getDepth()+1,this, this.getNodeTool()));
        this.getLeftNode().expand();
        //this.setOffspring(this.getOffspring()+this.getLeftNode().getOffspring()+1);

        expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth()+1);
        this.getNodeTool().addNodeNumber();
        this.setRightNode(new Expression(expressionNodeTypeSelection, this.getDepth()+1,this, this.getNodeTool()));
        this.getRightNode().expand();
        //this.setOffspring(this.getOffspring()+this.getRightNode().getOffspring()+1);

    }

    public void unaryExpressionExpand(){
        String operatorTypeSelection = this.selectUnaryOperatorExpression();
        this.setOperator(new Leaf(operatorTypeSelection, this.getDepth()+1,this, this.getNodeTool()));

        String expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth()+1);
        this.getNodeTool().addNodeNumber();
        this.setRightNode(new Expression(expressionNodeTypeSelection, this.getDepth()+1,this, this.getNodeTool()));
        this.getRightNode().expand();
        //this.setOffspring(this.getOffspring()+this.getRightNode().getOffspring()+1);
    }

    public void constantExpressionExpand(){
        String leafValue = this.getNodeTool().selectLeafValue();
        this.setOperator(new Leaf(leafValue, getDepth()+1,this, this.getNodeTool()));
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
        String res = "";
        if(this.getNodeType() == "BinaryExpression"){
            if(this.getLeftNode() != null && this.getRightNode() != null && this.getOperator() != null) {
                switch (this.getOperator().getValue()) {
                    case "+":
                        res += "+ " + this.getLeftNode().getPrefix(res) + " " + this.getRightNode().getPrefix(res);
                        break;
                    case "-":
                        res += "- "+this.getLeftNode().getPrefix(res) + " " + this.getRightNode().getPrefix(res);
                        break;
                    case "*":
                        res += "* "+this.getLeftNode().getPrefix(res) + " " + this.getRightNode().getPrefix(res);
                        break;
                    case "pow":
                        res += "pow "+this.getLeftNode().getPrefix(res) + " " + this.getRightNode().getPrefix(res);
                        break;
                }

            }
        }else if(this.getNodeType() == "UnaryExpression"){
            if(this.getRightNode() != null) {
                switch (this.getOperator().getValue()) {
                    case "cos":
                        res += "cos " + this.getRightNode().getPrefix(res);
                        break;
                    case "sin":
                        res += "sin " + this.getRightNode().getPrefix(res);
                        break;
                    case "atan":
                        res += "atan " + this.getRightNode().getPrefix(res);
                        break;
                    case "exp":
                        res += "exp " + this.getRightNode().getPrefix(res);
                        break;
                    case "log":
                        res += "log " + this.getRightNode().getPrefix(res);
                        break;
                    case "inv":
                        res += "inv " + this.getRightNode().getPrefix(res);
                        break;
                }

            }
        }else if(this.getNodeType() == "ConstantExpression"){
            if(this.getOperator() != null) {
                res += this.getOperator().getPrefix(res);

            }
        }

        return res;
    }

    @Override
    public double eval() {
        double res = 0.0;
        if(this.getNodeType() == "BinaryExpression") {
            if (this.getLeftNode() != null && this.getRightNode() != null)
                switch (this.getOperator().getValue()) {
                    case "+":
                        res = this.getLeftNode().eval() + this.getRightNode().eval();
                        break;
                    case "-":
                        res = this.getLeftNode().eval() - this.getRightNode().eval();
                        break;
                    case "*":
                        res = this.getLeftNode().eval() * this.getRightNode().eval();
                        break;
                    case "^":
                        res = Math.pow(this.getLeftNode().eval(), this.getRightNode().eval());
                        break;
                }
        }else if(this.getNodeType() == "UnaryExpression") {
            if (getRightNode() != null)
                switch (this.getOperator().getValue()) {
                    case "cos":
                        res = Math.cos(this.getRightNode().eval());
                        break;
                    case "sin":
                        res = Math.sin(this.getRightNode().eval());
                        break;
                    case "atan":
                        res = Math.atan(this.getRightNode().eval());
                        break;
                    case "exp":
                        res = Math.exp(this.getRightNode().eval());
                        break;
                    case "log":
                        res = Math.log(this.getRightNode().eval());
                        break;
                    case "inv":
                        res = 1 / this.getRightNode().eval();
                        break;
                }
        }else if(this.getNodeType() == "ConstantExpression")
            if(this.getOperator() != null)
                res = this.getOperator().eval();

        return res;
    }

    @Override
    public TreeElement clone(NodeTool nodeTool) {
        Node aux = new Expression(this.getNodeType(), this.getDepth(), this.getParent(), nodeTool);
        aux.setNodeNumber(this.getNodeNumber());
        if(this.getOperator() != null)
            ((Expression) aux).setOperator(new Leaf(this.getOperator().getValue(), this.getDepth()+1, aux, nodeTool));
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

        if(this.getLeftNode() != null){
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
        if(this.getLeftNode() != null && !found){
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
        if(this.getLeftNode() != null && !found) {
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
        return "Expression";
    }

    @Override
    public String toString() {
        String res ="";
        if(this.getNodeType() == "BinaryExpression"){
            res += this.getLeftNode().toString() + " " + this.getOperator().getValue() + " " + this.getRightNode().toString();
        }else if(this.getNodeType() == "UnaryExpression"){
            res += this.getOperator().getValue() + " " + this.getRightNode().toString();
        }else{
            if(this.getOperator() != null)
                res += this.getOperator().toString();
        }
        return res;
    }
}
