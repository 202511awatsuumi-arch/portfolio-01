package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                "/",
                                                "/index.html",
                                                "/about.html",
                                                "/access.html",
                                                "/skills.html",
                                                "/works.html",
                                                "/contact.html",
                                                "/contact",
                                                "/en/**",
                                                "/css/**",
                                                "/js/**",
                                                "/images/**",
                                                "/webjars/**")
                                        .permitAll()
                                        .requestMatchers("/admin/login")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .formLogin(
                        form ->
                                form.loginPage("/admin/login")
                                        .loginProcessingUrl("/admin/login")
                                        .usernameParameter("username")
                                        .passwordParameter("password")
                                        .successHandler(
                                                (request, response, authentication) -> {
                                                    boolean isAdmin =
                                                            authentication.getAuthorities().stream()
                                                                    .anyMatch(
                                                                            authority ->
                                                                                    "ROLE_ADMIN".equals(
                                                                                            authority.getAuthority()));
                                                    response.sendRedirect(
                                                            isAdmin
                                                                    ? "/admin/users"
                                                                    : "/admin/inquiries");
                                                })
                                        .failureUrl("/admin/login?error"))
                .logout(
                        logout ->
                                logout.logoutUrl("/admin/logout")
                                        .logoutSuccessUrl("/admin/login?logout")
                                        .invalidateHttpSession(true)
                                        .deleteCookies("JSESSIONID"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
