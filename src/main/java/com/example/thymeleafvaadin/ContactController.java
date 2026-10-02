package com.example.thymeleafvaadin;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("contacts", contactService.findAll());
        return "contacts/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("contact", new Contact());
        return "contacts/form";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable long id, Model model) {
        model.addAttribute("contact", findContact(id));
        return "contacts/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("contact") Contact contact, BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "contacts/form";
        }
        contactService.save(contact);
        redirectAttributes.addFlashAttribute("message",
                "Saved " + contact.getFirstName() + " " + contact.getLastName());
        return "redirect:/contacts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
        Contact contact = findContact(id);
        contactService.delete(id);
        redirectAttributes.addFlashAttribute("message",
                "Deleted " + contact.getFirstName() + " " + contact.getLastName());
        return "redirect:/contacts";
    }

    private Contact findContact(long id) {
        return contactService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
