package com.apptism.config;

import com.apptism.ui.StageManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de Spring para Apptism.
 *
 * <p>Registra los componentes que Spring no puede crear automáticamente
 * con sus anotaciones habituales ({@code @Component}, {@code @Service}…).
 * En concreto, el {@link StageManager} necesita recibir el contexto de Spring
 * en su constructor, por lo que se instancia aquí manualmente.</p>
 */

@Configuration
public class ApplicationConfig {

    /**
     * Registra el {@link StageManager} como bean único en el contexto de Spring.
     *
     * <p>El gestor de ventanas necesita el contexto de Spring para poder
     * crear los controladores con sus dependencias inyectadas. Spring
     * inyecta ese contexto automáticamente como parámetro del método.</p>
     *
     * @param context el contexto de Spring activo, inyectado automáticamente
     * @return la instancia única del {@link StageManager} para toda la aplicación
     */

    @Bean
    public StageManager stageManager(ApplicationContext context) {
        return new StageManager(context);
    }
}
