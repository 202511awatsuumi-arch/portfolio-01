package com.example.demo.service;

import com.example.demo.entity.ContactInquiry;
import com.example.demo.form.ContactForm;
import com.example.demo.repository.ContactInquiryRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ContactInquiryService {

    private final ContactInquiryRepository contactInquiryRepository;
    private final PreferredDateNormalizer preferredDateNormalizer;

    public ContactInquiryService(
            ContactInquiryRepository contactInquiryRepository,
            PreferredDateNormalizer preferredDateNormalizer) {
        this.contactInquiryRepository = contactInquiryRepository;
        this.preferredDateNormalizer = preferredDateNormalizer;
    }

    public void save(ContactForm contactForm, boolean englishPage) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setInquiryType(contactForm.getInquiryType());
        inquiry.setName(contactForm.getName());
        inquiry.setEmail(contactForm.getEmail());
        inquiry.setPhoneNumber(blankToNull(contactForm.getPhoneNumber()));
        inquiry.setPlan(blankToNull(contactForm.getPlan()));
        inquiry.setPreferredDate(parsePreferredDate(contactForm.getDate()));
        inquiry.setContactMethod(blankToNull(contactForm.getContactMethod()));
        inquiry.setRequestTypes(
                contactForm.getRequestType() == null || contactForm.getRequestType().isEmpty()
                        ? null
                        : String.join(", ", contactForm.getRequestType()));
        inquiry.setMessage(contactForm.getMessage());
        inquiry.setEnglishPage(englishPage);
        contactInquiryRepository.save(inquiry);
    }

    public Page<ContactInquiry> findPage(int page, int size) {
        return contactInquiryRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size));
    }

    public String normalizePreferredDate(String value) {
        return preferredDateNormalizer.normalizePreferredDate(value);
    }

    public LocalDate parsePreferredDate(String value) {
        return preferredDateNormalizer.parsePreferredDate(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
