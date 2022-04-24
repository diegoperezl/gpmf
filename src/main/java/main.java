import es.upm.etsisi.cf4j.data.BenchmarkDataModels;
import es.upm.etsisi.cf4j.data.DataModel;

import gpmf.GPMF;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class main {
  public static void main(String[] args) throws IOException {

    DataModel datamodel = BenchmarkDataModels.MovieLens100K();

    Map<String, Object> params = new HashMap<String, Object>();

    // params.put("numFactors", 6);
    // params.put("regularization", 0.095);
    // params.put("learningRate", 0.001);
    params.put("gens", 150);
    params.put("pbmut", 0.5);
    params.put("pbx", 1.0);
    params.put("popSize", 80);
    // params.put("numIters", 2);
    params.put("maxDepthInit", 50);
    params.put("maxDepthFinal", 300);
    params.put("maxNodesInit", 50);
    params.put("maxNodesFinal", 300);
    params.put("numChildren", 80);
    params.put("earlyStoppingValue", 0.0001);
    params.put("earlyStoppingCount", 10);
    params.put("seed", 42L);

    GPMF gpmf = new GPMF(datamodel, params);
    gpmf.fit();
  }
}
