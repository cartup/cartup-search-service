package com.cartup.search.modal;

import java.util.List;

import com.cartup.commons.repo.model.search.SortEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SearchRequest {
    private String orgId;
    private String userId;
    private String orgName;
    private String searchQuery;
    private Pagination pagination;
    private List<SortEntity> sortEntities;
    private List<FacetFilter> filters;
    private List<String> categories;
    @SerializedName("keyword_suggester")
    private String keywordSuggestor;
    
    private Gson gson = new Gson();

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }
    
    public String getUserId() {
        return userId;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public List<SortEntity> getSortEntities() {
        return sortEntities;
    }

    public void setSortEntities(List<SortEntity> sortEntities) {
        this.sortEntities = sortEntities;
    }

    public List<FacetFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<FacetFilter> filters) {
        this.filters = filters;
    }
    
    public KeywordSuggestorResponse getKeywordSuggestor() {
        return gson.fromJson(keywordSuggestor, KeywordSuggestorResponse.class);
    }
    
    public String getKeywordSuggestorString() {
        return keywordSuggestor;
    }
    
    public void setKeywordSuggestorString(String keywordSuggesstor) {
        this.keywordSuggestor = keywordSuggesstor;
    }

    public void setKeywordSuggestor(KeywordSuggestorResponse keywordSuggestor) {
        this.keywordSuggestor = gson.toJson(keywordSuggestor).toString();
    }

//    public static void main(String[] args){
//        SearchRequest sr = new SearchRequest();
//        Pagination pag = new Pagination();
//        pag.setStart(4);
//        pag.setRow(10);
//        sr.setOrgId("lef2f2f2jfjnf");
//        sr.setSearchQuery("hello pants");
//        sr.setPagination(pag);
//
//        List<SortEntity> sortEntities = new ArrayList<>();
//        SortEntity se = new SortEntity();
//        sortEntities.add(se);
//        se.setKey("Price low to high");
//        se.setType("double");
//        se.setValue("price_d desc");
//        sr.setSortEntities(sortEntities);
//
//        List<FacetFilter> filters = new ArrayList<>();
//        FacetFilter f = new FacetFilter();
//        f.setType("double");
//        f.setRepoFieldId("price_d");
//        f.setValue("[50 TO 60]");
//        filters.add(f);
//        sr.setFilters(filters);
//
//        System.out.println(new Gson().toJson(sr));
//
//    }
}
