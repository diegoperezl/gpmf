package gpmf;

import es.upm.etsisi.cf4j.data.DataModel;

import es.upm.etsisi.cf4j.recommender.Recommender;
import gpmf.gp.treeGenerator.Tree;
import gpmf.gp.treeGenerator.nodes.ConditionExpression;
import gpmf.gp.treeGenerator.nodes.Expression;
import gpmf.gp.treeGenerator.nodes.Node;
import gpmf.gp.treeGenerator.nodes.Statement;
import gpmf.mf.MF;
import qualityMeasures.prediction.MSE;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class GPMF extends Recommender {

  /** Number of latent factors */
  private final int numFactors;

  /** Regularization */
  private final double regularization;

  /** Learning Rate */
  private final double learningRate;

  /** Number of generations */
  private final int gens;

  /** Probability of mutation */
  private final double pbmut;

  /** Probability of crossover */
  private final double pbx;

  /** Size of population */
  private final int popSize;

  /** Number of iterations */
  private final int numIters;

  /** Max initial depth of trees */
  private final int maxDepthInit;

  /** Max final depth of trees */
  private final int maxDepthFinal;

  /** Max initial number of nodes of trees */
  private final int maxNodesInit;

  /** Max final number of nodes of trees */
  private final int maxNodesFinal;

  /** Number of children */
  private final int numChildren;

  /** Random generator of the model */
  private final Random rand;

  /** Seed of the model */
  private final long seed;

  /** HashMap of population trees */
  private Map<Integer, Tree> population = Collections.synchronizedMap(new HashMap<Integer, Tree>());

  /** HashMap of children trees */
  private Map<Integer, Tree> children = Collections.synchronizedMap(new HashMap<Integer, Tree>());

  /** HashMap of population scores */
  private Map<Integer, Double> scores = Collections.synchronizedMap(new HashMap<Integer, Double>());

  /** HashMap of children scores */
  private Map<Integer, Double> childrenScores =
      Collections.synchronizedMap(new HashMap<Integer, Double>());

  /** MF instance of best tree */
  private volatile MF bestMF = null;

  /** Number of invalid children */
  private int invalidChildren = 0;

  /**
   * Model constructor from a Map containing the model's hyper-parameters values. Map object must
   * contains the following keys:
   *
   * <ul>
   *   <li><b>numFactors</b>: int value with the number of factors.
   *   <li><b>regularization</b>: double value with the regularization.
   *   <li><b>learningRate</b>: double value with the learning rate.
   *   <li><b>gens</b>: int value with the number of generations.
   *   <li><b>pbmut</b>: double value with the probability of mutation.
   *   <li><b>pbx</b>: double value with the probability of crossover.
   *   <li><b>popSize</b>: int value with the population size.
   *   <li><b>numIters</b>: int value with the number of iterations.
   *   <li><b>numChildren</b>: int value with the number of children.
   *   <li><b>maxDepthInit</b>: int value with the maximum initial depth of trees.
   *   <li><b>maxDepthFinal</b>: int value with the maximum final depth of trees.
   *   <li><b>maxNodesInit</b>: int value with the maximum initial number of nodes.
   *   <li><b>maxNodesFinal</b>: int value with the maximum final number of nodes.
   *   <li><b><em>seed</em></b> (optional): random seed for random numbers generation. If missing,
   *       random value is used.
   * </ul>
   *
   * @param datamodel DataModel instance
   * @param params Model's hyper-parameters values
   */
  public GPMF(DataModel datamodel, Map<String, Object> params) {
    this(
        datamodel,
        (int) params.get("numFactors"),
        (double) params.get("regularization"),
        (double) params.get("learningRate"),
        (int) params.get("gens"),
        (double) params.get("pbmut"),
        (double) params.get("pbx"),
        (int) params.get("popSize"),
        (int) params.get("numIters"),
        (int) params.get("numChildren"),
        (int) params.get("maxDepthInit"),
        (int) params.get("maxDepthFinal"),
        (int) params.get("maxNodesInit"),
        (int) params.get("maxNodesFinal"),
        params.containsKey("seed") ? (long) params.get("seed") : System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param numFactors Number of factors
   * @param regularization Regularization
   * @param learningRate Learning rate
   * @param gens Number of generations
   * @param pbmut Probability of mutation
   * @param pbx Probability of crossover
   * @param popSize Population size
   * @param numIters Number of iterations
   * @param numChildren Number of children
   * @param maxDepthInit Max initial depth of trees
   * @param maxDepthFinal Max final depth of trees
   * @param maxNodesInit Max initial number of nodes
   * @param maxNodesFinal Max final number of nodes
   */
  public GPMF(
      DataModel datamodel,
      int numFactors,
      double regularization,
      double learningRate,
      int gens,
      double pbmut,
      double pbx,
      int popSize,
      int numIters,
      int numChildren,
      int maxDepthInit,
      int maxDepthFinal,
      int maxNodesInit,
      int maxNodesFinal) {
    this(
        datamodel,
        numFactors,
        regularization,
        learningRate,
        gens,
        pbmut,
        pbx,
        popSize,
        numIters,
        numChildren,
        maxDepthInit,
        maxDepthFinal,
        maxNodesInit,
        maxNodesFinal,
        System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param numFactors Number of factors
   * @param regularization Regularization
   * @param learningRate Learning rate
   * @param gens Number of generations
   * @param pbmut Probability of mutation
   * @param pbx Probability of crossover
   * @param popSize Population size
   * @param numIters Number of iterations
   * @param numChildren Number of children
   * @param maxDepthInit Max initial depth of trees
   * @param maxDepthFinal Max final depth of trees
   * @param maxNodesInit Max initial number of nodes
   * @param maxNodesFinal Max final number of nodes
   */
  public GPMF(
      DataModel datamodel,
      int numFactors,
      double regularization,
      double learningRate,
      int gens,
      double pbmut,
      double pbx,
      int popSize,
      int numIters,
      int numChildren,
      int maxDepthInit,
      int maxDepthFinal,
      int maxNodesInit,
      int maxNodesFinal,
      long seed) {

    super(datamodel);
    this.numFactors = numFactors;
    this.regularization = regularization;
    this.learningRate = learningRate;
    this.gens = gens;
    this.pbmut = pbmut;
    this.pbx = pbx;
    this.popSize = popSize;
    this.numIters = numIters;
    this.numChildren = numChildren;
    this.maxDepthInit = maxDepthInit;
    this.maxDepthFinal = maxDepthFinal;
    this.maxNodesInit = maxNodesInit;
    this.maxNodesFinal = maxNodesFinal;

    this.rand = new Random(seed);
    this.seed = seed;

    for (int i = 0; i < this.popSize; i++) {
      population.put(i, new Tree(this.maxDepthInit, this.numFactors, this.maxNodesInit, this.rand, 0));
      population.get(i).generateTree();
    }

    for (int i = 0; i < this.popSize; i++) {
      scores.put(i, 0.0);
    }
  }

  @Override
  public synchronized double predict(int userIndex, int itemIndex) {
    return this.bestMF.predict(userIndex, itemIndex);
  }

  @Override
  public void fit() {

    OutputStream out = null;
    try {
      String fileDate = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
      out = new FileOutputStream("scores/scores" + fileDate);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(out, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    //System.out.println("Generation number: " + 0);

    try {
      this.trainGeneration();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    int bestIndex = 0;
    double best = Double.MAX_VALUE;
    for (int i = 0; i < this.gens; i++) {
      try {
        this.newGeneration();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      for (int k = 0; k < this.popSize; k++) {
        try {
          writer.write(scores.get(k) + ";");
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (scores.get(k) < best) {
          best = scores.get(k);
          bestIndex = k;
        }
      }
      try {
        writer.write(best + "\n");
        writer.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }

      System.out.print("\nGeneration number " + i + " with best result: " + best);
      System.out.println(" | Number of invalid children: " + this.invalidChildren);
      this.invalidChildren = 0;
    }

    System.out.println("Best final result: " + best);
    // population.get(bestIndex).reset();
    // population.get(bestIndex).restructure();
    // DrawTree.draw(population.get(bestIndex));

    this.bestMF =
        new MF(
            datamodel,
            population.get(bestIndex),
            this.numFactors,
            this.numIters,
            this.regularization,
            this.learningRate,
            this.seed);
    this.bestMF.fit();

    try {
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void newGeneration() throws ExecutionException, InterruptedException {
    int[] parents1 = new int[this.numChildren / 2];
    int[] parents2 = new int[this.numChildren / 2];

    //System.out.println("Selecting parents...");
    this.selectParents(parents1, parents2);
    //System.out.println("CrossOver...");
    this.crossOver(parents1, parents2);
    //System.out.println("Mutation...");
    this.mutation();
    //System.out.println("Training children...");
    this.trainChildren();
    //System.out.println("Selecting survivors...");
    this.selectSurvivors();
  }

  private void selectSurvivors() {
    int totalSize = this.popSize + this.numChildren;
    HashMap<Integer, Double> totalScores = new HashMap<Integer, Double>();
    HashMap<Integer, Tree> totalPopulation = new HashMap<Integer, Tree>();
    int[] inserted = new int[this.popSize];
    for (int i = 0; i < this.popSize; i++) inserted[i] = -1;

    for (int i = 0; i < this.popSize; i++) {
      totalScores.put(i, scores.get(i));
      totalPopulation.put(i, population.get(i).clone());
    }
    for (int i = 0; i < this.numChildren; i++) {
      totalScores.put(i + this.popSize, childrenScores.get(i));
      totalPopulation.put(i + this.popSize, children.get(i).clone());
    }

    // Elite selection
    HashMap<Integer, Double> sorted = sortScoresByValue(totalScores);

    Iterator it = sorted.entrySet().iterator();
    Map.Entry<Integer, Double> elite1 = (Map.Entry) it.next();
    population.put(0, totalPopulation.get(elite1.getKey()));
    scores.put(0, totalScores.get(elite1.getKey()));
    Map.Entry<Integer, Double> elite2 = (Map.Entry) it.next();
    population.put(1, totalPopulation.get(elite2.getKey()));
    scores.put(1, totalScores.get(elite2.getKey()));

    inserted[0] = elite1.getKey();
    inserted[1] = elite2.getKey();

    // RouletteWheel Selection
    double N = 0;
    it = sorted.entrySet().iterator();
    double worstValue = 0;
    while (it.hasNext()) {
      Map.Entry<Integer, Double> entry = (Map.Entry) it.next();
      if (!(Double.isNaN(entry.getValue()) || Double.isInfinite(entry.getValue())))
        worstValue = entry.getValue();
    }

    it = sorted.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Double> entry = (Map.Entry) it.next();
      if (!(Double.isNaN(entry.getValue()) || Double.isInfinite(entry.getValue())))
        N += (entry.getValue() - worstValue);
      else totalSize--;
    }

    double[] probabilities = new double[totalSize];
    HashMap<Integer, Double> finalScores = new HashMap<Integer, Double>();
    HashMap<Integer, Tree> finalPopulation = new HashMap<Integer, Tree>();

    it = totalScores.entrySet().iterator();
    int k = 0;
    for (int i = 0; i < totalScores.size(); i++) {
      if (!(Double.isNaN(totalScores.get(i)) || Double.isInfinite(totalScores.get(i)))) {
        if (k == 0) {
          probabilities[k] = (totalScores.get(i) - worstValue) / N;
          finalScores.put(k, totalScores.get(i));
          finalPopulation.put(k, totalPopulation.get(i).clone());
        } else {
          probabilities[k] = probabilities[k - 1] + ((totalScores.get(i) - worstValue) / N);
          finalScores.put(k, totalScores.get(i));
          finalPopulation.put(k, totalPopulation.get(i).clone());
        }
        k++;
      }
    }

    // Select survivors
    for (int i = 2; i < this.popSize; i++) {
      double selection = this.rand.nextDouble();
      boolean found = false;
      for (int j = 0; j < probabilities.length && !found; j++) {
        int finalJ = j;
        if (selection < probabilities[j] && !IntStream.of(inserted).anyMatch(x -> x == finalJ)) {
          population.put(i, finalPopulation.get(j).clone());
          scores.put(i, finalScores.get(j));
          inserted[i] = j;
          found = true;
        }
      }
    }
  }

  private HashMap<Integer, Double> sortScoresByValue(HashMap<Integer, Double> hm) {
    List<Map.Entry<Integer, Double>> list =
        new LinkedList<Map.Entry<Integer, Double>>(hm.entrySet());

    Collections.sort(
        list,
        new Comparator<Map.Entry<Integer, Double>>() {
          public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
            return (o1.getValue()).compareTo(o2.getValue());
          }
        });

    HashMap<Integer, Double> temp = new LinkedHashMap<Integer, Double>();
    for (Map.Entry<Integer, Double> aa : list) {
      temp.put(aa.getKey(), aa.getValue());
    }
    return temp;
  }

  private HashMap<Integer, Tree> sortPopulationByValue(HashMap<Integer, Tree> hm) {
    List<Map.Entry<Integer, Tree>> list =
        new LinkedList<Map.Entry<Integer, Tree>>(
            (Collection<? extends Map.Entry<Integer, Tree>>) hm.entrySet());

    Collections.sort(
        list,
        new Comparator<Map.Entry<Integer, Tree>>() {
          public int compare(Map.Entry<Integer, Tree> o1, Map.Entry<Integer, Tree> o2) {
            return (o1.getValue().toString()).compareTo(o2.getValue().toString());
          }
        });

    HashMap<Integer, Tree> temp = new LinkedHashMap<Integer, Tree>();
    for (Map.Entry<Integer, Tree> aa : list) {
      temp.put(aa.getKey(), aa.getValue());
    }
    return temp;
  }

  private void trainChildren() throws ExecutionException, InterruptedException {
    ForkJoinPool myPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    myPool
        .submit(
            () ->
                children.entrySet().parallelStream()
                    .forEach(
                        entry -> {
                          try {
                            if (!Double.isNaN(entry.getValue().getScore())) {
                              Recommender mf =
                                  new MF(
                                      this.datamodel,
                                      entry.getValue().clone(),
                                      this.numFactors,
                                      this.numIters,
                                      this.regularization,
                                      this.learningRate,
                                      this.seed);
                              mf.fit();

                              MSE mseInstance = new MSE(mf);
                              double mse = mseInstance.getScore(1);

                              entry.getValue().setScore(mse);
                            }
                          } catch (Exception e) {
                            entry.getValue().setScore(Double.NaN);
                          }
                          if (Double.isNaN(entry.getValue().getScore())) {
                            this.invalidChildren++;
                          }
                          System.out.print(".");
                        }))
        .get();

    for (Map.Entry<Integer, Tree> individual : children.entrySet()) {
      childrenScores.put(individual.getKey(), individual.getValue().getScore());
    }
    System.gc();
  }

  private void mutation() {

    for (int i = 0; i < this.numChildren; i++) {
      if (this.rand.nextDouble() < this.pbmut) {

        int numNodes = children.get(i).getOffspring();
        int nodeMutation;
        Node node = null;
        String treeInstance1NodeClass = null;

        do {
          nodeMutation = (int) Math.floor(this.rand.nextDouble() * numNodes+1);
          try {
            node = children.get(i).getNode(nodeMutation);
            treeInstance1NodeClass = node.getNodeClass();
          } catch (Exception e) {
          }
        } while (node == null);

        Node mutation = null;

        switch (treeInstance1NodeClass) {
          case "Statement":
            String statementNodeTypeSelection = node.getNodeTool().selectStatement(0);
            mutation =
                new Statement(statementNodeTypeSelection, 0, node.getParent(), node.getNodeTool());
            break;
          case "Expression":
            String expressionNodeTypeSelection = node.getNodeTool().selectExpression(0);
            mutation =
                new Expression(
                    expressionNodeTypeSelection, 0, node.getParent(), node.getNodeTool());
            break;
          case "ConditionExpression":
            String conditionNodeTypeSelection = node.getNodeTool().selectConditionExpression();
            mutation =
                new ConditionExpression(
                    conditionNodeTypeSelection,
                    0,
                    node.getParent(),
                    node.getNodeTool().getIsWhileStmt(),
                    node.getNodeTool());
            break;
        }

        mutation.expand();
        children.get(i).setNode(mutation, nodeMutation);
        children.get(i).reset();
        children.get(i).restructure();
        if(children.get(i).getOffspring() > this.maxNodesFinal || children.get(i).getDepth() > this.maxDepthFinal){
          children.get(i).setScore(Double.NaN);
        }
      }
    }
  }

  private void crossOver(int[] parents1, int[] parents2) {
    for (int i = 0; i < this.numChildren / 2; i++) {
      if (this.rand.nextDouble() < this.pbx) {
        this.children.put(i * 2, population.get(parents1[i]).clone());
        this.children.put(i * 2 + 1, population.get(parents2[i]).clone());

        int numNodesParent1 = children.get(i * 2).getOffspring();
        int numNodesParent2 = children.get(i * 2 + 1).getOffspring();

        Node node1 = null;
        Node node2 = null;
        String treeInstance1NodeClass = null;
        String treeInstance2NodeClass = null;
        int nodeCrossover1 = -1;
        int nodeCrossover2 = -1;

        do {
          nodeCrossover1 = (int) Math.floor(this.rand.nextDouble() * numNodesParent1 + 1);
          nodeCrossover2 = (int) Math.floor(this.rand.nextDouble() * numNodesParent2 + 1);

          try {
            node1 = children.get(i * 2).getNode(nodeCrossover1);
            treeInstance1NodeClass = node1.getNodeClass();
          } catch (Exception e) {
          }

          try {
            node2 = children.get(i * 2 + 1).getNode(nodeCrossover2);
            treeInstance2NodeClass = node2.getNodeClass();
          } catch (Exception e) {
          }

        } while (treeInstance1NodeClass != treeInstance2NodeClass
            || node1 == null
            || node2 == null);

        this.children.get(i * 2).setNode(node2, nodeCrossover1);
        this.children.get(i * 2 + 1).setNode(node1, nodeCrossover2);

        this.children.get(i * 2).reset();
        this.children.get(i * 2).restructure();
        this.children.get(i * 2 + 1).reset();
        this.children.get(i * 2 + 1).restructure();
        if(children.get(i).getOffspring() > this.maxNodesFinal || children.get(i).getDepth() > this.maxDepthFinal){
          children.get(i*2).setScore(Double.NaN);
        }
        if(children.get(i).getOffspring() > this.maxNodesFinal || children.get(i).getDepth() > this.maxDepthFinal){
          children.get(i*2+1).setScore(Double.NaN);
        }
      }
    }
  }

  private void trainGeneration() throws ExecutionException, InterruptedException {
    ForkJoinPool myPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
    myPool
        .submit(
            () ->
                population.entrySet().parallelStream()
                    .forEach(
                        entry -> {
                          Recommender mf =
                              new MF(
                                  datamodel,
                                  entry.getValue(),
                                  this.numFactors,
                                  this.numIters,
                                  this.regularization,
                                  this.learningRate,
                                  this.seed);
                          mf.fit();

                          MSE mseInstance = new MSE(mf);
                          double mse = mseInstance.getScore(1);
                          entry.getValue().setScore(mse);
                        }))
        .get();

    for (Map.Entry<Integer, Tree> individual : population.entrySet()) {
      scores.put(individual.getKey(), individual.getValue().getScore());
    }
  }

  private void selectParents(int[] parents1, int[] parents2) {
    int[] gladiators = new int[4];
    for (int i = 0; i < this.numChildren / 2; i++) {
      for (int j = 0; j < gladiators.length; j++) {
        gladiators[j] = -1;
      }
      gladiators[0] = (int) Math.floor(this.rand.nextDouble() * this.popSize);
      do {
        gladiators[1] = (int) Math.floor(this.rand.nextDouble() * this.popSize);
      } while (compGladiator(1, gladiators));
      do {
        gladiators[2] = (int) Math.floor(this.rand.nextDouble() * this.popSize);
      } while (compGladiator(2, gladiators));
      do {
        gladiators[3] = (int) Math.floor(this.rand.nextDouble() * this.popSize);
      } while (compGladiator(3, gladiators));

      if (scores.get(gladiators[0]) < scores.get(gladiators[1])) parents1[i] = gladiators[0];
      else parents1[i] = gladiators[1];

      if (scores.get(gladiators[2]) < scores.get(gladiators[3])) parents2[i] = gladiators[2];
      else parents2[i] = gladiators[3];
    }
  }

  private boolean compGladiator(int gladiator, int[] gladiators) {
    boolean res = false;
    for (int i = 0; i < gladiators.length; i++) {
      if (gladiator != i) if (gladiators[i] == gladiators[gladiator]) res = true;
    }
    return res;
  }
}
