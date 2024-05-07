package com.vincent.musicbrainz0;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MusicBrainzController.class)
public class MusicBrainzControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

   @Test
public void testSearchArtistReturnsDataForGriff() throws Exception {
    String artistResponse = "[{\"score\":100,\"name\":\"Griff\",\"id\":\"b711d64a-32d1-4605-a366-0205d7256dc3\",\"type\":\"Person\"},"
                            + "{\"score\":99,\"name\":\"Griff\",\"id\":\"84b8a3bc-7e45-4b1e-a34c-bb1a99b7bf5e\",\"type\":\"Group\"},"
                            + "{\"score\":94,\"name\":\"Griff\",\"id\":\"bfd3b074-9d65-4bb9-9e14-ee1a84390304\",\"type\":\"\"}]";
    when(restTemplate.getForObject(any(String.class), eq(String.class))).thenReturn(artistResponse);

    mockMvc.perform(get("/api/artists?name=Griff"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(11))
            .andExpect(jsonPath("$[0].name").value("Griff"))
            .andExpect(jsonPath("$[0].id").value("b711d64a-32d1-4605-a366-0205d7256dc3")) 
            .andExpect(jsonPath("$[0].type").value("Person"));
}

    @Test
public void testSearchArtistReturnsAlbums() throws Exception {
    String artistResponse = "{\"artists\": [{\"id\":\"1\",\"name\":\"The Beatles\", \"score\": 100}]}";
    String albumsResponse = "{\"release-groups\": [{\"id\":\"de208292-8db5-3aed-a14a-b37a84d8c521\", \"title\":\"Please Please Me\", \"first-release-date\":\"1969-09-26\"}]}";
    
    when(restTemplate.getForObject(
            any(String.class),
            eq(String.class)))
        .thenReturn(artistResponse, albumsResponse);

    mockMvc.perform(get("/api/artists?name=The Beatles"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value("de208292-8db5-3aed-a14a-b37a84d8c521"))
        .andExpect(jsonPath("$[0].title").value("Please Please Me"))
        .andExpect(jsonPath("$[0].release-date").value("1963-03-22"));
}
}
