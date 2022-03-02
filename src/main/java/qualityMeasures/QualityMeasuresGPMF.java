package qualityMeasures;

import es.upm.etsisi.cf4j.data.TestUser;
import es.upm.etsisi.cf4j.qualityMeasure.QualityMeasure;
import es.upm.etsisi.cf4j.recommender.Recommender;

public class QualityMeasuresGPMF extends QualityMeasure {

    private Recommender recommender;
    public QualityMeasuresGPMF(Recommender recommender){
        super(recommender);
        this.recommender = recommender;
    }

    @Override
    protected double getScore(TestUser testUser, double[] predictions) {
        return 0;
    }
}
