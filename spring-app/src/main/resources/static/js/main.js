document.addEventListener("DOMContentLoaded", () => {
  const isEnglishPage = document.documentElement.lang === "en";
  const siteHeader = document.querySelector("[data-site-header]");
  const menuButton = document.querySelector("[data-menu-button]");
  const siteNav = document.querySelector("[data-site-nav]");

  const syncHeaderState = () => {
    if (!siteHeader) return;
    siteHeader.classList.toggle("is-scrolled", window.scrollY > 24);
  };

  syncHeaderState();
  window.addEventListener("scroll", syncHeaderState, { passive: true });

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
        closeMenu();
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

  const englishContactDate = document.querySelector('.page-contact input#date.input[name="date"]');
  if (
    isEnglishPage &&
    document.body.classList.contains("page-contact") &&
    englishContactDate &&
    englishContactDate.type === "text" &&
    typeof window.flatpickr === "function"
  ) {
    window.flatpickr(englishContactDate, {
      locale: {
        weekdays: {
          shorthand: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
          longhand: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
        },
        months: {
          shorthand: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
          longhand: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]
        },
        firstDayOfWeek: 0,
        rangeSeparator: " to ",
        weekAbbreviation: "Wk",
        scrollTitle: "Scroll to increment",
        toggleTitle: "Click to toggle",
        amPM: ["AM", "PM"],
        yearAriaLabel: "Year",
        monthAriaLabel: "Month",
        hourAriaLabel: "Hour",
        minuteAriaLabel: "Minute",
        time_24hr: false
      },
      dateFormat: "m/d/Y",
      ariaDateFormat: "F j, Y",
      allowInput: true,
      disableMobile: true,
      monthSelectorType: "static",
      prevArrow: "Prev",
      nextArrow: "Next"
    });
  }

  const contactForm = document.querySelector("[data-contact-form]");
  if (contactForm) {
    const status = contactForm.querySelector("[data-form-status]");
    const fields = Array.from(contactForm.querySelectorAll("[data-validate]"));
    const inquiryType = contactForm.querySelector("[data-inquiry-type]");
    const reservationSections = Array.from(contactForm.querySelectorAll("[data-reservation-only]"));
    const messageNote = contactForm.querySelector("#note-message");
    const contactMethodInputs = Array.from(contactForm.querySelectorAll('input[name="contactMethod"]'));
    const phoneNumberInput = contactForm.querySelector('#phoneNumber');
    const requestTypeInputs = Array.from(contactForm.querySelectorAll('input[name="requestType"]'));
    const requestTypeError = contactForm.querySelector('[data-error-for="requestType"]');
    const phoneNumberError = contactForm.querySelector('[data-error-for="phoneNumber"]');
    const requestTypeErrorText = isEnglishPage
      ? "Please select at least one request detail."
      : "ご希望内容を1つ以上選択してください。";
    const phoneNumberErrorText = isEnglishPage
      ? "Please enter your phone number if you prefer to be contacted by phone."
      : "電話でのご連絡を希望する場合は電話番号を入力してください。";

    const validators = {
      required: (value) => value.trim().length > 0,
      email: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()),
      select: (value) => value.trim().length > 0 && value !== ""
    };

    const messages = isEnglishPage
      ? {
          required: "Please fill out this field.",
          email: "Please enter a valid email address.",
          select: "Please select an option."
        }
      : {
          required: "入力してください。",
          email: "正しいメールアドレスを入力してください。",
          select: "選択してください。"
        };

    const invalidStatusText = isEnglishPage
      ? "Some required fields are missing or invalid. Please review the form."
      : "未入力または入力内容に誤りがあります。ご確認ください。";

    const successStatusText = isEnglishPage
      ? "Thank you for your message. We will review it and get back to you shortly."
      : "送信ありがとうございました。内容を確認のうえ、折り返しご連絡します。";

    const submitErrorText = isEnglishPage
      ? "We could not send your message right now. Please try again."
      : "現在送信できませんでした。時間をおいて再度お試しください。";

    const setFieldState = (field, errorMessage) => {
      const error = contactForm.querySelector(`[data-error-for="${field.id}"]`);
      if (!error) return;
      error.textContent = errorMessage || "";
      field.classList.toggle("is-error", Boolean(errorMessage));
      field.setAttribute("aria-invalid", errorMessage ? "true" : "false");
    };

    const clearStatus = () => {
      if (!status) return;
      status.textContent = "";
      status.classList.remove("is-success");
    };

    const applyServerErrors = (fieldErrors = {}) => {
      fields.forEach((field) => {
        const errorMessage = fieldErrors[field.name] || "";
        setFieldState(field, errorMessage);
      });

      if (requestTypeError) {
        requestTypeError.textContent = fieldErrors.requestType || "";
      }

      if (phoneNumberError) {
        phoneNumberError.textContent = fieldErrors.phoneNumber || "";
      }
    };

    const hasSelectedRequestType = () => requestTypeInputs.some((input) => input.checked);

    const setRequestTypeError = (errorMessage) => {
      if (!requestTypeError) return;
      requestTypeError.textContent = errorMessage || "";
    };

    const getSelectedContactMethod = () => {
      const selected = contactMethodInputs.find((input) => input.checked);
      return selected ? selected.value : "";
    };

    const setPhoneNumberError = (errorMessage) => {
      if (!phoneNumberError || !phoneNumberInput) return;
      phoneNumberError.textContent = errorMessage || "";
      phoneNumberInput.classList.toggle("is-error", Boolean(errorMessage));
      phoneNumberInput.setAttribute("aria-invalid", errorMessage ? "true" : "false");
    };

    const validatePhoneNumber = () => {
      const contactMethod = getSelectedContactMethod();
      const prefersPhone = contactMethod === "PHONE";
      if (!prefersPhone) {
        setPhoneNumberError("");
        return true;
      }

      if (phoneNumberInput && phoneNumberInput.value.trim().length > 0) {
        setPhoneNumberError("");
        return true;
      }

      setPhoneNumberError(phoneNumberErrorText);
      return false;
    };

    const ensureDefaultRequestType = () => {
      if (!hasSelectedRequestType() && requestTypeInputs.length > 0) {
        requestTypeInputs[0].checked = true;
      }
    };

    const validateRequestType = () => {
      const isWorkshop = !inquiryType || inquiryType.value === "workshop";
      if (!isWorkshop) {
        setRequestTypeError("");
        return true;
      }

      if (hasSelectedRequestType()) {
        setRequestTypeError("");
        return true;
      }

      setRequestTypeError(requestTypeErrorText);
      return false;
    };

    const normalizePreferredDate = (rawValue) => {
      if (!rawValue) {
        return "";
      }

      const value = rawValue.trim();

      if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return value;
      }

      const match = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
      if (match) {
        const [, month, day, year] = match;
        return `${year}-${month}-${day}`;
      }

      return value;
    };

    const validateField = (field) => {
      if (field.disabled) {
        setFieldState(field, "");
        return true;
      }

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

    const syncInquiryType = () => {
      const isWorkshop = !inquiryType || inquiryType.value === "workshop";

      reservationSections.forEach((section) => {
        section.hidden = !isWorkshop;
        section.setAttribute("aria-hidden", String(!isWorkshop));

        section.querySelectorAll("input, select, textarea").forEach((field) => {
          field.disabled = !isWorkshop;
          if (!isWorkshop && field.matches("[data-validate]")) {
            setFieldState(field, "");
          }
        });
      });

      if (messageNote) {
        messageNote.textContent = isWorkshop
          ? messageNote.dataset.noteWorkshop || ""
          : messageNote.dataset.noteStore || "";
      }

      if (isWorkshop) {
        ensureDefaultRequestType();
        validateRequestType();
      } else {
        setRequestTypeError("");
      }
    };

    fields.forEach((field) => {
      field.addEventListener("blur", () => validateField(field));
      field.addEventListener("input", () => {
        if (field.classList.contains("is-error")) {
          validateField(field);
        }
        clearStatus();
      });
      field.addEventListener("change", () => {
        validateField(field);
        clearStatus();
      });
    });

    requestTypeInputs.forEach((input) => {
      input.addEventListener("change", () => {
        validateRequestType();
        clearStatus();
      });
    });

    contactMethodInputs.forEach((input) => {
      input.addEventListener("change", () => {
        validatePhoneNumber();
        clearStatus();
      });
    });

    if (phoneNumberInput) {
      phoneNumberInput.addEventListener("input", () => {
        validatePhoneNumber();
        clearStatus();
      });
    }

    if (inquiryType) {
      inquiryType.addEventListener("change", syncInquiryType);
      syncInquiryType();
    }

    contactForm.addEventListener("submit", async (event) => {
      event.preventDefault();
      let isValid = true;

      fields.forEach((field) => {
        if (!field.disabled && !validateField(field)) {
          isValid = false;
        }
      });

      if (!validateRequestType()) {
        isValid = false;
      }

      if (!validatePhoneNumber()) {
        isValid = false;
      }

      if (!isValid) {
        if (status) {
          status.textContent = invalidStatusText;
          status.classList.remove("is-success");
        }
        const firstError = contactForm.querySelector(".is-error");
        if (firstError) {
          firstError.focus();
        }
        return;
      }

      try {
        const formData = new FormData(contactForm);
        if (formData.has("date")) {
          formData.set("date", normalizePreferredDate(String(formData.get("date") || "")));
        }

        const response = await fetch(contactForm.action, {
          method: contactForm.method || "POST",
          body: formData,
          headers: {
            "X-Requested-With": "XMLHttpRequest"
          }
        });

        const result = await response.json();

        if (!response.ok) {
          applyServerErrors(result.fieldErrors);
          if (status) {
            status.textContent = result.message || invalidStatusText;
            status.classList.remove("is-success");
          }
          const firstError = contactForm.querySelector(".is-error");
          if (firstError) {
            firstError.focus();
          }
          return;
        }

        if (status) {
          status.textContent = result.message || successStatusText;
          status.classList.add("is-success");
        }

        contactForm.reset();
        fields.forEach((field) => setFieldState(field, ""));
        setRequestTypeError("");
        setPhoneNumberError("");
        syncInquiryType();
      } catch (error) {
        if (status) {
          status.textContent = submitErrorText;
          status.classList.remove("is-success");
        }
      }
    });
  }
});
