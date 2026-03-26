document.addEventListener("DOMContentLoaded", () => {
  const menuButton = document.querySelector("[data-menu-button]");
  const siteNav = document.querySelector("[data-site-nav]");

  if (menuButton && siteNav) {
    const closeMenu = () => {
      menuButton.setAttribute("aria-expanded", "false");
      siteNav.classList.remove("is-open");
      document.body.classList.remove("is-locked");
    };

    const openMenu = () => {
      menuButton.setAttribute("aria-expanded", "true");
      siteNav.classList.add("is-open");
      document.body.classList.add("is-locked");
    };

    menuButton.addEventListener("click", () => {
      const expanded = menuButton.getAttribute("aria-expanded") === "true";
      if (expanded) {
        closeMenu();
      } else {
        openMenu();
      }
    });

    document.addEventListener("click", (event) => {
      if (
        siteNav.classList.contains("is-open") &&
        !siteNav.contains(event.target) &&
        !menuButton.contains(event.target)
      ) {
        closeMenu();
      }
    });

    siteNav.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        if (window.innerWidth < 1024) {
          closeMenu();
        }
      });
    });

    window.addEventListener("resize", () => {
      if (window.innerWidth >= 1024) {
        document.body.classList.remove("is-locked");
        siteNav.classList.remove("is-open");
        menuButton.setAttribute("aria-expanded", "false");
      }
    });
  }

  document.querySelectorAll('a[href^="#"]').forEach((link) => {
    link.addEventListener("click", (event) => {
      const href = link.getAttribute("href");
      if (!href || href === "#") {
        return;
      }

      const target = document.querySelector(href);
      if (!target) {
        return;
      }

      event.preventDefault();
      target.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  });

  document.querySelectorAll(".accordion__button").forEach((button) => {
    button.addEventListener("click", () => {
      const expanded = button.getAttribute("aria-expanded") === "true";
      button.setAttribute("aria-expanded", String(!expanded));
    });
  });

  const tabButtons = document.querySelectorAll(".tab-button");
  const faqPanels = document.querySelectorAll(".faq-panel");
  if (tabButtons.length && faqPanels.length) {
    tabButtons.forEach((button) => {
      button.addEventListener("click", () => {
        const targetId = button.dataset.tab;
        tabButtons.forEach((item) => item.setAttribute("aria-selected", "false"));
        button.setAttribute("aria-selected", "true");
        faqPanels.forEach((panel) => {
          panel.hidden = panel.id !== targetId;
        });
      });
    });
  }

  const contactForm = document.querySelector("[data-contact-form]");
  if (contactForm) {
    const status = contactForm.querySelector("[data-form-status]");
    const fields = Array.from(contactForm.querySelectorAll("[data-validate]"));

    const validators = {
      required: (value) => value.trim().length > 0,
      email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()),
      select: (value) => value.trim().length > 0 && value !== ""
    };

    const messages = {
      required: "入力してください。",
      email: "正しいメールアドレスを入力してください。",
      select: "選択してください。"
    };

    const setFieldState = (field, errorMessage) => {
      const error = contactForm.querySelector(`[data-error-for="${field.id}"]`);
      if (!error) return;
      error.textContent = errorMessage || "";
      field.classList.toggle("is-error", Boolean(errorMessage));
      field.setAttribute("aria-invalid", errorMessage ? "true" : "false");
    };

    const validateField = (field) => {
      const rules = field.dataset.validate.split("|");
      for (const rule of rules) {
        const validator = validators[rule];
        if (validator && !validator(field.value)) {
          setFieldState(field, messages[rule]);
          return false;
        }
      }
      setFieldState(field, "");
      return true;
    };

    fields.forEach((field) => {
      field.addEventListener("blur", () => validateField(field));
      field.addEventListener("input", () => {
        if (field.classList.contains("is-error")) {
          validateField(field);
        }
      });
      field.addEventListener("change", () => validateField(field));
    });

    contactForm.addEventListener("submit", (event) => {
      event.preventDefault();
      let isValid = true;
      fields.forEach((field) => {
        if (!validateField(field)) {
          isValid = false;
        }
      });

      if (!isValid) {
        if (status) {
          status.textContent = "未入力または入力内容に誤りがあります。ご確認ください。";
          status.classList.remove("is-success");
        }
        const firstError = contactForm.querySelector(".is-error");
        if (firstError) {
          firstError.focus();
        }
        return;
      }

      if (status) {
        status.textContent = "送信ありがとうございました。内容を確認のうえ、担当者よりご連絡します。";
        status.classList.add("is-success");
      }

      contactForm.reset();
      fields.forEach((field) => setFieldState(field, ""));
    });
  }
});
