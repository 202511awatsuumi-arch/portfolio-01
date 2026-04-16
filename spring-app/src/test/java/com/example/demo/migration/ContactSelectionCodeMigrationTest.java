package com.example.demo.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class ContactSelectionCodeMigrationTest {

    @Test
    void migratesLegacyDisplayValuesToCodeValuesWithoutChangingPhoneNumber() throws Exception {
        String url = "jdbc:h2:mem:contact-selection-migration;DB_CLOSE_DELAY=-1;MODE=LEGACY";

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("3")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO contact_inquiries "
                            + "(inquiry_type, name, email, phone_number, plan, preferred_date, contact_method, request_types, message, english_page, created_at) "
                            + "VALUES "
                            + "('workshop', 'Taro', 'taro@example.com', '090-1111-2222', 'スタンダードコース', DATE '2026-04-25', 'メール', '予約希望,空き状況確認', 'jp', FALSE, CURRENT_TIMESTAMP), "
                            + "('workshop', 'Alex', 'alex@example.com', '+81 90 3333 4444', 'I''d like to decide after consultation', DATE '2026-04-26', 'Phone', 'Booking Request,Plan Consultation', 'en', TRUE, CURRENT_TIMESTAMP)");
        }

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            ResultSet resultSet =
                    statement.executeQuery(
                            "SELECT contact_method, plan, request_types, phone_number "
                                    + "FROM contact_inquiries ORDER BY email");

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("contact_method")).isEqualTo("PHONE");
            assertThat(resultSet.getString("plan")).isEqualTo("CONSULT");
            assertThat(resultSet.getString("request_types"))
                    .isEqualTo("BOOKING_REQUEST,PLAN_CONSULTATION");
            assertThat(resultSet.getString("phone_number")).isEqualTo("+81 90 3333 4444");

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("contact_method")).isEqualTo("EMAIL");
            assertThat(resultSet.getString("plan")).isEqualTo("STANDARD");
            assertThat(resultSet.getString("request_types"))
                    .isEqualTo("BOOKING_REQUEST,AVAILABILITY_CHECK");
            assertThat(resultSet.getString("phone_number")).isEqualTo("090-1111-2222");

            assertThat(resultSet.next()).isFalse();
        }
    }
}
