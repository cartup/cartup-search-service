package com.cartup.search.modal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ProductResultDoc {
    private String name_s;
    private String product_id_s;
    private String product_id_d;
    private String smallImage_s;
    private String price_d;
    private String discounted_price_d;
    private String sku_s;
    private String image_s;
    private String image_2_s;
    private String currentPageUrl_s;
    private String color_s;
    private boolean variant_b;
    private List<String> linked_product_name_ss;
    private List<Double> linked_product_price_ds;
    private List<Double> linked_product_discountedprice_ds;
    private List<String> linked_product_sku_ss;
    private List<String> linked_variant_id_ss;
    private List<Double> stock_i_ds;
    private List<Long> linked_product_id_ls;
    private double stock_d;
    private String size_s;
	private String line_s;
    private List<String> parentCategories_ss;
    private String category_s;
    private boolean status_b;
    private List<String> badging_ss;

	public String getColor_s() {
        return color_s;
    }

    public ProductResultDoc setColor_s(String color_s) {
        this.color_s = color_s;
        return this;
    }

    public List<String> getLinked_variant_id_ss() {
        return linked_variant_id_ss;
    }

    public void setLinked_variant_id_ss(List<String> linked_variant_id_ss) {
        this.linked_variant_id_ss = linked_variant_id_ss;
    }


    public List<Long> getLinked_product_id_ls() {
        return linked_product_id_ls;
    }

    public void setLinked_product_id_ls(List<Long> linked_product_id_ls) {
        this.linked_product_id_ls = linked_product_id_ls;
    }

    public boolean isVariant_b() {
        return variant_b;
    }

    public void setVariant_b(boolean variant_b) {
        this.variant_b = variant_b;
    }

    public List<String> getLinked_product_name_ss() {
        return linked_product_name_ss;
    }

    public void setLinked_product_name_ss(List<String> linked_product_name_ss) {
        this.linked_product_name_ss = linked_product_name_ss;
    }

    public List<Double> getLinked_product_price_ds() {
        return linked_product_price_ds;
    }

    public void setLinked_product_price_ds(List<Double> linked_product_price_ds) {
        this.linked_product_price_ds = linked_product_price_ds;
    }

    public List<Double> getLinked_product_discountedprice_ds() {
        return linked_product_discountedprice_ds;
    }
    
    public List<String> getLinked_product_sku_ss() {
        return linked_product_sku_ss;
    }

    public void setLinked_product_discountedprice_ds(List<Double> linked_product_discountedprice_ds) {
        this.linked_product_discountedprice_ds = linked_product_discountedprice_ds;
    }
    
    public void setLinked_product_sku_ss(List<String> linked_product_sku_ss) {
        this.linked_product_sku_ss = linked_product_sku_ss;
    }
    

    public List<Double> getStock_i_ds() {
        return stock_i_ds;
    }

    public void setStock_i_ds(List<Double> stock_i_ds) {
        this.stock_i_ds = stock_i_ds;
    }

    public String getDescription_t() {
        return description_t;
    }

    public ProductResultDoc setDescription_t(String description_t) {
        this.description_t = description_t;
        return this;
    }

    private String description_t;

    public String getImage_s() {
        return image_s;
    }

    public void setImage_s(String image_s) {
        this.image_s = image_s;
    }

    public String getImage_2_s() {
        return image_2_s;
    }

    public ProductResultDoc setImage_2_s(String image_2_s) {
        this.image_2_s = image_2_s;
        return this;
    }
    
    public String getCannoical_url_s() {
        return cannoical_url_s;
    }

    public ProductResultDoc setCannoical_url_s(String cannoical_url_s) {
        this.cannoical_url_s = cannoical_url_s;
        return this;
    }

    private String cannoical_url_s;

    public String getName_s() {
        return name_s;
    }

    public ProductResultDoc setName_s(String name_s) {
        this.name_s = name_s;
        return this;
    }

    public String getProduct_id_d() {
        return product_id_d;
    }

    public ProductResultDoc setProduct_id_d(String product_id_d) {
        this.product_id_d = product_id_d;
        return this;
    }
    
    public String getProduct_id_s() {
        return product_id_s;
    }

    public ProductResultDoc setProduct_id_s(String product_id_s) {
        this.product_id_s = product_id_s;
        return this;
    }

    public String getSmallImage_s() {
        return smallImage_s;
    }

    public ProductResultDoc setSmallImage_s(String smallImage_s) {
        this.smallImage_s = smallImage_s;
        return this;
    }

    public String getPrice_d() {
        return price_d;
    }

    public ProductResultDoc setPrice_d(String price_d) {
        this.price_d = price_d;
        return this;
    }

    public String getSku_s() {
        return sku_s;
    }

    public ProductResultDoc setSku_s(String sku_s) {
        this.sku_s = sku_s;
        return this;
    }

    public String getDiscounted_price_d() {
        return discounted_price_d;
    }

    public ProductResultDoc setDiscounted_price_d(String discounted_price_d) {
        this.discounted_price_d = discounted_price_d;
        return this;
    }

    public String getCurrentPageUrl_s() {
        return currentPageUrl_s;
    }

    public ProductResultDoc setCurrentPageUrl_s(String currentPageUrl_s) {
        this.currentPageUrl_s = currentPageUrl_s;
        return this;
    }
    
    public double getStock_d() {
        return this.stock_d;
    }

    public ProductResultDoc setStock_d(double stock_d) {
        this.stock_d = stock_d;
        return this;
    }
    
    public String getSize_s() {
        return size_s;
    }

    public ProductResultDoc setSize_s(String size_s) {
        this.size_s = size_s;
        return this;
    }
    
    public String getLine_s() {
        return this.line_s;
    }

    public ProductResultDoc setLine_s(String line_s) {
        this.line_s = line_s;
        return this;
    }
    
    public List<String> getParentCategories_ss() {
		return parentCategories_ss;
	}

	public ProductResultDoc setParentCategories_ss(List<String> parentCategories_ss) {
		this.parentCategories_ss = parentCategories_ss;
		return this;
	}
	
	public String getCategory_s() {
        return category_s;
    }

    public ProductResultDoc setCategory_s(String category_s) {
        this.category_s = category_s;
        return this;
    }
    
    public boolean getStatus_b() {
        return this.status_b;
    }

    public ProductResultDoc setStatus_b(boolean status_b) {
        this.status_b = status_b;
        return this;
    }
    
    public List<BadgingConfigCollection> getBadging_ss() {
    	if(Optional.ofNullable(badging_ss).isPresent()) {
    		return badging_ss.stream().map(badge -> (BadgingConfigCollection)new Gson().fromJson(badge, new TypeToken<BadgingConfigCollection>() {}.getType())).collect(Collectors.toList());
    	} else {
    		return null;
    	}
	}

	public ProductResultDoc setBadging_ss(List<String> badging_ss) {
		this.badging_ss = badging_ss;
		return this;
	}
    
}
