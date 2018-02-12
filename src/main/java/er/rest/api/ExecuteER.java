package er.rest.api;

import com.google.gson.Gson;
import data.Record;
import er.ER;
import io.javalin.Context;
import io.javalin.HaltException;
import io.javalin.Handler;
import org.apache.commons.lang.StringUtils;

import javax.servlet.AsyncContext;
import java.util.*;

public class ExecuteER implements Handler {
    private Gson gson;
    private ER er;
    private Map<Integer, Runnable> tasks;

    public ExecuteER(){
        gson = new Gson();
        er = new ER();
        tasks = new HashMap<>();
    }

    @Override
    public void handle(Context ctx) throws Exception {
        AsyncContext asyncContext = ctx.request().startAsync();
        asyncContext.setTimeout(0);

        String body = ctx.body();
        RestParameters parameters = gson.fromJson(body, RestParameters.class);
        Map<String, Double> attres = parameters.attributes;
        parameters.matchers = new ArrayList<>(attres.keySet());
        parameters.attributes = new HashMap<>();
        attres.forEach((k,v) -> {
            String newkey = StringUtils.capitalize(k) + "Threshold";
            parameters.attributes.put(newkey, v);
        });

        if (!parameters.isValid())
            throw new HaltException(400, "Wrong parameters");

        Runnable r = new Runnable() {
            public void run() {
                try {
                    Set<Record> records = er.parseRecords(parameters, "data.ReferenceMatcherMerger");
                    Set<Record> results = er.runRSwoosh(records, parameters);
                    er.writeResults(results, parameters);
                    ctx.status(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                asyncContext.complete();
                tasks.remove(this.hashCode());
            }
        };

        tasks.put(r.hashCode(), r);
        asyncContext.start(r);
        ctx.status(202).result("Location: /queue/"+tasks.size());
    }
}