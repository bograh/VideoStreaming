# 🎬 VideoStreaming

A full-featured **video streaming backend** built with Spring Boot. Upload, transcode, and stream videos with
multi-resolution support, JWT-based authentication, and S3 storage.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Environment Variables](#environment-variables)
    - [Running Infrastructure](#running-infrastructure)
    - [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
    - [Authentication](#authentication)
    - [Videos](#videos)
- [Project Structure](#project-structure)
- [License](#license)

---

## Features

- **User Authentication** — Register, login, logout, and token refresh using JWT (access + refresh tokens via HTTP-only
  cookies).
- **Video Upload** — Upload videos (up to 1 GB) with thumbnails, metadata extraction via FFprobe, and async S3 storage.
- **Multi-Resolution Transcoding** — Automatic transcoding to multiple resolutions (144p–2160p) and codecs (H.264,
  H.265, AV1, MPEG4) using FFmpeg.
- **S3 Storage** — Videos, thumbnails, and transcoded files stored in Amazon S3 (or S3-compatible services like
  MinIO/LocalStack).
- **Pre-signed URLs** — Secure, time-limited URLs for video and thumbnail access.
- **Caching** — Caffeine-based caching for video listings and user data.
- **Pagination & Sorting** — Paginated video feeds with customizable sort fields and ordering.
- **Correlation IDs** — Request tracing via `X-Correlation-ID` headers for observability.
- **Global Exception Handling** — Consistent, structured error responses across all endpoints.
- **Actuator** — Spring Boot Actuator endpoints exposed for monitoring.
- **Sentry Integration** — Error tracking via Sentry.

---

## Tech Stack

| Layer           | Technology                         |
|-----------------|------------------------------------|
| **Language**    | Java 21                            |
| **Framework**   | Spring Boot 3.5                    |
| **Security**    | Spring Security + JWT (jjwt 0.13)  |
| **Database**    | PostgreSQL 17                      |
| **ORM**         | Spring Data JPA / Hibernate        |
| **Cache**       | Caffeine (in-process) + Redis      |
| **Storage**     | Amazon S3 (AWS SDK v2)             |
| **Transcoding** | FFmpeg / FFprobe                   |
| **Monitoring**  | Spring Boot Actuator, Sentry       |
| **Build**       | Maven Wrapper                      |
| **Utilities**   | Lombok                             |
| **Containers**  | Docker Compose (PostgreSQL, Redis) |

---

## Architecture Overview

1. **Upload flow** — Video is uploaded saved to a temp directory metadata extracted via FFprobe uploaded to S3
   persisted to PostgreSQL transcoding kicked off asynchronously.
2. **Transcoding flow** — Original video downloaded from S3 FFmpeg transcodes to each resolution/codec combination
   transcoded files uploaded back to S3 job status tracked in the database.
3. **Playback flow** — Client requests a video backend returns pre-signed S3 URLs for all available resolutions.

---

## Getting Started

### Prerequisites

- **Java 21** (JDK)
- **Maven** (or use the included `mvnw` wrapper)
- **Docker & Docker Compose** (for PostgreSQL and Redis)
- **FFmpeg & FFprobe** (must be available on `PATH`)
- **S3-compatible storage** (AWS S3, MinIO, LocalStack, etc.)

### Environment Variables

Copy the example environment file and fill in your values:

```
bash
cp .env.example .env
```

| Variable                        | Description                                    |
|---------------------------------|------------------------------------------------|
| `DATABASE_USER`                 | PostgreSQL username                            |
| `DATABASE_PASSWORD`             | PostgreSQL password                            |
| `DATABASE_NAME`                 | PostgreSQL database name                       |
| `DATABASE_URL`                  | JDBC connection URL                            |
| `AWS_ACCESS_KEY`                | AWS / S3 access key                            |
| `AWS_SECRET_KEY`                | AWS / S3 secret key                            |
| `AWS_S3_BUCKET_NAME`            | S3 bucket name                                 |
| `AWS_REGION`                    | AWS region (e.g. `us-east-1`)                  |
| `AWS_ENDPOINT`                  | Custom S3 endpoint (optional, for MinIO, etc.) |
| `AWS_PATH_STYLE_ACCESS_ENABLED` | Set to `true` for MinIO / LocalStack           |
| `JWT_SECRET`                    | Secret key for signing JWTs                    |
| `JWT_EXPIRATION`                | Access token expiration (ms)                   |
| `JWT_REFRESH_EXPIRATION`        | Refresh token expiration (ms)                  |
| `REDIS_PASSWORD`                | Redis password                                 |

### Running Infrastructure

Start PostgreSQL and Redis with Docker Compose:

```
bash
docker-compose up -d
```

### Running the Application

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080` by default.

---

## API Endpoints

### Authentication

| Method | Endpoint             | Auth   | Description              |
|--------|----------------------|--------|--------------------------|
| POST   | `/api/auth/register` | Public | Register a new user      |
| POST   | `/api/auth/login`    | Public | Login and receive tokens |
| POST   | `/api/auth/refresh`  | Cookie | Refresh the access token |
| POST   | `/api/auth/logout`   | Cookie | Clear the refresh token  |

### Videos

| Method | Endpoint                | Auth     | Description                           |
|--------|-------------------------|----------|---------------------------------------|
| POST   | `/api/videos/upload`    | Required | Upload a video (multipart/form-data)  |
| GET    | `/api/videos`           | Public   | List videos (paginated, sortable)     |
| GET    | `/api/videos/{videoId}` | Public   | Get video details with streaming URLs |

#### Upload Parameters

| Parameter     | Type            | Required | Description             |
|---------------|-----------------|----------|-------------------------|
| `video`       | `MultipartFile` | Yes      | Video file (up to 1 GB) |
| `thumbnail`   | `MultipartFile` | No       | Thumbnail image         |
| `title`       | `String`        | Yes      | Video title             |
| `description` | `String`        | Yes      | Video description       |
| `tags`        | `List<String>`  | No       | Tags for the video      |

#### List Query Parameters

| Parameter | Default     | Description                                                                 |
|-----------|-------------|-----------------------------------------------------------------------------|
| `page`    | `0`         | Page number                                                                 |
| `size`    | `16`        | Page size (max 50)                                                          |
| `sort`    | `createdAt` | Sort field (`title`, `viewCount`, `likeCount`, `dislikeCount`, `createdAt`) |
| `order`   | `DESC`      | Sort direction (`ASC` / `DESC`)                                             |
| `search`  | —           | Search query (optional)                                                     |

---

## Project Structure

```
src/main/java/dev/ograh/videostreaming/
├── config/                # Configuration (Async, Cache, S3, Security)
├── controller/            # REST controllers (Auth, Video)
├── dto/
│   ├── projection/        # JPA projections for optimized queries
│   ├── request/           # Incoming request DTOs
│   ├── response/          # Outgoing response DTOs
│   └── shared/            # Shared DTOs & events
├── entity/                # JPA entities (User, Video, VideoFile, Comment, etc.)
├── enums/                 # Enumerations (Resolution, VideoStatus, JobStatus, etc.)
├── exception/             # Custom exceptions & global handler
├── filter/                # Servlet filters (Correlation ID)
├── messaging/             # Event producers & consumers
├── repository/            # Spring Data JPA repositories
├── security/              # JWT filter, service, auth entry points
├── service/               # Business logic services
├── utils/                 # Helper utilities (FFmpeg, temp files, transcoding)
└── VideoStreamingApplication.java
```

---