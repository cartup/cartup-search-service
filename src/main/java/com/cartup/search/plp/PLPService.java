package com.cartup.search.plp;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.cartup.commons.constants.Constants;
import com.cartup.commons.exceptions.CartUpRepoException;
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
import com.cartup.commons.util.ValueUtil;
import com.cartup.search.modal.ProductInfo;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.modal.SearchResult;
import com.cartup.search.modal.VariantInfo;
import com.cartup.search.util.CacheMapDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PLPService {
	
    private static Logger logger = LoggerFactory.getLogger(PLPService.class);
    
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private SearchRepoClient client;
    
    private Gson gson;
    
    private RedisTemplate<String, String> redisTemplate;

    public PLPService(RedisTemplate<String, String> redisTemplate){
        this.client = RepoFactory.getSearchRepoClient();
        this.redisTemplate = redisTemplate;
        this.gson = new GsonBuilder().registerTypeAdapter(Map.class, new CacheMapDeserializer())
                	.create();
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
            CartUpSearchConfDocument docu = client.GetUsingOrgId(orgId, true);
            if (EmptyUtil.isNull(docu)){
                throw new CartUpServiceException(String.format("No search configuration is saved for org id %s", orgId));
            }
            SearchResult searchResult = new SearchResult();
            String configKey = String.format("%s:%s", plpRequest.getOrgId(), "plp_docs");
            if(this.redisTemplate.hasKey(configKey)) {
            	ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
                Map<String, Object> cacheMap = this.gson.fromJson(valueOperations.get(configKey), Map.class);
                searchResult = getPLPListing(null, null, cacheMap, plpRequest, reqParams, docu)
                        .setNumberofdocs(Integer.parseInt(cacheMap.get("numFound").toString()));
            } else {
            	logger.info("Found search conf for org id {}", orgId);
            	PLPBuilderTask task = new PLPBuilderTask(orgId, orgName, userId, reqParams, category, docu, plpRequest, false);
        		String solrQuery = task.build();
        		Map<String, Facet> facetMap = task.getFacetMap();
        		ProductsFacetResult res =  client.Execute(orgId, solrQuery, 1000);
        		searchResult = getPLPListing(res, facetMap, null, plpRequest, reqParams, docu)
        		        .setNumberofdocs(res.getNumFound());
            	cachePlpResponse(reqParams, plpRequest, orgId, userId, orgName, category, docu,
						configKey);
                
            }
            return searchResult.setCurrency(docu.getCurrency())
                    .setSortEntity(docu.getSortEntity())
                    .setPagination(Optional.ofNullable(docu.getPagination()).isPresent() ? docu.getPagination() : false)
                    .setPaginationCount(docu.getPaginationCount())
                    .setSearchSelector(docu.getSearchSelectors())
                    .setSearchTheme(docu.getSearchThemes());
        } catch (Exception e){
            logger.error("Failed to execute search", e);
            throw new CartUpServiceException(e.getMessage());
        }
    }

	private void cachePlpResponse(Map<String, String> reqParams, SearchRequest plpRequest, String orgId,
			String userId, String orgName, String category, CartUpSearchConfDocument docu, String configKey)
			throws IOException, CartUpServiceException, CartUpRepoException {
		executor.submit(() -> {
			try {
				PLPBuilderTask task = new PLPBuilderTask(orgId, orgName, userId, reqParams, category, docu, plpRequest, true);
				String solrQuery = task.build();
				Map<String, Facet> facetMap = task.getFacetMap();
				ProductsFacetResult res = client.Execute(orgId, solrQuery, 1000);

				SearchResult searchResult = getPLPListing(res, facetMap, null, plpRequest, reqParams, docu)
						.setNumberofdocs(res.getNumFound());

				ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
				Map<String, Object> cacheMap = new HashMap<>();
				cacheMap.put("docs", searchResult.getDocs());
				cacheMap.put("facets", searchResult.getFacetcount());
				cacheMap.put("numFound", res.getNumFound());
				
				valueOperations.set(configKey, this.gson.toJson(cacheMap), Duration.ofHours(1));
			} catch (CartUpRepoException | IOException | CartUpServiceException e) {
				logger.error("Exception occurred while caching plp response", e);
			}
		});
	}

    private SearchResult getPLPListing(ProductsFacetResult result, Map<String, Facet> facetMap, Map<String, Object> cacheMap, SearchRequest plpRequest, Map<String, String> reqParams, CartUpSearchConfDocument docu){
    	if(cacheMap != null ) {
    		SearchResult searchResult = setDocsByPages(plpRequest, (List<ProductInfo>)cacheMap.get("docs"), reqParams, docu);
    		searchResult.setFacetcount((List<FacetEntity>)cacheMap.get("facets"));
    		return searchResult;
    	} else {
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
    	                        .setRating(String.valueOf(doc.getRatingD()))
    	                        .setStock(doc.getStockD());
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

	private SearchResult setDocsByPages(SearchRequest plpRequest, List<ProductInfo> docs, Map<String, String> reqParams, CartUpSearchConfDocument docu) {
		Integer startIndex = 0;
		Integer endIndex = 10;
		SearchResult searchResult = new SearchResult();
		
		if (EmptyUtil.isNotNull(ValueUtil.get(() -> plpRequest.getPagination()))){
            if (EmptyUtil.isNotNull(plpRequest.getPagination().getRow())){
            	endIndex = plpRequest.getPagination().getRow();
            }

            if (EmptyUtil.isNotNull(plpRequest.getPagination().getStart())){
            	startIndex = plpRequest.getPagination().getStart();
            }
        } else {
            if (EmptyUtil.isNotNull(docu) && EmptyUtil.isNotNull(docu.getPaginationCount())){
            	endIndex = docu.getPaginationCount();
            }

            if (EmptyUtil.isNotEmpty(reqParams) && EmptyUtil.isNotEmpty(reqParams.get("start"))){
            	startIndex = new Integer(reqParams.get("start"));
            }
        }
		
		searchResult.setDocs(docs.subList(startIndex, docs.size() < endIndex ? docs.size() : endIndex));
		return searchResult;
	}
}
