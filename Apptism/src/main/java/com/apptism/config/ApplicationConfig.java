package com.apptism.config;

import com.apptism.ui.StageManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de Spring para Apptism.
 *
 * Aquí se registran los componentes que Spring no puede crear solo
 * con sus anotaciones habituales ({@code @Component}, {@code @Service}...).
 * En concreto, el {@link StageManager} necesita que le pasemos el contexto
 * de Spring a mano, así que lo creamos nosotros aquí.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Registra el {@link StageManager} como componente único en el contexto.
     *
     * El gestor de ventanas necesita el contexto de Spring para poder
     * crear los controladores con sus dependencias inyectadas. Spring
     * nos pasa ese contexto automáticamente como parámetro.
     *
     * @param context el contexto de Spring, inyectado automáticamente
     * @return la instancia única del {@link StageManager} para toda la app
     */
    @Bean
    public StageManager stageManager(ApplicationContext context) {
        return new StageManager(context);
    }
}
