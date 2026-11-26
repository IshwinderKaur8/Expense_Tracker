package com.ishwinder.expense_tracker.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            LoginAttemptService attempts) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // the web page itself and its assets (the page shows the login form)
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/error")
                    .permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())                       // default deny
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .formLogin(form -> form
                .loginProcessingUrl("/api/v1/auth/login")
                .successHandler((request, response, authentication) -> {
                    attempts.recordSuccess(loginKey(request));
                    response.setStatus(HttpServletResponse.SC_OK);
                })
                .failureHandler((request, response, exception) -> {
                    attempts.recordFailure(loginKey(request));
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }))
            .addFilterBefore(new LoginRateLimitFilter(attempts), UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler((request, response, authentication) ->
                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static String loginKey(HttpServletRequest request) {
        return LoginAttemptService.key(request.getRemoteAddr(), request.getParameter("username"));
    }
}