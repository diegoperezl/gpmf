package printer;

import gpmf.Individual;
import gpmf.gp.treeGenerator.Tree;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Printer {

  private OutputStream outCSV;
  private OutputStream outJSON;

  private Writer writerCSV;
  private Writer writerJSON;

  public Printer() {
    try {
      String fileDateCSV = new SimpleDateFormat("yyyyMMddHHmm'.csv'").format(new Date());
      outCSV = new FileOutputStream("scores/scores" + fileDateCSV);
      String fileDateJSON = new SimpleDateFormat("yyyyMMddHHmm'.json'").format(new Date());
      outJSON = new FileOutputStream("scores/json" + fileDateJSON);

      writerCSV = new OutputStreamWriter(outCSV, "UTF-8");
      writerJSON = new OutputStreamWriter(outJSON, "UTF-8");

      writerJSON.write("[\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void printGenerationHead() {
    try {
      writerJSON.write("\t{\n" + "\t\t\"population\": [\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void printGenerationBody(Map<Integer, Individual> population) {
    for (int k = 0; k < population.size(); k++) {
      try {
        writerCSV.write(population.get(k).getScore() + ";");

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
        if (Double.isInfinite(individualScore) || Double.isNaN(individualScore)) {
          writerJSON.write("\t\t\t\t\"scoreMSE\": null,\n");
        } else {
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

        if (k == population.size() - 1) {
          writerJSON.write("\t\t\t}\n");
        } else {
          writerJSON.write("\t\t\t},\n");
        }

        writerJSON.flush();
        writerCSV.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void printGenerationMetrics(
      Map<Integer, Individual> population,
      double scoreMedian,
      double maeScore,
      double mseScore,
      int invalidChildren,
      int generationNumber) {
    try {
      writerCSV.write(scoreMedian + ";" + maeScore + ";" + mseScore + "\n");
      writerJSON.write("\t\t],\n");
      writerJSON.write("\t\t\"generationNumber\": " + generationNumber + ",\n");
      writerJSON.write("\t\t\"scoreMedianMSE\": " + scoreMedian + ",\n");
      writerJSON.write("\t\t\"invalidChildren\": " + invalidChildren + ",\n");
      writerJSON.write("\t\t\"bestScore\": " + population.get(0).getScore() + ",\n");
      if (Double.isNaN(maeScore)) writerJSON.write("\t\t\"bestMAEScoreTest\": null,\n");
      else writerJSON.write("\t\t\"bestMSEScoreTest\": " + +maeScore + ",\n");
      if (Double.isNaN(mseScore)) writerJSON.write("\t\t\"bestMSEScoreTest\": null,\n");
      else writerJSON.write("\t\t\"bestMSEScoreTest\": " + +mseScore + ",\n");
      writerJSON.write("\t\t\"bestScoreIndividualId\": " + population.get(0).getId() + "\n");
      writerJSON.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void printGenerationEnd(boolean isLast) {
    try {
      if (isLast) {
        writerJSON.write("\t}\n");
      } else {
        writerJSON.write("\t},\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      writerJSON.write("]");
      writerCSV.close();
      writerJSON.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
