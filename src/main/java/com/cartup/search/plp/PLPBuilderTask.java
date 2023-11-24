package com.cartup.search.plp;

import static com.cartup.commons.repo.RepoConstants.EQUAL_QUERY_FILTER_TEMPLATE;
import static com.cartup.commons.repo.RepoConstants.ORG_ID_S;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.commons.repo.model.search.CartUpSearchConfDocument;
import com.cartup.commons.repo.model.search.CategoryFacet;
import com.cartup.commons.repo.model.search.Facet;
import com.cartup.commons.repo.model.search.QueryEntity;
import com.cartup.commons.repo.model.search.SortEntity;
import com.cartup.commons.util.EmptyUtil;
import com.cartup.commons.util.ValueUtil;
import com.cartup.search.modal.FacetFilter;
import com.cartup.search.modal.SearchRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PLPBuilderTask {
    private static Logger logger = LoggerFactory.getLogger(PLPBuilderTask.class);

    private static String AND = "&";
    private static final String SORT_KEY = "sort";
    private static final String FILTER_KEY = "filter";

    private String orgId;
    private String userId;
    private String orgName;
    private CartUpSearchConfDocument searchConf;
    private Map<String, CategoryFacet> facetBuildMap;
    private Map<String, String> params;
    private SearchRequest searchRequest;
    private String searchSpellCheckApiUrl;
    @SuppressWarnings("unused")
	private String categoryListApiUrl;
    private String category;
    private RASHttpRepoClient dasClient;

    // Filtered search queries is
    private List<String> filteredSearchQueries = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private StringBuffer solrQuery = new StringBuffer("defType=edismax&facet=on&facet.mincount=1");
    private Map<String, Facet> facetMap = new HashMap<>();
    public Map<String, Facet> getFacetMap(){
        return facetMap;
    }


    public PLPBuilderTask(String orgId, String orgName, String userId, Map<String, String> params, 
    		String category, CartUpSearchConfDocument searchConf, SearchRequest searchRequest){
        this.orgId = orgId;
        this.userId = userId;
        this.orgName = orgName;
        this.searchConf = searchConf;
        this.facetBuildMap = searchConf.getFacetMap();
        this.category = category;
        this.params = params;
        this.searchRequest = searchRequest;
        this.searchSpellCheckApiUrl = RepoFactory.getSearchSpellCheckApiUrl();
        this.categoryListApiUrl = RepoFactory.getCategoryListApiUrl();
        this.dasClient = new RASHttpRepoClient();
    }
    
    public PLPBuilderTask boostUserSignals() throws IOException, CartUpServiceException {
    	JSONObject json = new JSONObject();
    	json.put("org_s", orgName);
    	json.put("user_id", userId);
    	json.put("cat_id", category);
    	json.put("stats", "view_stats");
    	json.put("feature_stats", "features");
    	json.put("recent_views", true);

    	HashMap<String, HashMap<String, Double>> features_scores = new HashMap<String, HashMap<String, Double>>();
    	
    	try {
        	RASHttpRepoClient http = new RASHttpRepoClient();
        	JSONObject resp = http.getUserProfile(json.toString()); 
        	
        	if(resp.has("user_stats")) {
        		JSONObject userStats = (JSONObject) resp.get("user_stats");
        		JSONObject featuresViewStats = (JSONObject) ((JSONObject) resp.get("user_stats")).get("features_view_stats");
        		JSONArray features = (JSONArray) ((JSONObject) resp.get("user_stats")).get("features");
        		for(int i=0; i< features.length(); i++) {
        			if (featuresViewStats.isNull((String) features.get(i)))
        				continue;
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
        	}
    		//Boost User Signals
		} catch (Exception e) {
			log.error("Exception while boosting user signal", e);
		}
        return this;
    }

    public PLPBuilderTask getCategoryInfo(){
        //TODO pending api
        //categoryInfo = new CategoryInfo().setName("Tops");
        return this;
    }

    public String build() throws IOException, CartUpServiceException{     
        addOrgId(); //Done
        addCategory(); //Done
        addFilteredQueries(); //Done
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
        solrQuery.append(AND).append("fq=").append(String.format(EQUAL_QUERY_FILTER_TEMPLATE, ORG_ID_S, orgId));
    }

    public void addFilteredQueries() {
    	KeywordSuggestorHttpRepoClient nlp_obj = new KeywordSuggestorHttpRepoClient();
    	JSONObject json = new JSONObject();
    	json.put("org_s", orgName);
    	json.put("orgid", orgId);
    	json.put("user_id", userId);
    	json.put("keyword", category);
    	json.put("lang", "en");
 
    	try {
    		JSONObject nlp_payload = nlp_obj.getLangFeatures(json.toString());
    		if(!nlp_payload.get("status").equals("FAIL")) {
    			solrQuery.append(AND).append("q=").append(nlp_obj.getLemmaKeyword(nlp_payload));	
    		} else {
    			solrQuery.append(AND).append("q=").append(category);
    		}
		} catch (Exception e) {
			e.printStackTrace();
			solrQuery.append(AND).append("q=").append(category);
		}
        
    }

    public void addCategory() throws UnsupportedEncodingException {
    	solrQuery.append(AND).append("qf=").append("name_t^2 collection_txt^10 description_t^.5 keywords_ss^5 brand_s^5 sku_s^10 color_s^5 categories_ss^20 category_s^50");
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
    }

}
