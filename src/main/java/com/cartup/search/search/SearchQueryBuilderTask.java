package com.cartup.search.search;

import static com.cartup.commons.repo.RepoConstants.GREATER_THAN_OPERATOR;
import static com.cartup.commons.repo.RepoConstants.LESS_THAN_OPERATOR;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoConstants;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.commons.repo.model.search.CartUpOneWaySynonymDocument;
import com.cartup.commons.repo.model.search.CartUpSearchConfDocument;
import com.cartup.commons.repo.model.search.CartUpStopWordsDocument;
import com.cartup.commons.repo.model.search.CartUpSynonymDocument;
import com.cartup.commons.repo.model.search.CategoryFacet;
import com.cartup.commons.repo.model.search.Facet;
import com.cartup.commons.repo.model.search.FieldOrder;
import com.cartup.commons.repo.model.search.QueryEntity;
import com.cartup.commons.repo.model.search.SortEntity;
import com.cartup.commons.util.EmptyUtil;
import com.cartup.commons.util.ValueUtil;
import com.cartup.search.modal.FacetFilter;
import com.cartup.search.modal.KeyWordSuggestorRequest;
import com.cartup.search.modal.KeywordSuggestorResponse;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.plp.RASHttpRepoClient;
import com.google.gson.Gson;

public class SearchQueryBuilderTask {
    private static Logger logger = LoggerFactory.getLogger(SearchQueryBuilderTask.class);

    private static String AND = "&";
    private static final String SORT_KEY = "sort";
    private static final String FILTER_KEY = "filter";

    private String orgId;
    private CartUpSearchConfDocument searchConf;
    private Map<String, CategoryFacet> facetBuildMap;
    private Map<String, String> params;
    private SearchRequest searchRequest;
    private String searchSpellCheckApiUrl;
    @SuppressWarnings("unused")
	private String categoryListApiUrl;
    private String inputSearch;

    // Filtered search queries is
    private Set<String> filteredSearchQueries = new HashSet<>();
    private List<String> categories = new ArrayList<>();
    private StringBuffer solrQuery = new StringBuffer("defType=edismax&facet=on&facet.mincount=1");
    private Map<String, Facet> facetMap = new HashMap<>();
    public Map<String, Facet> getFacetMap(){
        return facetMap;
    }
    
    private RedisTemplate<String, String> redisTemplate;
    
    private Gson gson;
    
    private Set<String> synonymSet;

    public SearchQueryBuilderTask(String orgId, Map<String, String> params, 
    		String inputSearch, CartUpSearchConfDocument searchConf, SearchRequest searchRequest, RedisTemplate<String, String> redisTemplate){
        this.orgId = orgId;
        this.searchConf = searchConf;
        this.facetBuildMap = searchConf.getFacetMap();
        this.inputSearch = inputSearch;
        this.params = params;
        this.searchRequest = searchRequest;
        this.searchSpellCheckApiUrl = RepoFactory.getSearchSpellCheckApiUrl();
        this.categoryListApiUrl = RepoFactory.getCategoryListApiUrl();
        this.gson = new Gson();
        this.redisTemplate = redisTemplate;
    }

    
    public SearchQueryBuilderTask makeApiCall() throws IOException, CartUpServiceException {
    	this.inputSearch = searchRequest.getSearchQuery();
    	try {
	    	KeywordSuggestorResponse keywordInfo = new KeywordSuggestorResponse();
	        if (EmptyUtil.isNotNull(searchConf) && searchConf.isSpellcheck() && searchRequest.getKeywordSuggestor() != null){
	        	if (!params.containsKey("keyword_suggest"))
	        		keywordInfo = searchRequest.getKeywordSuggestor();
	        	else {
	        		Gson gson = new Gson();
	        		keywordInfo = gson.fromJson(params.get("keyword_suggest"), KeywordSuggestorResponse.class);
	        	}
	        	if(keywordInfo.getAnnotations() != null) {
	        		if (EmptyUtil.isNotEmpty(keywordInfo.getAnnotations().getSpellcheck().getCompound_suggestions())) {
		        		filteredSearchQueries.addAll(keywordInfo.getAnnotations().getSpellcheck().getCompound_suggestions());    		
		        	}
		        	
		        	if (EmptyUtil.isNotEmpty(keywordInfo.getAnnotations().getSpellcheck().getProduct_suggestions())) {
		        		filteredSearchQueries.addAll(keywordInfo.getAnnotations().getSpellcheck().getProduct_suggestions());    		
		        	}
	        	}
	        	
	        	if (filteredSearchQueries.size() == 0) 
	        		filteredSearchQueries.add(inputSearch);
	        	
	        	filteredSearchQueries = new LinkedHashSet<String>(filteredSearchQueries);
//				TODO Commenting this code till we fix the keyword suggester 
//	        	if(keywordInfo.getAnnotations() != null) {
//	        		if (EmptyUtil.isNotEmpty(keywordInfo.getAnnotations().getCategories().getCat_suggestions())) {
//		        		categories.addAll(keywordInfo.getAnnotations().getCategories().getCat_suggestions());    		
//		        	}
//	        	}
	        	
	        	categories = new ArrayList<String>(new LinkedHashSet<String>(categories));
	        	
            } else {
            	filteredSearchQueries.add(inputSearch);
            }
    	}catch (Exception e){
                logger.error("Failed to process spell check for input search {} for org id {}. {}", inputSearch, orgId, e);
                filteredSearchQueries.add(inputSearch);
        }
        return this;
    }

