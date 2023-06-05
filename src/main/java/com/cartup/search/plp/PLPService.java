package com.cartup.search.plp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cartup.commons.constants.Constants;
import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.commons.repo.SearchRepoClient;
import com.cartup.commons.repo.model.FacetEntity;
import com.cartup.commons.repo.model.product.SpotDyProductDocument;
import com.cartup.commons.repo.model.search.CartUpSearchConfDocument;
import com.cartup.commons.repo.model.search.Facet;
import com.cartup.commons.repo.model.search.FacetComparator;
import com.cartup.commons.repo.model.search.ProductsFacetResult;
import com.cartup.commons.repo.model.search.QueryEntity;
import com.cartup.commons.util.EmptyUtil;
import com.cartup.search.modal.ProductInfo;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.modal.SearchResult;
import com.cartup.search.modal.VariantInfo;

public class PLPService {
	
    private static Logger logger = LoggerFactory.getLogger(PLPService.class);

    private SearchRepoClient client;

    public PLPService(){
        this.client = RepoFactory.getSearchRepoClient();
    }

    public SearchResult process(Map<String, String> reqParams, SearchRequest plpRequest) throws CartUpServiceException {
        try {
            String orgId = (EmptyUtil.isNotEmpty(plpRequest.getOrgId())) ? plpRequest.getOrgId() : reqParams.get(Constants.ORG_ID);
            String userId = (EmptyUtil.isNotEmpty(plpRequest.getUserId())) ? plpRequest.getUserId() : reqParams.get(Constants.USER_ID);
            String orgName = (EmptyUtil.isNotEmpty(plpRequest.getOrgName())) ? plpRequest.getOrgName() : reqParams.get(Constants.ORG_NAME);
            String category = (EmptyUtil.isNotEmpty(plpRequest.getSearchQuery())) ? plpRequest.getSearchQuery() : reqParams.get(Constants.QUERY);
            if (EmptyUtil.isEmpty(orgId)){
                throw new CartUpServiceException("org id is empty");
            }

            if (EmptyUtil.isEmpty(category)){
                category = "";
                // throw new CartUpServiceException("search query is empty");

            }

            CartUpSearchConfDocument docu = client.GetUsingOrgId(orgId);
            if (EmptyUtil.isNull(docu)){
                throw new CartUpServiceException(String.format("No search configuration is saved for org id %s", orgId));
            }

            logger.info("Found search conf for org id {}", orgId);
            PLPBuilderTask task = new PLPBuilderTask(orgId, orgName, userId, reqParams, category, docu, plpRequest);
            String solrQuery = task.build();
            Map<String, Facet> facetMap = task.getFacetMap();
            ProductsFacetResult res =  client.Execute(orgId, solrQuery, 1000);
            return getPLPListing(res, facetMap)
                    .setCurrency(docu.getCurrency())
                    .setSortEntity(docu.getSortEntity())
                    .setNumberofdocs(res.getNumFound())
                    .setPagination(docu.getPaginationCount())
                    .setSearchSelector(docu.getSearchSelectors())
                    .setSearchTheme(docu.getSearchThemes());
        } catch (Exception e){
            logger.error("Failed to execute search", e);
            throw new CartUpServiceException(e.getMessage());
        }
    }

    private SearchResult getPLPListing(ProductsFacetResult result, Map<String, Facet> facetMap){
        List<ProductInfo> docs = new ArrayList<>();
        if (EmptyUtil.isNotEmpty(result.getResult())){
            for (SpotDyProductDocument doc : result.getResult()){
            	try {
	                ProductInfo info = new ProductInfo()
	                        .setName(doc.getNameS())
	                        .setPrice(String.valueOf(doc.getPriceD()))
	                        .setSmallImage(doc.getImageS())
	                        .setSku(doc.getSkuS())
	                        .setCurrentPageUrl(doc.getCannoicalUrlS())
	                        .setDescription(doc.getDescriptionT())
	                        .setDiscountedPrice(String.valueOf(doc.getDiscountePriceD()))
	                        .setRating(String.valueOf(doc.getRatingD()));
	                if (doc.getVariantB() != null && doc.getVariantB()){
	                    VariantInfo variantInfo = new VariantInfo(doc.getLinkedProductNameSs(), doc.getLinkedProductPriceDs(),
	                            doc.getLinkedProductDiscountedpriceDs(), doc.getStockIDs(), doc.getLinkedProductSkuSs(),
	                            doc.getLinkedProductIdLs(), doc.getLinkedVariantIdSs());
	
	                    info.setVariantInfo(variantInfo.generateVariantInfo());
	                }
	                docs.add(info);
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
            
        }

        Set<FacetEntity> facets = new HashSet<>();
        if (EmptyUtil.isNotEmpty(result.getFacetCounts())){
            for (Map.Entry<String, Integer> entry : result.getFacetCounts().entrySet()){
                Facet f = facetMap.get(entry.getKey());
                facets.add(
                        new FacetEntity()
                                .setDisplayName(f.getDisplayName())
                                .setDisplayType(f.getDisplayType())
                                .setValue(f.getValue().get(0))
                                //.setIndex(f.getValue().get(0).getIndex())
                                .setType(f.getType())
                                .setRepoFieldName(f.getRepoFieldName())
                                .setCount(entry.getValue())
                                .setOperator(f.getOperator())
                );
            }
        }

        if (EmptyUtil.isNotEmpty(result.getFacetFieldsMap())){
            for (Map.Entry<String, Map<String, Integer>> entry : result.getFacetFieldsMap().entrySet()){
                //entry.getKey() is repo_field_id which was added in SearchQueryBuilderTask in addFacets method
                Facet f = facetMap.get(entry.getKey());
                for (Map.Entry<String, Integer> fEntry : entry.getValue().entrySet()){
                    facets.add(
                            new FacetEntity()
                                    .setType(f.getType())
                                    .setDisplayName(f.getDisplayName())
                                    //.setIndex(f.getValue().get(0).getIndex())
                                    .setDisplayType(f.getDisplayType())
                                    .setValue(new QueryEntity().setValue(fEntry.getKey()).setName(fEntry.getKey()))
                                    .setRepoFieldName(f.getRepoFieldName())
                                    .setCount(fEntry.getValue())
                                    .setOperator(f.getOperator())
                    );
                }
            }
        }

        List<FacetEntity> orderedFacets = new ArrayList<>(facets);
        if (EmptyUtil.isNotEmpty(facets)){
            Collections.sort(orderedFacets, new FacetComparator());
        }

        return new SearchResult().setDocs(docs).setFacetcount(orderedFacets);
    }
}
