package com.apptism.config;

import com.apptism.ui.StageManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public StageManager stageManager(ApplicationContext context) {
        return new StageManager(context);
    }
}
