package gpmf.gp.treeGenerator.nodes;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import gpmf.gp.treeGenerator.TreeElement;

public class Statement extends Node {
  private Node conditionNode;
  private Node nextNode;

  public Statement(String nodeType, int depth, Node parent, NodeTool nodeTool) {
    this.setNodeType(nodeType);
    this.setDepth(depth);
    this.setParent(parent);
    this.setOffspring(0);
    this.setNodeTool(nodeTool);

    this.setOperator(null);
    this.setConditionNode(null);
    this.setNextNode(null);
  }

  public void setNextNode(Node nextNode) {
    this.nextNode = nextNode;
  }

  public Node getNextNode() {
    return nextNode;
  }

  public void setConditionNode(Node conditionNode) {
    this.conditionNode = conditionNode;
  }

  public Node getConditionNode() {
    return conditionNode;
  }

  @Override
  public void expand() {
    if (this.getNodeType() == "IFStmt") {
      ifStmtExpand();
    } else if (this.getNodeType() == "AssignStmt") {
      assignStmtExpand();
    }
  }

  public void assignStmtExpand() {

    this.setOperator(
        new Leaf(this.selectBinaryOperatorExpression(), getDepth() + 1, this, this.getNodeTool()));

    String expressionNodeTypeSelection = this.getNodeTool().selectExpression(this.getDepth() + 1);
    this.getNodeTool().addNodeNumber();
    this.setRightNode(
        new Expression(expressionNodeTypeSelection, this.getDepth() + 1, this, this.getNodeTool()));
    this.getRightNode().expand();

    if (!(this.getNodeTool().getCurrentNodeNumber() > this.getNodeTool().getMaxNodes()))
      if (this.getNodeTool().addNewStmtDecision()) {
        String statementNodeTypeSelection = this.getNodeTool().selectStatement(this.getDepth());
        this.getNodeTool().addNodeNumber();
        this.setOffspring(this.getOffspring() + 1);
        this.nextNode =
            new Statement(statementNodeTypeSelection, this.getDepth(), this, this.getNodeTool());
        this.nextNode.expand();
      }
  }

  public void ifStmtExpand() {
    super.getNodeTool().setHasCondition(true);
    String statementNodeTypeSelection = this.getNodeTool().selectStatement(this.getDepth() + 1);
    this.getNodeTool().addNodeNumber();
    this.setLeftNode(
        new Statement(statementNodeTypeSelection, this.getDepth() + 1, this, this.getNodeTool()));
    this.getLeftNode().expand();

    String middleNodeTypeSelection = this.getNodeTool().selectConditionExpression();
    this.getNodeTool().addNodeNumber();
    this.setConditionNode(
        new ConditionExpression(
            middleNodeTypeSelection, this.getDepth() + 1, this, this.getNodeTool()));
    this.getConditionNode().expand();

    if (this.getNodeTool().addElseStmtDecision()) {
      statementNodeTypeSelection = this.getNodeTool().selectStatement(this.getDepth() + 1);
      this.getNodeTool().addNodeNumber();
      this.setRightNode(
          new Statement(statementNodeTypeSelection, this.getDepth() + 1, this, this.getNodeTool()));
      this.getRightNode().expand();
    }
    if (!(this.getNodeTool().getCurrentNodeNumber() > this.getNodeTool().getMaxNodes()))
      if (this.getNodeTool().addNewStmtDecision()) {
        statementNodeTypeSelection = this.getNodeTool().selectStatement(this.getDepth());
        this.getNodeTool().addNodeNumber();
        this.setNextNode(
            new Statement(statementNodeTypeSelection, this.getDepth(), this, this.getNodeTool()));
        this.getNextNode().expand();
      }
  }

