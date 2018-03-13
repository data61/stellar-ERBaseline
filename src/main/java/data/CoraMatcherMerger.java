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

    private Map<String, JaroTFIDFMatcher> matchers;

    public CoraMatcherMerger(JSONConfig props)
            throws FileNotFoundException, UnsupportedEncodingException {

        _factory = new SimpleRecordFactory();
        gson = new Gson();

        matchers = new HashMap<>();
        props.attributes.forEach( (k, v) -> {
            matchers.put(k, new JaroTFIDFMatcher(v.floatValue()));
            System.out.println("Similarity Threshold - " + k + ":" + v);
        });

        keyAttributes = new ArrayList<>(props.attributes.keySet());
        format = DataFileFormat.fromString(props.dataFormat);
        type = new TypeToken<Map<String, String>>(){}.getType();
    }

    protected double calculateConfidence(double c1, double c2)
    {
        return 1.0;
    }

    protected boolean matchInternal(Record r1, Record r2) {
        Set<String> attrkeys = r1.getAttributes().keySet();
        List<String> gotAttrkeys = new ArrayList<>();

        keyAttributes.forEach(ka->{
            Optional<String> ret = attrkeys.stream().parallel().filter(attrkey->attrkey.contains(ka)).findAny();
            if (ret.isPresent()) {
                gotAttrkeys.add(ka);
            }
        });

        for (String key : gotAttrkeys) {
            Attribute a1 = r1.getAttribute(key);
            Attribute a2 = r2.getAttribute(key);

            if (a1 != null && a2 != null) {
                if (!ExistentialBooleanComparator.attributesMatch(a1, a2, matchers.get(key)))
                    return false;
            }
        }

        return true;
    }
}