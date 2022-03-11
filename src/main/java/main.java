import es.upm.etsisi.cf4j.data.BenchmarkDataModels;
import es.upm.etsisi.cf4j.data.DataModel;

import es.upm.etsisi.cf4j.util.optimization.GridSearchCV;
import es.upm.etsisi.cf4j.util.optimization.ParamsGrid;
import gpmf.GPMF;
import qualityMeasures.prediction.MSE;

import java.io.IOException;

public class main {
  public static void main(String[] args) throws IOException {

    DataModel datamodel = BenchmarkDataModels.MovieLens100K();

    ParamsGrid paramsGrid = new ParamsGrid();

    paramsGrid.addParam("numFactors", new int[] {6});
    paramsGrid.addParam("regularization", new double[] {0.095});
    paramsGrid.addParam("learningRate", new double[] {0.001});
    paramsGrid.addParam("gens", new int[] {150});
    paramsGrid.addParam("pbmut", new double[] {0.4});
    paramsGrid.addParam("pbx", new double[] {1.0});
    paramsGrid.addParam("popSize", new int[] {80});
    paramsGrid.addParam("numIters", new int[] {2});
    paramsGrid.addParam("maxDepthInit", new int[] {4});
    paramsGrid.addParam("maxDepthFinal", new int[] {8});
    paramsGrid.addParam("maxNodesInit", new int[] {5});
    paramsGrid.addParam("maxNodesFinal", new int[] {100});
    paramsGrid.addParam("numChildren", new int[] {80});

    paramsGrid.addFixedParam("seed", 42L);

    GridSearchCV gridSearch =
        new GridSearchCV(datamodel, paramsGrid, GPMF.class, MSE.class, 5, 42L);
    gridSearch.fit();

    gridSearch.printResults(5);
  }
}
