package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entity.ContactInquiry;
import com.example.demo.repository.ContactInquiryRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:contact-site-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false"
        })
class ContactSiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactInquiryRepository contactInquiryRepository;

    @BeforeEach
    void setUp() {
        contactInquiryRepository.deleteAll();
    }

    @Test
    void jpPageHasFirstRequestTypeCheckedByDefault() throws Exception {
        mockMvc.perform(get("/contact.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"予約希望\" checked")));
    }

    @Test
    void enPageHasFirstRequestTypeCheckedByDefault() throws Exception {
        mockMvc.perform(get("/en/contact.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Booking Request\" checked")));
    }

    @Test
    void savesIsoDateAsIs() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "スタンダードコース")
                        .param("date", "2026-04-29")
                        .param("contactMethod", "メール")
                        .param("requestType", "予約希望")
                        .param("message", "予約したいです"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getPreferredDate()).isEqualTo(LocalDate.of(2026, 4, 29));
    }

    @Test
    void normalizesUsDateBeforeSaving() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "04/25/2026")
                        .param("contactMethod", "Email")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getPreferredDate()).isEqualTo(LocalDate.of(2026, 4, 25));
    }

    @Test
    void rejectsUnsupportedDateFormat() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "25/04/2026")
                        .param("contactMethod", "Email")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.date").exists());

        assertThat(contactInquiryRepository.findAll()).isEmpty();
    }

    @Test
    void allowsBlankDateForStoreInquiry() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "store")
                        .param("name", "Hanako")
                        .param("email", "hanako@example.com")
                        .param("contactMethod", "メール")
                        .param("message", "営業時間を知りたいです"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getPreferredDate()).isNull();
    }

    @Test
    void jpAndEnFormsPersistTheSameNormalizedFormat() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "スタンダードコース")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "メール")
                        .param("requestType", "予約希望")
                        .param("message", "予約したいです"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "04/25/2026")
                        .param("contactMethod", "Email")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());

        assertThat(contactInquiryRepository.findAll())
                .extracting(ContactInquiry::getPreferredDate)
                .containsExactlyInAnyOrder(LocalDate.of(2026, 4, 25), LocalDate.of(2026, 4, 25));
    }

    @Test
    void allowsBlankRequestTypeForEnglishStoreInquiry() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "store")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("contactMethod", "Email")
                        .param("message", "Tell me your opening hours"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsEmptyRequestTypeForWorkshopApiCall() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "スタンダードコース")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "メール")
                        .param("message", "予約したいです"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.requestType").value("ご希望内容を1つ以上選択してください。"));
    }

    @Test
    void rejectsBlankContactMethodForApiCall() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactMethod").exists());
    }

    @Test
    void rejectsMissingPhoneNumberForJapanesePhoneContactApiCall() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "\u30b9\u30bf\u30f3\u30c0\u30fc\u30c9\u30b3\u30fc\u30b9")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "\u96fb\u8a71")
                        .param("requestType", "\u4e88\u7d04\u5e0c\u671b")
                        .param("message", "\u4e88\u7d04\u3057\u305f\u3044\u3067\u3059"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber")
                        .value("\u96fb\u8a71\u3067\u306e\u3054\u9023\u7d61\u3092\u5e0c\u671b\u3059\u308b\u5834\u5408\u306f\u96fb\u8a71\u756a\u53f7\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044\u3002"));
    }

    @Test
    void rejectsMissingPhoneNumberForEnglishPhoneContactApiCall() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "Phone")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber")
                        .value("Please enter your phone number if you prefer to be contacted by phone."));
    }

    @Test
    void allowsBlankPhoneNumberForJapaneseEmailContact() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "\u30b9\u30bf\u30f3\u30c0\u30fc\u30c9\u30b3\u30fc\u30b9")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "\u30e1\u30fc\u30eb")
                        .param("requestType", "\u4e88\u7d04\u5e0c\u671b")
                        .param("message", "\u4e88\u7d04\u3057\u305f\u3044\u3067\u3059"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsBlankPhoneNumberForEnglishEmailContact() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "Email")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());
    }

    @Test
    void savesPhoneNumberForJapanesePhoneContact() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "\u30b9\u30bf\u30f3\u30c0\u30fc\u30c9\u30b3\u30fc\u30b9")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "\u96fb\u8a71")
                        .param("phoneNumber", "090-1234-5678")
                        .param("requestType", "\u4e88\u7d04\u5e0c\u671b")
                        .param("message", "\u4e88\u7d04\u3057\u305f\u3044\u3067\u3059"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getPhoneNumber()).isEqualTo("090-1234-5678");
    }

    @Test
    void savesPhoneNumberForEnglishPhoneContact() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "Phone")
                        .param("phoneNumber", "+81 90 1234 5678")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getPhoneNumber()).isEqualTo("+81 90 1234 5678");
    }
}
