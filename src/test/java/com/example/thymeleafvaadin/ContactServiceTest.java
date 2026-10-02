package com.example.thymeleafvaadin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContactServiceTest {

    private static final int SEEDED_CONTACTS = 1004;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new ContactService();
    }

    @Test
    void seedsDemoContacts() {
        assertThat(contactService.count(null)).isEqualTo(SEEDED_CONTACTS);
        assertThat(contactService.findAll()).hasSize(SEEDED_CONTACTS);
    }

    @Test
    void findAllIsSortedByLastNameThenFirstName() {
        List<Contact> contacts = contactService.findAll();

        for (int i = 1; i < contacts.size(); i++) {
            Contact previous = contacts.get(i - 1);
            Contact current = contacts.get(i);
            int byLastName = previous.getLastName().compareTo(current.getLastName());
            assertThat(byLastName < 0
                    || byLastName == 0 && previous.getFirstName().compareTo(current.getFirstName()) <= 0)
                    .as("%s %s before %s %s", previous.getFirstName(), previous.getLastName(),
                            current.getFirstName(), current.getLastName())
                    .isTrue();
        }
    }

    @Test
    void consecutivePagesDoNotOverlap() {
        List<Contact> firstPage = contactService.find(null, "lastName", true, 0, 50);
        List<Contact> secondPage = contactService.find(null, "lastName", true, 50, 50);

        assertThat(firstPage).hasSize(50);
        assertThat(secondPage).hasSize(50);
        assertThat(firstPage).doesNotContainAnyElementsOf(secondPage);
    }

    @Test
    void pagingIsStableForEqualSortValues() {
        // Many contacts share a first name; ties are broken by id so pages don't shuffle between requests
        List<Contact> annas = contactService.find("anna", "firstName", true, 0, 1000);

        assertThat(annas).extracting(Contact::getId).isSorted();
        assertThat(contactService.find("anna", "firstName", true, 0, 1000)).isEqualTo(annas);
    }

    @Test
    void sortsAscendingAndDescending() {
        assertThat(contactService.find(null, "lastName", true, 0, 1).getFirst().getLastName()).isEqualTo("Baumann");
        assertThat(contactService.find(null, "lastName", false, 0, 1).getFirst().getLastName()).isEqualTo("Zimmermann");
    }

    @Test
    void sortingIsCaseInsensitive() {
        contactService.save(new Contact(null, "Zoe", "aardvark", "zoe@example.com", null));

        assertThat(contactService.find(null, "lastName", true, 0, 1).getFirst().getLastName()).isEqualTo("aardvark");
    }

    @Test
    void contactsWithoutPhoneAreSortedLast() {
        List<Contact> byPhone = contactService.find(null, "phone", true, 0, SEEDED_CONTACTS);

        assertThat(byPhone.getLast().getLastName()).isEqualTo("Torvalds");
        assertThat(byPhone.getLast().getPhone()).isNull();
    }

    @Test
    void unknownSortPropertyFallsBackToLastName() {
        assertThat(contactService.find(null, "doesNotExist", true, 0, 10))
                .isEqualTo(contactService.find(null, "lastName", true, 0, 10));
    }

    @Test
    void filterMatchesNameAndEmailCaseInsensitively() {
        assertThat(contactService.find("LOVELACE", "lastName", true, 0, 10))
                .extracting(Contact::getEmail)
                .containsExactly("ada@example.com");
        assertThat(contactService.find("ada lovelace", "lastName", true, 0, 10)).hasSize(1);
        assertThat(contactService.find("grace@example", "lastName", true, 0, 10)).hasSize(1);
        assertThat(contactService.count("no such contact")).isZero();
    }

    @Test
    void blankFilterMatchesEverything() {
        assertThat(contactService.count("")).isEqualTo(SEEDED_CONTACTS);
        assertThat(contactService.count("   ")).isEqualTo(SEEDED_CONTACTS);
    }

    @Test
    void countMatchesFilteredResults() {
        assertThat(contactService.count("hopper"))
                .isEqualTo(contactService.find("hopper", "lastName", true, 0, 1000).size());
    }

    @Test
    void offsetBeyondEndReturnsEmptyPage() {
        assertThat(contactService.find(null, "lastName", true, SEEDED_CONTACTS, 50)).isEmpty();
    }

    @Test
    void saveAssignsIdToNewContact() {
        Contact saved = contactService.save(new Contact(null, "Jane", "Doe", "jane@example.com", null));

        assertThat(saved.getId()).isNotNull();
        assertThat(contactService.findById(saved.getId())).containsSame(saved);
        assertThat(contactService.count(null)).isEqualTo(SEEDED_CONTACTS + 1);
    }

    @Test
    void saveWithExistingIdUpdatesContact() {
        Contact ada = contactService.find("lovelace", "lastName", true, 0, 1).getFirst();

        contactService.save(new Contact(ada.getId(), "Ada", "King", "ada@example.com", null));

        assertThat(contactService.findById(ada.getId())).hasValueSatisfying(
                contact -> assertThat(contact.getLastName()).isEqualTo("King"));
        assertThat(contactService.count(null)).isEqualTo(SEEDED_CONTACTS);
    }

    @Test
    void deleteRemovesContact() {
        Contact ada = contactService.find("lovelace", "lastName", true, 0, 1).getFirst();

        contactService.delete(ada.getId());

        assertThat(contactService.findById(ada.getId())).isEmpty();
        assertThat(contactService.count("lovelace")).isZero();
    }

    @Test
    void findByIdOfUnknownContactIsEmpty() {
        assertThat(contactService.findById(-1)).isEmpty();
    }
}
