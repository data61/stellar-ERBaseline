package er.rest.api;

import com.google.gson.Gson;
import data.Record;
import er.ER;
import er.rest.HttpRequestClient;
import io.javalin.Context;
import io.javalin.HaltException;
import io.javalin.Handler;
import org.apache.commons.lang.StringUtils;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.util.*;

public class ExecuteER implements Handler {
    private class Transaction {
        public String sessionId;
        public String resultUrl;
        public String prefix;
        public String fileSources;
        public String outputFile;
        public String dataformat;
        public Map<String, Double> attributes;
        public List<String> matchers;

        public RestParameters createRestParameter(){
            RestParameters rp = new RestParameters();
            rp.prefix = prefix;
            rp.fileSources = fileSources;
            rp.outputFile = outputFile;
            rp.dataformat = dataformat;

            Map<String, Double> attres = attributes;
            rp.matchers = new ArrayList<>(attres.keySet());
            rp.attributes = new HashMap<>();
            attres.forEach((k,v) -> {
                String newkey = StringUtils.capitalize(k) + "Threshold";
                rp.attributes.put(newkey, v);
            });

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

        Transaction transaction = gson.fromJson(body, Transaction.class);
        if (transaction == null) throw new HaltException(400, "Wrong transaction parameters");

        RestParameters parameters = transaction.createRestParameter();
        if (!parameters.isValid())
            throw new HaltException(400, "Wrong REST parameters");

        Runnable r = new Runnable() {
            public void run() {
                String id = transaction.sessionId;
                Map<String,String> obj = new HashMap<>();

                try {
                    Set<Record> records = er.parseRecords(parameters, "data.ReferenceMatcherMerger");
                    Set<Record> results = er.runRSwoosh(records, parameters);
                    er.writeResults(results, parameters);
                    ctx.status(200);

                    obj.put("sessionId", transaction.sessionId);
                    obj.put("result", "success");
                } catch (Exception e) {
                    obj.put("sessionId", transaction.sessionId);
                    obj.put("error", e.toString());
                    e.printStackTrace();
                }

                asyncContext.complete();
                tasks.remove(id);

                try {
                    httpClient.post(transaction.resultUrl, obj);
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