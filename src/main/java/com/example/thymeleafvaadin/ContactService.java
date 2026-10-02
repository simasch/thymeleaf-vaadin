package com.example.thymeleafvaadin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * In-memory store so the example runs without a database.
 */
@Service
public class ContactService {

    private final Map<Long, Contact> contacts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public ContactService() {
        save(new Contact(null, "Ada", "Lovelace", "ada@example.com", "+44 20 1234 5678"));
        save(new Contact(null, "Alan", "Turing", "alan@example.com", "+44 20 8765 4321"));
        save(new Contact(null, "Grace", "Hopper", "grace@example.com", "+1 202 555 0101"));
        save(new Contact(null, "Linus", "Torvalds", "linus@example.com", null));
    }

    public List<Contact> findAll() {
        return contacts.values().stream()
                .sorted(Comparator.comparing(Contact::getLastName).thenComparing(Contact::getFirstName))
                .toList();
    }

    public Optional<Contact> findById(long id) {
        return Optional.ofNullable(contacts.get(id));
    }

    public Contact save(Contact contact) {
        if (contact.getId() == null) {
            contact.setId(sequence.incrementAndGet());
        }
        contacts.put(contact.getId(), contact);
        return contact;
    }

    public void delete(long id) {
        contacts.remove(id);
    }
}
