package data;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VenueMatcher implements AtomicMatch {

    float threshold = 0.5F;
    JaroTFIDFMatcher matcher;
    Map<String, String> venuePairs;

    public VenueMatcher(String dict)
            throws FileNotFoundException, UnsupportedEncodingException {

        CsvParserSettings settings = new CsvParserSettings();
        CsvParser parser = new CsvParser(settings);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(dict), "UTF-8");
        List<String[]> allRows = parser.parseAll(reader);

        venuePairs = new HashMap<String, String>();
        for(String[] model : allRows) {
            venuePairs.put(model[0], model[1]);
        }

        matcher = new JaroTFIDFMatcher(threshold);

    }

    public VenueMatcher(float th, String dict)
            throws FileNotFoundException, UnsupportedEncodingException {

        CsvParserSettings settings = new CsvParserSettings();
        CsvParser parser = new CsvParser(settings);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(dict), "UTF-8");
        List<String[]> allRows = parser.parseAll(reader);

        venuePairs = new HashMap<String, String>();
        for(String[] model : allRows) {
            venuePairs.put(model[0], model[1]);
        }

        threshold = th;
        matcher = new JaroTFIDFMatcher(threshold);

    }

    public boolean valuesMatch(String s1, String s2) {
        Boolean ret = matcher.valuesMatch(s1, s2);

        if (venuePairs.size() == 0 || ret)
            return ret;

        for (Map.Entry<String, String> pair : venuePairs.entrySet()) {
            if (matcher.valuesMatch(s1, pair.getKey()) && matcher.valuesMatch(s2, pair.getValue()))
                return true;
            else if (matcher.valuesMatch(s2, pair.getKey()) && matcher.valuesMatch(s1, pair.getValue()))
                return true;
        }

        return false;
    }
}