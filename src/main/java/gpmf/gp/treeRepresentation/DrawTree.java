package gpmf.gp.treeRepresentation;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import gpmf.gp.treeGenerator.Tree;

import javax.imageio.ImageIO;
import java.io.File;

public class DrawTree extends Application {
  private static final Pane canvas = new Pane();
  private static Tree treeInstance;

  @Override
  public void start(Stage stage) {

    canvas.setStyle("-fx-background-color: white;");

    treeInstance.reset();
    treeInstance.restructure();

    int[] silhouette = new int[treeInstance.getDepth() + 2];

    treeInstance.draw(canvas, 0, 0, 0, 50, silhouette);

    Group root = new Group();
    VBox vb = new VBox();
    WritableImage wim = new WritableImage((int) canvas.getMinWidth(), (int) canvas.getMinHeight());
    canvas.snapshot(null, wim);
    Scene scene = new Scene(root, canvas.getMinWidth(), canvas.getMinHeight());
    stage.setScene(scene);
    stage.setTitle("");
    vb.getChildren().add(canvas);

    scene.setRoot(vb);

    File file = new File("trees/tree.png");

    try {
      ImageIO.write(SwingFXUtils.fromFXImage(wim, null), "png", file);
    } catch (Exception ignored) {
    }

    Platform.exit();
  }

  public static void draw(Tree tree) {
    treeInstance = tree;
    launch();
  }
}
