# Taco Cloud (Chapter 4)

This project is the Taco Cloud sample application from *Spring in Action*, Chapter 4. It demonstrates how to build a simple taco design and ordering system with Spring Boot, MVC controllers, Thymeleaf views, Spring Security, and Spring Data JPA backed by an embedded H2 database.

**Note:** This project has been upgraded to Spring Boot 3 and Spring Security 6.

## Prerequisites
- Java 17 or newer on the PATH (`java -version`)
- Apache Maven 3.5+ (`mvn -version`)

## Build and Run
```bash
mvn spring-boot:run
```
The application starts on `http://localhost:8080`. Use the web interface to design tacos, register users, and submit orders.

## Running Tests
```bash
mvn test
```
The project includes unit, integration, and Selenium-based UI tests.

## Project Highlights
- Spring Boot autoconfiguration with Actuator endpoints enabled for monitoring
- HTML views rendered with Thymeleaf templates
- Authentication and authorization via Spring Security, including test support
- Persistence layer implemented with Spring Data JPA using the in-memory H2 database
