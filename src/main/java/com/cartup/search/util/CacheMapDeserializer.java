package com.cartup.search.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cartup.commons.repo.model.FacetEntity;
import com.cartup.search.modal.ProductInfo;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class CacheMapDeserializer implements JsonDeserializer<Map<String, Object>> {
	
	private Gson gson = new Gson();
	
    @Override
    public Map<String, Object> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
        	Map<String, Object> cacheMap = new HashMap<>();

            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                if (jsonObject.has("docs")) {
                	
                    List<ProductInfo> docs = gson.fromJson(jsonObject.get("docs"), new TypeToken<List<ProductInfo>>() {}.getType());
                    cacheMap.put("docs", docs);
                }

                if (jsonObject.has("facets")) {
                    List<FacetEntity> facets = gson.fromJson(jsonObject.get("facets"), new TypeToken<List<FacetEntity>>() {}.getType());
                    cacheMap.put("facets", facets);
                }

                if (jsonObject.has("numFound")) {
                    int numFound = jsonObject.get("numFound").getAsInt();
                    cacheMap.put("numFound", numFound);
                }
            }
        	return cacheMap;
        } catch (NumberFormatException e) {
            return new HashMap<>();
        }
    }
    
}
