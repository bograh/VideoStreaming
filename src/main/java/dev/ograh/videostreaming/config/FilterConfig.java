package dev.ograh.videostreaming.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ograh.videostreaming.filter.RateLimitingFilter;
import dev.ograh.videostreaming.service.RateLimitService;
import dev.ograh.videostreaming.utils.UserHelper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public RateLimitingFilter rateLimitingFilter(
            RateLimitService rateLimitService, UserHelper userHelper, ObjectMapper objectMapper
    ) {
        return new RateLimitingFilter(rateLimitService, userHelper, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(1);
        return registrationBean;
    }

}