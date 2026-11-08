package com.javarush.jira.common.internal.config;

import com.javarush.jira.login.AuthUser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.UrlFilenameViewController;

import java.time.Duration;
import java.util.Properties;

//@EnableWebMvc : http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-spring-mvc-auto-configuration
@EnableAutoConfiguration
@Configuration
@AllArgsConstructor
@Slf4j
public class MvcConfig implements WebMvcConfigurer {
    private final AppProperties appProperties;

    // Redirect models become query parameters, so they must never contain credentials.
    private final HandlerInterceptor authInterceptor = new HandlerInterceptor() {
        @Override
        public void postHandle(jakarta.servlet.http.HttpServletRequest request,
                               jakarta.servlet.http.HttpServletResponse response, Object handler,
                               org.springframework.web.servlet.ModelAndView view) {
            if (view == null || view.getView() instanceof org.springframework.web.servlet.view.RedirectView
                    || (view.getViewName() != null && view.getViewName().startsWith("redirect:"))) return;
            AuthUser authUser = AuthUser.safeGet();
            if (authUser != null) {
                view.addObject("authUser", authUser);
                var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof com.javarush.jira.login.internal.jwt.AccessAuthentication access) {
                    view.addObject("accessToken", access.token());
                }
            }
        }
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(authInterceptor).excludePathPatterns("/api/**");
    }

    //  http://www.codejava.net/frameworks/spring/spring-mvc-url-based-view-resolution-with-urlfilenameviewcontroller-example
    @Bean
    public SimpleUrlHandlerMapping getUrlHandlerMapping() {
        return new SimpleUrlHandlerMapping() {{
            setMappings(new Properties() {{
                put("/view/**", new UrlFilenameViewController());
            }});
            setOrder(0);
            setInterceptors(authInterceptor, localeChangeInterceptor());
        }};
    }

    //  https://springdoc.org/index.html#how-can-i-deploy-springdoc-openapi-ui-behind-a-reverse-proxy
    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeadersRegistration(
            ForwardedHeaderFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        var resolver = new org.springframework.web.servlet.i18n.CookieLocaleResolver("language");
        resolver.setDefaultLocale(java.util.Locale.forLanguageTag("ru"));
        return resolver;
    }

    @Bean
    public org.springframework.web.servlet.i18n.LocaleChangeInterceptor localeChangeInterceptor() {
        var interceptor = new org.springframework.web.servlet.i18n.LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("file:./resources/static/");
        registry.setOrder(Integer.MAX_VALUE);
    }

    @Bean
//    https://stackoverflow.com/a/46966350/548473
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
