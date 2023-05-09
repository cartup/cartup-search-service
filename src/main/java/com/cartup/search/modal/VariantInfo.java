package com.cartup.search.modal;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
public class VariantInfo {

    private static Logger logger = LoggerFactory.getLogger(VariantInfo.class);

    private List<String> linked_product_name_ss;
    private List<String> linked_product_sku_ss;
    private List<Double> linked_product_price_ds;
    private List<Double> linked_product_discountedprice_ds;
    private List<Double> stock_i_ds;
    private List<Long> linked_product_id_ls;
    private List<String> linked_variant_id_ss;

    public VariantInfo(List<String> linked_product_name_ss, List<Double> linked_product_price_ds, List<Double> linked_product_discountedprice_ds, 
                        List<Double> stock_i_ds, List<String> linked_product_sku_ss, List<Long> linked_product_id_ls, List<String> linked_variant_id_ss) {
        this.linked_product_name_ss = linked_product_name_ss;
        this.linked_product_price_ds = linked_product_price_ds;
        this.linked_product_discountedprice_ds = linked_product_discountedprice_ds;
        this.stock_i_ds = stock_i_ds;
        this.linked_product_sku_ss = linked_product_sku_ss;
        this.linked_product_id_ls = linked_product_id_ls;
        this.linked_variant_id_ss = linked_variant_id_ss;
    }

    @SuppressWarnings("unchecked")
	public JSONArray generateVariantInfo(){
        int variantSize = linked_product_name_ss.size();
        JSONArray  variantInfoArray = new JSONArray();
        try
        {
            for (int index=0; index<variantSize; ++index)
            {
                try
                {
                    JSONObject variantObject = new JSONObject();
                    if(linked_product_name_ss == null){
                        variantObject.put("name", null);
                    } else {
                        variantObject.put("name", linked_product_name_ss.get(index));
                    }
    
                    if(linked_product_sku_ss == null){
                        variantObject.put("sku", null);
                    } else {
                        variantObject.put("sku", linked_product_sku_ss.get(index));
                    }
    
                    if(linked_variant_id_ss == null){
                        variantObject.put("variantId", null);
                    } else {
                        variantObject.put("variantId", linked_variant_id_ss.get(index));
                    }
    
                    if(linked_product_price_ds == null){
                        variantObject.put("price", null);
                    } else {
                        variantObject.put("price", String.valueOf(linked_product_price_ds.get(index)));
                    }
    
                    if(linked_product_id_ls == null){
                        variantObject.put("productId", null);
                    } else {
                        variantObject.put("productId", String.valueOf(linked_product_id_ls.get(index)));
                    }
    
                    if(stock_i_ds == null){
                        variantObject.put("stock", null);
                    } else {
                        variantObject.put("stock", String.valueOf(stock_i_ds.get(index)));
                    }
    
    
                    if(linked_product_discountedprice_ds == null){
                        variantObject.put("discountedPrice", null);
                    } else {
                        variantObject.put("discountedPrice", String.valueOf(linked_product_discountedprice_ds.get(index)));
                    }
                    variantInfoArray.add(variantObject);
                } catch (JSONException jse) {
                    logger.error("Exception occurred while populating variant related information", jse);
                }

            }
        } catch (JSONException jse) {
            logger.error("Exception occurred while populating variant related information", jse);
            JSONObject exception = new JSONObject();
            exception.put("Exception", "Exception occurred while populating variant related information");
            variantInfoArray.add(exception);
        }
        return variantInfoArray;
    }
}

