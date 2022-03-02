import es.upm.etsisi.cf4j.data.BenchmarkDataModels;
import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.util.optimization.ParamsGrid;
import es.upm.etsisi.cf4j.util.optimization.RandomSearch;
import es.upm.etsisi.cf4j.util.optimization.RandomSearchCV;
import gpmf.GPMF;
import org.apache.commons.math3.util.Pair;
import qualityMeasures.prediction.MAE;

import java.io.IOException;

public class main {
    public static void main(String[] args) throws IOException {

        DataModel datamodel = BenchmarkDataModels.MovieLens100K();

        ParamsGrid paramsGrid = new ParamsGrid();

        paramsGrid.addParam("numFactors", new int[]{6});
        paramsGrid.addParam("regularization", new double[]{0.095});
        paramsGrid.addParam("learningRate", new double[]{0.001});
        paramsGrid.addParam("gens", new int[]{150});
        paramsGrid.addParam("pbmut", new double[]{0.4});
        paramsGrid.addParam("pbx", new double[]{1.0});
        paramsGrid.addParam("popSize", new int[]{80});
        paramsGrid.addParam("numIters", new int[]{150});
        paramsGrid.addParam("maxDepth", new int[]{6});
        paramsGrid.addParam("maxNodes", new int[]{20});
        paramsGrid.addParam("numChildren", new int[]{80});

        paramsGrid.addFixedParam("seed", 42L);

        RandomSearch randomSearch = new RandomSearch(datamodel, paramsGrid, GPMF.class, MAE.class, 1, 42L);
        randomSearch.fit();

        randomSearch.printResults(1);
    }
}
