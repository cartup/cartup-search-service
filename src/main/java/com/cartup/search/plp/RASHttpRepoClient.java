package com.cartup.search.plp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cartup.commons.repo.RepoFactory;

public class RASHttpRepoClient {
    public static String dasHost =  RepoFactory.getConfigProperty("redis.api.server");

    public JSONObject getUserProfile(String body) throws Exception {
        HttpURLConnection conn;
        URL nurl = new URL(dasHost + "/user_stats");
        conn = (HttpURLConnection) nurl.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        // Send post request
        conn.setDoOutput(true);
        OutputStream os= new DataOutputStream(conn.getOutputStream());
        os.write(body.getBytes("UTF-8"));
        os.close();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            output = output.append(line);
        }
        br.close();

        conn.disconnect();
        JSONObject response = new JSONObject(output.toString());
		return response;
    }
    
    public static void main(String[] args) throws Exception {
    	 
    	RASHttpRepoClient http = new RASHttpRepoClient();
    	JSONObject json = new JSONObject();
    	json.put("org_s", "hunkemoller.de");
    	json.put("user_id", "1e1510d5-f5bb-44b1-a5e1-b4c18d66c737");
    	json.put("cat_id", "Nachthemden");
    	json.put("stats", "view_stats");
    	json.put("feature_stats", "features");
    	json.put("recent_views", true);
    	
    	// Sending get request
    	JSONObject resp = http.getUserProfile(json.toString()); 
    	System.out.println(resp.toString());
    	HashMap<String, HashMap<String, Double>> features_scores = new HashMap<String, HashMap<String, Double>>();
    	
    	try {
    		JSONObject featuresViewStats = (JSONObject) ((JSONObject) resp.get("user_stats")).get("features_view_stats");
    		JSONArray features = (JSONArray) ((JSONObject) resp.get("user_stats")).get("features");
    		for(int i=0; i< features.length(); i++) {
    			JSONObject fobj = (JSONObject) featuresViewStats.get((String) features.get(i));
    			HashMap<String, Double> feature_value_score = new HashMap<String, Double>();
    			Iterator<String> keys = fobj.keys();
    			while(keys.hasNext()) {
    			    String key = keys.next();
    			    feature_value_score.put(key, (Double) fobj.get(key));
    			}
    			features_scores.put((String) features.get(i),  feature_value_score);
    		} 		
    		
    		StringBuffer querybuffer = new StringBuffer();
    		//Build boosting Query
    		for (Map.Entry<String, HashMap<String, Double>> entry : features_scores.entrySet()) {
    			for (Map.Entry<String, Double> subEntry : entry.getValue().entrySet()) {
    				querybuffer.append("bq=" + entry.getKey() + ":(" + subEntry.getKey() + ")^" + subEntry.getValue());
    			}
    			querybuffer.append("&");
    		}
    		querybuffer.deleteCharAt(querybuffer.length()-1);
    		System.out.println(querybuffer.toString());
    		//Boost User Signals
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
   }

}
