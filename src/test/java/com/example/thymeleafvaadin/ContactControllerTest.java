package com.example.thymeleafvaadin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRendersGridWithContactsAsJson() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<vaadin-grid id=\"contacts\"")))
                .andExpect(content().string(containsString("\"lastName\":\"Lovelace\"")));
    }

    @Test
    void invalidContactIsRenderedWithErrorAttributes() throws Exception {
        mockMvc.perform(post("/contacts")
                        .param("firstName", "")
                        .param("lastName", "Doe")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("error-message=\"must not be blank\"")))
                .andExpect(content().string(containsString("must be a well-formed email address")));
    }

    @Test
    void validContactIsSavedAndRedirects() throws Exception {
        mockMvc.perform(post("/contacts")
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@example.com"))
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("message", "Saved Jane Doe"));
    }
}
