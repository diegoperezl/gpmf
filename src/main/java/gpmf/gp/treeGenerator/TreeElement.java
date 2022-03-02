package gpmf.gp.treeGenerator;

import gpmf.gp.treeGenerator.nodeSupport.NodeTool;
import javafx.scene.layout.Pane;
import gpmf.gp.treeGenerator.nodes.Node;

public abstract class TreeElement {
    private int depth;
    private Node parent;

    public abstract void draw(Pane canvas, int xStart, int yStart, int xEnd, int yEnd, int[] silhouette);
    public abstract String getPrefix(String currentPrefix);
    public abstract double eval();
    public abstract TreeElement clone(NodeTool nodeTool);

    public void setDepth(int depth){
        this.depth = depth;
    }

    public int getDepth(){
        return this.depth;
    }

    public void setParent(Node parent){
        this.parent = parent;
    }
    public Node getParent(){
        return this.parent;
    }

    public abstract String toString();

}
