package com.vincent.musicbrainz0;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MusicBrainzController.class)
public class MusicBrainzControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSearchArtistReturnsData() throws Exception {
        String artistName = "The Beatles";
        String expectedResponse = "{\"artists\": [{\"name\": \"The Beatles\"}]}";

        mockMvc.perform(get("/api/artists?name=" + artistName))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedResponse));
    }
}