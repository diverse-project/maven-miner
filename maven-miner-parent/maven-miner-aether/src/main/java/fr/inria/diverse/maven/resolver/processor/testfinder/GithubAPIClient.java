package fr.inria.diverse.maven.resolver.processor.testfinder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GithubAPIClient {
	public static String token = null;
	static TimeCredit credit;

	public static void init(String pToken) {
		token = pToken;
		if(token == null)
			credit = new TimeCredit(60*60*1000,100);
		else
			credit = new TimeCredit(60*60*1000,5000);
	}



	static String githubURL = "https://api.github.com";


	public static void main(String[] args) throws IOException, ParseException, GithubQuotaException, InterruptedException {
		System.out.println(
				getCommits("qos-ch/slf4j")
						.values()
						.stream()
						.collect(
								Collectors.joining( " " )
						)
		);
	}

	public static Map<String, String> getCommits(String userRepo) throws IOException, ParseException, GithubQuotaException, InterruptedException {
		JSONArray array = getTagsOrReleases(userRepo);
		Map<String, String> versionCommit = new HashMap<>();
		for(int i = 0; i < array.size(); i++) {
			JSONObject entry = (JSONObject) array.get(i);
			String version = (String) entry.get("name");
			String commitID = (String) ((JSONObject) entry.get("commit")).get("sha");
			versionCommit.put(version,commitID);
		}
		return versionCommit;
	}

	public static JSONArray getTagsOrReleases(String userRepo) throws IOException, ParseException, GithubQuotaException, InterruptedException {
		String tagQuery = githubURL + "/repos/" + userRepo + "/tags";
		return httpGetQuery(tagQuery);

	}

	public static JSONArray httpGetQuery(String query) throws IOException, ParseException, GithubQuotaException, InterruptedException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(query);
		if(token != null)
			request.setHeader("Authorization", "token " + token);
		credit.spend();
		HttpResponse result = httpClient.execute(request);
		if (result.getStatusLine().getStatusCode() == 403) {
			throw new GithubQuotaException();
		}

		String json = EntityUtils.toString(result.getEntity(), "UTF-8");
		JSONParser parser = new JSONParser();
		Object resultObject = parser.parse(json);

		if (resultObject instanceof JSONArray) {
			JSONArray array = (JSONArray) resultObject;
			return array;

		}
		return null;
	}
}