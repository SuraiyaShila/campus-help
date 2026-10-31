# BUP Assist: Campus Emergency Help & Resource Management System

A centralized web platform for campus emergency and medical requests, lost and found, and resource sharing.
Built for the Software & Requirements Engineering project, Department of ICT, Bangladesh University of Professionals.

**Live demo:** https://campus-help-ajgg.onrender.com

> The demo runs on a free plan. If nobody has opened it for 15 minutes, the first page load can take about a minute.

## Features

- **Sign up and sign in** with roles: Student, Faculty, Campus Security, Medical Staff, Hostel Authority, Administrator
- **Emergency and medical requests** with an SOS button, location (coordinates), urgency and status tracking
- **Responder dashboard**: each team (Medical, Security, Hostel) sees and updates its own requests
- **Lost and found**: report items with a photo, search and filter, claim with proof, Security approves or rejects
- **Resource sharing**: post resources, request them, owner accepts or rejects, mark as returned
- **Notifications** with an unread badge
- **Administrator dashboard** with totals, peak request times, item categories and the user list
- **Security**: BCrypt password hashing, server-side sessions, role checks on every API call

## Technology

| Layer | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript, Bootstrap 5 |
| Backend | Java 21, Spring Boot 4 (REST API, MVC) |
| Database | MySQL 8 (Aiven in production) |
| Hosting | Render (Docker) |
| Design and modeling | Figma, Draw.io |

## Architecture

Layered (three-tier) client-server architecture built as a modular monolith:
presentation layer (web client) -> application layer (Spring Boot modules) -> data layer (MySQL).

## Run locally

1. Install Java 21 or newer and MySQL 8, then create the database:

   ```sql
   CREATE DATABASE campus_help CHARACTER SET utf8mb4;
   ```

2. Create `src/main/resources/application-local.properties` (this file is git-ignored):

   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/campus_help?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Dhaka
   spring.datasource.username=root
   spring.datasource.password=YOUR_PASSWORD
   ```

3. Start the app:

   ```
   .\mvnw.cmd spring-boot:run
   ```

4. Open http://localhost:8080

## Team

| Name | ID |
|---|---|
| Mohammad Rafi Ahmed | 24549010081 |
| Suraiya Yeasmin | 24549010133 |
| Afifa Aziza | 24549010095 |

Supervisors: Nazneen Akhter (Bangladesh University of Professionals) and Suman Saha (University of Frontier Technology, Bangladesh).
