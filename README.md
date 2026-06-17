# TaskHive

A small task tracker in the spirit of Jira/Trello. You create workspaces, invite
people, set up projects inside them, and track tasks through a configurable status
workflow. Server-rendered with Thymeleaf — no separate frontend to run.

## Tech stack

- **Java 21**
- **Spring Boot 4** — Web MVC, Security, Data JPA, Cache, Validation
- **Thymeleaf** for the UI
- **PostgreSQL**
- **Liquibase**
- **Lombok**
- **Maven**

## Features

- Register / login with email + password (BCrypt hashing)
- Workspaces with members and roles
- Projects inside a workspace, each with its own project key
- Tasks: create, edit, assign, set priority, soft-delete, per-project numbering
- Status workflow with a state machine (only allowed transitions are possible)
- Comments on tasks
- Role-based access control + an admin panel for managing users
- JPA auditing (created/updated timestamps) and basic caching

## Getting started

### Prerequisites

- JDK 21+
- PostgreSQL running locally

### 1. Create the database

```bash
createdb taskhive
```

### 2. Configure the connection

Open `src/main/resources/application.properties` and set the datasource to match
your Postgres setup:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskhive
spring.datasource.username=your_user
spring.datasource.password=your_password
```

Liquibase creates all the tables on first start, so you don't need to set up the
schema yourself.

### 3. Run it

```bash
./mvnw spring-boot:run
```

Then open http://localhost:8080.

### Default admin account

A seed admin user is created on first run:

- **email:** `admin@taskhive.com`
- **password:** `admin123`

Or just register a new account from the login page.

## Notes

- The schema is owned by Liquibase and Hibernate runs in `validate` mode, so the
  entities and the database are kept in sync. If you've previously run against a
  database that already has the tables, start from a fresh/empty `taskhive` DB.
