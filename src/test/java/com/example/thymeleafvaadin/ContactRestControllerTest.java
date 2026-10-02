package com.example.thymeleafvaadin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContactRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactService contactService;

    @Test
    void defaultsToFirstPageOfFiftySortedByLastName() throws Exception {
        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items", hasSize(50)))
                .andExpect(jsonPath("$.items[0].lastName").value("Baumann"))
                .andExpect(jsonPath("$.total").value(contactService.count(null)));
    }

    @Test
    void returnsAllContactProperties() throws Exception {
        mockMvc.perform(get("/api/contacts").param("filter", "grace@example.com"))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].firstName").value("Grace"))
                .andExpect(jsonPath("$.items[0].lastName").value("Hopper"))
                .andExpect(jsonPath("$.items[0].email").value("grace@example.com"))
                .andExpect(jsonPath("$.items[0].phone").value("+1 202 555 0101"));
    }

    @Test
    void sortsDescending() throws Exception {
        mockMvc.perform(get("/api/contacts")
                        .param("sort", "lastName")
                        .param("direction", "desc"))
                .andExpect(jsonPath("$.items[0].lastName").value("Zimmermann"));
    }

    @Test
    void unknownSortPropertyFallsBackToLastName() throws Exception {
        mockMvc.perform(get("/api/contacts").param("sort", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lastName").value("Baumann"));
    }

    @Test
    void filtersAndReportsFilteredTotal() throws Exception {
        mockMvc.perform(get("/api/contacts")
                        .param("size", "2")
                        .param("filter", "turing"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].lastName").value("Turing"));
    }

    @Test
    void returnsRequestedPage() throws Exception {
        String secondItemOfFirstPage = contactService.find(null, "lastName", true, 1, 1).getFirst().getEmail();

        mockMvc.perform(get("/api/contacts")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].email").value(secondItemOfFirstPage));
    }

    @Test
    void pageBeyondEndIsEmptyButKeepsTotal() throws Exception {
        mockMvc.perform(get("/api/contacts")
                        .param("page", "1000")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(contactService.count(null)));
    }

    @Test
    void pageSizeIsCappedAt200() throws Exception {
        mockMvc.perform(get("/api/contacts").param("size", "100000"))
                .andExpect(jsonPath("$.items", hasSize(200)));
    }

    @Test
    void pageSizeIsAtLeastOne() throws Exception {
        mockMvc.perform(get("/api/contacts").param("size", "0"))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void negativePageIsTreatedAsFirstPage() throws Exception {
        mockMvc.perform(get("/api/contacts").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lastName").value("Baumann"));
    }

    @Test
    void nonNumericPageIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/contacts").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }
}
