# TIMOA User Server

> **사용자 인증 · 계정 · 팀 서버(워크스페이스) 관리 서버**  
> JWT 발급, 프로필 이미지(S3), 초대 코드 기반 팀 참가와 멤버 관리를 담당합니다.

![Java](https://img.shields.io/badge/Java_21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-jjwt_0.11-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
![MSSQL](https://img.shields.io/badge/MS_SQL-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat-square&logo=amazons3&logoColor=white)

---

## 개요

TIMOA에서 **"누가, 어느 팀에 속해 있는가"** 를 책임지는 서비스입니다.  
Discord의 서버 개념처럼, 사용자는 여러 **팀 서버**에 참가할 수 있고 각 서버 안에서 채팅·문서·코드·대시보드·음성 채널을 사용합니다.

Proxy Server의 **공개 경로(`/proxy/user`)** 로 노출되는 유일한 서비스이며, 여기서 발급한 JWT로 나머지 서비스에 접근합니다.

```
Client ── POST /api/users/login ──▶ User Server ──▶ JWT (HS, 24h) 발급
   │
   └── Authorization: Bearer <JWT> ──▶ Proxy Server ──▶ chat / document / code / dashboard / voice
```

## 주요 기능

### 인증 · 계정
- 회원가입 · 로그인 · 로그아웃 · 정보 수정 · 탈퇴
- 로그인 성공 시 **JWT 발급** (subject = 사용자 ID, 만료 24시간, HMAC 서명)
- 프로필 이미지는 **AWS S3**에 저장하고 DB에는 경로만 보관

### 팀 서버 관리
- 서버 생성 시 **16자리 초대 코드 자동 생성** (UUID 기반, 중복 시 재생성)
- 초대 코드로 서버 참가, 자진 탈퇴, **서버장의 멤버 강퇴**
- 멤버별 역할(`role`) 관리, 서버 멤버 목록 + 프로필 이미지 조회

### 실시간 알림 (SSE)
- 서버 참가·탈퇴·강퇴, 서버 정보 변경 등을 멤버에게 SSE로 브로드캐스트

## API

### 사용자 `/api/users`
| Method | Path | 설명 |
|---|---|---|
| `POST` | `/create/user` | 회원가입 |
| `POST` | `/login` | 로그인 (JWT 발급) |
| `POST` | `/logout` | 로그아웃 |
| `POST` | `/update/user` | 정보·프로필 이미지 수정 |
| `DELETE` | `/delete/user` | 회원 탈퇴 |
| `POST` | `/download-url` | 파일 업로드 후 다운로드 URL 발급 (S3) |

### 팀 서버 `/api/servers`
| Method | Path | 설명 |
|---|---|---|
| `POST` | `/create` | 서버 생성 (초대 코드 발급) |
| `POST` | `/update` | 서버 정보 수정 |
| `DELETE` | `/delete/{serverId}` | 서버 삭제 |
| `GET` | `/user/{userId}` | 사용자가 속한 서버 목록 |
| `GET` | `/members` | 서버 멤버 목록 |
| `GET` | `/get_invite_code/{serverId}` | 초대 코드 조회 |
| `POST` | `/join` | 초대 코드로 참가 |
| `DELETE` | `/leave` | 서버 탈퇴 |
| `DELETE` | `/getout` | 멤버 강퇴 (서버장) |

### SSE / 기타
| Method | Path | 설명 |
|---|---|---|
| `GET` | `/sse/user/{userId}` | 실시간 이벤트 구독 |
| `GET` | `/health` | 헬스체크 |

## 프로젝트 구조

```
src/main/java/TIMOA/userserver/
├── controller/   # User, Server, Sse, ping
├── service/      # User_service(JWT), Server_service(초대코드), Server_member_service, Sse_service
├── repository/   # JPA Repository + S3FileRepository
├── entity/       # Users, Servers, Server_members (+ 공유 DB의 채팅·대시보드·문서 엔티티)
├── DTO/
└── config/       # WebConfig, SseConfig, GlobalExceptionHandler
```

> 현재 TIMOA는 서비스들이 **하나의 MS SQL DB를 공유**합니다. 그래서 서버 생성 시 기본 채팅 채널을 함께 만들거나 멤버 탈퇴 시 작업 담당자 정보를 정리하는 등, 다른 도메인 엔티티를 직접 참조합니다. 이 결합도는 To-Be 설계에서 해소할 과제로 정리되어 있습니다.  
> → [ADR 0003 — Shared DB](https://github.com/TIMOA-MAIN/TIMOA_Docs/blob/main/decisions/architecture/0003-shared-db.md) · [To-Be: DB 분리 계획](https://github.com/TIMOA-MAIN/TIMOA_Docs/blob/main/architecture/to-be.md)

## 실행

```bash
./gradlew bootRun
```

### 설정 (`application.properties` / 환경 변수)
| 키 | 설명 |
|---|---|
| `spring.datasource.url` / `username` / `password` | MS SQL 접속 정보 |
| `jwt.secret` | JWT 서명 키 (HMAC, 충분히 긴 값) |
| `spring.cloud.aws.credentials.*` | AWS 자격 증명 (환경 변수로 주입) |
| `spring.cloud.aws.region.static` | S3 리전 |
| `server.port` | 기본 `8001` |

## 관련 레포
- [Desktop_Client](https://github.com/TIMOA-MAIN/Desktop_Client) — 로그인 · 서버 목록 UI
- [Proxy_Server](https://github.com/TIMOA-MAIN/Proxy_Server) — `/proxy/user`(공개), `/proxy/sse-user`, `/user-upload` 라우팅
