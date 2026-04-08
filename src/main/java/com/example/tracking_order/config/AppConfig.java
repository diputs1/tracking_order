package com.example.tracking_order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AppConfig {
    // This class enables JPA Auditing for BaseEntity @CreatedDate and @LastModifiedDate
}
