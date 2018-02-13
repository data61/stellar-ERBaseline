package er.rest;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;

public class HttpRequestClient {
    private  HttpClient httpClient;
    private Gson gson;

    public HttpRequestClient() {
        httpClient = HttpClientBuilder.create().build();
        gson = new Gson();
    }

    public int post(String url, Object para) throws IOException {
        HttpPost httpPost = new HttpPost(URI.create(url));
        StringEntity params = new StringEntity(gson.toJson(para));
        httpPost.setEntity(params);
        httpPost.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(httpPost);
        return response.getStatusLine().getStatusCode();
    }

    public String get(String url) throws IOException {
        HttpGet httpGet = new HttpGet(URI.create(url));
        HttpResponse response = httpClient.execute(httpGet);
        return EntityUtils.toString(response.getEntity(), "UTF-8");
    }
}
