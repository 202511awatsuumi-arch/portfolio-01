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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:contact-site-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "app.admin.seed.enabled=false"
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
    void rootPathReturnsPortfolioTopPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<!DOCTYPE html>")));
    }

    @Test
    void jpPageUsesCodeValuesForContactMethodPlanAndRequestTypes() throws Exception {
        mockMvc.perform(get("/contact.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("value=\"EMAIL\" checked")))
                .andExpect(content().string(Matchers.containsString("value=\"PHONE\"")))
                .andExpect(content().string(Matchers.containsString("value=\"STANDARD\"")))
                .andExpect(content().string(Matchers.containsString("value=\"PREMIUM\"")))
                .andExpect(content().string(Matchers.containsString("value=\"CONSULT\"")))
                .andExpect(content().string(Matchers.containsString("value=\"BOOKING_REQUEST\" checked")))
                .andExpect(content().string(Matchers.containsString("value=\"AVAILABILITY_CHECK\"")))
                .andExpect(content().string(Matchers.containsString("value=\"PLAN_CONSULTATION\"")));
    }

    @Test
    void enPageUsesCodeValuesForContactMethodPlanAndRequestTypes() throws Exception {
        mockMvc.perform(get("/en/contact.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("value=\"EMAIL\" checked")))
                .andExpect(content().string(Matchers.containsString("value=\"PHONE\"")))
                .andExpect(content().string(Matchers.containsString("value=\"STANDARD\"")))
                .andExpect(content().string(Matchers.containsString("value=\"PREMIUM\"")))
                .andExpect(content().string(Matchers.containsString("value=\"CONSULT\"")))
                .andExpect(content().string(Matchers.containsString("value=\"BOOKING_REQUEST\" checked")))
                .andExpect(content().string(Matchers.containsString("value=\"AVAILABILITY_CHECK\"")))
                .andExpect(content().string(Matchers.containsString("value=\"PLAN_CONSULTATION\"")));
    }

    @Test
    void savesIsoDateAsIs() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-29")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
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
                        .param("plan", "STANDARD")
                        .param("date", "04/25/2026")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
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
                        .param("plan", "STANDARD")
                        .param("date", "25/04/2026")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
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
                        .param("contactMethod", "EMAIL")
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
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "予約したいです"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "04/25/2026")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
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
                        .param("contactMethod", "EMAIL")
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
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
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
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactMethod").exists());
    }

    @Test
    void rejectsInvalidContactMethodCode() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "メール")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "予約したいです"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.contactMethod").exists());
    }

    @Test
    void rejectsInvalidPlanCode() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "Standard Course")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.plan").exists());
    }

    @Test
    void rejectsInvalidRequestTypeCode() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "Booking Request")
                        .param("message", "Book me in"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.requestType").exists());
    }

    @Test
    void rejectsMissingPhoneNumberForPhoneContactApiCall() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "PHONE")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "予約したいです"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber")
                        .value("電話でのご連絡を希望する場合は電話番号を入力してください。"));
    }

    @Test
    void allowsBlankPhoneNumberForEmailContact() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "BOOKING_REQUEST")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());
    }

    @Test
    void savesCodeValuesForWorkshopInquiry() throws Exception {
        mockMvc.perform(post("/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Taro")
                        .param("email", "taro@example.com")
                        .param("plan", "PREMIUM")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "PHONE")
                        .param("phoneNumber", "090-1234-5678")
                        .param("requestType", "BOOKING_REQUEST", "PLAN_CONSULTATION")
                        .param("message", "予約したいです"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getContactMethod()).isEqualTo("PHONE");
        assertThat(inquiry.getPlan()).isEqualTo("PREMIUM");
        assertThat(inquiry.getRequestTypes()).isEqualTo("BOOKING_REQUEST, PLAN_CONSULTATION");
        assertThat(inquiry.getPhoneNumber()).isEqualTo("090-1234-5678");
    }

    @Test
    void savesCodeValuesForEnglishInquiry() throws Exception {
        mockMvc.perform(post("/en/contact")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("inquiryType", "workshop")
                        .param("name", "Alex")
                        .param("email", "alex@example.com")
                        .param("plan", "CONSULT")
                        .param("date", "2026-04-25")
                        .param("contactMethod", "EMAIL")
                        .param("requestType", "AVAILABILITY_CHECK")
                        .param("message", "Book me in"))
                .andExpect(status().isOk());

        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);
        assertThat(inquiry.getContactMethod()).isEqualTo("EMAIL");
        assertThat(inquiry.getPlan()).isEqualTo("CONSULT");
        assertThat(inquiry.getRequestTypes()).isEqualTo("AVAILABILITY_CHECK");
    }
}
