package com.dev.project.url_shortener.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.dev.project.url_shortener.model.UrlModel;

import java.util.*;

public interface UrlRepository extends MongoRepository<UrlModel, String> {
    Optional<UrlModel> findByShortUrl(String shortUrl);   
    List<UrlModel> findByLongUrlOrderByCreatedAtDesc(String longUrl);
}