    public SearchQueryBuilderTask getCategoryInfo(){
        //TODO pending api
        //categoryInfo = new CategoryInfo().setName("Tops");
        return this;
    }

    public void setmm() {
    	String querywords = String.join(" ", filteredSearchQueries);
    	int countWords = querywords.split("\\s").length;
    	int mincount= (countWords < 4) ? 1: 2;
    	logger.info(String.format("Query String : %s %d", querywords, mincount));
    	solrQuery.append(AND).append("mm=" + String.valueOf(mincount) + "<50%25");	
    }
    
    
    public String build() throws IOException, CartUpServiceException{
    	
        processSynonymAndStopWordAlgorithm();
        if(synonymSet.size() > 0) {
    		int searchWord = searchRequest.getSearchQuery().split(" ").length;
        	double possibleWordCount = Math.ceil((searchWord*75)/100.0);
        	String mmValue = "mm=1";
        	if(synonymSet.size() > possibleWordCount) {
        		double synonymPercentage = (possibleWordCount/Double.valueOf(synonymSet.size())) * 100;
        		mmValue = String.format("mm=2<-%s", Math.round(synonymPercentage));
        	}
    		solrQuery = new StringBuffer(solrQuery.toString().replace("mm=2<75%25", mmValue));
    	}
        makeApiCall();
        addOrgId();
        addCategories();
        addFilteredQueries();
        addSearchableFields();
        setmm();
        addFacets();
        addPagination();
        addSortEntities();
        addFilters();
        boostUserSignals();
        solrQuery.append(AND).append("fq=visibility_b:").append(true);
        System.out.println(solrQuery.toString());
        return solrQuery.toString();
    }

    public void addOrgId() {
        solrQuery.append(AND).append("fq=").append(String.format(RepoConstants.EQUAL_QUERY_FILTER_TEMPLATE, RepoConstants.ORG_ID_S, orgId));
    }

    public void addPagination() {
        if (EmptyUtil.isNotNull(ValueUtil.get(() -> searchRequest.getPagination()))){
            if (EmptyUtil.isNotNull(searchRequest.getPagination().getRow())){
                solrQuery.append(AND).append("rows=").append(searchRequest.getPagination().getRow());
            }

            if (EmptyUtil.isNotNull(searchRequest.getPagination().getStart())){
                solrQuery.append(AND).append("start=").append(searchRequest.getPagination().getStart());
            }
        } else {
            if (EmptyUtil.isNotNull(searchConf) && EmptyUtil.isNotNull(searchConf.getPaginationCount())){
                solrQuery.append(AND).append("rows=").append(searchConf.getPaginationCount());
            }

            if (EmptyUtil.isNotEmpty(params) && EmptyUtil.isNotEmpty(params.get("start"))){
                solrQuery.append(AND).append("start=").append(params.get("start"));
            }
        }
    }

    public void addSortEntities() {
        if (EmptyUtil.isNotNull(ValueUtil.get(() -> searchRequest.getSortEntities())) &&
                EmptyUtil.isNotEmpty(searchRequest.getSortEntities())){
            for (SortEntity se : searchRequest.getSortEntities()){
                if (EmptyUtil.isNotNull(se) && EmptyUtil.isNotEmpty(se.getValue())){
                    solrQuery.append(AND).append("sort=").append(se.getValue());
                }
            }
        } else {
            if (params.containsKey(SORT_KEY) && EmptyUtil.isNotEmpty(params.get(SORT_KEY))){
                solrQuery.append(AND).append("sort=").append(params.get(SORT_KEY));
            }
        }
    }
    
