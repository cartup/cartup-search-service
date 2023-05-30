package com.cartup.search.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cartup.commons.constants.Constants;
import com.cartup.commons.exceptions.CartUpRepoException;
import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.CustomWidgetRepoClient;
import com.cartup.commons.repo.ProductRepoClient;
import com.cartup.commons.repo.RepoConstants;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.commons.repo.SearchConfRepoClient;
import com.cartup.commons.repo.SearchRepoClient;
import com.cartup.commons.repo.model.FacetEntity;
import com.cartup.commons.repo.model.product.SpotDyProductDocument;
import com.cartup.commons.repo.model.search.CartUpSearchConfDocument;
import com.cartup.commons.repo.model.search.Facet;
import com.cartup.commons.repo.model.search.FacetComparator;
import com.cartup.commons.repo.model.search.ProductsFacetResult;
import com.cartup.commons.repo.model.search.QueryEntity;
import com.cartup.commons.util.EmptyUtil;
import com.cartup.search.modal.ActionSet;
import com.cartup.search.modal.ProductInfo;
import com.cartup.search.modal.RuleSet;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.modal.SearchResult;
import com.cartup.search.modal.SearchRules;
import com.cartup.search.modal.VariantInfo;
import com.cartup.search.search.SearchQueryBuilderTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SearchService {

	private CustomWidgetRepoClient customWidgetRepoClient;

	private ProductRepoClient productRepoClient;

	private SearchConfRepoClient searchConfRepoClient;

	private Gson gson;

	private SearchRepoClient client;

	private CacheService cacheService;

	private RedisTemplate<String, String> redisTemplate;

	public SearchService(CacheService cacheService, RedisTemplate<String, String> redisTemplate) {
		gson = new Gson();
		this.customWidgetRepoClient = RepoFactory.getCustomWidgetRepoClient();
		this.productRepoClient = RepoFactory.getProductRepoClient();
		this.searchConfRepoClient = RepoFactory.getSearchConfRepoClient();
		this.client = RepoFactory.getSearchRepoClient();
		this.cacheService = cacheService;
		this.redisTemplate = redisTemplate;
		log.info("SearchService Initialization done");
	}

	public SearchResult processSearch(Map<String, String> reqParams, SearchRequest searchRequest) throws CartUpServiceException {
		try {

			List<ActionSet> actionSet = searchAlgorithm(searchRequest);

			String orgId = (EmptyUtil.isNotEmpty(searchRequest.getOrgId())) ? searchRequest.getOrgId() : reqParams.get(Constants.ORG_ID);
			String searchQuery = (EmptyUtil.isNotEmpty(searchRequest.getSearchQuery())) ? searchRequest.getSearchQuery() : reqParams.get(Constants.QUERY);
			if (EmptyUtil.isEmpty(orgId)){
				throw new CartUpServiceException("org id is empty");
			}

			if (EmptyUtil.isEmpty(searchQuery)){
				searchQuery = "";
			}

			CartUpSearchConfDocument docu = client.GetUsingOrgId(orgId);
			if (EmptyUtil.isNull(docu)){
				throw new CartUpServiceException(String.format("No search configuration is saved for org id %s", orgId));
			}
			log.info("Found search conf for org id {}", orgId);

			AtomicReference<List<SpotDyProductDocument>> pinnedProductsRef = new AtomicReference<>(new ArrayList<>());
			AtomicReference<List<String>> productsToBeRemovedRef = new AtomicReference<>(new ArrayList<>());
			try {
				if(!actionSet.isEmpty()) {
					actionSet.stream().forEach(action -> {
						if(action.getType() == 1) {
							// PIN AN ITEM
							try {
								pinnedProductsRef.set(new ArrayList<>(productRepoClient.ListUsingNames(searchRequest.getOrgId(), action.getValue(), true).values()));
							} catch (CartUpRepoException e) {
								log.error("Error occured while fetching the pinned items {}", action.getValue());
							}
						}
						if(action.getType() == 2) {
							// REMOVE KEYWORD
							StringBuilder newSearchQuery = new StringBuilder();
							action.getValue().stream().forEach(keywordToRemove -> {
								newSearchQuery.append(searchRequest.getSearchQuery().replaceAll(keywordToRemove, ""));
							});
							searchRequest.setSearchQuery(newSearchQuery.toString());
						}
						if(action.getType() == 3) {
							// REMOVE ITEM
							productsToBeRemovedRef.set(action.getValue());
						}
						if(action.getType() == 4) {
							// REPLACE A KEYWORD
							StringBuilder newSearchQuery = new StringBuilder();
							action.getValue().stream().forEach(keywordToReplace -> {
								newSearchQuery.append(searchRequest.getSearchQuery().replaceAll(keywordToReplace, action.getBufferField()));
							});
							searchRequest.setSearchQuery(newSearchQuery.toString());
						}
						if(action.getType() == 5) {
							// TODO BOOST THE SPECIFIED FIELD AND VALUE

						}
					});
				}
			} catch(Exception e) {
				log.error("Exception occured while loading search config", e);			
			}


			SearchQueryBuilderTask task = new SearchQueryBuilderTask(orgId, reqParams, searchQuery, docu, searchRequest, redisTemplate);
			String solrQuery = task.build();
			Map<String, Facet> facetMap = task.getFacetMap();
			ProductsFacetResult res =  client.Execute(orgId, solrQuery, 1000);
			SearchResult searchResult = toSearchResult(res, facetMap, pinnedProductsRef, productsToBeRemovedRef)
					.setCurrency(docu.getCurrency())
					.setSortEntity(docu.getSortEntity())
					.setPagination(docu.getPaginationCount())
					.setSearchSelector(docu.getSearchSelectors())
					.setSearchTheme(docu.getSearchThemes());
			searchResult.setNumberofdocs(searchResult.getDocs().size());
			return searchResult;
		} catch (Exception e){
			log.error("Failed to execute search", e);
			throw new CartUpServiceException(e.getMessage());
		}
	}

	private SearchResult toSearchResult(ProductsFacetResult result, Map<String, Facet> facetMap, AtomicReference<List<SpotDyProductDocument>> pinnedProductsRef, AtomicReference<List<String>> productsToBeRemovedRef){
		List<ProductInfo> docs = new ArrayList<>();
		List<SpotDyProductDocument> pinnedProducts = pinnedProductsRef.get();
		pinnedProducts.addAll(result.getResult());
		if (EmptyUtil.isNotEmpty(pinnedProducts)){
			for (SpotDyProductDocument doc : pinnedProducts){
				ProductInfo info = new ProductInfo()
						.setName(doc.getNameS())
						.setPrice(String.valueOf(doc.getPriceD()))
						.setSmallImage(doc.getImageS())
						.setSku(doc.getSkuS())
						.setCurrentPageUrl(doc.getCannoicalUrlS())
						.setDescription(doc.getDescriptionT())
						.setDiscountedPrice(String.valueOf(doc.getDiscountePriceD()))
						.setRating(String.valueOf(doc.getRatingD()))
						.setProductId(doc.getProductIdS())
						.setStock(doc.getStockD());
				if (Optional.ofNullable(doc.getVariantB()).isPresent() && doc.getVariantB() == true){
					VariantInfo variantInfo = new VariantInfo(doc.getLinkedProductNameSs(), doc.getLinkedProductPriceDs(),
							doc.getLinkedProductDiscountedpriceDs(), doc.getStockIDs(), doc.getLinkedProductSkuSs(),
							doc.getLinkedProductIdLs(), doc.getLinkedVariantIdSs());

					try {
						info.setVariantInfo(variantInfo.generateVariantInfo());
					} catch (Exception e) {
						
					}
				}
				docs.add(info);
			}
		}

		List<ProductInfo> resultDocs = docs.stream().filter(doc -> !productsToBeRemovedRef.get().contains(doc.getName().toLowerCase())).collect(Collectors.toList());

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

		return new SearchResult().setDocs(resultDocs).setFacetcount(orderedFacets);
	}

	private List<ActionSet> searchAlgorithm(SearchRequest searchRequest) {
		List<ActionSet> actionSet = new ArrayList<>();
		try {
			JSONObject cacheJsonObject = this.cacheService.getSearchConfig(searchRequest.getOrgId());

			if(!JSONObject.NULL.equals(cacheJsonObject)) {
				
				Map<String, Object> cacheMap = this.gson.fromJson(String.valueOf(cacheJsonObject), new TypeToken<Map<String, Object>>() {}.getType());

				String searchQuery = searchRequest.getSearchQuery();

				String[] searchKeywordList = searchQuery.split(" ");

				String startKeyword = searchKeywordList[0];
				int length = searchKeywordList.length;
				String endKeyword = searchKeywordList[length-1];

				Set<SearchRules> possibleSearchRuleSet = new HashSet<>();

				if(cacheMap.containsKey("ruleLookupMap")) {
					String ruleLookupJson = String.valueOf(cacheMap.get("ruleLookupMap"));
					Map<String, Set<SearchRules>> ruleMap = this.gson.fromJson(ruleLookupJson, new TypeToken<Map<String, Set<SearchRules>>>() {}.getType());

					// Check for scenario what if the given search query keyword matches startsWith and endsWith
					for(String keyword : searchKeywordList) {
						String startsWithKey = String.format("%d*_*%s", 1, keyword);
						if(ruleMap.containsKey(startsWithKey)) {
							possibleSearchRuleSet.addAll(ruleMap.get(startsWithKey));
						}
						String containsWithKey = String.format("%d*_*%s", 2, keyword);
						if(ruleMap.containsKey(containsWithKey)) {
							possibleSearchRuleSet.addAll(ruleMap.get(containsWithKey));
						}
						String endsWithKey = String.format("%d*_*%s", 3, keyword);
						if(ruleMap.containsKey(endsWithKey)) {
							possibleSearchRuleSet.addAll(ruleMap.get(endsWithKey));
						}
					}
					for(SearchRules searchRule : possibleSearchRuleSet) {
						StringBuilder ruleExpression = new StringBuilder();
						for(RuleSet ruleSet : searchRule.getRuleSet()) {
							String operator = StringUtils.isNotBlank(ruleSet.getOperator())? (ruleSet.getOperator().equals("AND") ? "&&" : ruleSet.getOperator().equals("OR") ? "||" : StringUtils.EMPTY) : StringUtils.EMPTY;
							if(ruleSet.getType() == 1) {
								ruleExpression.append(ruleSet.getValue().equals(startKeyword)).append(operator);
							}
							if(ruleSet.getType() == 2) {
								ruleExpression.append(searchQuery.contains(ruleSet.getValue())).append(operator);
							}

							if(ruleSet.getType() == 3) {
								ruleExpression.append(ruleSet.getValue().equals(endKeyword)).append(operator);
							}
						}
						// using Spring's SpEL to evaluate the boolean expression
						ExpressionParser parser = new SpelExpressionParser();
						Expression exp = parser.parseExpression(ruleExpression.toString());
						if(exp.getValue(Boolean.class)) {
							actionSet.addAll(searchRule.getActionSet());
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			log.error("Error while search computation", e);
		}
		if(!actionSet.isEmpty()) {
			return actionSet;
		}
		return new ArrayList<>();
	}

	@Scheduled(fixedDelayString="${REFRESH_CACHE_FIXED_DELAY}")
	public void refreshCache() {
		log.info("Cache refresh");
		SolrDocumentList result = new SolrDocumentList();

		try {
			if(this.customWidgetRepoClient == null) {
				RepoFactory.loadConfiguration();
			}
			result = this.customWidgetRepoClient.find("searchconf");
		} catch (Exception e) {
			log.error("Failed due to get search configuration because of the following exception ", e);
		}
		if(!result.isEmpty()) {
			List<Map<String, Object>> docs = new ArrayList<>();
			result.stream()
			.forEach(resultDoc -> {
				Map<String, Object> fieldMap = new HashMap<>();
				Iterator<Entry<String, Object>> ite = resultDoc.iterator();
				while(ite.hasNext()) {
					Entry<String, Object> entry = ite.next();
					if(entry.getKey().equals("orgID_s")) {
						fieldMap.put(entry.getKey(), entry.getValue());
					}
					if(entry.getKey().equals("search_rules_o")) {
						fieldMap.put(entry.getKey(), entry.getValue());
					}
					if(fieldMap.size() == 2) {
						docs.add(fieldMap);
					}
				}
			});
			Map<String, Object> cacheMap = new LinkedHashMap<>();
			docs.stream().forEach(searchConfig -> {
				Map<String, Object> keyMap = new LinkedHashMap<>();
				keyMap.put("ruleConfigMap", searchConfig.get("search_rules_o"));
				keyMap.put("ruleLookupMap", this.prepareSearchRuleMap(searchConfig.get("search_rules_o").toString(), searchConfig.get("orgID_s").toString()));
				cacheMap.put(searchConfig.get("orgID_s").toString(), keyMap);
			});
			// Calling this method to create a formatted rule map
			this.cacheService.updateCache(this.gson.toJson(cacheMap));
			List<String> liveCacheKeys = cacheMap.keySet().stream().map(orgId -> String.format("%s:%s", orgId, "search_config_stat")).collect(Collectors.toList());
			List<String> deadCacheKeys = this.redisTemplate.keys("*:search_config_stat").stream()
				.filter(cacheKey -> !liveCacheKeys.contains(cacheKey))
				.collect(Collectors.toList());
			this.redisTemplate.delete(deadCacheKeys);
			this.prepareStopWordAndSynonymMap();
		}
	}

	private String prepareSearchRuleMap(String searchString, String orgId) {
		Map<String, Set<SearchRules>> ruleMap = new LinkedHashMap<>();
		try {
			Set<SearchRules> searchRulesList = gson.fromJson(searchString, new TypeToken<Set<SearchRules>>() {}.getType());
			for(SearchRules searchRules : searchRulesList) {
				for(RuleSet rs : searchRules.getRuleSet()) {
					String ruleKey = rs.getKey();
					if(ruleMap.containsKey(ruleKey)) {
						Set<SearchRules> pseudoRuleKeyList = ruleMap.get(ruleKey);
						pseudoRuleKeyList.add(searchRules);
						ruleMap.put(ruleKey.toString(), pseudoRuleKeyList);
					} else {
						Set<SearchRules> ruleKeyList = new HashSet<>();
						ruleKeyList.add(searchRules);
						ruleMap.put(ruleKey.toString(), ruleKeyList);
					}
					if(rs.getType().intValue() == 2) {
						// Check for contains with, clone it for starts with and ends with
						keyLookup(ruleMap, rs.getKey(), rs.getValue());
					}
				}
			}
			return this.gson.toJson(ruleMap);
		} catch(Exception e) {
			log.error("Exception occurred {}", orgId, e);
		}
		return new JSONObject().toString();
	}

	private void keyLookup(Map<String, Set<SearchRules>> ruleMap, String lookupKey, String lookupString) {
		String splittedKey = lookupKey.split("\\*_\\*")[1]; 
		String startsWithLookupKey = String.format("%d*_*%s", 1, splittedKey);
		String endsWithLookupKey = String.format("%d*_*%s", 3, splittedKey);
		Set<SearchRules> existingSearchRules = ruleMap.get(lookupKey);
		if(ruleMap.containsKey(startsWithLookupKey)) {
			// Need to test this logic
			existingSearchRules.addAll(existingSearchRules);
		}
		if(ruleMap.containsKey(endsWithLookupKey)) {
			existingSearchRules.addAll(ruleMap.get(endsWithLookupKey));
		}
	}

	private void prepareStopWordAndSynonymMap() {
		try {
			Optional<SolrDocumentList> existingDocList = this.processStopWordsAndSynonymGetRequest();
			StringBuilder orgId = new StringBuilder();
			if(existingDocList.isPresent()) {
				Map<String, Object> orgBasedCacheMap = new HashMap<>();
				Map<String, Object> cacheMap = new HashMap<>();
				for(SolrDocument resultDoc : existingDocList.get()) {
					Map<String, Object> fieldMap = new HashMap<>();
					Iterator<Entry<String, Object>> ite = resultDoc.iterator();
					while(ite.hasNext()) {
						Entry<String, Object> entry = ite.next();
						fieldMap.put(entry.getKey(), entry.getValue());
						if(entry.getKey().equals(RepoConstants.ORG_ID_S)) {
							orgId = new StringBuilder(String.valueOf(entry.getValue()));
						}
					}
					cacheMap.put(fieldMap.get(RepoConstants.DOC_TYPE_S).toString(), fieldMap);
					orgBasedCacheMap.put(orgId.toString(), cacheMap);
				}

				List<String> liveCacheKeys = orgBasedCacheMap.entrySet().stream().map(entry -> {
					String cacheKey = String.format("%s:%s", entry.getKey(), "synonym_stop_words_config_stat");
					this.redisTemplate.opsForValue().set(cacheKey, this.gson.toJson(entry.getValue()));
					return cacheKey;
				}).collect(Collectors.toList());
				List<String> deadCacheKeys = this.redisTemplate.keys("*:synonym_stop_words_config_stat").stream()
					.filter(cacheKey -> !liveCacheKeys.contains(cacheKey))
					.collect(Collectors.toList());
				this.redisTemplate.delete(deadCacheKeys);
			}
		} catch (CartUpServiceException e) {
			log.info("Exception occurred while caching stop words/synonym", e);
		}
	}

	private Optional<SolrDocumentList> processStopWordsAndSynonymGetRequest() throws CartUpServiceException {
		try {
			SolrDocumentList documentList = searchConfRepoClient.findAll(Arrays.asList(RepoConstants.SEARCH_STOP_WORDS_DOC_TYPE, RepoConstants.SEARCH_ONE_WAY_SYNONYM_DOC_TYPE, RepoConstants.SEARCH_SYNONYM_DOC_TYPE));
			return Optional.ofNullable(documentList);
		} catch (Exception e){
			log.error("Failed to get search conf document", e);
		}
		return Optional.empty();
	}

}
