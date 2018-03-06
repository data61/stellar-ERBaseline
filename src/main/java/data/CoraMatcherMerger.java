package data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import utils.DataFileFormat;
import utils.JSONConfig;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.*;

public class CoraMatcherMerger extends BasicMatcherMerger implements
        MatcherMerger {

    public static String keyWords[] = new String[]{"full_name","address"};

    private JaroTFIDFMatcher nameMatcher;
    private JaroTFIDFMatcher addrMatcher;
    private List<String> keyAttributes;
    private DataFileFormat format;
    private Gson gson;
    private Map<String, String> gotAttrkeys;
    private Type type;

    public CoraMatcherMerger(JSONConfig props)
            throws FileNotFoundException, UnsupportedEncodingException {

        _factory = new SimpleRecordFactory();
        gson = new Gson();

        float fnf = props.attributes.get("full_name").floatValue();
        float addf = props.attributes.get("address").floatValue();

        keyAttributes = new ArrayList<>(props.attributes.keySet());
        format = DataFileFormat.fromString(props.dataFormat);

        nameMatcher = new JaroTFIDFMatcher(fnf);
        addrMatcher = new JaroTFIDFMatcher(addf);

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
                case "full_name":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, nameMatcher))
                            return false;
                    }
                    break;
                case "address":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, addrMatcher))
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