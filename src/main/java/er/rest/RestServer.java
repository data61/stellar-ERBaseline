package er.rest;

import io.javalin.Javalin;
import java.io.FileReader;

import er.rest.api.*;
import utils.ERProfile;

public class RestServer {

    private Javalin server;
    private ExecuteER er;

    public void start() throws Exception {
        server = Javalin.start(7000);
        server.get("/version", ctx -> {
            ctx.result(ERProfile.build().getVersion());
        });

        er = new ExecuteER();
        server.post("/deduplicate", er);
    }

    public void stop() throws Exception {
        server.stop();
    }
}