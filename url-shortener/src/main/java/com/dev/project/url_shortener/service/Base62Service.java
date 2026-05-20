package com.dev.project.url_shortener.service;
import org.springframework.stereotype.Service;

@Service
public class Base62Service {
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String encode(long num) {
        num = Math.abs(num);
        
        if (num == 0) return String.valueOf(ALPHABET.charAt(0));
        StringBuilder sb = new StringBuilder();
        while(num>0){
            int rem =(int) (num % 62);
            sb.append(ALPHABET.charAt(rem));
            num/=62;
        }
        return sb.reverse().toString();
    }
}
