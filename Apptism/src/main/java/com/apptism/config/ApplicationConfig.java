package com.apptism.config;

import com.apptism.ui.StageManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración central de Spring para Apptism.
 *
 * <p>Define los beans de infraestructura de la aplicación que no pueden
 * ser gestionados automáticamente por Spring mediante anotaciones
 * de estereotipo ({@code @Component}, {@code @Service}, etc.).
 *
 * <p>En particular, registra el {@link StageManager} como bean de Spring
 * para que pueda ser inyectado con {@code @Autowired} en los controladores
 * JavaFX sin necesidad de instanciarlo manualmente.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Registra el {@link StageManager} como bean singleton en el contexto de Spring.
     *
     * <p>El {@link StageManager} necesita el {@link ApplicationContext} para
     * poder instanciar los controladores JavaFX a través de Spring (de modo que
     * éstos tengan sus dependencias inyectadas). Por eso recibe el contexto
     * como parámetro, que Spring inyecta automáticamente.
     *
     * @param context contexto de Spring Boot, inyectado automáticamente
     * @return instancia única de {@link StageManager} para toda la aplicación
     */
    @Bean
    public StageManager stageManager(ApplicationContext context) {
        return new StageManager(context);
    }
}