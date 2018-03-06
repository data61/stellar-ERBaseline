import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import utils.JSONConfig;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import static org.junit.Assert.assertEquals;


public class TestJSONConfigParser {

    @Test
    public void testFile() throws FileNotFoundException {
        FileReader reader = new FileReader("src/test/resources/config.json");
        JSONConfig config = JSONConfig.createConfig(reader);
        assertEquals("datasets/ACM_DBLP", config.prefix);
        assertEquals("output.epgm", config.outputFile);
        assertEquals("data.ReferenceMatcherMerger", config.matcherMerger);
        assertEquals("epgm", config.dataFormat);
        assertEquals(Arrays.asList("ACM.csv,DBLP2.csv".split(",")), config.fileSources);

        Map tmp = new HashMap<String, Double>();
        String[] pairs = "year:1.0,venue:0.5,author:0.7,title:0.9".split(",");
        for (int i=0;i<pairs.length;i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split(":");
            tmp.put(keyValue[0], Double.valueOf(keyValue[1]));
        }

        assertEquals(tmp, config.attributes);

        String[][] tmp2 = {{"VenueDict","venueDict.csv"}};
        Map mapItems = ArrayUtils.toMap(tmp2);
        assertEquals(mapItems, config.options);
    }

    public void testString() throws FileNotFoundException {
        String line = null;

        try (FileReader reader = new FileReader("src/test/resources/config.json");
             BufferedReader buffer = new BufferedReader(reader)) {
            long length = 0;
            while ((line = buffer.readLine()) != null) {
                length += line.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        JSONConfig config = JSONConfig.createConfig(line);
        assertEquals("datasets/ACM_DBLP", config.prefix);
        assertEquals("output.xml", config.outputFile);
        assertEquals("data.ReferenceMatcherMerger", config.matcherMerger);
        assertEquals("csv", config.dataFormat);
        assertEquals(Arrays.asList("ACM.csv,DBLP2.csv".split(",")), config.fileSources);

        Map tmp = new HashMap<String, Double>();
        String[] pairs = "year:1.0,venue:0.5,author:0.7,title:0.9".split(",");
        for (int i=0;i<pairs.length;i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split(":");
            tmp.put(keyValue[0], Double.valueOf(keyValue[1]));
        }

        assertEquals(tmp, config.attributes);

        String[][] tmp2 = {{"VenueDict","venueDict.csv"}};
        Map mapItems = ArrayUtils.toMap(tmp2);
        assertEquals(mapItems, config.options);
    }
}