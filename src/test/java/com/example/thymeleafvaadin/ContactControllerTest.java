package com.example.thymeleafvaadin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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

    @Autowired
    private ContactService contactService;

    @Test
    void homeRedirectsToContacts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(redirectedUrl("/contacts"));
    }

    @Test
    void listRendersGridWithContactsAsJson() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/list"))
                .andExpect(content().string(containsString("<vaadin-grid id=\"contacts\"")))
                .andExpect(content().string(containsString("\"lastName\":\"Lovelace\"")))
                .andExpect(content().string(not(containsString("th:inline"))));
    }

    @Test
    void listEscapesContactDataInsideTheScript() throws Exception {
        Contact contact = contactService.save(
                new Contact(null, "</script><script>alert(1)</script>", "Mallory", "mallory@example.com", null));
        try {
            // "</script>" must be escaped, otherwise it would end the inline script and inject markup
            mockMvc.perform(get("/contacts"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            not(containsString("</script><script>alert(1)")),
                            containsString("\"<\\/script><script>alert(1)<\\/script>\""))));
        } finally {
            contactService.delete(contact.getId());
        }
    }

    @Test
    void layoutRendersAppLayoutNavigationAndBundle() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(content().string(allOf(
                        containsString("<vaadin-app-layout>"),
                        containsString("<vaadin-side-nav-item path=\"/contacts/lazy\">"),
                        containsString("src=\"/assets/main.js\""),
                        containsString("href=\"/assets/main.css\""))));
    }

    @Test
    void flashMessageIsRenderedAsNotification() throws Exception {
        mockMvc.perform(get("/contacts")
                        .flashAttr("message", "Contact Jane Doe deleted")
                        .flashAttr("messageTheme", "contrast"))
                .andExpect(content().string(
                        containsString("data-notification=\"Contact Jane Doe deleted\" data-theme=\"contrast\"")));
    }

    @Test
    void noNotificationWithoutFlashMessage() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(content().string(not(containsString("data-notification"))));
    }

    @Test
    void newFormIsEmptyAndHasNoDeleteButton() throws Exception {
        mockMvc.perform(get("/contacts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/form"))
                .andExpect(content().string(allOf(
                        containsString("New contact"),
                        containsString("name=\"firstName\""),
                        not(containsString("Delete contact")),
                        not(containsString("invalid=")))));
    }

    @Test
    void editFormShowsContactValues() throws Exception {
        Contact grace = contactService.find("hopper", "lastName", true, 0, 1).getFirst();

        mockMvc.perform(get("/contacts/{id}", grace.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Edit contact"),
                        containsString("value=\"Grace\""),
                        containsString("value=\"grace@example.com\""),
                        containsString("action=\"/contacts/" + grace.getId() + "/delete\""))));
    }

    @Test
    void deleteButtonOpensConfirmDialogInsteadOfSubmitting() throws Exception {
        Contact grace = contactService.find("hopper", "lastName", true, 0, 1).getFirst();

        mockMvc.perform(get("/contacts/{id}", grace.getId()))
                .andExpect(content().string(allOf(
                        containsString("<vaadin-button theme=\"error tertiary\" data-confirm=\"delete-dialog\">"),
                        containsString("<vaadin-confirm-dialog id=\"delete-dialog\""),
                        containsString("message=\"Grace Hopper will be permanently deleted.\""),
                        containsString("confirm-theme=\"error primary\""))));
    }

    @Test
    void editUnknownContactIsNotFound() throws Exception {
        mockMvc.perform(get("/contacts/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidContactIsRenderedWithErrorAttributes() throws Exception {
        mockMvc.perform(post("/contacts")
                        .param("firstName", "")
                        .param("lastName", "Doe")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/form"))
                .andExpect(model().attributeHasFieldErrors("contact", "firstName", "email"))
                .andExpect(content().string(containsString("error-message=\"must not be blank\"")))
                .andExpect(content().string(containsString("must be a well-formed email address")))
                // submitted values are kept so the user doesn't have to retype them
                .andExpect(content().string(containsString("value=\"not-an-email\"")));
    }

    @Test
    void invalidContactIsNotSaved() throws Exception {
        int before = contactService.count(null);

        mockMvc.perform(post("/contacts")
                .param("firstName", "Nobody")
                .param("lastName", "")
                .param("email", ""));

        assertThat(contactService.count(null)).isEqualTo(before);
    }

    @Test
    void tooLongPhoneIsRejected() throws Exception {
        mockMvc.perform(post("/contacts")
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("email", "jane@example.com")
                        .param("phone", "1".repeat(31)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("contact", "phone"))
                .andExpect(content().string(containsString("size must be between 0 and 30")));
    }

    @Test
    void validContactIsSavedAndRedirects() throws Exception {
        mockMvc.perform(post("/contacts")
                        .param("firstName", "Jane")
                        .param("lastName", "Createtest")
                        .param("email", "jane.createtest@example.com"))
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("message", "Contact Jane Createtest created"))
                .andExpect(flash().attribute("messageTheme", "success"));

        Contact saved = contactService.find("createtest", "lastName", true, 0, 10).getFirst();
        assertThat(saved.getEmail()).isEqualTo("jane.createtest@example.com");
        contactService.delete(saved.getId());
    }

    @Test
    void existingContactIsUpdated() throws Exception {
        Contact contact = contactService.save(new Contact(null, "John", "Updatetest", "john@example.com", null));
        int before = contactService.count(null);
        try {
            mockMvc.perform(post("/contacts")
                            .param("id", contact.getId().toString())
                            .param("firstName", "Johnny")
                            .param("lastName", "Updatetest")
                            .param("email", "johnny@example.com"))
                    .andExpect(redirectedUrl("/contacts"))
                    .andExpect(flash().attribute("message", "Contact Johnny Updatetest updated"))
                    .andExpect(flash().attribute("messageTheme", "success"));

            assertThat(contactService.count(null)).isEqualTo(before);
            assertThat(contactService.findById(contact.getId())).hasValueSatisfying(updated -> {
                assertThat(updated.getFirstName()).isEqualTo("Johnny");
                assertThat(updated.getEmail()).isEqualTo("johnny@example.com");
            });
        } finally {
            contactService.delete(contact.getId());
        }
    }

    @Test
    void contactIsDeletedAndRedirects() throws Exception {
        Contact contact = contactService.save(new Contact(null, "Dora", "Deletetest", "dora@example.com", null));

        mockMvc.perform(post("/contacts/{id}/delete", contact.getId()))
                .andExpect(redirectedUrl("/contacts"))
                .andExpect(flash().attribute("message", "Contact Dora Deletetest deleted"))
                .andExpect(flash().attribute("messageTheme", "contrast"));

        assertThat(contactService.findById(contact.getId())).isEmpty();
    }

    @Test
    void deleteUnknownContactIsNotFound() throws Exception {
        mockMvc.perform(post("/contacts/{id}/delete", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void lazyPageContainsNoContactData() throws Exception {
        mockMvc.perform(get("/contacts/lazy"))
                .andExpect(status().isOk())
                .andExpect(view().name("contacts/lazy"))
                .andExpect(model().attributeDoesNotExist("contacts"))
                .andExpect(content().string(allOf(
                        containsString("grid.dataProvider"),
                        // th:inline="javascript" renders the URL as a JavaScript string literal
                        containsString("const apiUrl = \"\\/api\\/contacts\";"),
                        not(containsString("Lovelace")))));
    }
}
