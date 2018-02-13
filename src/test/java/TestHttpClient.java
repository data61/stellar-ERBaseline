import er.rest.RestServer;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import er.rest.HttpRequestClient;

public class TestHttpClient {
    public MockHttpServer server;

    @Before
    public void beforeClass() throws Exception {
        server = new MockHttpServer();
        server.start();
    }
    @After
    public void afterClass() throws Exception {
        server.stop();
    }

    @Test
    public void testGet() throws IOException {
        HttpRequestClient client = new HttpRequestClient();
        String response = client.get("http://127.0.0.1:7777/get");

        assertEquals("44c8a9d8-0161-1000-5d6c-8623d70e48a5", response);
    }

    @Test
    public void testPost() throws IOException {
        Map<String,String> obj = new HashMap<>();
        obj.put("sessionId", "44c8a9d8-0161-1000-5d6c-8623d70e48a5");

        HttpRequestClient client = new HttpRequestClient();
        int code = client.post("http://127.0.0.1:7777/post", obj);

        assertEquals(code, 200);
    }
}