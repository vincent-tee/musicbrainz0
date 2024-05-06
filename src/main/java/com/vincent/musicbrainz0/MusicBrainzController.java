package com.vincent.musicbrainz0;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

/**
 * Controller class for interacting with the MusicBrainz API.
 */
@RestController
@RequestMapping("/api")
public class MusicBrainzController {
    private static final int SCORE_THRESHOLD = 85;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Searches for an artist by name and returns a list of matching artists or their albums.
     *
     * @param name The name of the artist to search for.
     * @return A ResponseEntity containing a JSON response with either a list of matching artists or the albums of a single matched artist.
     */
    @GetMapping(value = "/artists", produces = "application/json")
public ResponseEntity<String> searchArtist(@RequestParam String name) {
    URI uri = UriComponentsBuilder
            .fromHttpUrl("https://musicbrainz.org/ws/2/artist")
            .queryParam("query", "artist:" + name)
            .queryParam("fmt", "json")
            .queryParam("limit", "30")
            .encode(StandardCharsets.UTF_8)
            .build()
            .toUri();

    try {
        String response = restTemplate.getForObject(uri, String.class);
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode artistsNode = rootNode.path("artists");

        // Filter artists by score
        List<Map<String, Object>> filteredArtists = new ArrayList<>();
        for (JsonNode artistNode : artistsNode) {
            int score = artistNode.path("score").asInt();
            if (score > SCORE_THRESHOLD) {
                Map<String, Object> artistDetails = new HashMap<>();
                artistDetails.put("id", artistNode.path("id").asText());
                artistDetails.put("name", artistNode.path("name").asText());
                artistDetails.put("score", score);
                artistDetails.put("type", artistNode.path("type").asText());
                filteredArtists.add(artistDetails);
            }
        }

        if (filteredArtists.size() == 1) {
            // Only one artist after filtering, fetch albums
            String artistId = (String) filteredArtists.get(0).get("id");
            return fetchAlbumsForArtist(artistId);
        } else {
            // Return the filtered list of artists with only necessary fields
            String filteredResponse = objectMapper.writeValueAsString(filteredArtists);
            return ResponseEntity.ok(filteredResponse);
        }
    } catch (ResourceAccessException e) {
        // Handle network or other resource access errors
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to access MusicBrainz API");

    } catch (Exception e) {
        // Handle any other unexpected exceptions
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
}


    /**
     * Filters the list of artists based on their score and returns a JSON response.
     *
     * @param jsonResponse The JSON response containing the list of artists.
     * @return A ResponseEntity containing a JSON response with a list of filtered artists.
     * @throws IOException If there is an issue parsing the JSON response.
     */
    private ResponseEntity<String> filterArtistsByScore(String jsonResponse) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode artistsNode = rootNode.path("artists");
        if (artistsNode == null || !artistsNode.isArray()) {
            return ResponseEntity.ok("No artists found");
        }

        List<JsonNode> filteredArtists = new ArrayList<>();

        for (JsonNode artistNode : artistsNode) {
            int score = artistNode.path("score").asInt();
            if (score > SCORE_THRESHOLD) {
                filteredArtists.add(artistNode);
            }
        }

        if (filteredArtists.isEmpty()) {
            return ResponseEntity.ok("No artists found with score > " + SCORE_THRESHOLD);
        }

        String filteredResponse = objectMapper.writeValueAsString(filteredArtists);
        return ResponseEntity.ok(filteredResponse);
    }

    /**
     * Fetches the albums for a given artist and returns a JSON response.
     *
     * @param artistId The ID of the artist.
     * @return A ResponseEntity containing a JSON response with a list of the artist's albums.
     * @throws IOException If there is an issue parsing the JSON response.
     */
    private ResponseEntity<String> fetchAlbumsForArtist(String artistId) throws IOException {
        String albumsResponse = makeAlbumApiCall(artistId);
        List<Map<String, String>> albums = parseAlbumsResponse(albumsResponse);
        String filteredResponse = objectMapper.writeValueAsString(albums);
        return ResponseEntity.ok(filteredResponse);
    }

    /**
     * Makes an API call to fetch the albums for a given artist.
     *
     * @param artistId The ID of the artist.
     * @return The JSON response from the MusicBrainz API.
     */
    private String makeAlbumApiCall(String artistId) {
        URI albumUri = UriComponentsBuilder
                .fromHttpUrl("https://musicbrainz.org/ws/2/release-group")
                .queryParam("artist", artistId)
                .queryParam("fmt", "json")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        return restTemplate.getForObject(albumUri, String.class);
    }

    /**
     * Parses the JSON response containing the list of albums for an artist.
     *
     * @param albumsResponse The JSON response from the MusicBrainz API.
     * @return A list of maps containing the album details (id, title, release-date).
     * @throws IOException If there is an issue parsing the JSON response.
     */

private List<Map<String, String>> parseAlbumsResponse(String albumsResponse) throws IOException {
    JsonNode rootNode = objectMapper.readTree(albumsResponse);
    JsonNode releasesNode = rootNode.path("release-groups");

    List<Map<String, String>> albums = new ArrayList<>();
    if (releasesNode != null && releasesNode.isArray()) {
        for (JsonNode releaseNode : releasesNode) {
            Map<String, String> albumDetails = new HashMap<>();
            albumDetails.put("id", releaseNode.path("id").asText());
            albumDetails.put("title", releaseNode.path("title").asText());
            albumDetails.put("release-date", releaseNode.path("first-release-date").asText());
            albums.add(albumDetails);
        }
    }

    // Sort the list of albums by release date
    Collections.sort(albums, new Comparator<Map<String, String>>() {
        @Override
        public int compare(Map<String, String> o1, Map<String, String> o2) {
            return o1.get("release-date").compareTo(o2.get("release-date"));
        }
    });

    return albums;
}

}