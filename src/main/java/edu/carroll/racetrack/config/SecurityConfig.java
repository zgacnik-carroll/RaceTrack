package edu.carroll.racetrack.config;

import edu.carroll.racetrack.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

/**
 * Spring Security configuration for protecting all application routes.
 */
@Configuration
public class SecurityConfig {
    private static final String SESSION_EXPIRED_ATTRIBUTE = "sessionExpiredAfterLogin";
    private static final String LOGIN_REDIRECT_PATH = "/oauth2/authorization/okta";

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserService userService;

    /**
     * Creates the security configuration with the collaborators required for
     * OIDC logout and post-login authorization checks.
     *
     * @param clientRegistrationRepository OAuth client registrations used for OIDC logout
     * @param userService application-level authorization service
     */
    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                          UserService userService) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userService = userService;
    }

    /**
     * Builds a logout success handler that redirects to Okta's end_session endpoint,
     * terminating both the local session and the Okta session.
     *
     * @return configured OIDC logout success handler
     */
    @Bean
    LogoutSuccessHandler oidcLogoutSuccessHandler() {
        // Build Spring Security's OIDC-aware logout handler so logout terminates
        // the upstream Okta session instead of only clearing the local session.
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // After Okta finishes logout, send the browser back to the app's login page
        // with the logout flag expected by the UI.
        handler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
        return handler;
    }

    /**
     * Builds the authentication-success handler that performs the application's
     * second authorization gate after Okta login.
     *
     * @return success handler that redirects either to the app or the unauthorized page
     */
    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            // The app authorizes by local email presence after Okta authentication,
            // so reject successful Okta logins that are not in the RaceTrack user table.
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser && !userService.isAuthorizedEmail(oidcUser)) {
                response.sendRedirect("/unauthorized-user");
                return;
            }

            // If the previous session died before login completed, preserve that state
            // long enough to show the session-expired notice after reauthentication.
            HttpSession session = request.getSession(false);
            boolean sessionExpired = false;
            if (session != null) {
                sessionExpired = Boolean.TRUE.equals(session.getAttribute(SESSION_EXPIRED_ATTRIBUTE));
                session.removeAttribute(SESSION_EXPIRED_ATTRIBUTE);
            }

            // Successful logins return either to the normal home route or to the same
            // route with a session-expired notice when the prior session was stale.
            response.sendRedirect(sessionExpired ? "/?sessionExpired" : "/");
        };
    }

    /**
     * Defines the HTTP security rules for the application.
     *
     * @param http mutable HTTP security builder
     * @return configured security filter chain
     * @throws Exception when security configuration fails
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Require authentication for every route and allow the unauthorized page
                // only after a real login has already occurred.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/unauthorized-user").authenticated()
                        .anyRequest().authenticated()
                )
                // Send users through the Okta OIDC login flow and then apply the
                // app-specific post-login email authorization check.
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(authenticationSuccessHandler())
                        .loginPage("/oauth2/authorization/okta")
                )
                // Treat missing or stale HTTP sessions as a fresh login event by
                // clearing the old session cookie and redirecting back to Okta.
                .sessionManagement(session -> session
                        .invalidSessionStrategy((request, response) -> {
                            clearSessionCookie(request.getContextPath(), response::addCookie);
                            response.sendRedirect(LOGIN_REDIRECT_PATH);
                        })
                )
                // Reclassify CSRF failures caused by expired sessions into a relogin
                // flow while preserving normal 403 behavior for real access denial.
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isSessionExpiryFailure(accessDeniedException)) {
                                markSessionExpired(request.getSession(true));
                                response.sendRedirect(LOGIN_REDIRECT_PATH);
                                return;
                            }
                            response.sendError(403);
                        })
                )
                // Log out both the local Spring session and the upstream Okta session,
                // then return the user to the post-logout login page.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Returns whether the access-denied event was caused by an expired session
     * surfacing as a CSRF token failure instead of a true authorization failure.
     *
     * @param exception access-denied exception raised by Spring Security
     * @return {@code true} when the failure should trigger a relogin flow
     */
    private boolean isSessionExpiryFailure(AccessDeniedException exception) {
        // Spring surfaces expired-session CSRF issues as access denial, so detect
        // those cases explicitly and convert them into a relogin flow upstream.
        return exception instanceof InvalidCsrfTokenException
                || exception instanceof MissingCsrfTokenException;
    }

    /**
     * Marks the current session so the login-success handler can show a
     * session-expired message after the user authenticates again.
     *
     * @param session HTTP session to annotate
     */
    private void markSessionExpired(HttpSession session) {
        // Store a short-lived marker so the next successful login can explain why
        // the user was forced back through authentication.
        session.setAttribute(SESSION_EXPIRED_ATTRIBUTE, Boolean.TRUE);
    }

    /**
     * Clears the current session cookie on the active context path so the
     * browser stops presenting a stale session identifier on the next request.
     *
     * @param contextPath servlet context path used to scope the cookie
     * @param cookieConsumer callback used to add the clearing cookie to the response
     */
    private void clearSessionCookie(String contextPath, java.util.function.Consumer<Cookie> cookieConsumer) {
        // Overwrite the browser's session cookie at the active context path so
        // future requests do not keep sending a dead JSESSIONID value.
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath(contextPath == null || contextPath.isBlank() ? "/" : contextPath);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookieConsumer.accept(cookie);
    }
}