  @Override
  public void draw(Pane canvas, int xStart, int yStart, int xEnd, int yEnd, int[] silhouette) {
    xEnd = silhouette[this.getDepth()];

    if (this.getOperator() != null)
      this.getOperator().draw(canvas, xEnd, yEnd, xEnd, yEnd + 100, silhouette);
    if (this.getConditionNode() != null)
      this.getConditionNode().draw(canvas, xEnd, yEnd, xEnd, yEnd + 100, silhouette);
    if (this.getLeftNode() != null)
      this.getLeftNode().draw(canvas, xEnd, yEnd, xEnd, yEnd + 100, silhouette);
    if (this.getRightNode() != null)
      this.getRightNode().draw(canvas, xEnd, yEnd, xEnd, yEnd + 100, silhouette);
    if (this.getNextNode() != null)
      this.getNextNode().draw(canvas, xEnd, yEnd, xEnd + 100, yEnd, silhouette);

    Line line = new Line(xStart, yStart, xEnd, yEnd);
    canvas.getChildren().add(line);
    Circle circle = new Circle(xStart, yStart, 20, Paint.valueOf("white"));
    circle.toBack();
    canvas.getChildren().add(circle);
    Text txt = new Text(xEnd - 5, yEnd + 15, this.getNodeType());
    txt.toFront();
    canvas.getChildren().add(txt);

    if (canvas.getMinHeight() < yEnd + 100) canvas.setMinHeight(yEnd + 100);
    if (canvas.getMinWidth() < xEnd + 100) canvas.setMinWidth(xEnd + 100);
  }

  @Override
  public String getPrefix(String currentPrefix) {
    String res = currentPrefix;
    if (this.getNodeType() == "IFStmt") {
      if (this.getConditionNode() != null) {
        if (this.getConditionNode().eval() == 0.0) {
          if (this.getLeftNode() != null) res = this.getLeftNode().getPrefix(res);
        } else if (this.getRightNode() != null) {
          res = this.getRightNode().getPrefix(res);
        }
      }
      if (this.nextNode != null) res = this.nextNode.getPrefix("") + " " + res;
    } else if (this.getNodeType() == "AssignStmt") {
      if (this.getRightNode() != null) {
        if (!this.getNodeTool().getPrintFirstResult()) {
          res = this.getOperator().getValue() + " One " + this.getRightNode().getPrefix(res);
          this.getNodeTool().setPrintFirstResult(true);
        } else {
          res = this.getOperator().getValue() + " " + this.getRightNode().getPrefix(res);
        }
        this.eval();
      }
      if (nextNode != null) res = this.nextNode.getPrefix("") + " " + res;
    }
    return res;
  }

  @Override
  public double eval() {
    double res = 1.0;

    switch (this.getOperator().getValue()) {
      case "+":
        res = this.getNodeTool().getResult() + this.getRightNode().eval();
        break;
      case "-":
        res = this.getNodeTool().getResult() - this.getRightNode().eval();
        break;
      case "*":
        res = this.getNodeTool().getResult() * this.getRightNode().eval();
        break;
      case "pow":
        res = Math.pow(this.getNodeTool().getResult(), this.getRightNode().eval());
        break;
    }
    this.getNodeTool().setResult(res);
    return res;
  }

  @Override
  public TreeElement clone(NodeTool nodeTool) {
    Node aux = new Statement(this.getNodeType(), this.getDepth(), this.getParent(), nodeTool);
    aux.setNodeNumber(this.getNodeNumber());

    if (this.getOperator() != null)
      ((Statement) aux).setOperator((Leaf) this.getOperator().clone(nodeTool));
    if (this.getConditionNode() != null)
      ((Statement) aux).setConditionNode((Node) this.getConditionNode().clone(nodeTool));
    if (this.getRightNode() != null) aux.setRightNode((Node) this.getRightNode().clone(nodeTool));
    if (this.getLeftNode() != null) aux.setLeftNode((Node) this.getLeftNode().clone(nodeTool));
    if (this.nextNode != null)
      ((Statement) aux).setNextNode((Node) this.getNextNode().clone(nodeTool));

    return aux;
  }

