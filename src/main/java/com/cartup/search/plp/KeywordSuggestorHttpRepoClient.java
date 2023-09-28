package com.cartup.search.plp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.apache.http.util.EntityUtils;

import com.cartup.commons.repo.RepoFactory;

public class KeywordSuggestorHttpRepoClient {
    public static String keywordSuggestorHost =  RepoFactory.getConfigProperty("keywordsuggestor.api.server");

    public JSONObject getLangFeatures(String jsonBody) {
        String jsonResponse = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(keywordSuggestorHost + "/language");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept", "application/json");
            
            StringEntity stringEntity = new StringEntity(jsonBody, "UTF-8");
            httpPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    jsonResponse = EntityUtils.toString(entity, "UTF-8");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new JSONObject(jsonResponse);
    }
    
    
    public String getLemmaKeyword(JSONObject resp) {
    	String keyword = null;
    	JSONObject annotations = (JSONObject) resp.get("annotations");
    	JSONObject lang_features = (JSONObject) annotations.get("lang_features");
    	if (lang_features.has("lemma_search_keyword")) {
    		keyword = lang_features.getString("lemma_search_keyword");
    	}
    	return keyword;
    }

}
