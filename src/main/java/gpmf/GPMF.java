package gpmf;

import es.upm.etsisi.cf4j.data.DataModel;

import es.upm.etsisi.cf4j.recommender.Recommender;
import es.upm.etsisi.cf4j.util.optimization.GridSearchCV;
import es.upm.etsisi.cf4j.util.optimization.ParamsGrid;
import gpmf.gp.treeGenerator.Tree;
import gpmf.gp.treeGenerator.nodes.ConditionExpression;
import gpmf.gp.treeGenerator.nodes.Expression;
import gpmf.gp.treeGenerator.nodes.Node;
import gpmf.gp.treeGenerator.nodes.Statement;
import gpmf.gp.treeRepresentation.DrawTree;
import gpmf.mf.MF;
import qualityMeasures.prediction.MAE;
import qualityMeasures.prediction.MSE;
//import qualityMeasures.prediction.MSE;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class GPMF extends Recommender {

  /** Number of generations */
  private final int gens;

  /** Probability of mutation */
  private final double pbmut;

  /** Probability of crossover */
  private final double pbx;

  /** Size of population */
  private final int popSize;

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

  /** Early stopping value */
  private final double earlyStoppingValue;

  /** Early stopping count */
  private final int earlyStoppingCount;

  /** Random generator of the model */
  private final Random rand;

  /** Seed of the model */
  private final long seed;

  /** HashMap of population trees */
  private Map<Integer, Individual> population = new HashMap<Integer, Individual>();

  /** HashMap of children trees */
  private Map<Integer, Individual> children = new HashMap<Integer, Individual>();

  /** HashMap of population scores */
  private Map<Integer, Double> scores = new HashMap<Integer, Double>();

  /** HashMap of children scores */
  private Map<Integer, Double> childrenScores = new HashMap<Integer, Double>();

  /** MF instance of best tree */
  private volatile MF bestMF = null;

  /** Number of invalid children */
  private int invalidChildren = 0;

  /** Id counter */
  private int idCount = 0;

  /**
   * Model constructor from a Map containing the model's hyper-parameters values. Map object must
   * contains the following keys:
   *
   * <ul>
   *   <li><b>gens</b>: int value with the number of generations.
   *   <li><b>pbmut</b>: double value with the probability of mutation.
   *   <li><b>pbx</b>: double value with the probability of crossover.
   *   <li><b>popSize</b>: int value with the population size.
   *   <li><b>numChildren</b>: int value with the number of children.
   *   <li><b>maxDepthInit</b>: int value with the maximum initial depth of trees.
   *   <li><b>maxDepthFinal</b>: int value with the maximum final depth of trees.
   *   <li><b>maxNodesInit</b>: int value with the maximum initial number of nodes.
   *   <li><b>maxNodesFinal</b>: int value with the maximum final number of nodes.
   *   <li><b>earlyStoppingValue</b>: double value with the early stopping bound, if the mean of the
   *       scores is less than this value the early stopping counter is increased by one.
   *   <li><b>earlyStoppingCount</b>: int value with the bound of generations without improvement,
   *       this improvement is limited by the value earlyStoppingValue.
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
        (int) params.get("gens"),
        (double) params.get("pbmut"),
        (double) params.get("pbx"),
        (int) params.get("popSize"),
        (int) params.get("numChildren"),
        (int) params.get("maxDepthInit"),
        (int) params.get("maxDepthFinal"),
        (int) params.get("maxNodesInit"),
        (int) params.get("maxNodesFinal"),
        (double) params.get("earlyStoppingValue"),
        (int) params.get("earlyStoppingCount"),
        params.containsKey("seed") ? (long) params.get("seed") : System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param gens Number of generations
   * @param pbmut Probability of mutation
   * @param pbx Probability of crossover
   * @param popSize Population size
   * @param numChildren Number of children
   * @param maxDepthInit Max initial depth of trees
   * @param maxDepthFinal Max final depth of trees
   * @param maxNodesInit Max initial number of nodes
   * @param maxNodesFinal Max final number of nodes
   * @param earlyStoppingValue Limit that considers that a generation has improved
   * @param earlyStoppingCount Limit of number of generations without improvement
   */
  public GPMF(
      DataModel datamodel,
      int gens,
      double pbmut,
      double pbx,
      int popSize,
      int numChildren,
      int maxDepthInit,
      int maxDepthFinal,
      int maxNodesInit,
      int maxNodesFinal,
      double earlyStoppingValue,
      int earlyStoppingCount) {
    this(
        datamodel,
        gens,
        pbmut,
        pbx,
        popSize,
        numChildren,
        maxDepthInit,
        maxDepthFinal,
        maxNodesInit,
        maxNodesFinal,
        earlyStoppingValue,
        earlyStoppingCount,
        System.currentTimeMillis());
  }

  /**
   * Model constructor
   *
   * @param datamodel DataModel instance
   * @param gens Number of generations
   * @param pbmut Probability of mutation
   * @param pbx Probability of crossover
   * @param popSize Population size
   * @param numChildren Number of children
   * @param maxDepthInit Max initial depth of trees
   * @param maxDepthFinal Max final depth of trees
   * @param maxNodesInit Max initial number of nodes
   * @param maxNodesFinal Max final number of nodes
   * @param earlyStoppingValue Limit that considers that a generation has improved
   * @param earlyStoppingCount Limit of number of generations without improvement
   */
  public GPMF(
      DataModel datamodel,
      int gens,
      double pbmut,
      double pbx,
      int popSize,
      int numChildren,
      int maxDepthInit,
      int maxDepthFinal,
      int maxNodesInit,
      int maxNodesFinal,
      double earlyStoppingValue,
      int earlyStoppingCount,
      long seed) {

    super(datamodel);
    this.gens = gens;
    this.pbmut = pbmut;
    this.pbx = pbx;
    this.popSize = popSize;
    this.numChildren = numChildren;
    this.maxDepthInit = maxDepthInit;
    this.maxDepthFinal = maxDepthFinal;
    this.maxNodesInit = maxNodesInit;
    this.maxNodesFinal = maxNodesFinal;
    this.earlyStoppingValue = earlyStoppingValue;
    this.earlyStoppingCount = earlyStoppingCount;

    this.rand = new Random(seed);
    this.seed = seed;

    int[] scalar = new int[] {1, 10, 100, 1000};
    for (int i = 0; i < this.popSize; i++) {
      double learningRate =
          this.rand.nextDouble() / scalar[(int) Math.floor(this.rand.nextDouble() * 4)];
      double regularization =
          this.rand.nextDouble() / scalar[(int) Math.floor(this.rand.nextDouble() * 4)];
      int numIters = (int) Math.floor(this.rand.nextDouble() * 30 + 1) * 10;
      int numFactors = 6;

      Tree individualTree =
          new Tree(this.maxDepthInit, numFactors, this.maxNodesInit, this.rand, 0);
      individualTree.generateTree();
      population.put(
          i,
          new Individual(
              this.idCount++, learningRate, regularization, numIters, numFactors, individualTree, -1, -1));
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
    OutputStream outCSV = null;
    OutputStream outJSON = null;
    try {
      String fileDateCSV = new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date());
      outCSV = new FileOutputStream("scores/scores" + fileDateCSV);
      String fileDateJSON = new SimpleDateFormat("yyyyMMddHHmm'.json'").format(new Date());
      outJSON = new FileOutputStream("scores/json" + fileDateJSON);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    Writer writerCSV = null;
    Writer writerJSON = null;
    try {
      writerCSV = new OutputStreamWriter(outCSV, "UTF-8");
      writerJSON = new OutputStreamWriter(outJSON, "UTF-8");

      writerJSON.write("[\n");

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    double previousMean = 0;


    boolean[] train = new boolean[popSize];
    for (int i = 0; i < popSize; i++) {
      train[i] = true;
    }
    try {
      this.trainGeneration(train, popSize);


      boolean cleanFirstGeneration;
      do{
        cleanFirstGeneration = true;
        int poolSize = 0;
        for (int i = 0; i < popSize; i++) {
          train[i] = false;
        }
        for (int i = 0; i < popSize; i++) {
          if (Double.isNaN(population.get(i).getScore()) || Double.isInfinite(population.get(i).getScore())){

            System.out.println("Regenerando");

            int[] scalar = new int[] {1, 10, 100, 1000};

            double learningRate =
                    this.rand.nextDouble() / scalar[(int) Math.floor(this.rand.nextDouble() * 4)];
            double regularization =
                    this.rand.nextDouble() / scalar[(int) Math.floor(this.rand.nextDouble() * 4)];
            int numIters = (int) Math.floor(this.rand.nextDouble() * 30 + 1) * 10;
            int numFactors = 6;

            population.get(i).setNumIters(numIters);
            population.get(i).setRegularization(regularization);
            population.get(i).setNumFactors(numFactors);
            population.get(i).setLearningRate(learningRate);
            population.get(i).getTree().regenerate();

            cleanFirstGeneration = false;
            train[i] = true;
            poolSize++;
          }
        }
        trainGeneration(train,poolSize);
      }while(!cleanFirstGeneration);

    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    double totalScoresValue = 0;
    int totalScoresCount = 0;

    for (int k = 0; k < this.popSize; k++) {
      // writerCSV.write(scores.get(k) + ";");
      if (!Double.isNaN(scores.get(k))) {
        totalScoresValue += scores.get(k);
        totalScoresCount++;
      }
    }

    previousMean = totalScoresValue / totalScoresCount;

    int finishCount = 0;

    for (int i = 0; i < this.gens && finishCount < this.earlyStoppingCount; i++) {
      try {
        writerJSON.write("\t{\n" + "\t\t\"population\": [\n");
      } catch (IOException e) {
        e.printStackTrace();
      }

      try {
        this.newGeneration();
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      totalScoresValue = 0;
      totalScoresCount = 0;

      for (int k = 0; k < this.popSize; k++) {
        try {
          writerCSV.write(scores.get(k) + ";");

          writerJSON.write("\t\t\t{\n");
          writerJSON.write("\t\t\t\t\"id\": " + population.get(k).getId() + ",\n");
          writerJSON.write(
              "\t\t\t\t\"learningRate\": " + population.get(k).getLearningRate() + ",\n");
          writerJSON.write(
              "\t\t\t\t\"regularization\": " + population.get(k).getRegularization() + ",\n");
          writerJSON.write("\t\t\t\t\"numIters\": " + population.get(k).getNumIters() + ",\n");
          writerJSON.write("\t\t\t\t\"parent1Id\": " + population.get(k).getParent1() + ",\n");
          writerJSON.write("\t\t\t\t\"parent2Id\": " + population.get(k).getParent2() + ",\n");
          writerJSON.write(
              "\t\t\t\t\"treeRepresentation\": \"" + population.get(k).getTree().print() + "\",\n");
          double individualScore = population.get(k).getScore();
          if(Double.isInfinite(individualScore) || Double.isNaN(individualScore)){
            writerJSON.write("\t\t\t\t\"scoreMSE\": null,\n");
          }else{
            writerJSON.write("\t\t\t\t\"scoreMSE\": " + population.get(k).getScore() + ",\n");
          }
          writerJSON.write("\t\t\t\t\"isMutated\": " + population.get(k).isMutated() + ",\n");

          Tree program = population.get(k).getBeforeMutation();
          if (program != null) {
            writerJSON.write("\t\t\t\t\"treeBeforeMutation\": \"" + program.print() + "\",\n");
          } else {
            writerJSON.write("\t\t\t\t\"treeBeforeMutation\": null,\n");
          }
          writerJSON.write(
              "\t\t\t\t\"numNodes\": " + population.get(k).getTree().getOffspring() + ",\n");
          writerJSON.write("\t\t\t\t\"depth\": " + population.get(k).getTree().getDepth() + "\n");

          if (k == this.popSize - 1) {
            writerJSON.write("\t\t\t}\n");
          } else {
            writerJSON.write("\t\t\t},\n");
          }
          writerJSON.flush();
          if (!Double.isNaN(scores.get(k)) && !Double.isInfinite(scores.get(k))) {
            totalScoresValue += scores.get(k);
            totalScoresCount++;
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      double scoresMean = totalScoresValue / totalScoresCount;
      if (Math.abs(scoresMean - previousMean) < this.earlyStoppingValue) {
        finishCount++;
      } else {
        finishCount = 0;
      }

      previousMean = scoresMean;

      try {

        MF best = new MF(datamodel, population.get(0), 42L);
        best.fit();

        MAE maeInstance = new MAE(best);
        MSE mseInstance = new MSE(best);

        writerCSV.write(scoresMean + ";" +maeInstance.getScore()+";"+mseInstance.getScore()+"\n");
        writerCSV.flush();

        writerJSON.write("\t\t],\n");
        writerJSON.write("\t\t\"generationNumber\": " + i + ",\n");
        writerJSON.write("\t\t\"scoreAverageMSE\": " + scoresMean + ",\n");
        writerJSON.write("\t\t\"invalidChildren\": " + this.invalidChildren + ",\n");
        writerJSON.write("\t\t\"bestScore\": " + population.get(0).getScore() + ",\n");
        writerJSON.write("\t\t\"bestScoreIndividualId\": " + population.get(0).getId() + "\n");


        if (i == this.gens - 1 || finishCount == this.earlyStoppingCount) {
          writerJSON.write("\t}\n");
        } else {
          writerJSON.write("\t},\n");
        }
        writerJSON.flush();

      } catch (IOException e) {
        e.printStackTrace();
      }

      System.out.print(
          "\nGeneration number " + i + " with best result: " + population.get(0).getScore());
      System.out.println(" | Number of invalid children: " + this.invalidChildren);

      this.invalidChildren = 0;
    }

    try {
      writerJSON.write("]");
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("Best final result: " + population.get(0).getScore());

    this.bestMF = new MF(datamodel, population.get(0), this.seed);
    this.bestMF.fit();

    DrawTree.draw(this.bestMF.getTree());

    try {
      writerCSV.close();
      writerJSON.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void newGeneration() throws ExecutionException, InterruptedException {
    int[] parents1 = new int[this.numChildren / 2];
    int[] parents2 = new int[this.numChildren / 2];

    // System.out.println("Selecting parents...");
    this.selectParents(parents1, parents2);
    // System.out.println("CrossOver...");
    this.crossOver(parents1, parents2);
    // System.out.println("Mutation...");
    this.mutation();
    // System.out.println("Training children...");
    this.trainChildren();
    // System.out.println("Selecting survivors...");
    this.selectSurvivors();
  }

  private void selectSurvivors() {
    int totalSize = this.popSize + this.numChildren;
    HashMap<Integer, Double> totalScores = new HashMap<Integer, Double>();
    HashMap<Integer, Individual> totalPopulation = new HashMap<Integer, Individual>();
    int[] inserted = new int[this.popSize];

    for (int i = 0; i < this.popSize; i++) inserted[i] = -1;

    for (int i = 0; i < this.popSize; i++) {
      totalScores.put(i, scores.get(i));
      totalPopulation.put(i, population.get(i));
    }
    for (int i = 0; i < this.numChildren; i++) {
      totalScores.put(i + this.popSize, childrenScores.get(i));
      totalPopulation.put(i + this.popSize, children.get(i));
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
    HashMap<Integer, Individual> finalPopulation = new HashMap<Integer, Individual>();

    it = totalScores.entrySet().iterator();
    int k = 0;
    for (int i = 0; i < totalScores.size(); i++) {
      if (!(Double.isNaN(totalScores.get(i)) || Double.isInfinite(totalScores.get(i)))) {
        if (k == 0) {
          probabilities[k] = (totalScores.get(i) - worstValue) / N;
          finalScores.put(k, totalScores.get(i));
          finalPopulation.put(k, totalPopulation.get(i));
        } else {
          probabilities[k] = probabilities[k - 1] + ((totalScores.get(i) - worstValue) / N);
          finalScores.put(k, totalScores.get(i));
          finalPopulation.put(k, totalPopulation.get(i));
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
          population.put(i, finalPopulation.get(j));
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

  private void trainChildren() throws InterruptedException {

    long startTime = System.currentTimeMillis();

    Thread[] pool = new Thread[numChildren];
    for (int i = 0; i < numChildren; i++) {

      ParamsGrid paramsGrid = new ParamsGrid();

      paramsGrid.addFixedParam("individual", children.get(i));
      paramsGrid.addFixedParam("seed", this.seed);

      GridSearchCV gridSearchMF =
          new GridSearchCV(datamodel, paramsGrid, MF.class, MSE.class, 5, this.seed);

      Trainer t = new Trainer(gridSearchMF, children.get(i), true, this.rand);
      pool[i] = new Thread(t, String.valueOf(i));
      pool[i].start();
    }

    for (int i = 0; i < pool.length; i++) {
      pool[i].join();
      if (Double.isNaN(children.get(i).getScore())) this.invalidChildren++;
    }

    long endTime = System.currentTimeMillis();
    long timeElapsed = endTime - startTime;

    System.out.println();
    System.out.println("Tiempo entrenando: " + (timeElapsed / 1000));

    for (Map.Entry<Integer, Individual> individual : children.entrySet()) {
      childrenScores.put(individual.getKey(), individual.getValue().getScore());
    }
    System.gc();
  }

  private void mutation() {

    for (int i = 0; i < this.numChildren; i++) {

      // Tree mutation
      if (this.rand.nextDouble() < this.pbmut) {
        children.get(i).setBeforeMutation(children.get(i).getTree().clone());
        children.get(i).setMutated(true);
        int numNodes = children.get(i).getTree().getOffspring();
        int nodeMutation;
        Node node = null;
        String treeInstance1NodeClass = null;

        do {
          nodeMutation = (int) Math.floor(this.rand.nextDouble() * numNodes + 1);
          try {
            node = children.get(i).getTree().getNode(nodeMutation);
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
                    conditionNodeTypeSelection, 0, node.getParent(), node.getNodeTool());
            break;
        }

        mutation.expand();
        children.get(i).getTree().setNode(mutation, nodeMutation);
        children.get(i).getTree().reset();
        children.get(i).getTree().restructure();
        if (children.get(i).getTree().getOffspring() > this.maxNodesFinal
            || children.get(i).getTree().getDepth() > this.maxDepthFinal) {
          children.get(i).setScore(Double.NaN);
        }
      }

      // learningRate mutation
      if (this.rand.nextDouble() < this.pbmut) {
        boolean mulDiv = this.rand.nextBoolean();
        if (mulDiv) {
          if (children.get(i).getLearningRate() * 10 <= 1.0) {
            children.get(i).setLearningRate(children.get(i).getLearningRate() * 10);
          } else {
            children.get(i).setLearningRate(children.get(i).getLearningRate() / 10);
          }
        }
      }

      // regularization mutation
      if (this.rand.nextDouble() < this.pbmut) {
        boolean mulDiv = this.rand.nextBoolean();
        if (mulDiv) {
          if (children.get(i).getRegularization() * 10 <= 1.0) {
            children.get(i).setRegularization(children.get(i).getRegularization() * 10);
          } else {
            children.get(i).setRegularization(children.get(i).getRegularization() / 10);
          }
        }
      }

      // numIters mutation
      if (this.rand.nextDouble() < this.pbmut) {
        boolean mulDiv = this.rand.nextBoolean();
        if (mulDiv) {
          if (children.get(i).getNumIters() - 10 > 0) {
            children.get(i).setNumIters(children.get(i).getNumIters() - 10);
          } else {
            children.get(i).setNumIters(children.get(i).getNumIters() + 10);
          }
        }
      }
    }
  }

  private void crossOver(int[] parents1, int[] parents2) {
    for (int i = 0; i < this.numChildren / 2; i++) {
      if (this.rand.nextDouble() < this.pbx) {
        double learninGrateCross =
            (population.get(parents1[i]).getLearningRate()
                    + population.get(parents2[i]).getLearningRate())
                / 2;
        double regularizationCross =
            (population.get(parents1[i]).getRegularization()
                    + population.get(parents2[i]).getRegularization())
                / 2;
        int numItersCross =
            (int)
                Math.floor(
                    (population.get(parents1[i]).getNumIters()
                            + population.get(parents2[i]).getNumIters())
                        / 2);
        int numFactorsCross =
            (int)
                Math.floor(
                    (population.get(parents1[i]).getNumFactors()
                            + population.get(parents2[i]).getNumFactors())
                        / 2);

        this.children.put(
            i * 2,
            new Individual(
                this.idCount++,
                learninGrateCross,
                regularizationCross,
                numItersCross,
                numFactorsCross,
                population.get(parents1[i]).getTree().clone(),
                population.get(parents1[i]).getId(),
                population.get(parents2[i]).getId()));
        this.children.put(
                i * 2+1,
            new Individual(
                this.idCount++,
                learninGrateCross,
                regularizationCross,
                numItersCross,
                numFactorsCross,
                population.get(parents2[i]).getTree().clone(),
                population.get(parents1[i]).getId(),
                population.get(parents2[i]).getId()));

        children.get(i * 2).getTree().restructure();
        children.get(i * 2 + 1).getTree().restructure();

        int numNodesParent1 = children.get(i * 2).getTree().getOffspring();
        int numNodesParent2 = children.get(i * 2 + 1).getTree().getOffspring();

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
            node1 = children.get(i * 2).getTree().getNode(nodeCrossover1);
            treeInstance1NodeClass = node1.getNodeClass();
          } catch (Exception e) {
          }

          try {
            node2 = children.get(i * 2 + 1).getTree().getNode(nodeCrossover2);
            treeInstance2NodeClass = node2.getNodeClass();
          } catch (Exception e) {
          }

        } while (treeInstance1NodeClass != treeInstance2NodeClass
            || node1 == null
            || node2 == null);

        this.children.get(i * 2).getTree().setNode(node2, nodeCrossover1);
        this.children.get(i * 2 + 1).getTree().setNode(node1, nodeCrossover2);

        this.children.get(i * 2).getTree().reset();
        this.children.get(i * 2).getTree().restructure();
        this.children.get(i * 2 + 1).getTree().reset();
        this.children.get(i * 2 + 1).getTree().restructure();
        if (children.get(i * 2).getTree().getOffspring() > this.maxNodesFinal
            || children.get(i * 2).getTree().getDepth() > this.maxDepthFinal) {
          children.get(i * 2).setScore(Double.NaN);
        }
        if (children.get(i * 2 + 1).getTree().getOffspring() > this.maxNodesFinal
            || children.get(i * 2 + 1).getTree().getDepth() > this.maxDepthFinal) {
          children.get(i * 2 + 1).setScore(Double.NaN);
        }
      }
    }
  }

  private void trainGeneration(boolean[] train, int poolSize) throws ExecutionException, InterruptedException {
    Thread[] pool = new Thread[poolSize];
    int poolIndex = 0;
    for (int i = 0; i < popSize; i++) {
      if (train[i]) {
        ParamsGrid paramsGrid = new ParamsGrid();

        paramsGrid.addFixedParam("individual", population.get(i));
        paramsGrid.addFixedParam("seed", this.seed);

        GridSearchCV gridSearchCV =
            new GridSearchCV(datamodel, paramsGrid, MF.class, MSE.class, 5, this.seed);

        Trainer t = new Trainer(gridSearchCV, population.get(i), false, this.rand);
        pool[poolIndex] = new Thread(t, String.valueOf(i));
        pool[poolIndex].start();
        poolIndex++;
      }
    }

    for (int i = 0; i < pool.length; i++) {
      pool[i].join();
    }

    for (Map.Entry<Integer, Individual> individual : population.entrySet()) {
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

class Trainer implements Runnable {

  private final GridSearchCV gridSearchCV;
  private final Individual individual;
  private final boolean isChild;
  private final Random rand;

  Trainer(GridSearchCV gridSearchCV, Individual individual, boolean isChild, Random rand) {
    this.gridSearchCV = gridSearchCV;
    this.individual = individual;
    this.isChild = isChild;
    this.rand = rand;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    double mse = Double.POSITIVE_INFINITY;

    try {
      this.gridSearchCV.fit();
      mse = this.gridSearchCV.getBestScore();
      individual.setScore(mse);
    } catch (Exception e) {
      mse = Double.NaN;
      individual.setScore(mse);
    }

    long endTime = System.currentTimeMillis();
    System.out.println();
    System.out.println("########################################");
    System.out.println(
        "Ha tardado: "
            + ((endTime - startTime) / 1000)
            + " con "
            + individual.getTree().getOffspring()
            + " nodos y "
            + individual.getTree().getDepth()
            + " profundidad | learningRate: " + individual.getLearningRate() + ", regularization: "+individual.getRegularization()+" y numIters: "+individual.getNumIters()+

                "  Score: "
            + mse);
    System.out.println("########################################");
  }
}
