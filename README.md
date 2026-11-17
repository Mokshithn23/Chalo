# ChalO — Discover Experiences. Find Your Crew.

ChalO is an experience-first adventure discovery platform built with **Java and Spring Boot**. It helps people discover trips and outdoor activities, publish their own adventures, request to join other hosts, and communicate with accepted crew members.

Instead of organizing discovery around rigid travel categories, ChalO uses interest tags such as trekking, camping, photography, road trips, beaches, cycling, and more.

> **Project status:** Academic / portfolio project. The repository contains the application source and local-development configuration template. See [Current scope and limitations](#current-scope-and-limitations) before deploying it publicly.

---

## Table of Contents

- [Features](#features)
- [Screens and user journey](#screens-and-user-journey)
- [Technology stack](#technology-stack)
- [Application architecture](#application-architecture)
- [Database design](#database-design)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Running the application](#running-the-application)
- [Project structure](#project-structure)
- [Core workflows](#core-workflows)
- [Security notes](#security-notes)
- [Current scope and limitations](#current-scope-and-limitations)
- [Future improvements](#future-improvements)
## Features

### Account and profile management

- User registration and login.
- Profile page with editable profile information and bio.
- Password-based authentication managed by Spring Security.
- Role-aware access for regular users and administrator accounts.

### Adventure hosting

- Create and publish an adventure with a title, description, date, location, participant limit, and interest tags.
- Add photo URLs to an adventure gallery and select a cover image.
- View adventure details and host information.
- Manage adventures from the user's own adventure area.
- Cancel an adventure.

### Discover and explore

- Browse published adventures.
- Filter discovery using interest tags and location search.
- View adventure dates, locations, descriptions, photos, and available information before requesting to join.
- Explore activities across multiple interests rather than a single fixed category.

### Join requests and crew management

- Send a join request to an adventure host.
- Hosts can review incoming requests and accept or reject them.
- View crews associated with adventures and accepted participation.
- Access to an adventure's crew chat is restricted to its host and users with an accepted join request.

### Crew chat and notifications

- Send messages in an adventure's crew chat.
- Messages are persisted in the database.
- In-app notifications keep users informed about relevant activity, including join-request updates.
- Mark notifications as read or mark all as read.

## Screens and user journey

A typical journey through ChalO looks like this:

1. **Register** and create a user account.
2. **Explore** adventures using interests and location.
3. Open an adventure to review its details and host information.
4. **Request to join** and wait for the host's decision.
5. The host reviews the request and accepts or rejects it.
6. Once accepted, the participant can access the adventure's **crew chat** with the host and other accepted participants.
7. Manage hosted adventures, joined crews, profile details, and notifications from the application.

### Screenshots

Add screenshots of your running application here. Suggested captures:

| Screen | Suggested file |
|---|---|
| Dashboard | `docs/screenshots/dashboard.png` |
| Explore adventures | `docs/screenshots/explore.png` |
| Adventure details | `docs/screenshots/adventure-details.png` |
| Create adventure | `docs/screenshots/create-adventure.png` |
| Join requests | `docs/screenshots/join-requests.png` |
| Crew chat | `docs/screenshots/crew-chat.png` |
| User profile | `docs/screenshots/profile.png` |

After adding the images, you can embed them here using standard Markdown image syntax.

## Technology stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Application framework | Spring Boot 3.4.5 |
| Web layer | Spring MVC |
| Server-side views | Thymeleaf |
| Frontend | HTML, CSS, Bootstrap, JavaScript |
| Authentication and authorization | Spring Security |
| ORM / persistence | Spring Data JPA and Hibernate |
| Database | MySQL |
| Build and dependency management | Apache Maven |
| Validation | Jakarta Bean Validation / Spring Boot Validation |
| Boilerplate reduction | Lombok |
| Development support | Spring Boot DevTools |
| Testing dependencies | Spring Boot Test and Spring Security Test |

## Application architecture

ChalO follows a layered Spring Boot MVC structure:

```text
Browser
   │
   ▼
Spring MVC Controllers
   │
   ▼
Service Layer
   │
   ▼
Spring Data JPA Repositories
   │
   ▼
MySQL Database

Controllers ──► Thymeleaf Templates ──► HTML response
```

- **Controllers** handle incoming HTTP requests, coordinate application operations, and prepare model data for views.
- **Services** contain application logic such as authentication, notifications, and crew-chat operations.
- **Repositories** provide database access through Spring Data JPA.
- **Entities** represent persistent domain objects and their relationships.
- **DTOs** carry and validate form data.
- **Configuration and security classes** configure Spring Security, web behavior, CSRF handling, and initial tag data.
- **Thymeleaf templates** render the user interface on the server.

For a more detailed technical view, see [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Database design

The main domain entities include:

| Entity | Purpose |
|---|---|
| `User` | Registered user, profile, credentials, and role information |
| `Adventure` | Adventure details, host, date, location, capacity, status, and tags |
| `Tag` | Interest labels used to classify and discover adventures |
| `AdventurePhoto` | Photo entries associated with an adventure |
| `JoinRequest` | A user's request to participate in an adventure and its status |
| `Chat` | Crew conversation associated with an adventure |
| `Message` | A message sent in a crew chat |
| `Notification` | In-app notification for a user |
| `ChatParticipant` | Chat-participant relationship model |

Important relationships include:

- One user can host multiple adventures.
- An adventure can have multiple interest tags and photos.
- An adventure can receive multiple join requests.
- An adventure has a crew chat when one is created.
- A chat contains messages sent by users.
- Users can receive multiple notifications.

### Entity relationship diagram

The repository includes an ER diagram under `docs/`:

![ChalO entity relationship diagram](docs/er-diagram.png)

Alternative formats: [`SVG`](docs/er-diagram.svg) · [`DOT source`](docs/er-diagram.dot)

## Getting started

### Prerequisites

Install the following before running the application:

- **JDK 21**
- **MySQL Server 8.x** (or a compatible MySQL installation)
- **Apache Maven 3.8+** (or use the Maven wrapper if one is added to the repository)
- An IDE such as IntelliJ IDEA or Eclipse (optional)

Check your Java and Maven installations:

```bash
java -version
mvn -version
```

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/<your-repository>.git
cd <your-repository>
```

Replace the URL and directory with your GitHub repository details.

### 2. Create the database configuration

Copy the example configuration:

**Windows PowerShell**

```powershell
Copy-Item src/main/resources/application.properties.example src/main/resources/application.properties
```

**macOS / Linux**

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Open `src/main/resources/application.properties` and enter the credentials for your local MySQL server. See [Configuration](#configuration).

### 3. Start MySQL

Make sure your MySQL server is running. The configured JDBC URL creates the `chalo` database automatically if it does not already exist, provided the configured database user has permission to create it.

### 4. Build and run

From the directory containing `pom.xml`:

```bash
mvn clean package
mvn spring-boot:run
```

Alternatively, run `ChalOApplication.java` from your IDE.

### 5. Open ChalO

Visit:

```text
http://localhost:8080
```

The application entry route directs unauthenticated visitors to the login flow. Register an account to begin exploring and hosting adventures.

## Configuration

The committed `application.properties.example` is the configuration template. A local `application.properties` is intentionally excluded from Git so that database credentials are not committed.

Example configuration:

```properties
spring.application.name=chalO

spring.datasource.url=jdbc:mysql://localhost:3306/chalo?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.thymeleaf.cache=false
server.port=8080
```

**Important:** use your own local database username and password. Never commit `application.properties`, production credentials, API keys, or other secrets. For production, use environment variables or a secrets manager and review the Hibernate schema-management setting before deployment.

## Running the application

Useful Maven commands:

| Command | Description |
|---|---|
| `mvn clean` | Remove generated build output |
| `mvn compile` | Compile application source |
| `mvn test` | Run automated tests |
| `mvn package` | Build the application JAR |
| `mvn spring-boot:run` | Run the application using the Spring Boot Maven plugin |

The packaged JAR is generated under `target/` after a successful package build. Run it with:

```bash
java -jar target/chalo-0.0.1-SNAPSHOT.jar
```

## Project structure

```text
chalO/
├── docs/
│   ├── er-diagram.dot
│   ├── er-diagram.png
│   └── er-diagram.svg
├── src/
│   ├── main/
│   │   ├── java/com/chalo/
│   │   │   ├── config/        # Spring configuration and startup data
│   │   │   ├── controller/    # MVC controllers
│   │   │   ├── dto/           # Form and request DTOs
│   │   │   ├── model/         # JPA entities and enums
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   ├── security/      # User-details integration
│   │   │   ├── service/       # Application services
│   │   │   └── ChalOApplication.java
│   │   └── resources/
│   │       ├── templates/     # Thymeleaf pages
│   │       ├── application.properties.example
│   │       └── schema.sql
│   └── test/                  # Automated tests, when present
├── .gitignore
├── ARCHITECTURE.md
├── pom.xml
└── README.md
```

## Core workflows

### Adventure discovery

1. A user opens the Explore page.
2. The user browses or filters adventures using available tags and location criteria.
3. The application retrieves matching published adventures from the database.
4. The user opens an adventure to view its details.

### Join request and approval

1. An interested user submits a join request for an adventure.
2. The host views the incoming request list.
3. The host accepts or rejects the request.
4. The application updates the request status and creates relevant in-app notifications.
5. Accepted participants gain access to the adventure's crew chat.

### Crew chat

1. The host or an accepted participant opens the adventure chat.
2. The application checks whether the current user is allowed to access it.
3. A chat is created on first access if one does not already exist.
4. Messages are saved to MySQL and displayed in the chat interface.

## Security notes

- Spring Security protects authenticated application areas and applies role-based authorization.
- Passwords are stored as BCrypt hashes rather than plain text.
- CSRF protection is enabled for form-based state-changing requests.
- Crew chat access is checked against the adventure host / accepted join-request relationship.
- Local secrets belong in the ignored `application.properties`, not in source control.

Before a public deployment, configure HTTPS, secure database credentials, production logging, file-upload restrictions, backups, and appropriate database permissions. Review all authorization rules and error handling for the deployment environment.

## Current scope and limitations

This README describes the implementation represented in the repository. The following points should be kept in mind:

- The project uses a server-rendered Spring MVC and Thymeleaf interface; it is not a React or Angular SPA.
- Crew messaging is backed by database persistence and ordinary HTTP requests; the project does not include a WebSocket/STOMP stack.
- Adventure location coordinates exist in the domain model, but a full interactive map/tracking feature is not part of the current scope.
- The example configuration uses `spring.jpa.hibernate.ddl-auto=update` for local development. This should be reviewed and replaced with a controlled migration strategy for production.
- The repository should be tested in the target environment before being described as production-ready. External service availability, database configuration, and deployment behavior depend on the environment.

## Future improvements

Potential next steps for the project include:

- Add automated integration tests for authentication, adventure creation, join requests, and chat authorization.
- Introduce database migrations with Flyway or Liquibase.
- Add an interactive map and richer location discovery.
- Improve chat delivery with WebSockets if real-time messaging becomes a requirement.
- Add pagination, sorting, and more advanced discovery filters.
- Add image upload storage with validation and a managed object-storage provider.
- Add deployment documentation, monitoring, and CI workflows.
