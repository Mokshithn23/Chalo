package com.chalo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ENTRY POINT of the whole application.
 *
 * WHAT IT DOES
 *   When you press "Run" in IntelliJ (or run `mvn spring-boot:run`), Java starts here
 *   because this class contains the main() method.
 *
 * WHAT @SpringBootApplication MEANS
 *   It is a shortcut for three annotations at once:
 *     1. @Configuration          -> this class can declare Spring beans.
 *     2. @EnableAutoConfiguration -> Spring Boot looks at the libraries in pom.xml
 *                                    (web, JPA, security, thymeleaf, mysql ...) and
 *                                    configures them automatically.
 *     3. @ComponentScan          -> Spring scans THIS package (com.chalo) and every
 *                                    sub-package (controller, service, repository,
 *                                    config ...) to find classes annotated with
 *                                    @Controller, @Service, @Component, @Configuration ...
 *                                    and creates objects ("beans") of them.
 *
 * STARTUP ORDER (simplified)
 *   1. Read src/main/resources/application.properties (database URL, password, port).
 *   2. Connect to MySQL and let Hibernate create/update the tables (ddl-auto=update).
 *   3. Create all beans (controllers, services, repositories, security config).
 *   4. Run every CommandLineRunner (see TagDataLoader -> inserts the interest tags).
 *   5. Start the embedded Tomcat web server on port 8080.
 */
@SpringBootApplication
public class ChalOApplication {

    /**
     * Standard Java main method. SpringApplication.run() boots the entire framework.
     *
     * @param args command-line arguments (not used in this project)
     */
    public static void main(String[] args) {
        SpringApplication.run(ChalOApplication.class, args);
    }
}
