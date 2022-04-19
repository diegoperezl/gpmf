package gpmf;

import gpmf.gp.treeGenerator.Tree;

public class Individual {

    private int id;
    private double learningRate;
    private double regularization;
    private int numIters;
    private int numFactors;
    private Tree tree;
    private int parent1;
    private int parent2;

    private double score;

    private boolean mutated;
    private Tree beforeMutation;

    public Individual(int id, double learningRate, double regularization, int numIters, int numFactors, Tree tree, int parent1, int parent2){
        this.id = id;
        this.learningRate = learningRate;
        this.regularization = regularization;
        this.numIters = numIters;
        this.numFactors = numFactors;
        this.tree = tree;
        this.parent1 = parent1;
        this.parent2 = parent2;

        this.score = Double.NaN;

        this.mutated = false;
        this.beforeMutation = null;
    }

    public int getId() {
        return id;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public double getRegularization() {
        return regularization;
    }

    public int getNumIters() {
        return numIters;
    }

    public int getNumFactors() {
        return numFactors;
    }

    public Tree getTree() {
        return tree;
    }

    public int getParent1() {
        return parent1;
    }

    public int getParent2() {
        return parent2;
    }

    public double getScore() {
        return score;
    }

    public boolean isMutated() {
        return mutated;
    }

    public Tree getBeforeMutation() {
        return beforeMutation;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public void setRegularization(double regularization) {
        this.regularization = regularization;
    }

    public void setNumIters(int numIters) {
        this.numIters = numIters;
    }

    public void setNumFactors(int numFactors) {
        this.numFactors = numFactors;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public void setParent1(int parent1) {
        this.parent1 = parent1;
    }

    public void setParent2(int parent2) {
        this.parent2 = parent2;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setMutated(boolean mutated) {
        this.mutated = mutated;
    }

    public void setBeforeMutation(Tree beforeMutation) {
        this.beforeMutation = beforeMutation;
    }
}
