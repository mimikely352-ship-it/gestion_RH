package com.sirh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Point d'entrée. Étend {@link SpringBootServletInitializer} pour permettre le déploiement
 * en WAR sur un Tomcat externe (10.1+) sans web.xml : le conteneur détecte l'initializer via
 * le ServletContainerInitializer de Spring (META-INF/services), déclaré par spring-web.
 * Le main() reste utile pour lancer l'application en Tomcat embarqué pendant le développement
 * local (cible Ant "run").
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
