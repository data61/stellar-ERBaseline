import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import er.rest.RestServer;
import utils.ERProfile;
import er.rest.api.RestParameters;

public class TestPostExecuteER {
	public RestServer server;

	@Before
	public void beforeClass() throws Exception {
		server = new RestServer();
		server.start();
	}
	@After
	public void afterClass() throws Exception {
		server.stop();
	}

	@Test
	public void testVersion() throws IOException, XmlPullParserException {
		String version = ERProfile.build().getVersion();

		HttpGet httpGet = new HttpGet(URI.create("http://127.0.0.1:7000/version"));
		HttpClient httpclient = HttpClientBuilder.create().build();
		HttpResponse response = httpclient.execute(httpGet);

		assertEquals(version, EntityUtils.toString(response.getEntity(), "UTF-8"));
	}

	@Test
	public void testWrongParameters() throws UnsupportedEncodingException, IOException {
		HttpPost httpPost = new HttpPost(URI.create("http://127.0.0.1:7000/deduplicate"));

		RestParameters rp = new RestParameters();
		rp.prefix = "datasets/ACM_DBLP";
		rp.dataformat = "epgm";
		rp.fileSources = "er_acm_dblp2.epgm";
		rp.outputFile = "output.epgm";

//		Map<String, Double> am = new HashMap<>();
//		am.put("year", 1.0);
//		am.put("venue", 0.5);
//		am.put("author", 0.7);
//		am.put("title", 0.9);
		rp.attributes = new HashMap<>();

		Gson gson = new Gson();
		StringEntity params = new StringEntity(gson.toJson(rp));
		httpPost.setEntity(params);
		httpPost.setHeader("Content-type", "application/json");

		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpResponse response = httpClient.execute(httpPost);

		assertEquals(400, response.getStatusLine().getStatusCode());
	}

//	@Test
//	public void testAsyncTask() throws UnsupportedEncodingException, IOException {
//		HttpPost httpPost = new HttpPost(URI.create("http://127.0.0.1:7000/deduplicate"));
//
//		RestParameters rp = new RestParameters();
//		rp.prefix = "datasets/ACM_DBLP";
//		rp.dataformat = "epgm";
//		rp.fileSources = "er_acm_dblp2.epgm";
//		rp.outputFile = "output.epgm";
//
//		Map<String, Double> am = new HashMap<>();
//		am.put("venue", 0.5);
//		am.put("author", 0.7);
//		am.put("title", 0.9);
//		rp.attributes = am;
//
//		Gson gson = new Gson();
//		StringEntity params = new StringEntity(gson.toJson(rp));
//		httpPost.setEntity(params);
//		httpPost.setHeader("Content-type", "application/json");
//
//		HttpClient httpClient = HttpClientBuilder.create().build();
//		HttpResponse response = httpClient.execute(httpPost);
//
//		assertEquals(202, response.getStatusLine().getStatusCode());
//	}
}
