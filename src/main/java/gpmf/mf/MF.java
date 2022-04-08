package gpmf.mf;

import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.data.Item;
import es.upm.etsisi.cf4j.data.User;
import es.upm.etsisi.cf4j.recommender.Recommender;
import gpmf.gp.treeGenerator.Tree;
import sym_derivation.symderivation.SymFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MF extends Recommender {

  /** Learning Rate * */
  private final double learningRate;

  /** Regularization * */
  private final double regularization;

  /** Number of Factors * */
  private final int numFactors;

  /** Number of iterations * */
  private int numIters;

  /** SymFunction instance * */
  private SymFunction sf = null;

  /** Tree instance * */
  private Tree treeInstance;

  /** User latent factors matrix p * */
  private double[][] p;

  /** User latent factors matrix q * */
  private double[][] q;

  /** Seed of the model* */
  private Random seed;

  /**
   * Model constructor from a Map containing the model's hyper-parameters values. Map object must
   * contains the following keys:
   *
   * <ul>
   *   <li><b>tree</b>: Tree value with the tree to evaluate.
   *   <li><b>numFactors</b>: int value with the number of latent factors.
   *   <li><b>numIters</b> int value with the number of iterations.
   *   <li><b>regularization</b> double value with the regularization.
   *   <li><b>learningRate</b> double value with the learning rate.
   *   <li><b><em>seed</em></b> (optional): random seed for random numbers generation. If missing, *
   *       random value is used.
   * </ul>
   *
   * @param datamodel DataModel instance
   * @param params Model's hyper-parameters values
   */
  public MF(DataModel datamodel, Map<String, Object> params) {
    this(
        datamodel,
        (Tree) params.get("tree"),
        (int) params.get("numFactors"),
        (int) params.get("numIters"),
        (double) params.get("regularization"),
        (double) params.get("learningRate"),
        params.containsKey("seed") ? (long) params.get("seed") : System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param tree Tree instance
   * @param numFactors Number of latent factors
   * @param numIters Number of iterations
   * @param regularization Regularization
   * @param learningRate Learning rate
   */
  public MF(
      DataModel datamodel,
      Tree tree,
      int numFactors,
      int numIters,
      double regularization,
      double learningRate) {
    this(
        datamodel,
        tree,
        numFactors,
        numIters,
        regularization,
        learningRate,
        System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param tree Tree instance
   * @param numFactors Number of latent factors
   * @param numIters Number of iterations
   * @param regularization Regularization
   * @param learningRate Learning rate
   * @param seed Seed for random numbers generation
   */
  public MF(
      DataModel datamodel,
      Tree tree,
      int numFactors,
      int numIters,
      double regularization,
      double learningRate,
      long seed) {
    super(datamodel);

    this.numFactors = numFactors;
    this.numIters = numIters;
    this.regularization = regularization;
    this.learningRate = learningRate;
    this.treeInstance = tree;

    this.seed = new Random(seed);

    // users factors initialization
    this.p = new double[datamodel.getNumberOfUsers()][numFactors];
    for (User user : super.getDataModel().getUsers()) {
      p[user.getUserIndex()] = this.random(this.numFactors, 0, 1);
    }

    // items factors initialization
    this.q = new double[datamodel.getNumberOfItems()][numFactors];
    for (Item item : super.getDataModel().getItems()) {
      q[item.getItemIndex()] = this.random(this.numFactors, 0, 1);
    }
  }

  public void fit() {
    boolean hasCondition = true;

    SymFunction[] puSfDiff = new SymFunction[this.numFactors];
    SymFunction[] qiSfDiff = new SymFunction[this.numFactors];

    for (int iter = 1; iter <= this.numIters; iter++) {

      // compute gradient
      double[][] dp = new double[super.getDataModel().getNumberOfUsers()][this.numFactors];
      double[][] dq = new double[super.getDataModel().getNumberOfItems()][this.numFactors];

      String oldFunc = "";

      for (User user : super.getDataModel().getUsers()) {
        for (int i = 0; i < user.getNumberOfRatings(); i++) {
          int itemIndex = user.getItemAt(i);

          HashMap<String, Double> params = getParams(p[user.getUserIndex()], q[itemIndex]);

          if (hasCondition) {
            treeInstance.reset();
            treeInstance.setFactorsValues(params);
            String func = treeInstance.getPrefix();

            if (!func.equals(oldFunc)) {
              try {
                this.sf = SymFunction.parse(func);
              } catch (Exception e) {
                this.sf = SymFunction.parse("Zero");
              }
              for (int k = 0; k < this.numFactors; k++) {
                puSfDiff[k] = this.sf.diff("pu" + k);
                qiSfDiff[k] = this.sf.diff("qi" + k);
              }
              if (hasCondition) hasCondition = treeInstance.getNodeTool().getHasCondition();
              oldFunc = func;
            }
          }

          double prediction = this.sf.eval(params);
          double error = user.getRatingAt(i) - prediction;

          for (int k = 0; k < this.numFactors; k++) {
            dp[user.getUserIndex()][k] +=
                this.learningRate
                    * (error * puSfDiff[k].eval(params)
                        - this.regularization * p[user.getUserIndex()][k]);
            dq[itemIndex][k] +=
                this.learningRate
                    * (error * qiSfDiff[k].eval(params) - this.regularization * q[itemIndex][k]);
          }
        }
      }

      // update users factors
      for (User user : super.getDataModel().getUsers()) {
        for (int k = 0; k < this.numFactors; k++) {
          p[user.getUserIndex()][k] += dp[user.getUserIndex()][k];
        }
      }

      // update items factors
      for (Item item : super.getDataModel().getItems()) {
        for (int k = 0; k < this.numFactors; k++) {
          q[item.getItemIndex()][k] += dq[item.getItemIndex()][k];
        }
      }
    }
  }

  @Override
  public synchronized double predict(int userIndex, int itemIndex) {
    HashMap<String, Double> params = getParams(p[userIndex], q[itemIndex]);
    treeInstance.reset();
    treeInstance.setFactorsValues(params);
    String func = treeInstance.getPrefix();

    SymFunction[] puSfDiff;
    SymFunction[] qiSfDiff;

    try {
      this.sf = SymFunction.parse(func);

      puSfDiff = new SymFunction[this.numFactors];
      qiSfDiff = new SymFunction[this.numFactors];

      for (int k = 0; k < this.numFactors; k++) {
        puSfDiff[k] = this.sf.diff("pu" + k);
        qiSfDiff[k] = this.sf.diff("qi" + k);
      }
    } catch (Exception e) {
      func = "Zero";

      this.sf = SymFunction.parse(func);

      puSfDiff = new SymFunction[this.numFactors];
      qiSfDiff = new SymFunction[this.numFactors];

      for (int k = 0; k < this.numFactors; k++) {
        puSfDiff[k] = this.sf.diff("pu" + k);
        qiSfDiff[k] = this.sf.diff("qi" + k);
      }
    }

    double prediction = this.sf.eval(params);
    return prediction;
  }

  public Tree getTree() {
    return this.treeInstance;
  }

  private double random(double min, double max) {
    return seed.nextDouble() * (max - min) + min;
  }

  private double[] random(int size, double min, double max) {
    double[] d = new double[size];
    for (int i = 0; i < size; i++) d[i] = this.random(min, max);
    return d;
  }

  private HashMap<String, Double> getParams(double[] pu, double[] qi) {
    HashMap<String, Double> map = new HashMap<>();
    for (int k = 0; k < this.numFactors; k++) {
      map.put("pu" + k, pu[k]);
      map.put("qi" + k, qi[k]);
    }
    return map;
  }
}
