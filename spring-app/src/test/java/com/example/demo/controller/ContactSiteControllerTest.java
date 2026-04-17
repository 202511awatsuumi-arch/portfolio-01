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
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        contactInquiryRepository.deleteAll();
    }

    @Test
    void adminInquiriesUsesNewestFirstPaginationWithTenItemsPerPage() throws Exception {
        IntStream.rangeClosed(1, 12).forEach(this::saveInquiry);

        mockMvc.perform(get("/admin/inquiries"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("<th>No</th>")))
                .andExpect(content().string(Matchers.containsString(">1</td>")))
                .andExpect(content().string(Matchers.containsString("詳細")))
                .andExpect(content().string(Matchers.containsString("Sender 12")))
                .andExpect(content().string(Matchers.containsString("Sender 3")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Sender 2"))))
                .andExpect(content().string(Matchers.containsString("page=2")));

        mockMvc.perform(get("/admin/inquiries").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(">11</td>")))
                .andExpect(content().string(Matchers.containsString(">12</td>")))
                .andExpect(content().string(Matchers.containsString("Sender 2")))
                .andExpect(content().string(Matchers.containsString("Sender 1")))
                .andExpect(content().string(Matchers.not(Matchers.containsString("Sender 12"))));
    }

    @Test
    void adminInquiryDetailDisplaysSingleInquiry() throws Exception {
        saveInquiry(1);
        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);

        mockMvc.perform(get("/admin/inquiries/" + inquiry.getId()).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("お問い合わせ詳細")))
                .andExpect(content().string(Matchers.containsString(">1</dd>")))
                .andExpect(content().string(Matchers.containsString("Sender 1")))
                .andExpect(content().string(Matchers.containsString("sender1@example.com")))
                .andExpect(content().string(Matchers.containsString("/admin/inquiries?page=2")));
    }

    @Test
    void adminInquiryEditDisplaysExistingData() throws Exception {
        saveInquiry(1);
        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);

        mockMvc.perform(get("/admin/inquiries/" + inquiry.getId() + "/edit").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("お問い合わせ編集")))
                .andExpect(content().string(Matchers.containsString("Sender 1")))
                .andExpect(content().string(Matchers.containsString("sender1@example.com")))
                .andExpect(content().string(Matchers.containsString("value=\"2\"")));
    }

    @Test
    void adminInquiryUpdateSavesChangesAndRedirectsToDetail() throws Exception {
        saveInquiry(1);
        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);

        mockMvc.perform(post("/admin/inquiries/" + inquiry.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", inquiry.getId().toString())
                        .param("returnPage", "2")
                        .param("inquiryType", "workshop")
                        .param("name", "Updated Sender")
                        .param("email", "updated@example.com")
                        .param("phoneNumber", "090-9999-0000")
                        .param("plan", "PREMIUM")
                        .param("date", "2026-05-01")
                        .param("contactMethod", "PHONE")
                        .param("requestType", "BOOKING_REQUEST", "PLAN_CONSULTATION")
                        .param("message", "Updated message"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                        "/admin/inquiries/" + inquiry.getId() + "?page=2"));

        ContactInquiry updated = contactInquiryRepository.findById(inquiry.getId()).orElseThrow();
        assertThat(updated.getInquiryType()).isEqualTo("workshop");
        assertThat(updated.getName()).isEqualTo("Updated Sender");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getPhoneNumber()).isEqualTo("090-9999-0000");
        assertThat(updated.getPlan()).isEqualTo("PREMIUM");
        assertThat(updated.getPreferredDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(updated.getContactMethod()).isEqualTo("PHONE");
        assertThat(updated.getRequestTypes()).isEqualTo("BOOKING_REQUEST, PLAN_CONSULTATION");
        assertThat(updated.getMessage()).isEqualTo("Updated message");
    }

    @Test
    void adminInquiryUpdateRerendersEditWhenWorkshopRequestTypeIsMissing() throws Exception {
        saveInquiry(1);
        ContactInquiry inquiry = contactInquiryRepository.findAll().get(0);

        mockMvc.perform(post("/admin/inquiries/" + inquiry.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("id", inquiry.getId().toString())
                        .param("returnPage", "1")
                        .param("inquiryType", "workshop")
                        .param("name", "Updated Sender")
                        .param("email", "updated@example.com")
                        .param("plan", "STANDARD")
                        .param("date", "2026-05-01")
                        .param("contactMethod", "EMAIL")
                        .param("message", "Updated message"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("お問い合わせ編集")));
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

    private void saveInquiry(int number) {
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setInquiryType("store");
        inquiry.setName("Sender " + number);
        inquiry.setEmail("sender" + number + "@example.com");
        inquiry.setContactMethod("EMAIL");
        inquiry.setMessage("Message " + number);
        inquiry.setEnglishPage(false);
        ContactInquiry saved = contactInquiryRepository.save(inquiry);
        jdbcTemplate.update(
                "UPDATE contact_inquiries SET created_at = DATEADD('MINUTE', ?, CURRENT_TIMESTAMP) WHERE id = ?",
                number,
                saved.getId());
    }
}
