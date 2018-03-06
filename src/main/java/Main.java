import com.beust.jcommander.JCommander;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import data.Record;
import er.rest.HttpRequestClient;
import org.json.simple.JSONObject;
import utils.ERProfile;
import er.rest.RestServer;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import er.ER;
import utils.JSONConfig;

public class Main {
    static RestServer restServer;
    static ER er;

    //main function reads in a property file which specifies FileSource, MatcherMerger,
    // and Algorithm(probably next step)
    public static void main(String[] args) throws Exception  {

        ERProfile erProfile = ERProfile.build();
        Parameters para = new Parameters();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(para)
                .build();
        jCommander.parse(args);
        jCommander.setProgramName(erProfile.getName(), erProfile.getVersion());

        if (para.help) {
            jCommander.usage();
            return;
        } else if (para.rest) {
            restServer = new RestServer();
            restServer.start();
        } else if (para.cli != null) {
            ER er = new ER();
            Set<Record> records = er.parseConfigFile(para.cli);
            Set<Record> results = er.runRSwoosh(records);
            System.out.println("Output results: " + results.size());

            er.writeResults(results);
        } else if (para.url != null && para.jsonDir != null) {
            HttpRequestClient postClient = new HttpRequestClient();

            JSONConfig rp = JSONConfig.createConfig(new FileReader(para.jsonDir));
            JsonObject jobj = new JsonObject();
            jobj.addProperty("sessionId","44c8a9d8-0161-1000-5d6c-8623d70e48a5");
            jobj.addProperty("resultUrl","https://requestb.in/1jveop11");
            jobj.addProperty("prefix",rp.prefix);

            JsonArray jarray = new JsonArray();
            rp.fileSources.forEach(f -> jarray.add(f));
            jobj.add("fileSources",jarray);

            jobj.addProperty("dataFormat",rp.dataFormat);
            jobj.addProperty("outputFile",rp.outputFile);

            JsonObject jattr = new JsonObject();
            rp.attributes.forEach((k,v)->jattr.addProperty(k,v));
            jobj.add("attributes",jattr);

            JsonObject jopts = new JsonObject();
            rp.options.forEach((k,v)->jopts.addProperty(k,v));
            jobj.add("options",jopts);

            System.out.println(jobj.toString());
            int code = postClient.post(para.url, jobj);
            System.out.println("HTTP Response code: " + code);
        } else {
            jCommander.usage();
            return;
        }
    }
}
