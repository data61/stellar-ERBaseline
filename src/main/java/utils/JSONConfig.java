package utils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.util.List;
import java.util.Map;

public class JSONConfig {

    public String prefix;
    public List<String> fileSources;
    public String outputFile;
    public String matcherMerger;
    public String dataFormat;
    public Map<String, Double> attributes;
    public Map<String, String> options;

    public String toString() {
        String output = "";
        if (prefix != null) output = output + "prefix: " + prefix;
        if (fileSources != null) output = output + "\nfileSources: " + fileSources.toString();
        if (outputFile != null) output = output + "\noutputFile: " + outputFile;
        if (matcherMerger != null) output = output + "\nmatcherMerger: " + matcherMerger;
        if (dataFormat != null) output = output + "\nformat: " + dataFormat;
        if (attributes != null) output = output + "\nattributes: " + attributes.toString();
        if (options != null) output = output + "\noptions: " + options.toString();
        return output;
    }

    public boolean isValid() {
        if (attributes.size() < 1)
            return false;

        try {
            DataFileFormat format = DataFileFormat.fromString(dataFormat);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (fileSources == null || outputFile == null)
            return false;

        if (fileSources.size() == 0 || outputFile.length() == 0)
            return false;

        return true;
    }

    static public JSONConfig createConfig(FileReader reader){
        try {
            Gson gson = new Gson();
            JsonReader jreader = new JsonReader(reader);
            JSONConfig config = gson.fromJson(jreader, JSONConfig.class);
            if (config == null)
                throw new JsonSyntaxException("empty properties");
            return config;
        } catch (JsonSyntaxException jse) {
            throw jse;
        } catch (JsonIOException jie) {
            throw jie;
        }
    }

    static public JSONConfig createConfig(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, JSONConfig.class);
    }
}