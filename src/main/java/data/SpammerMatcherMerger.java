package data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import utils.DataFileFormat;
import utils.JSONConfig;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.*;

public class SpammerMatcherMerger extends BasicMatcherMerger implements
        MatcherMerger {

    public static String keyWords[] = new String[]{"name","sex","ageGroup"};

    private JaroTFIDFMatcher nameMatcher;
    private JaroTFIDFMatcher sexMatcher;
    private JaroTFIDFMatcher ageMatcher;

    private List<String> keyAttributes;
    private DataFileFormat format;
    private Gson gson;
    private Map<String, String> gotAttrkeys;
    private Type type;

    public SpammerMatcherMerger(JSONConfig props)
            throws FileNotFoundException, UnsupportedEncodingException {

        _factory = new SimpleRecordFactory();
        gson = new Gson();

        float nf = props.attributes.get("name").floatValue();
        float sf = props.attributes.get("sex").floatValue();
        float af = props.attributes.get("ageGroup").floatValue();


        System.out.println("Similarity Threshold - Name: " + nf + " Gender: " + sf + " Age: "+ af);

        keyAttributes = new ArrayList<>(props.attributes.keySet());
        format = DataFileFormat.fromString(props.dataFormat);

        nameMatcher = new JaroTFIDFMatcher(nf);
        sexMatcher = new JaroTFIDFMatcher(sf);
        ageMatcher = new JaroTFIDFMatcher(af);

        type = new TypeToken<Map<String, String>>(){}.getType();
    }

    protected double calculateConfidence(double c1, double c2)
    {
        return 1.0;
    }

    protected boolean matchInternal(Record r1, Record r2) {
        Set<String> attrkeys = r1.getAttributes().keySet();
        Map<String, String> gotAttrkeys = new HashMap<>();

        keyAttributes.forEach(ka->{
            Optional<String> ret = attrkeys.stream().parallel().filter(attrkey->attrkey.toLowerCase().contains(ka.toLowerCase())).findAny();
            if (ret.isPresent()) {
                gotAttrkeys.put(ka.toLowerCase(), ret.get());
            }
        });

        for (Map.Entry<String, String> entry  : gotAttrkeys.entrySet()) {
            String attrKey = entry.getValue();
            String propKey = entry.getKey();
            Attribute a1 = r1.getAttribute(attrKey);
            Attribute a2 = r2.getAttribute(attrKey);

            switch (propKey) {
                case "name":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, nameMatcher))
                            return false;
                    }
                    break;
                case "sex":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, sexMatcher))
                            return false;
                    }
                    break;
                case "ageGroup":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, ageMatcher))
                            return false;
                    }
                    break;
                default:
                    break;
            }
        }

        return true;
    }
}