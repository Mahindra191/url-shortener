package com.dev.project.url_shortener.controller;

import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Http;
import org.springframework.web.bind.annotation.*;
import com.dev.project.url_shortener.service.UrlService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import com.dev.project.url_shortener.dto.UrlRequest;
import com.dev.project.url_shortener.model.UrlModel;
import com.dev.project.url_shortener.repository.UrlRepository;

@RestController
@RequestMapping
// 🎯 MASTER CORS KEY: Authorizes both your local machine environment AND your live global Vercel frontend domain
@CrossOrigin(
    origins = { "http://localhost:5173", "https://url-926.vercel.app" }, 
    allowedHeaders = "*", 
    methods = { RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS }
)
public class UrlController {
    
    @Autowired
    private UrlService urlService;
    
    @Autowired
    private UrlRepository urlRepository;

    @PostMapping("/shorten")
    public ResponseEntity<?> createShortUrl(@RequestBody UrlRequest request) {
        try {
            String shortUrlResult = urlService.shortenUrl(request);
            return ResponseEntity.ok(shortUrlResult);
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if (msg.contains("||")) {
                String[] parts = msg.split("\\|\\|");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", parts[0],
                    "fallbackUrl", parts[1]
                ));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
    }

    @GetMapping("/long/{shortUrl}")
    public ResponseEntity<String> getLongUrl(@PathVariable String shortUrl, HttpServletRequest request){
        String clientIp = request.getRemoteAddr();
        if(!urlService.isAllowed(clientIp)){
            return ResponseEntity.status(429).body("Too many requests. Please try again later.");
        }
        String longUrl = urlService.getLongUrl(shortUrl);
        return ResponseEntity.ok(longUrl);
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirectUrl(@PathVariable String shortUrl) {
        if ("favicon.ico".equals(shortUrl) || "error".equals(shortUrl)) {
            return ResponseEntity.notFound().build();
        }
        String longUrl = urlService.getLongUrl(shortUrl);

        if (longUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}