import es.upm.etsisi.cf4j.data.BenchmarkDataModels;
import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.qualityMeasure.prediction.MSE;
import es.upm.etsisi.cf4j.util.optimization.GridSearchCV;
import es.upm.etsisi.cf4j.util.optimization.ParamsGrid;
import es.upm.etsisi.cf4j.util.optimization.RandomSearch;
import es.upm.etsisi.cf4j.util.optimization.RandomSearchCV;
import gpmf.GPMF;

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
    paramsGrid.addParam("numIters", new int[] {100});
    paramsGrid.addParam("maxDepth", new int[] {6});
    paramsGrid.addParam("maxNodes", new int[] {20});
    paramsGrid.addParam("numChildren", new int[] {80});

    paramsGrid.addFixedParam("seed", 42L);

    GridSearchCV gridSearch =
        new GridSearchCV(datamodel, paramsGrid, GPMF.class, MSE.class, 5, 42L);
    gridSearch.fit();

    gridSearch.printResults(5);
  }
}
