package com.example.demo.controller;

import com.example.demo.form.ContactForm;
import com.example.demo.form.ContactInquiryEditForm;
import com.example.demo.service.ContactInquiryService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.example.demo.entity.ContactInquiry;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ContactSiteController {

    private static final Set<String> ALLOWED_CONTACT_METHODS = Set.of("EMAIL", "PHONE");
    private static final Set<String> ALLOWED_PLANS = Set.of("STANDARD", "PREMIUM", "CONSULT");
    private static final Set<String> ALLOWED_REQUEST_TYPES =
            Set.of("BOOKING_REQUEST", "AVAILABILITY_CHECK", "PLAN_CONSULTATION");

    private final ContactInquiryService contactInquiryService;

    public ContactSiteController(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    @GetMapping({"/", "/index.html"})
    public String index() {
        return "index";
    }

    @GetMapping("/about.html")
    public String about() {
        return "about";
    }

    @GetMapping("/access.html")
    public String access() {
        return "access";
    }

    @GetMapping("/skills.html")
    public String skills() {
        return "skills";
    }

    @GetMapping("/works.html")
    public String works() {
        return "works";
    }

    @GetMapping("/contact.html")
    public String contact(Model model) {
        model.addAttribute("isEnglishPage", false);
        return "contact";
    }

    @GetMapping("/en/index.html")
    public String enIndex() {
        return "en/index";
    }

    @GetMapping("/en/about.html")
    public String enAbout() {
        return "en/about";
    }

    @GetMapping("/en/access.html")
    public String enAccess() {
        return "en/access";
    }

    @GetMapping("/en/skills.html")
    public String enSkills() {
        return "en/skills";
    }

    @GetMapping("/en/works.html")
    public String enWorks() {
        return "en/works";
    }

    @GetMapping("/en/contact.html")
    public String enContact(Model model) {
        model.addAttribute("isEnglishPage", true);
        return "en/contact";
    }

    @GetMapping({"/admin/contacts", "/admin/inquiries"})
    public String adminContacts(
            @RequestParam(name = "page", defaultValue = "1") int page,
            Authentication authentication,
            Model model) {
        int pageSize = 10;
        int currentPage = Math.max(page, 1);
        Page<ContactInquiry> contactPage =
                contactInquiryService.findPage(currentPage - 1, pageSize);

        model.addAttribute("contacts", contactPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", Math.max(contactPage.getTotalPages(), 1));
        model.addAttribute("hasPrevious", contactPage.hasPrevious());
        model.addAttribute("hasNext", contactPage.hasNext());
        model.addAttribute(
                "isAdmin",
                authentication != null
                        && authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        return "admin/inquiries";
    }

    @GetMapping("/admin/inquiries/{id}")
    public String adminInquiryDetail(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Authentication authentication,
            Model model) {
        ContactInquiry contact = contactInquiryService.findById(id).orElse(null);
        if (contact == null) {
            return "redirect:/admin/inquiries?page=" + Math.max(page, 1);
        }

        model.addAttribute("contact", contact);
        model.addAttribute("returnPage", Math.max(page, 1));
        model.addAttribute("rowNumber", contactInquiryService.getDisplayNumber(contact));
        model.addAttribute(
                "isAdmin",
                authentication != null
                        && authentication.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        return "admin/inquiry-detail-view";
    }

    @GetMapping("/admin/inquiries/{id}/edit")
    public String adminInquiryEdit(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        ContactInquiry contact = contactInquiryService.findById(id).orElse(null);
        if (contact == null) {
            return "redirect:/admin/inquiries?page=" + Math.max(page, 1);
        }

        if (!model.containsAttribute("contactInquiryEditForm")) {
            model.addAttribute(
                    "contactInquiryEditForm",
                    contactInquiryService.toEditForm(contact, Math.max(page, 1)));
        }

        return "admin/inquiry-edit";
    }

    @PostMapping("/admin/inquiries/{id}")
    public String updateInquiry(
            @PathVariable("id") Long id,
            @Valid
                    @ModelAttribute("contactInquiryEditForm")
                    ContactInquiryEditForm contactInquiryEditForm,
            BindingResult bindingResult,
            Model model) {
        validateContactForm(toContactForm(contactInquiryEditForm), bindingResult, false);

        int returnPage =
                Math.max(
                        contactInquiryEditForm.getReturnPage() != null
                                ? contactInquiryEditForm.getReturnPage()
                                : 1,
                        1);

        if (bindingResult.hasErrors()) {
            contactInquiryEditForm.setReturnPage(returnPage);
            model.addAttribute("contactInquiryEditForm", contactInquiryEditForm);
            return "admin/inquiry-edit";
        }

        if (contactInquiryService.update(id, contactInquiryEditForm).isEmpty()) {
            return "redirect:/admin/inquiries?page=" + returnPage;
        }

        return "redirect:/admin/inquiries/" + id + "?page=" + returnPage;
    }

    @PostMapping("/contact")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitContact(
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        return handleContactSubmission(contactForm, bindingResult, false, requestedWith);
    }

    @PostMapping("/en/contact")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitEnglishContact(
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        return handleContactSubmission(contactForm, bindingResult, true, requestedWith);
    }

    private ResponseEntity<Map<String, Object>> handleContactSubmission(
            ContactForm contactForm,
            BindingResult bindingResult,
            boolean english,
            String requestedWith) {
        validateContactForm(contactForm, bindingResult, english);

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse(bindingResult, english));
        }

        contactInquiryService.save(contactForm, english);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(
                "message",
                english
                        ? "Thank you for your message. We will review it and get back to you shortly."
                        : "送信ありがとうございました。内容を確認のうえ、折り返しご連絡します。");
        response.put("requestedWith", requestedWith);
        return ResponseEntity.ok(response);
    }

    private void validateContactForm(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        validateWorkshopFields(contactForm, bindingResult, english);
        validatePreferredDate(contactForm, bindingResult, english);
        validateContactMethod(contactForm, bindingResult, english);
        validateRequestType(contactForm, bindingResult, english);
        validatePhoneNumber(contactForm, bindingResult, english);
    }

    private void validateWorkshopFields(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (!"workshop".equals(contactForm.getInquiryType())) {
            validatePlanCode(contactForm, bindingResult, english);
            return;
        }

        if (isBlank(contactForm.getPlan())) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "plan",
                            english ? "Please select an option." : "選択してください。"));
        } else {
            validatePlanCode(contactForm, bindingResult, english);
        }

        if (isBlank(contactForm.getDate())) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "date",
                            english ? "Please fill out this field." : "入力してください。"));
        }
    }

    private void validatePreferredDate(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (isBlank(contactForm.getDate())) {
            return;
        }

        try {
            contactInquiryService.normalizePreferredDate(contactForm.getDate());
        } catch (IllegalArgumentException ex) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "date",
                            english
                                    ? "Please enter the date as YYYY-MM-DD or MM/DD/YYYY."
                                    : "日付は YYYY-MM-DD または MM/DD/YYYY 形式で入力してください。"));
        }
    }

    private void validateContactMethod(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (isBlank(contactForm.getContactMethod())) {
            return;
        }

        if (!ALLOWED_CONTACT_METHODS.contains(contactForm.getContactMethod())) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "contactMethod",
                            english ? "Please select a valid contact method." : "正しいご連絡方法を選んでください。"));
        }
    }

    private void validateRequestType(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        boolean hasSelection =
                contactForm.getRequestType() != null
                        && contactForm.getRequestType().stream().anyMatch(this::isNotBlank);

        if ("workshop".equals(contactForm.getInquiryType()) && !hasSelection) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "requestType",
                            english
                                    ? "Please select at least one request detail."
                                    : "ご希望内容を1つ以上選択してください。"));
            return;
        }

        boolean hasInvalidCode = hasSelection
                && contactForm.getRequestType().stream()
                        .filter(this::isNotBlank)
                        .anyMatch(value -> !ALLOWED_REQUEST_TYPES.contains(value));
        if (hasInvalidCode) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "requestType",
                            english ? "Please select valid request details." : "正しいご希望内容を選んでください。"));
        }
    }

    private void validatePlanCode(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (isBlank(contactForm.getPlan())) {
            return;
        }

        if (!ALLOWED_PLANS.contains(contactForm.getPlan())) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "plan",
                            english ? "Please select a valid option." : "正しい選択肢を選んでください。"));
        }
    }

    private void validatePhoneNumber(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (!"PHONE".equals(contactForm.getContactMethod())) {
            return;
        }

        if (isBlank(contactForm.getPhoneNumber())) {
            bindingResult.addError(
                    new FieldError(
                            bindingResult.getObjectName(),
                            "phoneNumber",
                            english
                                    ? "Please enter your phone number if you prefer to be contacted by phone."
                                    : "電話でのご連絡を希望する場合は電話番号を入力してください。"));
        }
    }

    private Map<String, Object> errorResponse(BindingResult bindingResult, boolean english) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        bindingResult.getFieldErrors()
                .forEach(
                        error ->
                                fieldErrors.putIfAbsent(
                                        error.getField(), resolveFieldMessage(error, english)));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(
                "message",
                english
                        ? "Some required fields are missing or invalid. Please review the form."
                        : "未入力または入力内容に誤りがあります。ご確認ください。");
        response.put("fieldErrors", fieldErrors);
        return response;
    }

    private String resolveFieldMessage(FieldError error, boolean english) {
        if ("Email".equals(error.getCode())) {
            return english
                    ? "Please enter a valid email address."
                    : "正しいメールアドレスを入力してください。";
        }

        if ("NotBlank".equals(error.getCode())) {
            return english ? "Please fill out this field." : "入力してください。";
        }

        return error.getDefaultMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    private ContactForm toContactForm(ContactInquiryEditForm editForm) {
        ContactForm contactForm = new ContactForm();
        contactForm.setInquiryType(editForm.getInquiryType());
        contactForm.setName(editForm.getName());
        contactForm.setEmail(editForm.getEmail());
        contactForm.setPhoneNumber(editForm.getPhoneNumber());
        contactForm.setPlan(editForm.getPlan());
        contactForm.setDate(editForm.getDate());
        contactForm.setContactMethod(editForm.getContactMethod());
        contactForm.setRequestType(editForm.getRequestType());
        contactForm.setMessage(editForm.getMessage());
        return contactForm;
    }
}
