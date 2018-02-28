package data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import er.rest.api.RestParameters;
import org.jvnet.hk2.internal.Collector;
import org.netlib.lapack.Ssycon;
import utils.DataFileFormat;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ReferenceMatcherMerger extends BasicMatcherMerger implements
        MatcherMerger {

    //TODO: tmp hack. Add new matchers for Cora and Spammer datasets
    private JaroTFIDFMatcher nameMatcher;
    private JaroTFIDFMatcher addrMatcher;
    private JaroTFIDFMatcher sexMatcher;
    private JaroTFIDFMatcher ageMatcher;
    private JaroTFIDFMatcher stringMatcher;
    private JaroTFIDFMatcher authorMatcher;
    private VenueMatcher venueMatcher;
    private List<String> keyAttributes;
    private DataFileFormat format;
    private Gson gson;

    public ReferenceMatcherMerger(Properties props)
            throws FileNotFoundException, UnsupportedEncodingException {

        _factory = new SimpleRecordFactory();
        gson = new Gson();

        String tt = props.getProperty("TitleThreshold");
        float tf = tt == null ? 0.9F : Float.parseFloat(tt);

        String au = props.getProperty("AuthorThreshold");
        float auf = au == null ? 0.7F : Float.parseFloat(au);

        String vt = props.getProperty("VenueThreshold");
        float vtf = vt == null ? 0.5F : Float.parseFloat(vt);

        //TODO: tmp hack. Add new thresholds for Cora and Spammer datasets
        String nt = props.getProperty("NameThreshold");
        float nf = nt == null ? 0.9F : Float.parseFloat(nt);

        String adt = props.getProperty("AddrThreshold");
        float adf = adt == null ? 0.5F : Float.parseFloat(adt);

        String st = props.getProperty("SexThreshold");
        float sf = st == null ? 1.0F : Float.parseFloat(st);

        String at = props.getProperty("AgeGroupThreshold");
        float af = at == null ? 1.0F : Float.parseFloat(at);


        keyAttributes = Arrays.asList(props.getProperty("Attributes").split(",")).stream().map(String::trim).collect(Collectors.toList());

        format = DataFileFormat.fromString(props.getProperty("Dataformat"));

        //TODO: tmp hack. Add new matchers for Cora and Spammer datasets
        nameMatcher = new JaroTFIDFMatcher(nf);
        addrMatcher = new JaroTFIDFMatcher(adf);
        sexMatcher = new JaroTFIDFMatcher(sf);
        ageMatcher = new JaroTFIDFMatcher(af);

        stringMatcher = new JaroTFIDFMatcher(tf);
        authorMatcher = new JaroTFIDFMatcher(auf);
        venueMatcher = new VenueMatcher(vtf, props.getProperty("Prefix")+"/"+props.getProperty("VenueDict"));
    }

    public ReferenceMatcherMerger(RestParameters props)
            throws FileNotFoundException, UnsupportedEncodingException {

        _factory = new SimpleRecordFactory();
        gson = new Gson();

        // TODO: temp hack of switching between datasets!!!
        Boolean IsACMDBLP = false;
        Boolean IsCoraTerror = false;
        Boolean IsSpammerER = false;
        if (props.fileSources.toLowerCase().contains("cora")){
            IsCoraTerror = true;
        } else if (props.fileSources.toLowerCase().contains("spam")){
            IsSpammerER = true;
        } else if (props.fileSources.toLowerCase().contains("dblp")){
            IsACMDBLP = true;
        }

        // TODO: temp hack of switching between datasets!!!
        if (IsCoraTerror){
            float nf = props.attributes.get("NameThreshold").floatValue();
            float adf = props.attributes.get("AddrThreshold").floatValue();

            nameMatcher = new JaroTFIDFMatcher(nf);
            addrMatcher = new JaroTFIDFMatcher(adf);

        } else if (IsSpammerER){
            float nf = props.attributes.get("NameThreshold").floatValue();
            float sf = props.attributes.get("SexThreshold").floatValue();
            float af = props.attributes.get("AgeGroupThreshold").floatValue();

            nameMatcher = new JaroTFIDFMatcher(nf);
            sexMatcher = new JaroTFIDFMatcher(sf);
            ageMatcher = new JaroTFIDFMatcher(af);

        } else if (IsACMDBLP){
            float tf = props.attributes.get("TitleThreshold").floatValue();
            float auf = props.attributes.get("AuthorThreshold").floatValue();
            float vtf = props.attributes.get("VenueThreshold").floatValue();

            stringMatcher = new JaroTFIDFMatcher(tf);
            authorMatcher = new JaroTFIDFMatcher(auf);

            System.out.println("#### vtf " + vtf);
            venueMatcher = new VenueMatcher(vtf, props.prefix+"/"+"venueDict.csv");
        }

        keyAttributes = props.matchers;
        format = DataFileFormat.fromString(props.dataformat);

        System.out.println("@@@@@@@@@ done");
    }

    protected double calculateConfidence(double c1, double c2)
    {
        return 1.0;
    }

    protected boolean matchInternal(Record r1, Record r2) {
        Set<String> attrkeys = r1.getAttributes().keySet();
        Map<String, String> gotAttrkeys = new HashMap<>();
        Type type = new TypeToken<Map<String, String>>(){}.getType();

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
                case "full_name": case "name":
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
                case "year":
                    Iterator i1 = a1.iterator();
                    Iterator i2 = a2.iterator();
                    int year1 = Integer.valueOf((String)i1.next());
                    int year2 = Integer.valueOf((String)i2.next());
                    if (year1 != year2)
                        return false;
                    break;
                case "venue":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, venueMatcher))
                            return false;
                    }
                    break;
                case "title":
                    if (a1 != null && a2 != null) {
                        if (!ExistentialBooleanComparator.attributesMatch(a1, a2, stringMatcher))
                            return false;
                    }
                    break;
                case "author":
                    if (a1 != null && a2 != null) {
                        List<String> authors1 = new ArrayList<>();
                        List<String> authors2 = new ArrayList<>();

                        if (a1.getValuesCount() > 0) {
                            i1 = a1.iterator();
                            while (i1.hasNext()) {
                                String authorString = (String) i1.next();
                                try {
                                    Map<String, String> mjson = gson.fromJson(authorString, type);
                                    authors1.add(mjson.get("name"));
                                } catch (Exception e) {
                                    authors1.add(authorString);
                                }
                            }
                        }

                        if (a2.getValuesCount() > 0) {
                            i2 = a2.iterator();
                            while (i2.hasNext()) {
                                String authorString = (String) i2.next();
                                try {
                                    Map<String, String> mjson = gson.fromJson(authorString, type);
                                    authors2.add(mjson.get("name"));
                                } catch (Exception e) {
                                    authors2.add(authorString);
                                }
                            }
                        }

                        Attribute tmpAttr1, tmpAttr2;
                        if (authors1.size() > 1 || authors2.size() > 1) {
                            Collections.sort(authors1);
                            Collections.sort(authors2);

                            String authors1Str = String.join(",", authors1);
                            String authors2Str = String.join(",", authors2);

                            tmpAttr1 = new Attribute(a1.getType());
                            tmpAttr2 = new Attribute(a2.getType());
                            tmpAttr1.addValue(authors1Str);
                            tmpAttr2.addValue(authors2Str);
                        } else {
                            tmpAttr1 = a1;
                            tmpAttr2 = a2;
                        }

                        if (!ExistentialBooleanComparator.attributesMatch(tmpAttr1, tmpAttr2, authorMatcher))
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