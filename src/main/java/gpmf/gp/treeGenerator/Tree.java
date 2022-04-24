package gpmf.gp.treeGenerator;

import javafx.scene.layout.Pane;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import gpmf.gp.treeGenerator.nodes.Node;
import gpmf.gp.treeGenerator.nodes.Statement;

import java.util.HashMap;
import java.util.Random;

public class Tree {

  private double score;
  private int maxDepth;
  private int numFactors;
  private int maxNodes;
  private Node root;
  private int offspring;
  private NodeTool nodeTool;
  private Random rand;

  public Tree(int maxDepth, int numFactors, int maxNodes, Random rand, double score) {
    this.setMaxDepth(maxDepth);
    this.setNumFactors(numFactors);
    this.setMaxNodes(maxNodes);
    this.setRoot(null);
    this.setOffspring(0);
    this.rand = rand;
    this.score = score;
  }

  public void generateTree() {
    this.nodeTool = new NodeTool(this.maxDepth, this.maxNodes, 0, this.numFactors, this.rand);
    this.root = new Statement("AssignStmt", 0, root, nodeTool);
    root.expand();
    this.setOffspring(nodeTool.getCurrentNodeNumber());
  }

  public void regenerate() {
    this.reset();
    this.generateTree();
  }

  public Tree clone() {
    Tree aux =
        new Tree(
            this.getMaxDepth(), this.getNumFactors(), this.getMaxNodes(), this.rand, this.score);
    aux.setNodeTool(this.nodeTool.clone());
    aux.setOffspring(this.getOffspring());
    aux.setRoot((Node) this.getRoot().clone(aux.getNodeTool()));
    return aux;
  }

  public int getDepth() {
    return this.nodeTool.getDepth();
  }

  public double getScore() {
    return this.score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public void setFactorsValues(double[] factorsValues) {
    nodeTool.setFactorsValues(factorsValues);
  }

  public void setFactorsValues(HashMap<String, Double> factorsValues) {
    nodeTool.setFactorsValues(factorsValues);
  }

  public void reset() {
    nodeTool.reset();
  }

  public void restructure() {
    nodeTool.setCurrentNodeNumber(0);
    nodeTool.setHasCondition(false);
    this.root.restructure(0);
    this.setOffspring(nodeTool.getCurrentNodeNumber());
  }

  public String getPrefix() {
    String prefix = root.getPrefix("");
    return prefix.replaceAll("  ", " ");
  }

  public void draw(Pane canvas, int xStart, int yStart, int xEnd, int yEnd, int[] silhouette) {
    root.draw(canvas, xStart, yStart, xEnd, yEnd, silhouette);
  }

  public String print() {
    return this.root.toString();
  }

  public void setRand(Random rand) {
    this.rand = rand;
  }

  public Random getRand() {
    return this.rand;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public int getMaxDepth() {
    return this.maxDepth;
  }

  public void setNumFactors(int numFactors) {
    this.numFactors = numFactors;
  }

  public int getNumFactors() {
    return this.numFactors;
  }

  public void setMaxNodes(int maxNodes) {
    this.maxNodes = maxNodes;
  }

  public int getMaxNodes() {
    return this.maxNodes;
  }

  public void setRoot(Node root) {
    this.root = root;
  }

  public Node getRoot() {
    return this.root;
  }

  public void setOffspring(int offspring) {
    this.offspring = offspring;
  }

  public int getOffspring() {
    return this.offspring;
  }

  public void setNodeTool(NodeTool nodeTool) {
    this.nodeTool = nodeTool;
  }

  public NodeTool getNodeTool() {
    return this.nodeTool;
  }

  public Node getNode(int numNode) {
    return root.getNode(numNode);
  }

  public void setNode(Node node, int numNode) {
    if (numNode == 0) root = (Node) node.clone(this.getNodeTool());
    else root.setNode(node, numNode);
  }
}
