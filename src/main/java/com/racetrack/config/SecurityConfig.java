package com.racetrack.config;

import com.racetrack.service.UserService;
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
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
        return handler;
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Object principal = authentication.getPrincipal();
            if (principal instanceof OidcUser oidcUser && !userService.isAuthorizedEmail(oidcUser)) {
                response.sendRedirect("/unauthorized-user");
                return;
            }

            HttpSession session = request.getSession(false);
            boolean sessionExpired = false;
            if (session != null) {
                sessionExpired = Boolean.TRUE.equals(session.getAttribute(SESSION_EXPIRED_ATTRIBUTE));
                session.removeAttribute(SESSION_EXPIRED_ATTRIBUTE);
            }

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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/unauthorized-user").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(authenticationSuccessHandler())
                        .loginPage("/oauth2/authorization/okta")
                )
                .sessionManagement(session -> session
                        .invalidSessionStrategy((request, response) -> {
                            clearSessionCookie(request.getContextPath(), response::addCookie);
                            response.sendRedirect(LOGIN_REDIRECT_PATH);
                        })
                )
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
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    private boolean isSessionExpiryFailure(AccessDeniedException exception) {
        return exception instanceof InvalidCsrfTokenException
                || exception instanceof MissingCsrfTokenException;
    }

    private void markSessionExpired(HttpSession session) {
        session.setAttribute(SESSION_EXPIRED_ATTRIBUTE, Boolean.TRUE);
    }

    private void clearSessionCookie(String contextPath, java.util.function.Consumer<Cookie> cookieConsumer) {
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath(contextPath == null || contextPath.isBlank() ? "/" : contextPath);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookieConsumer.accept(cookie);
    }
}
