import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.javalin.Javalin;

import java.lang.reflect.Type;
import java.util.Map;

public class MockHttpServer {
    private Javalin server;

    public void start() throws Exception {
        server = Javalin.start(7777);
        server.get("/get", ctx -> {
            ctx.result("44c8a9d8-0161-1000-5d6c-8623d70e48a5");
        });

        server.post("/post", ctx -> {
            String body = ctx.body();

            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Gson gson = new Gson();
            Map<String, String> myMap = gson.fromJson(body, type);
            String id = myMap.get("sessionId");
            if (id != null && id.equals("44c8a9d8-0161-1000-5d6c-8623d70e48a5")) {
                ctx.status(200);
            } else
                ctx.status(404);
        });
    }

    public void stop() throws Exception {
        server.stop();
    }
}