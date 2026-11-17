package com.chalo.controller;

import com.chalo.dto.RegistrationForm;
import com.chalo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Pages for signing up and logging in.
 *
 * WHAT IS A CONTROLLER?
 *   A class that receives web requests. Each method is tied to a URL:
 *     @GetMapping("/register")  -> runs when the browser opens /register
 *     @PostMapping("/register") -> runs when a form is submitted to /register
 *   The returned String is the NAME OF A THYMELEAF TEMPLATE in src/main/resources/templates
 *   (e.g. "auth/register" -> templates/auth/register.html), or "redirect:/url" to send the
 *   browser to another URL.
 *   The "Model" is a bag of data handed to the template.
 *
 * URLs handled here:
 *   GET  /register     show sign-up form
 *   POST /register     create account
 *   GET  /login        show login form  (the POST is handled by Spring Security itself)
 *   GET  /admin/login  show admin login form
 */
@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    // Business logic for registration (injected by Spring)
    private final AuthenticationService authenticationService;

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * GET /register - shows an empty sign-up form.
     * "form" is the model attribute the template binds its inputs to (th:object="${form}").
     */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    /**
     * POST /register - the sign-up form was submitted.
     *
     * @Valid                 -> run the validation annotations of RegistrationForm.
     * @ModelAttribute("form")-> fill a RegistrationForm from the submitted fields.
     * BindingResult         -> holds the validation errors (must come right after the form).
     */
    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult,
            HttpServletRequest  request,
            HttpServletResponse response) {

        // Invalid input -> show the same page again; Thymeleaf prints the error messages.
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authenticationService.register(form, request, response);
        } catch (AuthenticationService.EmailAlreadyInUseException exception) {
            // Attach a custom error to the "email" field and show the form again
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    "This email is already registered. Please log in.");
            return "auth/register";
        }

        // Auto-login is complete — session already holds a valid SecurityContext
        return "redirect:/dashboard";
    }

    // ── User login ────────────────────────────────────────────────────────────
    // Spring Security processes POST /login automatically via UsernamePassword-
    // AuthenticationFilter. This controller only serves the GET (the login page).

    /**
     * GET /login - shows the login page.
     * Spring Security redirects to /login?error after a failed attempt and
     * to /login?logout after logging out; the optional request parameters below detect that.
     */
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    // ── Admin login ───────────────────────────────────────────────────────────
    // POST /admin/login is handled by Spring Security's admin filter chain
    // (SecurityConfig @Order(1)). This controller only serves the GET.

    /** GET /admin/login - shows the admin login page (templates/admin/login.html). */
    @GetMapping("/admin/login")
    public String showAdminLoginForm(
            @RequestParam(required = false) String error,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage",
                    "Invalid credentials or insufficient permissions.");
        }
        return "admin/login";
    }
}
