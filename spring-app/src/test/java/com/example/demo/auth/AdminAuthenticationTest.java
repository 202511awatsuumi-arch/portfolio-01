package com.example.demo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.mapper.UserMapper;
import com.example.demo.form.UserEditForm;
import com.example.demo.model.UserAccount;
import com.example.demo.service.UserManagementService;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:user-management-test;DB_CLOSE_DELAY=-1;MODE=LEGACY",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true",
            "app.admin.username=admin",
            "app.admin.password=admin1234"
        })
class AdminAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserManagementService userManagementService;

    @Test
    void adminOnlyCanAccessAdminUsers() throws Exception {
        UserAccount user = createUser("staff", "USER", true, "password123");
        MockHttpSession session = login(user.getUsername(), "password123");

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/admin/login"));
    }

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void loginFailsWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    void duplicateUsernameIsRejectedOnCreate() throws Exception {
        MockHttpSession session = login("admin", "admin1234");

        mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .session(session)
                        .param("username", "admin")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void createdPasswordIsHashed() throws Exception {
        MockHttpSession session = login("admin", "admin1234");

        mockMvc.perform(post("/admin/users")
                        .with(csrf())
                        .session(session)
                        .param("username", "newuser")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "USER")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserAccount created = userMapper.selectByUsername("newuser");
        assertThat(created).isNotNull();
        assertThat(created.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", created.getPasswordHash())).isTrue();
    }

    @Test
    void editUpdatesRoleAndEnabled() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        UserAccount user = createUser("editor", "USER", true, "password123");

        mockMvc.perform(post("/admin/users/{id}", user.getId())
                        .with(csrf())
                        .session(session)
                        .param("id", String.valueOf(user.getId()))
                        .param("username", "editor")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserAccount updated = userMapper.selectById(user.getId());
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void editResetsPassword() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        UserAccount user = createUser("resetme", "USER", true, "password123");

        mockMvc.perform(post("/admin/users/{id}", user.getId())
                        .with(csrf())
                        .session(session)
                        .param("id", String.valueOf(user.getId()))
                        .param("username", "resetme")
                        .param("role", "USER")
                        .param("enabled", "true")
                        .param("password", "newpassword123")
                        .param("confirmPassword", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        UserAccount updated = userMapper.selectById(user.getId());
        assertThat(passwordEncoder.matches("newpassword123", updated.getPasswordHash())).isTrue();
    }

    @Test
    void editRejectsPasswordMismatch() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        UserAccount user = createUser("mismatch", "USER", true, "password123");
        String beforeHash = userMapper.selectById(user.getId()).getPasswordHash();

        mockMvc.perform(post("/admin/users/{id}", user.getId())
                        .with(csrf())
                        .session(session)
                        .param("id", String.valueOf(user.getId()))
                        .param("username", "mismatch")
                        .param("role", "USER")
                        .param("enabled", "true")
                        .param("password", "newpassword123")
                        .param("confirmPassword", "wrongconfirm"))
                .andExpect(status().isOk());

        UserAccount updated = userMapper.selectById(user.getId());
        assertThat(updated.getPasswordHash()).isEqualTo(beforeHash);
    }

    @Test
    void userCannotDeleteSelf() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        int beforeCount = userMapper.selectAll().size();

        mockMvc.perform(post("/admin/users/{id}/delete", userMapper.selectByUsername("admin").getId())
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(userMapper.selectAll()).hasSize(beforeCount);
    }

    @Test
    void adminCanDeleteAnotherAdminWhenMultipleEnabledAdminsExist() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        UserAccount otherAdmin = createUser("admin-delete-target", "ADMIN", true, "password123");

        mockMvc.perform(post("/admin/users/{id}/delete", otherAdmin.getId())
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        assertThat(userMapper.selectById(otherAdmin.getId())).isNull();
    }

    @Test
    void lastEnabledAdminCannotBeDeleted() {
        UserAccount admin = userMapper.selectByUsername("admin");

        assertThat(admin).isNotNull();
        assertThatThrownBy(() -> userManagementService.delete(admin.getId(), "operator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(UserManagementService.LAST_ADMIN_DELETE_FORBIDDEN_MESSAGE);

        assertThat(userMapper.selectById(admin.getId())).isNotNull();
    }

    @Test
    void adminRoleCanBeChangedWhenAnotherEnabledAdminExists() {
        UserAccount otherAdmin = createUser("admin-role-target", "ADMIN", true, "password123");
        UserEditForm form = editForm(otherAdmin.getId(), "USER", true);

        userManagementService.update(form);

        UserAccount updated = userMapper.selectById(otherAdmin.getId());
        assertThat(updated.getRole()).isEqualTo("USER");
        assertThat(updated.isEnabled()).isTrue();
    }

    @Test
    void lastEnabledAdminCannotBeChangedToUser() {
        UserAccount admin = userMapper.selectByUsername("admin");
        UserEditForm form = editForm(admin.getId(), "USER", true);

        assertThatThrownBy(() -> userManagementService.update(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(UserManagementService.LAST_ADMIN_ROLE_CHANGE_FORBIDDEN_MESSAGE);

        UserAccount updated = userMapper.selectById(admin.getId());
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.isEnabled()).isTrue();
    }

    @Test
    void adminCanBeDisabledWhenAnotherEnabledAdminExists() {
        UserAccount otherAdmin = createUser("admin-disable-target", "ADMIN", true, "password123");
        UserEditForm form = editForm(otherAdmin.getId(), "ADMIN", false);

        userManagementService.update(form);

        UserAccount updated = userMapper.selectById(otherAdmin.getId());
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    void lastEnabledAdminCannotBeDisabled() {
        UserAccount admin = userMapper.selectByUsername("admin");
        UserEditForm form = editForm(admin.getId(), "ADMIN", false);

        assertThatThrownBy(() -> userManagementService.update(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(UserManagementService.LAST_ADMIN_DISABLE_FORBIDDEN_MESSAGE);

        UserAccount updated = userMapper.selectById(admin.getId());
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.isEnabled()).isTrue();
    }

    @Test
    void deletingUserAlsoDeletesLinkedTodos() throws Exception {
        MockHttpSession session = login("admin", "admin1234");
        UserAccount user = createUser("todo-owner", "USER", true, "password123");
        jdbcTemplate.update(
                "INSERT INTO todos (user_id, title, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                user.getId(),
                "test todo");

        mockMvc.perform(post("/admin/users/{id}/delete", user.getId())
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        Integer todoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM todos WHERE user_id = ?",
                Integer.class,
                user.getId());
        assertThat(todoCount).isZero();
        assertThat(userMapper.selectById(user.getId())).isNull();
    }

    @Test
    void deleteMethodIsTransactional() throws Exception {
        Method method = UserManagementService.class.getMethod("delete", Long.class, String.class);
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void usersTableExists() throws Exception {
        try (Connection connection =
                        DriverManager.getConnection("jdbc:h2:mem:user-management-test;DB_CLOSE_DELAY=-1;MODE=LEGACY", "sa", "");
                ResultSet resultSet =
                        connection.getMetaData().getTables(null, null, "USERS", null)) {
            assertThat(resultSet.next()).isTrue();
        }
    }

    @Test
    void initialAdminCanLogin() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult loginResult =
                mockMvc.perform(post("/admin/login")
                                .with(csrf())
                                .param("username", username)
                                .param("password", password))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }

    private UserAccount createUser(String username, String role, boolean enabled, String rawPassword) {
        UserAccount user = new UserAccount();
        LocalDateTime now = LocalDateTime.now();
        user.setUsername(username);
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user;
    }

    private UserEditForm editForm(Long id, String role, boolean enabled) {
        UserEditForm form = new UserEditForm();
        form.setId(id);
        form.setRole(role);
        form.setEnabled(enabled);
        return form;
    }
}
