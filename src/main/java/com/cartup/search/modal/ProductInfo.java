package com.cartup.search.modal;

import java.util.List;

import org.json.simple.JSONArray;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInfo {
    private String name;
    private String productId;
    private String sku;
    private String price;
    private String discountedPrice;
    private String smallImage;
    private String image_2;
    private String currentPageUrl;
    private String description;
    private JSONArray variantInfo;
    private String rating;
    private String color;
    private List<BadgingConfigCollection> badging;
    private double stock;
    
    public double getStock() {
        return stock;
    }

    public ProductInfo setStock(double stock) {
        this.stock = stock;
        return this;
    }

    public String getRating() {
        return rating;
    }

    public ProductInfo setRating(String rating) {
        this.rating = rating;
        return this;
    }

    public String getColor() {
        return color;
    }

    public ProductInfo setColor(String color) {
        this.color = color;
        return this;
    }

    public JSONArray getVariantInfo() {
        return variantInfo;
    }

    public void setVariantInfo(JSONArray variantInfo) {
        this.variantInfo = variantInfo;
    }

    public String getName() {
        return name;
    }

    public ProductInfo setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ProductInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDiscountedPrice() {
        return discountedPrice;
    }

    public ProductInfo setDiscountedPrice(String discountedPrice) {
        this.discountedPrice = discountedPrice;
        return this;
    }

    public String getSku() {
        return sku;
    }

    public ProductInfo setSku(String sku) {
        this.sku = sku;
        return this;
    }

    public String getPrice() {
        return price;
    }

    public ProductInfo setPrice(String price) {
        this.price = price;
        return this;
    }

    public String getSmallImage() {
        return smallImage;
    }

    public ProductInfo setSmallImage(String smallImage) {
        this.smallImage = smallImage;
        return this;
    }
    
    public String getImage2() {
        return image_2;
    }

    public ProductInfo setImage2(String image_2) {
        this.image_2 = image_2;
        return this;
    }
    

    public String getCurrentPageUrl() {
        return currentPageUrl;
    }

    public ProductInfo setCurrentPageUrl(String currentPageUrl) {
        this.currentPageUrl = currentPageUrl;
        return this;
    }
    
    public String getProductId() {
        return productId;
    }

    public ProductInfo setProductId(String productId) {
        this.productId = productId;
        return this;
    }
    
    public List<BadgingConfigCollection> getBadging() {
		return this.badging;
	}

	public ProductInfo setBadging(List<BadgingConfigCollection> badging) {
		this.badging = badging;
		return this;
	}
    
}
