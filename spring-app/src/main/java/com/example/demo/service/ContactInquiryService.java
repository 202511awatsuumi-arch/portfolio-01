package com.example.demo.service;

import com.example.demo.entity.ContactInquiry;
import com.example.demo.form.ContactForm;
import com.example.demo.form.ContactInquiryEditForm;
import com.example.demo.repository.ContactInquiryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
        return contactInquiryRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc(
                PageRequest.of(page, size));
    }

    public Page<ContactInquiry> findDeletedPage(int page, int size) {
        return contactInquiryRepository.findAllByDeletedAtIsNotNullOrderByDeletedAtDescCreatedAtDesc(
                PageRequest.of(page, size));
    }

    public List<ContactInquiry> findAll() {
        return contactInquiryRepository.findAll().stream()
                .filter(inquiry -> inquiry.getDeletedAt() == null)
                .sorted((left, right) -> {
                    int createdAtComparison = right.getCreatedAt().compareTo(left.getCreatedAt());
                    if (createdAtComparison != 0) {
                        return createdAtComparison;
                    }
                    return right.getId().compareTo(left.getId());
                })
                .toList();
    }

    public Optional<ContactInquiry> findById(Long id) {
        return contactInquiryRepository.findByIdAndDeletedAtIsNull(id);
    }

    public ContactInquiryEditForm toEditForm(ContactInquiry inquiry, int returnPage) {
        ContactInquiryEditForm form = new ContactInquiryEditForm();
        form.setId(inquiry.getId());
        form.setInquiryType(inquiry.getInquiryType());
        form.setName(inquiry.getName());
        form.setEmail(inquiry.getEmail());
        form.setPhoneNumber(inquiry.getPhoneNumber());
        form.setPlan(inquiry.getPlan());
        form.setDate(
                inquiry.getPreferredDate() != null ? inquiry.getPreferredDate().toString() : null);
        form.setContactMethod(inquiry.getContactMethod());
        form.setRequestType(splitRequestTypes(inquiry.getRequestTypes()));
        form.setMessage(inquiry.getMessage());
        form.setReturnPage(returnPage);
        return form;
    }

    public Optional<ContactInquiry> update(Long id, ContactInquiryEditForm form) {
        return contactInquiryRepository.findByIdAndDeletedAtIsNull(id)
                .map(
                        inquiry -> {
                            inquiry.setInquiryType(form.getInquiryType());
                            inquiry.setName(form.getName());
                            inquiry.setEmail(form.getEmail());
                            inquiry.setPhoneNumber(blankToNull(form.getPhoneNumber()));
                            inquiry.setPlan(blankToNull(form.getPlan()));
                            inquiry.setPreferredDate(parsePreferredDate(form.getDate()));
                            inquiry.setContactMethod(blankToNull(form.getContactMethod()));
                            inquiry.setRequestTypes(joinRequestTypes(form.getRequestType()));
                            inquiry.setMessage(form.getMessage());
                            return contactInquiryRepository.save(inquiry);
                        });
    }

    public boolean softDelete(Long id) {
        return contactInquiryRepository.findByIdAndDeletedAtIsNull(id)
                .map(
                        inquiry -> {
                            inquiry.setDeletedAt(LocalDateTime.now());
                            contactInquiryRepository.save(inquiry);
                            return true;
                })
                .orElse(false);
    }

    public boolean restore(Long id) {
        return contactInquiryRepository.findByIdAndDeletedAtIsNotNull(id)
                .map(
                        inquiry -> {
                            inquiry.setDeletedAt(null);
                            contactInquiryRepository.save(inquiry);
                            return true;
                        })
                .orElse(false);
    }

    public long getDisplayNumber(ContactInquiry inquiry) {
        return contactInquiryRepository.countNewerThan(inquiry.getCreatedAt(), inquiry.getId()) + 1;
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

    private List<String> splitRequestTypes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String joinRequestTypes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }
}
