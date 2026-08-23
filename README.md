# Slack Clone

A full-stack Slack-style team communication application built with Angular, Spring Boot, and PostgreSQL.

## Technology Stack

### Frontend

* Angular
* TypeScript
* Tailwind CSS

### Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* WebSocket and STOMP
* Flyway
* Maven

### Database

* PostgreSQL 16
* Docker Compose

## Project Structure

```text
slack-clone/
├── backend/
├── frontend/
├── docker-compose.yml
├── .gitignore
└── README.md
```

## Requirements

Before starting the application, install:

* Java 21
* Node.js 22 or newer
* npm
* Angular CLI
* Docker Desktop
* Git

## Database Configuration

PostgreSQL runs inside a Docker container.

Current development database configuration:

```text
Database: slack_clone_db
Username: slack_user
Password: slack_password
Host: localhost
Host port: 5433
Container port: 5432
```

Port `5433` is used on the host because the default PostgreSQL port `5432` was already occupied by another PostgreSQL service.

The Spring Boot datasource URL is:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/slack_clone_db
```

## Starting the Application

The database, backend, and frontend should run in separate processes.

### 1. Start PostgreSQL

Open PowerShell in the project root:

```powershell
cd E:\amar\praksa\slack-clone
docker compose up -d
```

Check whether the PostgreSQL container is running:

```powershell
docker compose ps
```

### 2. Start the Backend

Open another PowerShell terminal:

```powershell
cd E:\amar\praksa\slack-clone\backend
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

The health endpoint is available at:

```text
http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "message": "Slack Clone backend is running"
}
```

On Linux or macOS, start the backend with:

```bash
cd backend
./mvnw spring-boot:run
```

### 3. Start the Frontend

Open another PowerShell terminal:

```powershell
cd E:\amar\praksa\slack-clone\frontend
npm install
ng serve
```

The frontend runs at:

```text
http://localhost:4200
```

The `npm install` command is required the first time the project is started or whenever frontend dependencies change.

For normal development after the dependencies are installed, use:

```powershell
cd E:\amar\praksa\slack-clone\frontend
ng serve
```

## Stopping the Database

To stop PostgreSQL without deleting its stored data, run:

```powershell
docker compose down
```

Do not normally use:

```powershell
docker compose down -v
```

The `-v` option deletes the PostgreSQL Docker volume and all local database data.

## Database Migrations

Flyway migrations are located in:

```text
backend/src/main/resources/db/migration
```

Migration files must follow this naming format:

```text
V1__init.sql
V2__create_users.sql
V3__create_workspaces.sql
```

Each migration name contains:

1. The letter `V`
2. A migration version number
3. Two underscores
4. A descriptive migration name
5. The `.sql` extension

Do not modify an existing migration after it has already been applied to the database.

Create a new migration whenever the database structure needs to change.

## Current Project Status

The following parts have been completed:

* Angular frontend created
* Angular routing configured
* Tailwind CSS configured
* Spring Boot backend created
* Java 21 and Maven configured
* PostgreSQL 16 configured through Docker Compose
* PostgreSQL exposed through host port `5433`
* Spring Boot connected to PostgreSQL
* Spring Data JPA configured
* Flyway configured
* Initial Flyway migration created
* Health endpoint created
* Temporary Spring Security configuration added
* Git repository initialized
* GitHub repository connected
* Initial project pushed to GitHub
* Root `.gitignore` added

## Next Development Phase

The next development phase is authentication.

The planned authentication tasks are:

1. Create the `users` database table
2. Create the `User` JPA entity
3. Create the `UserRepository`
4. Add registration
5. Hash passwords using BCrypt
6. Add login
7. Generate JWT access tokens
8. Add JWT authentication filtering
9. Add the current-user endpoint
10. Create Angular login and registration pages
11. Add an Angular authentication service
12. Add an HTTP interceptor
13. Add route protection with an authentication guard

Planned authentication endpoints:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

## Planned Core Features

The first major version of the application will include:

* User registration
* User login
* JWT authentication
* Workspaces
* Workspace members
* Workspace roles
* Workspace invitations
* Public channels
* Private channels
* Channel membership
* Message history
* Real-time messaging
* Editing messages
* Soft-deleting messages
* Basic Slack-style interface
* Backend permission checks

## Planned Advanced Features

After the core version is complete, the following features may be added:

* Direct messages
* Group direct messages
* Message threads
* Emoji reactions
* File attachments
* Online presence
* Typing indicators
* Notifications
* Unread message counts
* Mentions
* Message search
* Member search
* User profiles
* User avatars
* Status messages
* Notification preferences

## Planned Architecture

The main application architecture is:

```text
Angular frontend
       |
       | REST API
       v
Spring Boot backend
       |
       | JPA / Hibernate
       v
PostgreSQL database
```

Real-time messaging will use:

```text
Angular STOMP client
       |
       | WebSocket
       v
Spring Boot WebSocket server
       |
       | Save message
       v
PostgreSQL
```

A real-time message flow will work like this:

1. A user writes a message in Angular.
2. Angular sends the message through WebSocket.
3. Spring Boot authenticates the user.
4. Spring Boot checks whether the user can access the channel.
5. Spring Boot saves the message in PostgreSQL.
6. Spring Boot broadcasts the saved message.
7. Connected channel members receive the message immediately.

REST endpoints will still be used to load existing message history.

## Planned Backend Structure

The Spring Boot backend will be organised by feature:

```text
com.amar.slackclone
├── auth
├── user
├── workspace
├── channel
├── message
├── directmessage
├── reaction
├── attachment
├── notification
├── security
├── websocket
├── common
└── config
```

Each feature will generally contain:

* Controller
* Service
* Repository
* Entity
* DTOs
* Mapper
* Exceptions

JPA entities will not be returned directly from controllers.

Controllers will return DTOs.

## Planned Frontend Structure

The Angular frontend will be organised like this:

```text
src/app
├── core
│   ├── auth
│   ├── guards
│   ├── interceptors
│   ├── services
│   └── models
├── shared
│   ├── components
│   ├── directives
│   └── pipes
├── features
│   ├── auth
│   ├── workspaces
│   ├── channels
│   ├── messages
│   ├── direct-messages
│   ├── notifications
│   └── profile
└── layout
    ├── workspace-sidebar
    ├── channel-sidebar
    ├── chat-header
    └── chat-layout
```

## Security Principles

The application will use:

* BCrypt password hashing
* JWT authentication
* Role-based authorisation
* Workspace membership checks
* Channel access checks
* Input validation
* CORS configuration
* Secure invitation tokens
* File upload restrictions
* Environment variables for secrets
* Backend permission validation

Passwords must never be stored in plain text.

Secrets must never be committed to GitHub.

Hiding a button in Angular is not a security measure. Every sensitive action must also be validated and authorised by the Spring Boot backend.


## Daily Development Workflow

From the project root, start PostgreSQL:

```powershell
docker compose up -d
```

Start the backend in another terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Start the frontend in another terminal:

```powershell
cd frontend
ng serve
```

Development addresses:

```text
Frontend: http://localhost:4200
Backend:  http://localhost:8080
Health:   http://localhost:8080/api/health
Database: localhost:5433
```

## Repository

```text
https://github.com/amarkosovac11/slack-clone
```

## Author

Amar Kosovac
