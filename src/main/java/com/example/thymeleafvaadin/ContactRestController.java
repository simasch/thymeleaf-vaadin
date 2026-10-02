package com.example.thymeleafvaadin;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON endpoint used by the lazy-loading grid in contacts/lazy.html.
 * The grid asks for one page at a time while the user scrolls.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactRestController {

    private static final int MAX_PAGE_SIZE = 200;

    private final ContactService contactService;

    public ContactRestController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ContactPage find(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "50") int size,
                            @RequestParam(defaultValue = "lastName") String sort,
                            @RequestParam(defaultValue = "asc") String direction,
                            @RequestParam(required = false) String filter) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        List<Contact> items = contactService.find(filter, sort, !"desc".equalsIgnoreCase(direction),
                Math.max(page, 0) * pageSize, pageSize);
        return new ContactPage(items, contactService.count(filter));
    }

    public record ContactPage(List<Contact> items, int total) {
    }
}
