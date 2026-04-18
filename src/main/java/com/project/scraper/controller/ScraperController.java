package com.project.scraper.controller;

import com.project.scraper.model.WebData;
import com.project.scraper.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

/*REST controller that exposes the scraping capability over HTTP*/
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")   //allow the static front-end to call the API on the same origin
public class ScraperController {

    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);

    private final ScraperService scraperService;

    public ScraperController(ScraperService scraperService){
        this.scraperService = scraperService;
    }

    @GetMapping("/scrape")
    public ResponseEntity<?> scrape(@RequestParam(name = "url") String url){

        try{
            WebData result = scraperService.scrape(url);
            return ResponseEntity.ok(result);

        } 
        catch(IllegalArgumentException e){
            log.warn("Bad request for URL '{}': {}", url, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("BAD_REQUEST", e.getMessage(), url));

        } 
        catch(SocketTimeoutException e){
            log.warn("Timeout scraping '{}': {}", url, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.REQUEST_TIMEOUT)
                    .body(errorBody("TIMEOUT",
                            "The target server did not respond within 5 seconds.", url));

        } 
        catch(IOException e){
            log.error("IO error scraping '{}': {}", url, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SCRAPE_FAILED", e.getMessage(), url));

        } 
        catch(Exception e){
            log.error("Unexpected error scraping '{}'", url, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("INTERNAL_ERROR",
                            "An unexpected error occurred. Please try again.", url));
        }
    }


    /*Builds a consistent JSON error body as an ordered map*/
    private Map<String, String> errorBody(String error, String message, String url) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error",   error);
        body.put("message", message != null ? message : "Unknown error");
        body.put("url",     url);
        return body;
    }
}


