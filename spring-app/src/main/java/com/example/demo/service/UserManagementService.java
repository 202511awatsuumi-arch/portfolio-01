package com.example.demo.service;

import com.example.demo.form.UserCreateForm;
import com.example.demo.form.UserEditForm;
import com.example.demo.mapper.TodoMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "ADMIN");
    public static final String USER_NOT_FOUND_MESSAGE = "ユーザーが見つかりません";
    public static final String SELF_DELETE_FORBIDDEN_MESSAGE = "自分自身は削除できません";
    public static final String LAST_ADMIN_DELETE_FORBIDDEN_MESSAGE = "最後の管理者は削除できません";
    public static final String LAST_ADMIN_ROLE_CHANGE_FORBIDDEN_MESSAGE = "最後の管理者の権限は変更できません";
    public static final String LAST_ADMIN_DISABLE_FORBIDDEN_MESSAGE = "最後の管理者を無効化できません";

    private final UserMapper userMapper;
    private final TodoMapper todoMapper;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(
            UserMapper userMapper, TodoMapper todoMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.todoMapper = todoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAccount> findAll() {
        return userMapper.selectAll();
    }

    public UserAccount findById(Long id) {
        return userMapper.selectById(id);
    }

    public UserAccount findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public void create(UserCreateForm form) {
        UserAccount user = new UserAccount();
        LocalDateTime now = LocalDateTime.now();
        user.setUsername(form.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
    }

    public void update(UserEditForm form) {
        UserAccount existing = userMapper.selectById(form.getId());
        if (existing == null) {
            throw new IllegalArgumentException(USER_NOT_FOUND_MESSAGE);
        }

        if (isLastEnabledAdmin(existing)) {
            if (!"ADMIN".equals(form.getRole())) {
                throw new IllegalArgumentException(LAST_ADMIN_ROLE_CHANGE_FORBIDDEN_MESSAGE);
            }
            if (!form.isEnabled()) {
                throw new IllegalArgumentException(LAST_ADMIN_DISABLE_FORBIDDEN_MESSAGE);
            }
        }

        existing.setRole(form.getRole());
        existing.setEnabled(form.isEnabled());
        existing.setUpdatedAt(LocalDateTime.now());
        userMapper.update(existing);

        if (hasText(form.getPassword())) {
            userMapper.updatePassword(existing.getId(), passwordEncoder.encode(form.getPassword()));
        }
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        UserAccount existing = userMapper.selectById(id);
        if (existing == null) {
            return;
        }

        if (existing.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException(SELF_DELETE_FORBIDDEN_MESSAGE);
        }

        if (isLastEnabledAdmin(existing)) {
            throw new IllegalArgumentException(LAST_ADMIN_DELETE_FORBIDDEN_MESSAGE);
        }

        todoMapper.deleteByUserId(id);
        userMapper.delete(id);
    }

    public boolean isDuplicateUsername(String username) {
        return userMapper.selectByUsername(username.trim()) != null;
    }

    public boolean isValidRole(String role) {
        return ALLOWED_ROLES.contains(role);
    }

    public boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isLastEnabledAdmin(UserAccount user) {
        return user.isEnabled()
                && "ADMIN".equals(user.getRole())
                && countEnabledAdmins() == 1;
    }

    private long countEnabledAdmins() {
        return userMapper.selectAll().stream()
                .filter(UserAccount::isEnabled)
                .filter(account -> "ADMIN".equals(account.getRole()))
                .count();
    }
}
