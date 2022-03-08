package gpmf.gp.treeGenerator.nodes;

import gpmf.gp.treeGenerator.TreeElement;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;

public abstract class Node extends TreeElement {

  private int nodeNumber;
  private String nodeType;
  private int offspring;

  private Node rightNode;
  private Node leftNode;
  private Leaf operator;

  private NodeTool nodeTool;

  private String[] binaryOperators = {"+", "-", "*", "pow"};
  private String[] unaryOperators = {"cos", "sin", "atan", "exp", "log", "inv"};
  private String[] comparisonOperators = {"<", ">", "<=", ">="};
  private String[] multiOperators = {"&&", "||"};

  public abstract void restructure(int depth);

  public abstract String getNodeClass();

  public abstract void expand();

  public String selectBinaryOperatorExpression() {
    int operatorSelection =
        (int) Math.floor(this.nodeTool.getRandom().nextDouble() * binaryOperators.length);
    return binaryOperators[operatorSelection];
  }

  public String selectUnaryOperatorExpression() {
    int operatorSelection =
        (int) Math.floor(this.nodeTool.getRandom().nextDouble() * unaryOperators.length);
    return unaryOperators[operatorSelection];
  }

  public String selectComparisonOperatorExpression() {
    int operatorSelection =
        (int) Math.floor(this.nodeTool.getRandom().nextDouble() * comparisonOperators.length);
    return comparisonOperators[operatorSelection];
  }

  public String selectMultiOperatorExpression() {
    int operatorSelection =
        (int) Math.floor(this.nodeTool.getRandom().nextDouble() * multiOperators.length);
    return multiOperators[operatorSelection];
  }

  public void setNodeTool(NodeTool nodeTool) {
    this.nodeTool = nodeTool;
  }

  public NodeTool getNodeTool() {
    return this.nodeTool;
  }

  public void setNodeNumber(int nodeNumber) {
    this.nodeNumber = nodeNumber;
  }

  public int getNodeNumber() {
    return this.nodeNumber;
  }

  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public String getNodeType() {
    return this.nodeType;
  }

  public void setOffspring(int offspring) {
    this.offspring = offspring;
  }

  public int getOffspring() {
    return this.offspring;
  }

  public void setRightNode(Node node) {
    this.rightNode = node;
  }

  public Node getRightNode() {
    return this.rightNode;
  }

  public void setLeftNode(Node node) {
    this.leftNode = node;
  }

  public Node getLeftNode() {
    return this.leftNode;
  }

  public void setOperator(Leaf operator) {
    this.operator = operator;
  }

  public Leaf getOperator() {
    return this.operator;
  }

  public abstract void setNode(Node node, int nodeNumber);

  public abstract Node getNode(int nodeNumber);
}
