package com.cartup.search.controller;

import java.util.Map;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.cartup.commons.exceptions.CartUpServiceException;
import com.cartup.commons.repo.RepoFactory;
import com.cartup.search.modal.SearchRequest;
import com.cartup.search.modal.SearchResult;
import com.cartup.search.plp.PLPService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@EnableAutoConfiguration
public class PlpController {
	
	private Gson gson;

    private PLPService service;
	
	public PlpController() {
        try{
            gson = new Gson();
            RepoFactory.loadConfiguration();
            this.service = new PLPService();
        } catch (Exception e){
            log.error("Failed to initialize widget controller", e);
        }
    }
	
	@CrossOrigin
    @RequestMapping(value = "/v1/widgetserver/plp", method = RequestMethod.GET, produces = "application/json")
    protected ResponseEntity<String> getSearchResult(@RequestParam Map<String, String> reqParams) {
        try {
            log.info(String.format("Get PLP request : %s", gson.toJson(reqParams)));
            SearchRequest plpRequest = gson.fromJson(reqParams.get("request"), SearchRequest.class);
            SearchResult res  = service.process(reqParams, plpRequest);
            return ResponseEntity.ok(gson.toJson(res));
        } catch (CartUpServiceException cse) {
            log.error("Error while validating plp request", cse);
            return new ResponseEntity<>(cse.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error while processing plp request", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	@CrossOrigin
    @RequestMapping(value = "/v1/widgetserver/plp", method = RequestMethod.POST, produces = "application/json")
    protected ResponseEntity<String> getPlpRequestWithReqBody(@RequestBody Map<String, String> reqParams) {
        try {
            log.info(String.format("Get PLP request : %s", gson.toJson(reqParams)));
            SearchRequest plpRequest = gson.fromJson(reqParams.toString(), SearchRequest.class);
            SearchResult res  = service.process(reqParams, plpRequest);
            return ResponseEntity.ok(gson.toJson(res));
        } catch (CartUpServiceException cse) {
            log.error("Error while validating plp request", cse);
            return new ResponseEntity<>(cse.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error while processing plp request", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
