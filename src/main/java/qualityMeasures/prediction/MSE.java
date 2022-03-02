package qualityMeasures.prediction;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.recommender.Recommender;
import qualityMeasures.QualityMeasuresGPMF;

public class MSE extends QualityMeasuresGPMF {

    private Recommender recommender;
    public MSE(Recommender recommender){
        super(recommender);
        this.recommender = recommender;
    }

    @Override
    public double getScore () {
        double sum = 0;
        int count = 0;

        for (TestUser user : recommender.getDataModel().getTestUsers()) {
            int userIndex = user.getUserIndex();

            for (int i = 0; i < user.getNumberOfTestRatings(); i++) {
                int itemIndex = user.getTestItemAt(i);
                double rating = user.getTestRatingAt(i);
                double prediction = recommender.predict(userIndex, itemIndex);

                sum += Math.pow(rating - prediction, 2);
                count++;
            }
        }
        return  sum / count;
    }
}
