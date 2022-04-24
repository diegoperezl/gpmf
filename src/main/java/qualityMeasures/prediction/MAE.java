package qualityMeasures.prediction;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.recommender.Recommender;
import qualityMeasures.QualityMeasure;

/**
 * Override MSE class of cf4j library. This class calculates the Mean Squared Error (MSE) between
 * the predictions and the test ratings.
 *
 * <p>MSE = &#8721;(&lt;test item rating prediction&gt; - &lt;test item rating&gt;)<sup>2</sup> /
 * &lt;number of predictions&gt;
 */
public class MAE extends QualityMeasure {

  /**
   * Constructor of the class which basically calls the father's one
   *
   * @param recommender Recommender instance for which the MSE are going to be computed
   */
  public MAE(Recommender recommender) {
    super(recommender);
  }

  @Override
  public double getScore(TestUser testUser, double[] predictions) {

    double sum = 0d;
    int count = 0;

    for (int pos = 0; pos < testUser.getNumberOfTestRatings(); pos++) {
      sum += Math.abs(predictions[pos] - testUser.getTestRatingAt(pos));
      count++;
    }

    return (count == 0) ? Double.NaN : (sum / count);
  }
}
