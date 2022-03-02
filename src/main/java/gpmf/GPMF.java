package gpmf;


import es.upm.etsisi.cf4j.data.DataModel;
import es.upm.etsisi.cf4j.recommender.Recommender;
import gpmf.gp.treeGenerator.Tree;
import gpmf.gp.treeGenerator.nodes.ConditionExpression;
import gpmf.gp.treeGenerator.nodes.Expression;
import gpmf.gp.treeGenerator.nodes.Node;
import gpmf.gp.treeGenerator.nodes.Statement;
import gpmf.gp.treeRepresentation.DrawTree;
import gpmf.mf.MF;
import qualityMeasures.prediction.MAE;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class GPMF extends Recommender{

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

    /** Max depth of trees */
    private final int maxDepth;

    /** Max number of nodes of trees */
    private final int maxNodes;

    /** Number of children */
    private final int numChildren;

    /** Seed of the model */
    private final Random seed;

    /** HashMap of population trees */
    private HashMap<Integer,Tree> population = new HashMap<Integer, Tree>();

    /** HashMap of children trees */
    private HashMap<Integer,Tree> children = new HashMap<Integer, Tree>();

    /** HashMap of population scores */
    private HashMap<Integer,Double> scores = new HashMap<Integer, Double>();

    /** HashMap of children scores */
    private HashMap<Integer,Double> childrenScores = new HashMap<Integer, Double>();

    /** MF instance of best tree */
    private MF bestMF = null;

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
     *   <li><b>maxDepth</b>: int value with the maximum depth of trees.
     *   <li><b>maxNodes</b>: int value with the maximum number of nodes.
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
                (int) params.get("maxDepth"),
                (int) params.get("maxNodes"),
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
     * @param maxDepth Max depth of trees
     * @param maxNodes Max nodes of trees
     */
    public GPMF(DataModel datamodel,
                int numFactors,
                double regularization,
                double learningRate,
                int gens,
                double pbmut,
                double pbx,
                int popSize,
                int numIters,
                int numChildren,
                int maxDepth,
                int maxNodes) {
        this(datamodel,
            numFactors,
            regularization,
            learningRate,
            gens,
            pbmut,
            pbx,
            popSize,
            numIters,
            numChildren,
            maxDepth,
            maxNodes,
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
     * @param maxDepth Max depth of trees
     * @param maxNodes Max nodes of trees
     */
    public GPMF(DataModel datamodel,
                int numFactors,
                double regularization,
                double learningRate,
                int gens,
                double pbmut,
                double pbx,
                int popSize,
                int numIters,
                int numChildren,
                int maxDepth,
                int maxNodes,
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
        this.maxDepth = maxDepth;
        this.maxNodes = maxNodes;

        this.seed = new Random(seed);

        for(int i = 0;i<this.popSize;i++) {
            population.put(i,new Tree(this.maxDepth, this.numFactors, this.maxNodes));
            population.get(i).generateTree();
        }

        for(int i = 0; i<this.popSize;i++){
            scores.put(i,0.0);
        }
    }

    @Override
    public double predict(int userIndex, int itemIndex) {
        return this.bestMF.predict(userIndex, itemIndex);
    }

    @Override
    public void fit() {

        OutputStream out = null;
        try {
            String fileDate = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
            out = new FileOutputStream("scores/scores"+fileDate);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        System.out.println("Generation number: "+0);

        try {
            this.trainGeneration();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int bestIndex = 0;
        double best=Double.MAX_VALUE;
        for(int i = 0;i<this.gens;i++) {
            try {
                this.newGeneration();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int k = 0; k < this.popSize; k++) {
                try {
                    writer.write(scores.get(k)+";");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(scores.get(k)<best) {
                    best = scores.get(k);
                    bestIndex = k;
                }
            }
            try {
                writer.write(best+"\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Generation number "+i+" with best result: "+best);

        }

        System.out.println("Best final result: "+best);
        population.get(bestIndex).reset();
        population.get(bestIndex).restructure();
        DrawTree.draw(population.get(bestIndex));

        this.bestMF = new MF(datamodel, children.get(0), this.numFactors, this.numIters, this.regularization, this.learningRate);

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void newGeneration() throws ExecutionException, InterruptedException {
        int[] parents1 = new int[this.numChildren/2];
        int[] parents2 = new int[this.numChildren/2];

        System.out.println("Selecting parents...");
        this.selectParents(parents1, parents2);
        System.out.println("CrossOver...");
        this.crossOver(parents1, parents2);
        System.out.println("Mutation...");
        this.mutation();
        System.out.println("Training children...");
        this.trainChildren();
        System.out.println("Selecting survivors...");
        this.selectSurvivors();

    }

    private void selectSurvivors(){
        int totalSize=this.popSize+this.numChildren;
        HashMap<Integer,Double> totalScores = new HashMap<Integer, Double>();
        HashMap<Integer,Tree> totalPopulation = new HashMap<Integer, Tree>();
        int[] inserted = new int[this.popSize];
        for (int i  = 0;i<this.popSize;i++) inserted[i] = -1;

        for(int i = 0;i<this.popSize;i++){
            totalScores.put(i,scores.get(i));
            totalPopulation.put(i,population.get(i).clone());
        }
        for(int i = 0;i<this.numChildren;i++){
            totalScores.put(i+this.popSize,childrenScores.get(i));
            totalPopulation.put(i+this.popSize,children.get(i).clone());
        }

        //Elite selection
        HashMap<Integer,Double> sorted = sortByValue(totalScores);

        Iterator it = sorted.entrySet().iterator();
        Map.Entry<Integer,Double> elite1 = (Map.Entry)it.next();
        population.put(0,totalPopulation.get(elite1.getKey()));
        scores.put(0,totalScores.get(elite1.getKey()));
        Map.Entry<Integer,Double> elite2 = (Map.Entry)it.next();
        population.put(1,totalPopulation.get(elite2.getKey()));
        scores.put(1,totalScores.get(elite2.getKey()));

        inserted[0] = elite1.getKey();
        inserted[1] = elite2.getKey();

        //RouletteWheel Selection
        double[] probabilities = new double[totalSize];

        double N = 0;
        it = sorted.entrySet().iterator();
        double worstValue = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, Double> entry = (Map.Entry)it.next();
            worstValue = entry.getValue();
        }

        it = sorted.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Double> entry = (Map.Entry)it.next();
            N += (entry.getValue()-worstValue);
        }

        it = totalScores.entrySet().iterator();
        for(int i = 0;i<totalSize;i++) {
            if (i == 0) {
                probabilities[i] = (totalScores.get(i) - worstValue) / N;
            }else{
                probabilities[i] = probabilities[i-1] + ((totalScores.get(i) - worstValue) / N);
            }
        }

        //Select survivors
        for(int i = 2;i<this.popSize;i++){
            double selection = this.seed.nextDouble();
            boolean found = false;
            for(int j = 0;j<probabilities.length && !found;j++){
                int finalJ = j;
                if(selection < probabilities[j] && !IntStream.of(inserted).anyMatch(x -> x == finalJ)){
                    population.put(i,totalPopulation.get(j).clone());
                    scores.put(i,totalScores.get(j));
                    inserted[i] = j;
                    found = true;
                }
            }
        }

    }

    private HashMap<Integer, Double> sortByValue(HashMap<Integer, Double> hm) {
        List<Map.Entry<Integer, Double> > list = new LinkedList<Map.Entry<Integer, Double> >(hm.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, Double> >() {
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        HashMap<Integer, Double> temp = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private void trainChildren() throws ExecutionException, InterruptedException {
        ForkJoinPool myPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        myPool.submit(() ->
            children.entrySet ()
                    .parallelStream ()
                    .forEach (entry -> {
                        Integer key = entry.getKey ();

                        Recommender mf = new MF(datamodel, children.get(key), this.numFactors, this.numIters, this.regularization, this.learningRate);
                        mf.fit();
                        MAE maeInstance = new MAE(mf);
                        double mae = maeInstance.getScore();

                        while(Double.isNaN(mae)){
                            children.get(key).regenerate();
                            mf = new MF(datamodel, children.get(key), this.numFactors, this.numIters, this.regularization, this.learningRate);
                            mf.fit();
                            maeInstance = new MAE(mf);
                            mae = maeInstance.getScore();
                        }

                        childrenScores.put(key,mae);
                        System.out.println("MAE of tree "+key+": "+mae);

                    })
        ).get();

    }

    private void mutation(){

        for(int i = 0; i<this.numChildren;i++){
            if(this.seed.nextDouble()<this.pbmut){
                children.get(i).reset();
                children.get(i).restructure();
                int numNodes = children.get(i).getOffspring();
                int nodeMutation;
                Node node = null;
                String treeInstance1NodeClass = null;

                do {
                    nodeMutation = (int) Math.floor(this.seed.nextDouble() * numNodes);
                    try {
                        node = children.get(i).getNode(nodeMutation);
                        treeInstance1NodeClass = node.getNodeClass();
                    }catch (Exception e){}
                }while(node==null);

                Node mutation = null;

                switch (treeInstance1NodeClass) {
                    case "Statement":
                        String statementNodeTypeSelection =  node.getNodeTool().selectStatement(0);
                        mutation = new Statement(statementNodeTypeSelection, 0, node.getParent(), node.getNodeTool());
                        break;
                    case "Expression":
                        String expressionNodeTypeSelection = node.getNodeTool().selectExpression(0);
                        mutation = new Expression(expressionNodeTypeSelection, 0, node.getParent(), node.getNodeTool());
                        break;
                    case "ConditionExpression":
                        String conditionNodeTypeSelection = node.getNodeTool().selectConditionExpression();
                        mutation = new ConditionExpression(conditionNodeTypeSelection, 0, node.getParent(), node.getNodeTool().getIsWhileStmt(), node.getNodeTool());
                        break;
                }

                mutation.expand();
                children.get(i).setNode(mutation, nodeMutation);
                children.get(i).reset();
                children.get(i).restructure();
            }
        }


    }

    private void crossOver(int[] parents1,int[] parents2){
        for(int i = 0; i<this.numChildren/2;i++){
            if(this.seed.nextDouble()<this.pbx) {
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
                    nodeCrossover1 = (int) Math.floor(this.seed.nextDouble() * numNodesParent1);
                    nodeCrossover2 = (int) Math.floor(this.seed.nextDouble() * numNodesParent2);
                    try {
                        node1 = children.get(i * 2).getNode(nodeCrossover1);
                        treeInstance1NodeClass = node1.getNodeClass();
                    } catch (Exception e) { }

                    try {
                        node2 = children.get(i * 2 + 1).getNode(nodeCrossover2);
                        treeInstance2NodeClass = node2.getNodeClass();
                    } catch (Exception e) { }

                } while (treeInstance1NodeClass != treeInstance2NodeClass || node1 == null || node2 == null);


                this.children.get(i * 2).setNode(node2, nodeCrossover1);
                this.children.get(i * 2 + 1).setNode(node1, nodeCrossover2);

                this.children.get(i * 2).reset();
                this.children.get(i * 2).restructure();
                this.children.get(i * 2 + 1).reset();
                this.children.get(i * 2 + 1).restructure();
            }
        }
    }

    private void trainGeneration() throws ExecutionException, InterruptedException {
        ForkJoinPool myPool = new ForkJoinPool(80);
        myPool.submit(() ->
            population.entrySet ()
                    .parallelStream ()
                    .forEach (entry -> {
                        Integer key = entry.getKey ();

                        Recommender mf = new MF(datamodel, population.get(key), this.numFactors, this.numIters, this.regularization, this.learningRate);
                        mf.fit();

                        MAE maeInstance = new MAE(mf);
                        double mae = maeInstance.getScore();

                        while(Double.isNaN(mae)){
                            population.get(key).regenerate();
                            mf = new MF(datamodel, population.get(key), this.numFactors, this.numIters, this.regularization, this.learningRate);
                            mf.fit();

                            maeInstance = new MAE(mf);
                            mae = maeInstance.getScore();
                        }

                        scores.put(key,mae);
                        System.out.println("MAE of tree "+key+": "+mae);

                    })
        ).get();
    }

    private void selectParents(int[] parents1, int[] parents2){
        int[] gladiators = new int[4];
        for(int i = 0;i<this.numChildren/2;i++){
            for (int j = 0; j < gladiators.length; j++) {
                gladiators[j] = -1;
            }
            gladiators[0] = (int) Math.floor(this.seed.nextDouble() * this.popSize);
            do {
                gladiators[1] = (int) Math.floor(this.seed.nextDouble() * this.popSize);
            } while (compGladiator(1,gladiators));
            do {
                gladiators[2] = (int) Math.floor(this.seed.nextDouble() * this.popSize);
            } while (compGladiator(2,gladiators));
            do {
                gladiators[3] = (int) Math.floor(this.seed.nextDouble() * this.popSize);
            } while (compGladiator(3,gladiators));

            if (scores.get(gladiators[0]) < scores.get(gladiators[1]))
                parents1[i] = gladiators[0];
            else
                parents1[i] = gladiators[1];

            if (scores.get(gladiators[2]) < scores.get(gladiators[3]))
                parents2[i] = gladiators[2];
            else
                parents2[i] = gladiators[3];
        }
    }

    private boolean compGladiator(int gladiator, int[] gladiators){
        boolean res = false;
        for(int i = 0;i<gladiators.length;i++){
            if(gladiator!=i)
                if(gladiators[i]==gladiators[gladiator])
                    res = true;
        }
        return res;
    }
}
