package com.example.demo.controller;

import com.example.demo.form.ContactForm;
import com.example.demo.service.ContactInquiryService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

// Replaced by ContactSiteController to keep route behavior stable while removing mojibake-prone strings.
public class SiteController {

    private final ContactInquiryService contactInquiryService;

    public SiteController(ContactInquiryService contactInquiryService) {
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

    @GetMapping("/admin/contacts")
    public String adminContacts(Model model) {
        model.addAttribute("contacts", contactInquiryService.findAll());
        return "admin/contacts";
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
        validateWorkshopFields(contactForm, bindingResult, english);

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

    private void validateWorkshopFields(
            ContactForm contactForm, BindingResult bindingResult, boolean english) {
        if (!"workshop".equals(contactForm.getInquiryType())) {
            return;
        }

        if (isBlank(contactForm.getPlan())) {
            bindingResult.addError(
                    new FieldError(
                            "contactForm",
                            "plan",
                            english ? "Please select an option." : "選択してください。"));
        }

        if (isBlank(contactForm.getDate())) {
            bindingResult.addError(
                    new FieldError(
                            "contactForm",
                            "date",
                            english ? "Please fill out this field." : "入力してください。"));
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
}
