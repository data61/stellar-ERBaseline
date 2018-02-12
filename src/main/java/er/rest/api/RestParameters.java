package er.rest.api;


//  FileSources=er_acm_dblp2.epgm
//	OutputFile=output.epgm
//	Dataformat=epgm
//	Attributes = {year=1.0, venue=0.5, author=0.7, title=0.9}

import java.util.*;
import java.util.stream.Collectors;

import utils.DataFileFormat;

public class RestParameters {
    public String prefix;
    public String fileSources;
    public String outputFile;
    public String dataformat;
    public Map<String, Double> attributes;
    public List<String> matchers;

    public boolean isValid() {
        if (attributes.size() < 1)
            return false;

        try {
            DataFileFormat format = DataFileFormat.fromString(dataformat);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (fileSources == null || outputFile == null)
            return false;

        if (fileSources.length() == 0 || outputFile.length() == 0)
            return false;

        return true;
    }

    public void setPropertiesFromConfigFile(Properties props) {
        prefix = props.getProperty("Prefix");
        fileSources = props.getProperty("FileSources");
        outputFile = props.getProperty("OutputFile");
        dataformat = props.getProperty("Dataformat");

        attributes = new HashMap<>();
        matchers = new ArrayList<>();
        List<String> atts = Arrays.asList(props.getProperty("Attributes")
                .split(",")).stream()
                .map(String::trim).collect(Collectors.toList());

        Set<String> keys = (Set<String>)(Collection<?>)props.keySet();
        atts.forEach(att->{
            keys.forEach(lk->{
                if (lk.toLowerCase().contains(att.toLowerCase())
                        && lk.contains("Threshold")) {
                    attributes.put(lk, Double.parseDouble((String) props.get(lk)));
                    matchers.add(att.toLowerCase());
                }
            });
        });

        System.out.println(attributes.toString());
    }
}
