package gpmf.gp.treeGenerator.nodeSupport;

import java.util.HashMap;

public class NodeTool {
    private int maxDepth;
    private int maxNodes;
    private int currentNodeNumber;
    private HashMap<String, Double> factorsValues;
    private String[] factors;
    private int numFactors;

    private double result = 1;
    private boolean printFirstResult = false;
    private boolean isWhileStmt = false;

    public NodeTool(int maxDepth, int maxNodes, int currentNodeNumber, int numFactors){
        this.setMaxDepth(maxDepth);
        this.setMaxNodes(maxNodes);
        this.setCurrentNodeNumber(currentNodeNumber);
        this.setFactors(numFactors);
        this.numFactors = numFactors;
    }

    public NodeTool clone(){
        NodeTool aux = new NodeTool(this.maxDepth, this.maxNodes, this.currentNodeNumber, this.numFactors);
        aux.setFactorsValues(this.factorsValues);
        aux.setResult(this.result);
        aux.setPrintFirstResult(this.printFirstResult);
        aux.setIsWhileStmt(this.isWhileStmt);
        return aux;

    }

    public void setMaxDepth(int maxDepthParam){ this.maxDepth = maxDepthParam;};
    public int getMaxDepth(){ return this.maxDepth;};
    public void setMaxNodes(int numberNodes){ this.maxNodes = numberNodes;}
    public  int getMaxNodes(){ return this.maxNodes;}
    public  void setCurrentNodeNumber(int number){ this.currentNodeNumber = number;}
    public  int getCurrentNodeNumber(){return this.currentNodeNumber;}
    public  void setResult(double resultParam){ this.result = resultParam;};
    public  double getResult(){ return this.result;};
    public  void setPrintFirstResult(boolean firstResult){this.printFirstResult = firstResult;}
    public  boolean getPrintFirstResult(){return this.printFirstResult;}
    public  void setIsWhileStmt(boolean isWhile){this.isWhileStmt = isWhile;}
    public  boolean getIsWhileStmt(){return this.isWhileStmt;}
    public  void addNodeNumber(){this.currentNodeNumber++;}

    public  void setFactors(int numFactors){
        this.factors = new String[numFactors*2+2];
        for(int i=0;i<numFactors;i++){
            this.factors[i] = "pu"+i;
        }
        for(int i=numFactors;i<numFactors*2;i++){
            this.factors[i] = "qi"+(i-numFactors);
        }
        this.factors[numFactors*2] = "Zero";
        this.factors[numFactors*2+1] = "One";
    }

    public  double getFactor(String key){return this.factorsValues.get(key);}

    public  void setFactorsValues(double[] factorsParam){
        this.factorsValues = new HashMap<String, Double>();
        for(int i=0;i<factorsParam.length/2;i++){
            this.factorsValues.put("pu"+i,factorsParam[i]);
        }
        for(int i=factorsParam.length/2;i<factorsParam.length;i++){
            this.factorsValues.put("qi"+(i-factorsParam.length/2),factorsParam[i]);
        }
        this.factorsValues.put("One",1.0);
        this.factorsValues.put("Zero",0.0);
    }

    public  void setFactorsValues(HashMap<String, Double> factorsValues){
        this.factorsValues = (HashMap<String, Double>) factorsValues.clone();
        this.factorsValues.put("One",1.0);
        this.factorsValues.put("Zero",0.0);
    }

    public  void reset(){
        this.setPrintFirstResult(false);
        this.setResult(1.0);
        this.setCurrentNodeNumber(0);
    }

    public  String selectLeafValue(){
        int factorSelection = (int)Math.floor(Math.random()*this.factors.length);
        return this.factors[factorSelection];
    }



    public  String selectStatement(int depth){
        double statementNodeSelection = Math.random();
        String statementNodeTypeSelection = null;

        double assignProb = (0.55+0.15*depth);
        double ifProb = (0.45-0.15*depth)/2+(7.5*depth);
        if(depth>=this.maxDepth || this.currentNodeNumber>this.maxNodes){
            statementNodeTypeSelection = "AssignStmt";
        }else{
            if(statementNodeSelection<=assignProb)
                statementNodeTypeSelection = "AssignStmt";
            else if(statementNodeSelection>assignProb && statementNodeSelection<=(assignProb+ifProb))
                statementNodeTypeSelection = "IFStmt";
            else
                statementNodeTypeSelection = "WHILEStmt";
        }
        return statementNodeTypeSelection;
    }

    public  String selectExpression(int depth){
        double expressionNodeSelection = Math.random();
        String expressionNodeTypeSelection = null;

        double constantProb = (0.3333+0.0666*depth);
        double restProb = (0.3333-0.0666*depth);
        if(depth>=this.maxDepth || this.currentNodeNumber>maxNodes){
            expressionNodeTypeSelection = "ConstantExpression";
        }else{
            if(expressionNodeSelection<=constantProb)
                expressionNodeTypeSelection = "ConstantExpression";
            else if(expressionNodeSelection>constantProb && expressionNodeSelection<=(constantProb+restProb))
                expressionNodeTypeSelection = "BinaryExpression";
            else
                expressionNodeTypeSelection = "UnaryExpression";
        }
        return expressionNodeTypeSelection;
    }

     public String selectConditionExpression(){
        double conditionNodeSelection = Math.random();
        String conditionNodeTypeSelection = null;

        if(conditionNodeSelection<=0.80)
            conditionNodeTypeSelection = "ComparisonExpression";
        else if(conditionNodeSelection>0.80 && conditionNodeSelection<=0.85)
            conditionNodeTypeSelection = "MultiConditionExpression";
        else
            conditionNodeTypeSelection = "NegationExpression";
        return conditionNodeTypeSelection;
    }

     public boolean addNewStmtDecision(){
        boolean decision = false;
        if(Math.random()<=0.5) decision = true;
        return decision;
    }

     public boolean addElseStmtDecision(){
        boolean decision = false;
        if(Math.random()<=0.8) decision = true;
        return decision;
    }
}
