package com.cartup.search.modal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SearchRequestWrapper {
	
	@SerializedName("org_id")
	private String orgId;
	@SerializedName("user_id")
	private String userId;
	@SerializedName("keyword_s")
	private String keyword;
	@SerializedName("features_ss")
	private List<String> features;
	@SerializedName("entities_ss")
	private Map<String, String> entities;
	@SerializedName("intent_s")
	private String intent;
	@SerializedName("size_d")
	private float size;
	@SerializedName("product_s")
	private String productName;
	@SerializedName("price_d")
	private float price;
	@SerializedName("price_range_d")
	private String priceRange;
	@SerializedName("quantity_l")
	private float quantity;
	@SerializedName("gender_s")
	private String gender;
	@SerializedName("sku_s")
	private String sku;
	@SerializedName("categories_ss")
	private List<String> categories;
	@SerializedName("color_s")
	private String color;
	@SerializedName("brand_s")
	private String brand;
	
	public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("features_txt", features);
        map.put("entities_txt", entities);
        map.put("intent_txt", intent);
        map.put("size_d", size);
        map.put("price_d", price);
        if(price == 0.0) {
        	map.put("price_d", priceRange);
        }
        map.put("stock_d", quantity);
        map.put("gender_s", gender);
        map.put("sku_s", sku);
        map.put("color_s", color);
        map.put("brand_s", brand);

        return map;
    }


}
