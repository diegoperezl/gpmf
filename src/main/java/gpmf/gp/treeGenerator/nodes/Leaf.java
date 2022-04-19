package gpmf.gp.treeGenerator.nodes;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import gpmf.gp.treeGenerator.TreeElement;

public class Leaf extends TreeElement {
  private String value;
  private NodeTool nodeTool;

  public Leaf(String value, int depth, Node parent, NodeTool nodeTool) {
    this.setValue(value);
    this.setDepth(depth);
    this.setParent(parent);
    this.setNodeTool(nodeTool);
  }

  public void setNodeTool(NodeTool nodeTool) {
    this.nodeTool = nodeTool;
  }

  public NodeTool getNodeTool() {
    return this.nodeTool;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void draw(Pane canvas, int xStart, int yStart, int xEnd, int yEnd, int[] silhouette) {
    xEnd = silhouette[this.getDepth()];

    for (int i = 0; i < silhouette.length; i++) {
      silhouette[i] += 50;
    }

    Line line = new Line(xStart, yStart, xEnd, yEnd);
    canvas.getChildren().add(line);
    Circle circle = new Circle(xStart, yStart, 30, Paint.valueOf("white"));
    circle.toBack();
    canvas.getChildren().add(circle);
    Text txt = new Text(xEnd - 5, yEnd + 15, this.value);
    txt.toFront();
    canvas.getChildren().add(txt);

    if (canvas.getMinHeight() < yEnd + 100) canvas.setMinHeight(yEnd + 100);
    if (canvas.getMinWidth() < xEnd + 100) canvas.setMinWidth(xEnd + 100);
  }

  @Override
  public String getPrefix(String currentPrefix) {
    String res = currentPrefix;
    if (this.value == "result") {
      res = "const " + this.eval();
    } else {
      res = this.value + " ";
    }
    return res;
  }

  @Override
  public double eval() {
    double res = 0.0;
    switch (this.value) {
      case "result":
        res = this.nodeTool.getResult();
        break;
      case "One":
        res = 1.0;
        break;
      case "Zero":
        res = 0.0;
        break;
      default:
        res = this.nodeTool.getFactor(this.value);
    }
    return res;
  }

  @Override
  public TreeElement clone(NodeTool nodeTool) {
    return new Leaf(this.value, this.getDepth(), this.getParent(), nodeTool);
  }

  @Override
  public String toString() {
    if ((this.value.charAt(0) == 'p' || this.value.charAt(0) == 'q') && !this.value.equals("pow")) {
      //return this.value + " (" + this.nodeTool.getFactor(this.value) + ")";
      return this.value;
    } else return this.value;
  }
}
