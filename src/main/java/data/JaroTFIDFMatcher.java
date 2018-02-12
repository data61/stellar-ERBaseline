package data;

import com.wcohen.ss.*;

public class JaroTFIDFMatcher implements AtomicMatch {
    private float threshold = 0.9F;
    private SoftTFIDF jaroTFID = null;

    public JaroTFIDFMatcher() {
        jaroTFID = new SoftTFIDF(new Jaro(), threshold);
    }

    public JaroTFIDFMatcher(float th) {
        threshold = th;
        Jaro jaro = new Jaro();
        jaroTFID = new SoftTFIDF(jaro, threshold);
    }

    public boolean valuesMatch(String s1, String s2) {
        BasicStringWrapper sw1 = new BasicStringWrapper(s1);
        BasicStringWrapper sw2 = new BasicStringWrapper(s2);

        double tmp = jaroTFID.score(sw1, sw2);
        if (tmp > threshold)
            return true;
        else
            return false;

    }
}