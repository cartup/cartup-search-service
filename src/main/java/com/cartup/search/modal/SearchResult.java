package com.cartup.search.modal;


import java.util.ArrayList;
import java.util.List;

import com.cartup.commons.repo.model.FacetEntity;
import com.cartup.commons.repo.model.search.SearchSelector;
import com.cartup.commons.repo.model.search.SearchSortEntity;
import com.cartup.commons.repo.model.search.SearchTheme;
import com.cartup.commons.util.EmptyUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown=true)
public class SearchResult {
    private String widgetname;
    private String qtype;
    private String success;
    private Integer numberofdocs;
    private String currency;
    private boolean pagination;
    private Integer paginationCount;
    private SearchSortEntity sortEntity;
    private List<ProductInfo> docs = new ArrayList<>();
    private List<FacetEntity> facetcount;
    private SearchSelector searchSelector;
    private SearchTheme searchTheme;

    public SearchTheme getSearchTheme() {
        return searchTheme;
    }

    public SearchResult setSearchTheme(SearchTheme searchTheme) {
        this.searchTheme = searchTheme;
        return this;
    }

    public SearchSelector getSearchSelector() {
        return searchSelector;
    }

    public SearchResult setSearchSelector(SearchSelector searchSelector) {
        this.searchSelector = searchSelector;
        return this;
    }

    public Integer getPaginationCount() {
        return paginationCount;
    }

    public SearchResult setPaginationCount(Integer paginationCount) {
        this.paginationCount = paginationCount;
        return this;
    }
    
    public boolean getPagination() {
        return pagination;
    }

    public SearchResult setPagination(boolean pagination) {
        this.pagination = pagination;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public SearchSortEntity getSortEntity() {
        return sortEntity;
    }

    public SearchResult setSortEntity(SearchSortEntity sortEntity) {
        this.sortEntity = sortEntity;
        return this;
    }

    public SearchResult setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public List<FacetEntity> getFacetcount() {
        return facetcount;
    }

    public SearchResult setFacetcount(List<FacetEntity> facetcount) {
        this.facetcount = facetcount;
        return this;
    }

    public String getWidgetname() {
        return widgetname;
    }

    public SearchResult setWidgetname(String widgetname) {
        this.widgetname = widgetname;
        return this;
    }

    public String getQtype() {
        return qtype;
    }

    public SearchResult setQtype(String qtype) {
        this.qtype = qtype;
        return this;
    }

    public String getSuccess() {
        return success;
    }

    public SearchResult setSuccess(String success) {
        this.success = success;
        return this;
    }

    public Integer getNumberofdocs() {
        return numberofdocs;
    }

    public SearchResult setNumberofdocs(Integer numberofdocs) {
        this.numberofdocs = numberofdocs;
        return this;
    }

    public List<ProductInfo> getDocs() {
        return docs;
    }

    public SearchResult setDocs(List<ProductInfo> docs) {
        if (EmptyUtil.isNotEmpty(docs)){
            this.docs = docs;
            numberofdocs = docs.size();
        }
        return this;
    }
}
