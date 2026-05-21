package com.dev.project.url_shortener.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.dev.project.url_shortener.model.UrlModel;
import com.dev.project.url_shortener.repository.UrlRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;
import com.dev.project.url_shortener.dto.UrlRequest;
import com.dev.project.url_shortener.exception.UrlNotFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Set;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.MalformedURLException;

@Service
public class UrlService {
    @Autowired UrlRepository urlRepository;
    @Autowired MongoTemplate mongoTemplate;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired Base62Service base62Service;

    // 🔬 FIXED: Extraneous DTO Autowired field removed entirely.

    public String shortenUrl(UrlRequest urlRequest) {
        String longUrl = urlRequest.getLongUrl();
        String customAlias = urlRequest.getCustomAlias();

        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        // 1. Uniformly clean and normalize the target URL layout
        String sanitizedUrl = longUrl.replace("\\", "").replace("\"", "").trim();
        sanitizedUrl = sanitizedUrl.replaceAll("^(https?://)?(www\\.)?", "https://");
        if (sanitizedUrl.endsWith("/")) {
            sanitizedUrl = sanitizedUrl.substring(0, sanitizedUrl.length() - 1);
        }

        String shortUrl;

        // 2. Custom Alias Strategy Routing
        if (customAlias != null && !customAlias.isBlank()) {
            shortUrl = customAlias.trim();
            if (shortUrl.matches("^\\d+$")) {
                throw new IllegalArgumentException("Custom aliases cannot be purely numeric.");
            }

            Optional<UrlModel> conflictingUrl = urlRepository.findByShortUrl(shortUrl);
            if (conflictingUrl.isPresent()) {
                // Deduplication Fallback
                List<UrlModel> existing = urlRepository.findByLongUrlOrderByCreatedAtDesc(sanitizedUrl);
                if (!existing.isEmpty()) {
                    return existing.get(0).getShortUrl();
                }
                throw new IllegalArgumentException("Custom alias '" + shortUrl + "' is already taken.");
            }
        } else {
            // 3. Auto-Generation Strategy with Resilient Local Fallback Counter
            List<UrlModel> existingUrl = urlRepository.findByLongUrlOrderByCreatedAtDesc(sanitizedUrl);
            if (!existingUrl.isEmpty()) {
                return existingUrl.get(0).getShortUrl();
            }

            Long uniqueId;
            try {
                uniqueId = redisTemplate.opsForValue().increment("url_id_counter");
                if (uniqueId == null) uniqueId = System.currentTimeMillis();
            } catch (Exception e) {
                // 🎯 REDIS RESILIENCY FALLBACK: If Redis connection drops, use a timestamp index token
                uniqueId = System.currentTimeMillis();
            }
            shortUrl = base62Service.encode(uniqueId);
        }

        // 4. Write Directly to Persistent MongoDB Instance Storage
        UrlModel url = new UrlModel();
        url.setLongUrl(sanitizedUrl);
        url.setShortUrl(shortUrl);
        url.setCreatedAt(LocalDateTime.now());
        url.setExpiresAt(LocalDateTime.now().plusDays(30));
        urlRepository.save(url);

        try {
            redisTemplate.opsForValue().set(shortUrl, sanitizedUrl, Duration.ofHours(2));
        } catch (Exception e) {
            // Silently catch Redis secondary cache errors to prevent breaking core execution flow
        }

        return shortUrl;
    }

    public String getLongUrl(String shortUrl) {
        String cacheKey = "url:" + shortUrl;

        // 1. Check Redis Cache First
        String cachedLongUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedLongUrl != null) {
            System.out.println("🚀 Cache Hit! URL fetched from Redis.");
            return cachedLongUrl;
        }

        // 2. Cache Miss - Query MongoDB Database
        System.out.println("💾 Cache Miss! Querying MongoDB...");
        return urlRepository.findByShortUrl(shortUrl).map(urlModel -> {
            String longUrl = urlModel.getLongUrl();
                    
                    // Ensure the destination contains a proper browser protocol
            if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
                longUrl = "https://" + longUrl;
            }

                    // 3. Populate Redis Cache for subsequent fast lookups (Expires in 2 hours)
            redisTemplate.opsForValue().set(cacheKey, longUrl, java.time.Duration.ofHours(2));
            return longUrl;
            }).orElse(null); // Return null if the short link doesn't exist in our DB
    }

    public void updateClickCounter(String shortUrl){
        String redisKey = "click_counter:" + shortUrl;
        redisTemplate.opsForValue().increment(redisKey);
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void syncClickCounters(){
        Set<String> keys = redisTemplate.keys("click_counter:*");
        if(keys==null||keys.isEmpty()) return;
        for(String key: keys){
            String shortUrl = key.replace("click_counter:", "");
            String clickCountStr = redisTemplate.opsForValue().get(key);
            if(clickCountStr!=null){
                long clicks = Long.parseLong(clickCountStr);
                Query query = new Query(Criteria.where("shortUrl").is(shortUrl));
                Update update = new Update().set("clickCounter", clicks);
                mongoTemplate.updateFirst(query, update, UrlModel.class);
            }
        }
    }

    public boolean isAllowed(String clientIp){
        String redisKey = "rate_limit:" + clientIp;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if(count!=null && count==1){
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }
        return count!=null && count<=10;
    }

    public boolean isValidUrl(String url) { 
        if (url == null || url.isBlank()) return false;

        try { 
            // 1. Basic structural check using Java's built-in parser
            new URL(url).toURI(); 
            
            // 2. Strict character check: Ensure it doesn't contain a misplaced '@' in the authority component
            if (url.contains("@")) {
                return false;
            }

            return url.startsWith("http://") || url.startsWith("https://"); 
        } catch (MalformedURLException | URISyntaxException e) {
            return false; 
        } 
    }
    private String getCanonicalUrlKey(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        try {
            String clean = rawUrl.trim().toLowerCase();
            // Fallback prefixing to help URI parse structural paths correctly
            if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
                clean = "https://" + clean;
            }
            
            java.net.URI uri = new java.net.URI(clean);
            String host = uri.getHost();
            if (host == null) return clean;
            
            // Strip out common leading www. subdomains
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            
            String path = uri.getPath();
            // Remove trailing root backslashes
            if (path != null && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            
            String query = uri.getQuery();
            return host + (path != null ? path : "") + (query != null ? "?" + query : "");
        } catch (Exception e) {
            // Fallback strategy if URI parsing hits an edge case character exception
            return rawUrl.toLowerCase().replaceAll("^(https?://)?(www\\.)?", "").replaceAll("/+$", "");
        }
    }
}