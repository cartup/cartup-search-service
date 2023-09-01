package com.cartup.search.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cartup.commons.repo.model.product.SpotDyProductDocument;
import com.cartup.commons.util.EmptyUtil;
import com.cartup.search.modal.ProductInfo;
import com.cartup.search.modal.ProductResultDoc;
import com.cartup.search.modal.VariantInfo;
import com.google.gson.Gson;

public class SearchUtils {

	public static List<ProductInfo> convertToProductInfo(List<SpotDyProductDocument> documents) {
		Gson gson = new Gson();
		return documents.stream()
				.filter(docu -> docu.isStatusB() && docu.isVisibilityB())
				.map(docu -> {
			String docString = gson.toJson(docu);
			ProductResultDoc productResultDoc = gson.fromJson(docString, ProductResultDoc.class);
			ProductInfo info = new ProductInfo().setName(productResultDoc.getName_s())
					.setPrice(productResultDoc.getPrice_d()).setSmallImage(getImage(productResultDoc))
					.setSku(productResultDoc.getSku_s()).setDiscountedPrice(productResultDoc.getDiscounted_price_d())
					.setCurrentPageUrl(productResultDoc.getCurrentPageUrl_s())
					.setImage2(productResultDoc.getImage_2_s()).setColor(productResultDoc.getColor_s())
					.setStock(productResultDoc.getStock_d());
			if (Optional.ofNullable(productResultDoc.getBadging_ss()).isPresent()) {
				org.json.simple.JSONArray badging = new org.json.simple.JSONArray();
				badging.addAll(productResultDoc.getBadging_ss());
				info.setBadging(badging);
			}
			if (EmptyUtil.isEmpty(productResultDoc.getCurrentPageUrl_s())
					&& EmptyUtil.isNotEmpty(productResultDoc.getCannoical_url_s())) {
				info.setCurrentPageUrl(productResultDoc.getCannoical_url_s());
			}
			if (productResultDoc.isVariant_b()) {
				VariantInfo variantInfos = new VariantInfo(productResultDoc.getLinked_product_name_ss(),
						productResultDoc.getLinked_product_price_ds(),
						productResultDoc.getLinked_product_discountedprice_ds(), productResultDoc.getStock_i_ds(),
						productResultDoc.getLinked_product_sku_ss(), productResultDoc.getLinked_product_id_ls(),
						productResultDoc.getLinked_variant_id_ss());

				info.setVariantInfo(variantInfos.generateVariantInfo());
			}
			if (EmptyUtil.isNotNull(productResultDoc.getProduct_id_s())) {
				info.setProductId(productResultDoc.getProduct_id_s());
			}
			return info;
		}).collect(Collectors.toList());
	}

	private static String getImage(ProductResultDoc doc) {
		if (EmptyUtil.isNotEmpty(doc.getSmallImage_s())) {
			return doc.getSmallImage_s();
		} else if (EmptyUtil.isNotEmpty(doc.getImage_s())) {
			return doc.getImage_s();
		} else if (EmptyUtil.isNotEmpty(doc.getCannoical_url_s())) {
			return doc.getCannoical_url_s();
		} else if (EmptyUtil.isNotEmpty(doc.getCurrentPageUrl_s())) {
			return doc.getCurrentPageUrl_s();
		}
		return "";
	}

}
