package com.example.demo.controller;

import com.example.demo.form.UserCreateForm;
import com.example.demo.form.UserEditForm;
import com.example.demo.model.UserAccount;
import com.example.demo.service.UserManagementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        int pageSize = 10;
        int currentPage = Math.max(page, 1);
        long totalUsers = userManagementService.countAllUsers();
        int totalPages = Math.max((int) Math.ceil((double) totalUsers / pageSize), 1);

        model.addAttribute("users", userManagementService.findPage(currentPage - 1, pageSize));
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hasPrevious", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("userCreateForm")) {
            model.addAttribute("userCreateForm", new UserCreateForm());
        }
        return "admin/users/new";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("userCreateForm") UserCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        validateCreateForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("userCreateForm", form);
            return "admin/users/new";
        }

        userManagementService.create(form);
        redirectAttributes.addFlashAttribute("successMessage", "ユーザーを登録しました");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        UserAccount user = userManagementService.findById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }

        if (!model.containsAttribute("userEditForm")) {
            UserEditForm form = new UserEditForm();
            form.setId(user.getId());
            form.setUsername(user.getUsername());
            form.setRole(user.getRole());
            form.setEnabled(user.isEnabled());
            model.addAttribute("userEditForm", form);
        }

        model.addAttribute("targetUser", user);
        return "admin/users/edit";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("userEditForm") UserEditForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        UserAccount user = userManagementService.findById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }

        form.setId(id);
        form.setUsername(user.getUsername());
        validateEditForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("targetUser", user);
            return "admin/users/edit";
        }

        try {
            userManagementService.update(form);
        } catch (IllegalArgumentException ex) {
            rejectEditOperationError(ex, bindingResult);
            model.addAttribute("targetUser", user);
            return "admin/users/edit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "ユーザーを更新しました");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            userManagementService.delete(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "ユーザーを削除しました");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void validateCreateForm(UserCreateForm form, BindingResult bindingResult) {
        if (!userManagementService.passwordsMatch(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "パスワードが一致しません");
        }
        if (!userManagementService.isValidRole(form.getRole())) {
            bindingResult.rejectValue("role", "invalid", "ロールが不正です");
        }
        if (!bindingResult.hasFieldErrors("username")
                && userManagementService.isDuplicateUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "そのユーザー名は既に使用されています");
        }
    }

    private void validateEditForm(UserEditForm form, BindingResult bindingResult) {
        if (!userManagementService.isValidRole(form.getRole())) {
            bindingResult.rejectValue("role", "invalid", "ロールが不正です");
        }

        boolean hasPassword = userManagementService.hasText(form.getPassword());
        boolean hasConfirmPassword = userManagementService.hasText(form.getConfirmPassword());
        if (hasPassword || hasConfirmPassword) {
            if (hasPassword && form.getPassword().length() < 8) {
                bindingResult.rejectValue("password", "length", "パスワードは8文字以上で入力してください");
            }
            if (!userManagementService.passwordsMatch(form.getPassword(), form.getConfirmPassword())) {
                bindingResult.rejectValue("confirmPassword", "mismatch", "パスワードが一致しません");
            }
        }
    }

    private void rejectEditOperationError(
            IllegalArgumentException ex, BindingResult bindingResult) {
        if (UserManagementService.LAST_ADMIN_ROLE_CHANGE_FORBIDDEN_MESSAGE.equals(ex.getMessage())) {
            bindingResult.rejectValue("role", "lastAdminRole", ex.getMessage());
            return;
        }
        if (UserManagementService.LAST_ADMIN_DISABLE_FORBIDDEN_MESSAGE.equals(ex.getMessage())) {
            bindingResult.rejectValue("enabled", "lastAdminDisable", ex.getMessage());
            return;
        }
        bindingResult.reject("updateError", ex.getMessage());
    }
}
