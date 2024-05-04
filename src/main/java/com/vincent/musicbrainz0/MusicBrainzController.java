package com.vincent.musicbrainz0;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/artists")
public class MusicBrainzController {
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<String> searchArtist(@RequestParam String name) {
        String apiUrl = "https://musicbrainz.org/ws/2/artist?query=" + name + "&fmt=json";
        String response = restTemplate.getForObject(apiUrl, String.class);
        return ResponseEntity.ok(response);
    }
}
