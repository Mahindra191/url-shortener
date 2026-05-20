package com.dev.project.url_shortener.model;

import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Document(collection = "urls")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class UrlModel {
    @Id
    private String id;
    @Indexed
    private String longUrl;
    @Indexed(unique = true)
    private String shortUrl;
    private LocalDateTime createdAt;
    private long clickCounter;
    @Indexed(expireAfterSeconds = 0)
    private LocalDateTime expiresAt;
}
