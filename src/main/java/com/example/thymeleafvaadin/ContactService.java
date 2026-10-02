package com.example.thymeleafvaadin;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * In-memory store so the example runs without a database.
 */
@Service
public class ContactService {

    private static final String[] FIRST_NAMES = {"Anna", "Ben", "Clara", "David", "Emma", "Felix", "Greta",
            "Hugo", "Ida", "Jonas", "Klara", "Leo", "Mia", "Noah", "Olivia", "Paul", "Rosa", "Simon", "Tina", "Yann"};
    private static final String[] LAST_NAMES = {"Meier", "Müller", "Schmid", "Keller", "Weber", "Huber",
            "Schneider", "Fischer", "Brunner", "Baumann", "Frei", "Zimmermann", "Moser", "Gerber", "Widmer"};
    private static final int GENERATED_CONTACTS = 1000;

    private static final Map<String, Comparator<Contact>> SORT_PROPERTIES = Map.of(
            "firstName", comparing(Contact::getFirstName),
            "lastName", comparing(Contact::getLastName),
            "email", comparing(Contact::getEmail),
            "phone", comparing(Contact::getPhone));

    private final Map<Long, Contact> contacts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public ContactService() {
        save(new Contact(null, "Ada", "Lovelace", "ada@example.com", "+44 20 1234 5678"));
        save(new Contact(null, "Alan", "Turing", "alan@example.com", "+44 20 8765 4321"));
        save(new Contact(null, "Grace", "Hopper", "grace@example.com", "+1 202 555 0101"));
        save(new Contact(null, "Linus", "Torvalds", "linus@example.com", null));

        // Enough rows to make lazy loading in the grid worthwhile
        for (int i = 0; i < GENERATED_CONTACTS; i++) {
            String firstName = FIRST_NAMES[i % FIRST_NAMES.length];
            String lastName = LAST_NAMES[(i / FIRST_NAMES.length) % LAST_NAMES.length];
            String email = "%s.%s.%d@example.com".formatted(firstName, lastName, i).toLowerCase(Locale.ROOT);
            save(new Contact(null, firstName, lastName, email, "+41 79 %03d %02d %02d".formatted(i, i % 100, i % 97)));
        }
    }

    public List<Contact> findAll() {
        return contacts.values().stream()
                .sorted(Comparator.comparing(Contact::getLastName).thenComparing(Contact::getFirstName))
                .toList();
    }

    /**
     * Returns one page of contacts, optionally filtered by name or email and sorted by the given property.
     */
    public List<Contact> find(String filter, String sortProperty, boolean ascending, int offset, int limit) {
        Comparator<Contact> comparator = SORT_PROPERTIES.getOrDefault(sortProperty, SORT_PROPERTIES.get("lastName"));
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return contacts.values().stream()
                .filter(contact -> matches(contact, filter))
                .sorted(comparator.thenComparing(Contact::getId))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public int count(String filter) {
        return (int) contacts.values().stream()
                .filter(contact -> matches(contact, filter))
                .count();
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

    private static boolean matches(Contact contact, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String term = filter.toLowerCase(Locale.ROOT);
        return (contact.getFirstName() + " " + contact.getLastName() + " " + contact.getEmail())
                .toLowerCase(Locale.ROOT)
                .contains(term);
    }

    private static Comparator<Contact> comparing(Function<Contact, String> property) {
        return Comparator.comparing(property, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
