package com.chalo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Forces Spring Security 6's deferred CSRF token to load before Thymeleaf starts
 * writing the response body.
 *
 * BACKGROUND (what is CSRF?)
 *   CSRF = Cross-Site Request Forgery. Another website could trick a logged-in user's
 *   browser into submitting a form to our site. To stop that, every POST form must
 *   carry a secret, random "CSRF token". Thymeleaf adds it automatically when you use
 *   th:action="@{/something}" on a form.
 *
 * THE PROBLEM THIS CLASS SOLVES
 *   Without this, CsrfFilter stores a DeferredCsrfToken in the request attribute.
 *   When Thymeleaf's th:action triggers getToken() ~12KB into the template (after
 *   the CSS block has already filled Tomcat's 8KB response buffer and committed the
 *   response), saveToken() tries to add a Set-Cookie header to an already-committed
 *   response, causing an IllegalStateException that closes the connection mid-transfer.
 *   (Our pages have a big inline <style> block, so this really happens here.)
 *
 * THE FIX
 *   preHandle() runs before the controller method, before any response body is written.
 *   Touching the token here creates the session and sets JSESSIONID while the response
 *   is still uncommitted, so the later th:action call hits the cached token only.
 *
 * WHERE IT IS USED
 *   Registered in WebConfig.addInterceptors(...) so it runs for every MVC request.
 */
public class CsrfTokenHandlerInterceptor implements HandlerInterceptor {

    /**
     * Called by Spring MVC BEFORE the controller method runs.
     *
     * @param request  the incoming HTTP request (Spring Security stores the CSRF token inside it)
     * @param response the HTTP response being built (nothing has been written yet)
     * @param handler  the controller method that is about to run (not used here)
     * @return true = "continue processing the request normally"
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // Spring Security puts the (lazy) token in a request attribute named after the class.
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Calling getToken() makes the lazy token actually load now (creates session/cookie).
            csrfToken.getToken();
        }
        return true;
    }
}