    public SearchQueryBuilderTask boostUserSignals() throws IOException, CartUpServiceException {
    	List<String> categories = searchRequest.getKeywordSuggestor().getAnnotations().getCategories().getCat_suggestions();
    	if(!categories.isEmpty()) {
    		JSONObject json = new JSONObject();
        	json.put("org_s", searchRequest.getOrgName());
        	json.put("user_id", searchRequest.getUserId());
        	json.put("cat_id", categories.get(0));
        	json.put("stats", "view_stats");
        	json.put("feature_stats", "features");
        	json.put("recent_views", true);

        	HashMap<String, HashMap<String, Double>> features_scores = new HashMap<String, HashMap<String, Double>>();
        	
        	try {
            	RASHttpRepoClient http = new RASHttpRepoClient();
            	JSONObject resp = http.getUserProfile(json.toString()); 
            	JSONObject userStats = (JSONObject) resp.get("user_stats");
        		JSONObject featuresViewStats = (JSONObject) userStats.get("features_view_stats");
        		JSONArray features = (JSONArray) userStats.get("features");
        		for(int i=0; i< features.length(); i++) {
        			if(!featuresViewStats.get((String) features.get(i)).equals(JSONObject.NULL)) {
        				JSONObject fobj = (JSONObject) featuresViewStats.get((String) features.get(i));
            			HashMap<String, Double> feature_value_score = new HashMap<String, Double>();
            			@SuppressWarnings("unchecked")
        				Iterator<String> keys = fobj.keys();
            			while(keys.hasNext()) {
            			    String key = keys.next();
            			    feature_value_score.put(key, (Double) fobj.get(key));
            			}
            			features_scores.put((String) features.get(i),  feature_value_score);
        			}
        		} 		
        		
        		StringBuffer querybuffer = new StringBuffer();
        		//Build boosting Query
        		for (Map.Entry<String, HashMap<String, Double>> entry : features_scores.entrySet()) {
        			for (Map.Entry<String, Double> subEntry : entry.getValue().entrySet()) {
        				
        				querybuffer.append("bq=" + entry.getKey() + ":(" +   "\"" +  URLEncoder.encode(subEntry.getKey().replace("%", "").replace("/", ""),
        						StandardCharsets.UTF_8.toString())  + "\")^" + subEntry.getValue());
        			}
        			querybuffer.append("&");
        		}
        		if (userStats.has("range_featuresview_stats")) {
        			JSONObject rangeViewStats = (JSONObject) userStats.get("range_featuresview_stats");
        			JSONArray range_features = (JSONArray) userStats.get("range_features");
        			for(int i=0; i< range_features.length(); i++) {
        				if(!rangeViewStats.get((String) range_features.get(i)).equals(JSONObject.NULL)) {
	        				JSONArray fobj = (JSONArray) rangeViewStats.get((String) range_features.get(i));
	        				querybuffer.append("bq=" + (String) range_features.get(i) + ":["  + 
	        						Double.parseDouble(fobj.getString(2)) + " TO " + Double.parseDouble(fobj.getString(3)) + "]^10");
	        				querybuffer.append("&");
        				}
        			}
        	     }
        		if(StringUtils.isNotBlank(querybuffer)) {
        			querybuffer.deleteCharAt(querybuffer.length()-1);
        		}
        		solrQuery.append(AND).append(querybuffer.toString());
        		//Boost User Signals
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
        return this;
    }

    //This will add a facet filter is there is any selected by user, value in facet should be put in right order by ui
    public void addFilters() {
        //first create map of filterType and List<FacetFilter> and then iterate over map and for each key, values should be OR operation
        //and for different keys, it should be AND operation
        Map<String, List<FacetFilter>> filterCache = new HashMap<>();
        List<CategoryFacet> facets = getCategoryFacet();
        if (EmptyUtil.isNotNull(ValueUtil.get(() -> searchRequest.getFilters())) &&
                EmptyUtil.isNotEmpty(searchRequest.getFilters())){
            for(FacetFilter ff : searchRequest.getFilters()){
                if (filterCache.containsKey(ff.getRepoFieldId())){
                    List<FacetFilter> oldC = filterCache.get(ff.getRepoFieldId());
                    oldC.add(ff);
                    filterCache.put(ff.getRepoFieldId(), oldC);
                } else {
                    List<FacetFilter> newff = new ArrayList<>();
                    newff.add(ff);
                    filterCache.put(ff.getRepoFieldId(), newff);
                }
            }

            //create query from map
            for (Map.Entry<String, List<FacetFilter>> entry : filterCache.entrySet()){
                List<FacetFilter> filters = entry.getValue();
                
                String tag = "";
                for (CategoryFacet facet : facets)
                    if (EmptyUtil.isNotNull(facet) && EmptyUtil.isNotEmpty(facet.getFacets()))
                        for (Facet f : facet.getFacets()){
                            if (!EmptyUtil.isNotEmpty(f.getValue()) && f.getRepoFieldName().equals(entry.getKey())){
                            	tag = "{!tag=" + "\"" +  entry.getKey() + "_tag\"}";
                            }
                        }
                    
                
                
                //for single repo field names, conside all filters as OR operation
                StringBuffer filBuff = new StringBuffer();
                filBuff.append("(");
                for (FacetFilter ff : filters){
                    String fValue = getFilterValue(ff);
                    if (EmptyUtil.isNotEmpty(fValue)){
                        if (filBuff.length() > 1){
                            filBuff.append(" ").append("OR").append(" ");
                        }
                        try {
							filBuff.append(URLEncoder.encode(fValue, StandardCharsets.UTF_8.toString()));
						} catch (UnsupportedEncodingException e) {
							logger.error("Exception occurred while encoding the given filter query value", e);
						}
                    }
                }
                filBuff.append(")");

                if (filBuff.length() > 3){
                    //for different repo field names, conside all filters as AND operation
                    solrQuery.append(AND).append("fq=").append(tag + filBuff.toString());
                }
            }
        } else {
            if (params.containsKey(FILTER_KEY)){
                solrQuery.append(AND).append("fq=").append(params.get(FILTER_KEY));
            }
        }
    }

    private String getFilterValue(FacetFilter ff){
        if (EmptyUtil.isNotEmpty(ff.getRepoFieldId()) && EmptyUtil.isNotEmpty(ff.getValue())){
        	if (ff.getRepoFieldId().endsWith("_s"))
        		return new StringBuffer(ff.getRepoFieldId()).append(":").append("\"" + ff.getValue().replaceAll("\"", "\\\\\"") + "\"").toString();
        	else
        		return new StringBuffer(ff.getRepoFieldId()).append(":").append(ff.getValue()).toString();
        }
        return "";
    }

    public void addFilteredQueries() {
    	String finalFilterQuery = new LinkedHashSet<>(Arrays.asList(filteredSearchQueries.stream().map(String::toLowerCase).collect(Collectors.joining(" ")).split(" "))).stream().map(String::toLowerCase).collect(Collectors.joining(" "));
    	if(searchRequest.getKeywordSuggestor() != null) {
    		String lemmaKeyword = this.getLemmaKeyword(new JSONObject(searchRequest.getKeywordSuggestorString()));
    		if(finalFilterQuery.length() > 0) {
    			if(lemmaKeyword != null) {
    				finalFilterQuery = String.format("%s OR %s", finalFilterQuery, lemmaKeyword);
    			}
        	} else {
        		if(lemmaKeyword != null) {
    				finalFilterQuery = String.format("%s", finalFilterQuery, lemmaKeyword);
    			}
        	}
    	}
    	solrQuery.append(AND).append("q=").append(String.join(" ", finalFilterQuery));
    	
    }
    
    public String getLemmaKeyword(JSONObject resp) {
    	String keyword = null;
    	JSONObject annotations = (JSONObject) resp.get("annotations");
    	JSONObject lang_features = (JSONObject) annotations.get("lang_features");
    	if (lang_features.has("lemma_search_keyword")) {
    		keyword = lang_features.getString("lemma_search_keyword");
    	}
    	return keyword;
    }

    public void addCategories() throws UnsupportedEncodingException {
        if (EmptyUtil.isNotEmpty(categories)) {
            StringBuffer catSb = new StringBuffer("(");
            for (String cat : categories){
                catSb.append(String.format("\"%s\"", URLEncoder.encode(cat, "UTF-8"))).append("^2.0,");
            }
            catSb.append(")");
            solrQuery.append(AND).append("bq=").append(String.format(RepoConstants.EQUAL_QUERY_FILTER_TEMPLATE, RepoConstants.CAT_NAME_SS, catSb.toString()));
        }
    }

    public void addSearchableFields() {
        // qf=namel_t description_t
        solrQuery.append(AND).append("qf=");
        double boostValue = 10.0;
        double boostFactor = 2.0;
        
        if (RepoFactory.getConfigProperty("search.field.boosting.value") != null) {
        	boostValue = Double.parseDouble(RepoFactory.getConfigProperty("search.field.boosting.value"));
        }

        if (RepoFactory.getConfigProperty("search.field.boosting.factor") != null) {
        	boostFactor = Double.parseDouble(RepoFactory.getConfigProperty("search.field.boosting.factor"));
        }
        
        if (EmptyUtil.isNotNull(searchConf) && EmptyUtil.isNotEmpty(searchConf.getSearchableFields())){
            for(FieldOrder fo : searchConf.getSearchableFields()){
                solrQuery.append(fo.getRepoFieldName());
                /*if (EmptyUtil.isNotEmpty(fo.getBoost())){
                    solrQuery.append("^").append(fo.getBoost());
                }*/
                solrQuery.append("^").append(boostValue);
                boostValue = boostValue/boostFactor;
                solrQuery.append(" ");
            }
        }
    }

    public void addFacets() {
        List<CategoryFacet> facets = getCategoryFacet();
        for (CategoryFacet facet : facets){
            if (EmptyUtil.isNotNull(facet) && EmptyUtil.isNotEmpty(facet.getFacets())){
                for (Facet f : facet.getFacets()){
                    if (EmptyUtil.isNotEmpty(f.getValue())){
                        for (QueryEntity qe : f.getValue()){
                            String facetKey = String.format("%s:%s", f.getRepoFieldName(), qe.getValue());
                        	if (f.getOperator().equals(GREATER_THAN_OPERATOR)) {
                        		facetKey = String.format("%s:[%s TO %s]",f.getRepoFieldName(), qe.getValue(), "*");
                    		} else if (f.getOperator().equals(LESS_THAN_OPERATOR)) {
                    			facetKey = String.format("%s:[%s TO %s]",f.getRepoFieldName(), "*", qe.getValue());
                    		}
                            facetMap.put(facetKey, new Facet(f.getType(), f.getDisplayType(), 
                            		f.getRepoFieldName(), f.getDisplayName(), f.getOperator(), qe));
                            solrQuery.append(AND).append("facet.query=").append(facetKey);
                        }
                    } else {
                        facetMap.put(f.getRepoFieldName(), new Facet(f.getType(), f.getDisplayType(), 
                        		f.getRepoFieldName(), f.getDisplayName(), f.getOperator()));
                        solrQuery.append(AND).append("facet.field={!ex=" + "\"" +
                        		f.getRepoFieldName() + "_tag\"}").append(f.getRepoFieldName());
                    }
                }
            }
        }
    }

    public String formatFacetValue(String repoField, String value) {
        if (repoField.endsWith("ss") && !value.startsWith("(")){
            value = String.format("(\"%s\")", value);
        }
        return value;
    }

    public List<CategoryFacet> getCategoryFacet() {
        List<CategoryFacet> facets = new ArrayList<>();
        if (EmptyUtil.isNotNull(facetBuildMap) && facetBuildMap.containsKey("default")){
            facets.add(facetBuildMap.get("default"));
        }
        for (String cat : categories) {
        	if (EmptyUtil.isNotNull(facetBuildMap) && facetBuildMap.containsKey(cat)){
                facets.add(facetBuildMap.get(cat));
                break;
            }
        	
        }
        return facets;
        /*if (EmptyUtil.isNotNull(facetBuildMap) && EmptyUtil.isNotNull(categoryInfo)){
            while (categoryInfo != null){
                if (facetBuildMap.containsKey(categoryInfo.getName()) && categoryInfo.getName() != "default")  {
                    facets.add(facetBuildMap.get(categoryInfo.getName()));
                }
                categoryInfo = categoryInfo.getParent();
            }
        }*/      
    }

    public KeywordSuggestorResponse getKeywordSuggest(String inputSearch) throws IOException, CartUpServiceException {
        if (EmptyUtil.isNotEmpty(searchSpellCheckApiUrl)){
            try {
                KeyWordSuggestorRequest request = new KeyWordSuggestorRequest();
                request.setKeyword(inputSearch);
                request.setLang("en");
                request.setCookie("cookie");
                request.setDevice("iphone");
                request.setOrgid(orgId);
                request.setAutoSuggest(true);
                request.setFuzzySuggest(false);
                request.setSpellWordSegementation(false);
                request.setType("key-suggestion/search/filter");
                request.setLanguageSpellCheck(false);
                request.setAlgo("vsm");
                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<KeywordSuggestorResponse> response
                        = restTemplate.postForEntity(searchSpellCheckApiUrl, request, KeywordSuggestorResponse.class);
                System.out.println("Get keyword Suugest " + response.getBody());
                
                return response.getBody();
            } catch (Exception e){
                logger.error("Failed to process spell check for input search {} for org id {}. {}", inputSearch, orgId, e);
                throw new CartUpServiceException(e.getMessage());
            }
        }
        return null;
    }
    
    private void processSynonymAndStopWordAlgorithm() {
    	// Using this variable to change the mm value if synonyms are used
    	this.synonymSet = new HashSet<>();
		String configKey = String.format("%s:%s", searchRequest.getOrgId(), "synonym_stop_words_config_stat");

		if(this.redisTemplate.hasKey(configKey)) {
			String value = this.redisTemplate.opsForValue().get(configKey);
			CartUpStopWordsDocument stopWordsDoc = this.gson.fromJson(new JSONObject(value).get("stopwords").toString(), CartUpStopWordsDocument.class);
			CartUpSynonymDocument synonymDoc = this.gson.fromJson(new JSONObject(value).get("synonym").toString(), CartUpSynonymDocument.class);
			CartUpOneWaySynonymDocument oneWaySynonymDoc = this.gson.fromJson(new JSONObject(value).get("onewaysynonym").toString(), CartUpOneWaySynonymDocument.class);

			AtomicReference<Set<String>> searchQueryWords = new AtomicReference<>(new LinkedHashSet<>(Arrays.asList(searchRequest.getSearchQuery().split(" "))));
			// removing all the stop words configured for that orgId
			stopWordsDoc.getStopWordsData().getStopWords().stream()
			.forEach(stopWord -> {
				searchQueryWords.set(searchQueryWords.get().stream().filter(word -> !word.equals(stopWord)).collect(Collectors.toSet()));
			});
			searchRequest.setSearchQuery(searchQueryWords.get().stream().collect(Collectors.joining(" ")));

			// Two way synonym
			Map<String, Set<String>> synonymPermutatedMap = new HashMap<>();
			synonymDoc.getSynonymData().getSynonyms().stream()
			.forEach(synonym -> {
				for (String s : synonym) {
					Set<String> values = new HashSet<>(synonym);
					values.remove(s);
					if(synonymPermutatedMap.containsKey(s)) {
						Set<String> existingValue = new HashSet<>();
						existingValue.addAll(synonymPermutatedMap.get(s));
						existingValue.addAll(values);
						synonymPermutatedMap.put(s, existingValue);
					} else {
						synonymPermutatedMap.put(s, values);
					}
				}
			});
			String resultQuery = Arrays.asList(searchRequest.getSearchQuery().split(" ")).stream()
					.filter(synonym -> synonymPermutatedMap.containsKey(synonym))
					.map(synonym -> {
						synonymSet.addAll(synonymPermutatedMap.get(synonym));
						return synonym;
					}).flatMap(synonym -> synonymPermutatedMap.get(synonym).stream()).collect(Collectors.joining(" "));
			if(StringUtils.isNotBlank(resultQuery)) {
				searchRequest.setSearchQuery(searchRequest.getSearchQuery().concat(" ").concat(resultQuery));
			}

			// Checking for matching synonym, if true, then appending all the synonym to the search query 
			oneWaySynonymDoc.getOneWaySynonymData().getOneWaySynonyms().entrySet().stream()
			.forEach(entrySet -> {
				if(searchRequest.getSearchQuery().contains(entrySet.getKey())) {
					String synonymns = entrySet.getValue().stream()
							.filter(synonym -> !searchRequest.getSearchQuery().contains(synonym))
							.map(synonym -> {
								synonymSet.add(synonym);
								return synonym;
							}).collect(Collectors.joining(" "));
					searchRequest.setSearchQuery(searchRequest.getSearchQuery().concat(" ").concat(synonymns));
				}
			});
			filteredSearchQueries.add(searchRequest.getSearchQuery());
		}
	}
}
