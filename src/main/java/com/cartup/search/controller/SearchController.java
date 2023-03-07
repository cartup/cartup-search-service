package com.cartup.search.controller;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.modal.SearchResult;
import com.cartup.search.service.CacheService;
import com.cartup.search.service.SearchService;
import com.google.gson.Gson;

@Controller
@EnableAutoConfiguration
public class SearchController {
    private static Logger logger = LoggerFactory.getLogger(SearchController.class);

    private Gson gson;

    private SearchService service;

    public SearchController(CacheService cacheService) {
        try{
            gson = new Gson();
            RepoFactory.loadConfiguration();
            this.service = new SearchService(cacheService);
        } catch (Exception e){
            logger.error("Failed to initialize widget controller", e);
        }
    }

    @RequestMapping(value = "/v1/widgetserver/search/result", method = RequestMethod.GET, produces = "application/json")
    protected ResponseEntity<String> getSearchResult(@RequestParam Map<String, String> reqParams) {
        try {
            logger.info(String.format("Get review widget request : %s", gson.toJson(reqParams)));
            SearchRequest searchRequest = gson.fromJson(reqParams.get("request"), SearchRequest.class);
            SearchResult res  = service.processSearch(reqParams, searchRequest);
            return ResponseEntity.ok(gson.toJson(res));
        } catch (CartUpServiceException cse) {
            logger.error("Error while validating search request", cse);
            return new ResponseEntity<>(cse.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error while processing search request", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