  @Override
  public void restructure(int depth) {
    if (depth > super.getNodeTool().getDepth()) super.getNodeTool().setDepth(depth);
    this.setDepth(depth);
    this.setNodeNumber(this.getNodeTool().getCurrentNodeNumber());

    if (this.getLeftNode() != null) {
      this.getNodeTool().addNodeNumber();
      this.getLeftNode().restructure(depth + 1);
    }
    if (this.getConditionNode() != null && this.getNodeType() != "AssignStmt") {
      super.getNodeTool().setHasCondition(true);
      this.getNodeTool().addNodeNumber();
      this.getConditionNode().restructure(depth + 1);
    }
    if (this.getRightNode() != null) {
      this.getNodeTool().addNodeNumber();
      this.getRightNode().restructure(depth + 1);
    }
    if (this.nextNode != null) {
      this.getNodeTool().addNodeNumber();
      this.nextNode.restructure(depth);
    }
  }

  @Override
  public void setNode(Node node, int nodeNumber) {
    boolean found = false;
    if (this.getLeftNode() != null && !found) {
      if (this.getLeftNode().getNodeNumber() == nodeNumber) {
        this.setLeftNode((Node) node.clone(this.getNodeTool()));
        found = true;
      } else {
        this.getLeftNode().setNode(node, nodeNumber);
      }
    }
    if (this.getConditionNode() != null && !found && this.getNodeType() != "AssignStmt") {
      if (this.getConditionNode().getNodeNumber() == nodeNumber) {
        this.setConditionNode((Node) node.clone(this.getNodeTool()));
        found = true;
      } else {
        this.getConditionNode().setNode(node, nodeNumber);
      }
    }
    if (this.getRightNode() != null && !found) {
      if (this.getRightNode().getNodeNumber() == nodeNumber) {
        this.setRightNode((Node) node.clone(this.getNodeTool()));
        found = true;
      } else {
        this.getRightNode().setNode(node, nodeNumber);
      }
    }
    if (this.nextNode != null && !found) {
      if (this.nextNode.getNodeNumber() == nodeNumber) {
        this.setNextNode((Node) node.clone(this.getNodeTool()));
        found = true;
      } else {
        this.nextNode.setNode(node, nodeNumber);
      }
    }
  }

  @Override
  public Node getNode(int nodeNumber) {
    Node node = null;
    Node auxNode = null;
    boolean found = false;

    if (this.getNodeNumber() == nodeNumber) {
      node = this;
      found = true;
    }
    if (this.getLeftNode() != null && !found) {
      auxNode = this.getLeftNode().getNode(nodeNumber);
      if (auxNode != null) {
        node = auxNode;
        found = true;
      }
    }
    if (this.getConditionNode() != null && !found && this.getNodeType() != "AssignStmt") {
      auxNode = this.getConditionNode().getNode(nodeNumber);
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
    if (this.nextNode != null && !found) {
      auxNode = this.nextNode.getNode(nodeNumber);
      if (auxNode != null) {
        node = auxNode;
        found = true;
      }
    }

    return node;
  }

  @Override
  public String getNodeClass() {
    return "Statement";
  }

  @Override
  public String toString() {
    String res = "";

    for (int i = 0; i < this.getDepth(); i++) res += "\t";

    if (this.getNodeType() == "IFStmt") {
      res += this.getNodeType() + " (" + this.getConditionNode().toString() + "){\n";
      res += this.getLeftNode().toString() + "\n";
      for (int i = 0; i < this.getDepth(); i++) res += "\t";
      res += "}";
      if (this.getRightNode() != null) {
        res += " else {\n";
        res += this.getRightNode().toString() + "\n";
        for (int i = 0; i < this.getDepth(); i++) res += "\t";
        res += "}";
      }
      if (this.nextNode != null) res += "\n" + this.nextNode.toString();
    } else if (this.getNodeType() == "AssignStmt") {
      res += "result " + this.getOperator().toString() + "= " + this.getRightNode().toString();
      if (this.nextNode != null) res += "\n" + this.nextNode.toString();
    }
    return res;
  }
}
