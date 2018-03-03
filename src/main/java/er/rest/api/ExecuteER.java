package er.rest.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import data.Record;
import data.ReferenceMatcherMerger;
import er.ER;
import er.rest.HttpRequestClient;
import io.javalin.Context;
import io.javalin.HaltException;
import io.javalin.Handler;
import utils.JSONConfig;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ExecuteER implements Handler {

    private class Transaction {
        public String sessionId;
        public String resultUrl;
        public String prefix;
        public List<String> fileSources;
        public String outputFile;
        public String dataFormat;
        public String matcherMerger;
        public Map<String, Double> attributes;
        public Map<String, String> options;

        public JSONConfig createJSONConfig(){
            JSONConfig rp = new JSONConfig();
            rp.prefix = prefix;
            rp.fileSources = fileSources;
            rp.outputFile = outputFile;
            rp.dataFormat = dataFormat;
            rp.attributes = attributes;
            rp.matcherMerger = matcherMerger;
            rp.options = options;
            return rp;
        }
    }

    private Gson gson;
    private ER er;
    private Map<String, Runnable> tasks;
    private HttpRequestClient httpClient;

    public ExecuteER(){
        gson = new Gson();
        er = new ER();
        tasks = new HashMap<>();
        httpClient = new HttpRequestClient();
    }

    @Override
    public void handle(Context ctx) throws Exception {
        AsyncContext asyncContext = ctx.request().startAsync();
        asyncContext.setTimeout(0);

        String body = ctx.body();
        System.out.println(body);

        Transaction transaction = gson.fromJson(body, Transaction.class);
        if (transaction == null) throw new HaltException(400, "Wrong transaction parameters");

        JSONConfig parameters = transaction.createJSONConfig();

        if (!parameters.isValid())
            throw new HaltException(400, "Wrong REST parameters");

        Runnable r = new Runnable() {
            public void run() {
                String id = transaction.sessionId;
                Map<String,String> obj = new HashMap<>();

                try {
                    Set<Record> records = er.parseRecords(parameters);
                    Set<Record> results = er.runRSwoosh(records, parameters);
                    er.writeResults(results, parameters);
                    ctx.status(200);

                    if (id != null) obj.put("sessionId", transaction.sessionId);
                    obj.put("result", "success");
                } catch (Exception e) {
                    if (id != null) obj.put("sessionId", transaction.sessionId);
                    obj.put("error", e.toString());
                    e.printStackTrace();
                }

                asyncContext.complete();
                tasks.remove(id);

                try {
                    if (transaction.resultUrl != null) httpClient.post(transaction.resultUrl, obj);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        tasks.put(transaction.sessionId, r);
        asyncContext.start(r);
        ctx.status(202).result("Location: /queue/"+tasks.size());
    }
}