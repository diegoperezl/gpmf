package qualityMeasures.prediction;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.recommender.Recommender;
import qualityMeasures.QualityMeasuresGPMF;

public class MAE extends QualityMeasuresGPMF {

    private Recommender recommender;
    public MAE(Recommender recommender){
        super(recommender);
        this.recommender = recommender;
    }

    @Override
    public double getScore() {
        double sum = 0;
        int count = 0;

        for (TestUser user: recommender.getDataModel().getTestUsers()) {
            for (int i = 0; i < user.getNumberOfTestRatings(); i++) {
                int itemIndex = user.getTestItemAt(i);
                double rating = user.getTestRatingAt(i);
                double prediction = recommender.predict(user.getUserIndex(), itemIndex);

                sum += Math.abs(prediction - rating);
                count++;
            }
        }
        return  sum / count;
    }


/*
    public double MAE () {
        double sumTotal = 0;
        int countTotal = 0;

        for (TestUser testUser: recommender.getDataModel().getTestUsers()) {
            double sum = 0d;
            int count = 0;

            for (int i = 0; i < testUser.getNumberOfTestRatings(); i++) {
                int itemIndex = testUser.getTestItemAt(i);

                double rating = testUser.getTestRatingAt(i);
                double prediction = this.recommender.predict(testUser.getUserIndex(), itemIndex);

                sum += Math.abs(prediction - rating);
                count++;
            }

            sumTotal += sum / count;
            countTotal++;
        }
        return sumTotal / countTotal;
    }


 */
}
