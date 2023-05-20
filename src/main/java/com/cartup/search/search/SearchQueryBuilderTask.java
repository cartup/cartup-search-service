package com.cartup.search.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoConstants;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.commons.repo.model.search.CartUpSearchConfDocument;
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
    private List<String> filteredSearchQueries = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private StringBuffer solrQuery = new StringBuffer("defType=edismax&facet=on&facet.mincount=1&mm=2<75%25");
    private Map<String, Facet> facetMap = new HashMap<>();
    public Map<String, Facet> getFacetMap(){
        return facetMap;
    }

    public SearchQueryBuilderTask(String orgId, Map<String, String> params, 
    		String inputSearch, CartUpSearchConfDocument searchConf, SearchRequest searchRequest){
        this.orgId = orgId;
        this.searchConf = searchConf;
        this.facetBuildMap = searchConf.getFacetMap();
        this.inputSearch = inputSearch;
        this.params = params;
        this.searchRequest = searchRequest;
        this.searchSpellCheckApiUrl = RepoFactory.getSearchSpellCheckApiUrl();
        this.categoryListApiUrl = RepoFactory.getCategoryListApiUrl();
    }

    
    public SearchQueryBuilderTask makeApiCall() throws IOException, CartUpServiceException {
    	
    	try {
	    	KeywordSuggestorResponse keywordInfo = new KeywordSuggestorResponse();
	        if (EmptyUtil.isNotNull(searchConf) && searchConf.isSpellcheck()){
	        	if (!params.containsKey("keyword_suggest"))
	        		keywordInfo = getKeywordSuggest(inputSearch);
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
	        	
	        	filteredSearchQueries = new ArrayList<String>(new LinkedHashSet<String>(filteredSearchQueries));
	        	if(keywordInfo.getAnnotations() != null) {
	        		if (EmptyUtil.isNotEmpty(keywordInfo.getAnnotations().getCategories().getCat_suggestions())) {
		        		categories.addAll(keywordInfo.getAnnotations().getCategories().getCat_suggestions());    		
		        	}
	        	}
	        	
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
    	int mincount= (countWords < 3) ? 1: 2;
    	solrQuery.append(AND).append("mm=" + String.valueOf(mincount) + "<75%25");	
    }
    
    
    public String build() throws IOException, CartUpServiceException{
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
                        filBuff.append(fValue);
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
        		return new StringBuffer(ff.getRepoFieldId()).append(":").append("\"" + ff.getValue() + "\"").toString();
        	else
        		return new StringBuffer(ff.getRepoFieldId()).append(":").append(ff.getValue()).toString();
        }
        return "";
    }

    public void addFilteredQueries() {
        solrQuery.append(AND).append("q=").append(String.join(" ", filteredSearchQueries));
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
}
