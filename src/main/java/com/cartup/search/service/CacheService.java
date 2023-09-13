package com.cartup.search.service;

import java.net.URI;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CacheService {

	@Value("${CACHE_ENDPOINT}")
	private String cacheUri;
	
	@Autowired
	private RestTemplate restTemplate;

	public JSONObject updateCache(String searchConfig) {
		String url = this.cacheUri + "/store_search_config";
		try {
			URI cachedSearchUri = new URIBuilder(url).build();
			long time = System.currentTimeMillis();
			ResponseEntity<String> response = restTemplate.postForEntity(cachedSearchUri, searchConfig, String.class);
			log.info("Request time for cache URI is {} ms", System.currentTimeMillis() - time);
			if(response.getStatusCode().is2xxSuccessful()) {
				log.info("Cache service status code : {}", response.getStatusCode());
				return new JSONObject(response.getBody());
			} else {
				log.error("Cache service status code : {} - {}", response.getStatusCode(), response.getBody());
			}
		} catch (Exception exception) {
			log.error("Error occured while accessing refreshing search configuration cache : ", exception);
		}
		return null;
	}
	
	public JSONObject getSearchConfig(String orgId) {
		String url = this.cacheUri + "/get_search_config";
		try {
			URI cachedSearchUri = new URIBuilder(url).addParameter("orgID_s", orgId).build();
			long time = System.currentTimeMillis();
			ResponseEntity<String> response = restTemplate.getForEntity(cachedSearchUri, String.class);
			log.info("Request time for cache URI is {} ms", System.currentTimeMillis() - time);
			if(response.getStatusCode().is2xxSuccessful()) {
				log.info("Cache service status code : {}", response.getStatusCode());
				return new JSONObject(response.getBody());
			} else {
				log.error("Cache service status code : {} - {}", response.getStatusCode(), response.getBody());
			}
		} catch (Exception exception) {
			log.error("Error occured while accessing refreshing search configuration cache : ", exception);
		}
		return null;
	}
	
	public JSONObject getClipEmbVtr(String type, String payloadKey, String payload) {
		String url = this.cacheUri + "/get_embeddings";
		try {
			URI embeddingUrl = new URIBuilder(url).build();
			long time = System.currentTimeMillis();
			JSONObject reqBody = new JSONObject();
			reqBody.put("type", type);
			reqBody.put(payloadKey, payload);
			
			ResponseEntity<String> response = restTemplate.postForEntity(embeddingUrl, reqBody.toString(), String.class);
			log.info("Request time for embedding URI is {} ms", System.currentTimeMillis() - time);
			if(response.getStatusCode().is2xxSuccessful()) {
				log.info("Embedding API status code : {}", response.getStatusCode());
				return new JSONObject(response.getBody());
			} else {
				log.error("Embedding API status code : {} - {}", response.getStatusCode(), response.getBody());
			}
		} catch (Exception exception) {
			log.error("Error occured while accessing embedding API : ", exception);
		}
		return null;
	}

}
