# PLAN-system-scale-upgrade.md
## Nâng cấp `tracking_order` lên Production-Grade Scalable System

> **Agent**: `@project-planner` + `@backend-specialist` + `@orchestrator`
> **Mode**: PLANNING ONLY — No code in this file
> **Created**: 2026-05-05
> **Stack**: Spring Boot 3.5 / Java 17 / MySQL / Docker → k3s/EKS

---

## 📋 Overview

Hệ thống `tracking_order` hiện là một **Modular Monolith** chạy Docker Compose, MySQL đơn lẻ, chưa có cache, chưa có async messaging. 
Thay vì thiết kế over-engineering ngay từ đầu, hệ thống sẽ được nâng cấp theo hướng **Pragmatic**, chia thành 2 Phase:
1. **Phase A (Thực tế):** Hardening hệ thống, xử lý nút thắt cơ bản (DB, Cache, Security), thiết lập đo lường.
2. **Phase B (Khi có nhu cầu thật):** Chỉ triển khai Kafka, Kubernetes, WebSocket, AI khi Phase A bị bottleneck (có số liệu chứng minh).

---

## ✅ Success Criteria

| Criteria | Đo lường |
|----------|---------|
| Order API Stability | k6 load test: Tìm ra rps tối đa của Phase A (VD: 500 rps) với p95 < 200ms, < 1% error |
| Zero-downtime deploy | Schema Migration (Flyway) an toàn, Rolling update không drop request |
| DB Reliability | MySQL replica backup an toàn, **không bị Replication Lag trên core flow** |
| Cache hit rate > 80% | Prometheus metric: `cache.hit_ratio` |
| Security & Rate Limit | Chặn Brute Force, DDoS (HTTP 429) |
| Observability | Mọi request có trace ID trong Grafana |
| Auto-scale triggered | HPA scale out khi Kafka Lag hoặc Request Rate tăng |

---

## 🏗️ Project Type

**BACKEND** — Spring Boot Microservices (gradual extraction from monolith)
**Primary Agents**: `@backend-specialist`, `@security-auditor`, `@devops-engineer`

---

## 🛠️ Tech Stack Decisions

| Component | Free (Dev/Learn) | Production (khi có tiền) | Rationale |
|-----------|-----------------|--------------------------|-----------|
| **Container** | Docker Compose | Docker + k3s/EKS | Same Dockerfile |
| **Orchestration** | k3d (local) | AWS EKS | Same Kubernetes YAML |
| **DB Primary** | MySQL Docker | RDS MySQL Multi-AZ | Same JDBC URL |
| **Schema Mgmt** | Flyway | Flyway | Quản lý version DB chuẩn |
| **Cache/Limit** | Redis Stack Docker | ElastiCache Redis | Phục vụ Cache & Rate Limiting |
| **Messaging** | Kafka (Bitnami) Docker | AWS MSK | Chỉ dùng ở Phase B |
| **API Gateway** | Traefik OSS | Kong Gateway / AWS ALB | Same routing rules |
| **Auth** | Keycloak Docker | AWS Cognito | Same OAuth2 endpoints |
| **Monitoring** | Prometheus + Grafana | Same (OSS on EKS) | 100% same |
| **Tracing** | Jaeger | AWS X-Ray / Tempo | OpenTelemetry abstraction |
| **CI/CD** | GitHub Actions | GitLab CI / AWS CodeBuild| Tự động hóa Deploy |

---

## 🗺️ Dependency Graph

```text
[PHASE A - THỰC TẾ]
P0-Migration/Security ──────────────────────────────────────┐
        │                                                   │
P1-Redis (Cache & Rate Limit)                               │
        │                                                   │
P2-MySQL-HA (Read-After-Write) ─────────┐                   │
        │                               │                   ▼
P3-Resilience4j ────────┤               │             P11-WebSocket
        │               │               │             (Limit connections)
        ▼               ▼               ▼                   │
P4-Observability  P5-Idempotency   P6-CI/CD                 │
        │               │                                   │
        └───────┬───────┘                                   │
                ▼                                           │
  [STOP & MEASURE LOAD HERE]                                │
                │                                           │
[PHASE B - KHI CẦN THẬT]                                    │
                │                                           │
                ▼                                           │
          P7-Kafka ──────────► P8-Events ──────► P9-NotifConsumer (Idempotent)
                                                       │
                                                       ▼
                                                P10-K8s & HPA (Custom Metrics)
                                                       │
                                            ┌──────────┘
                                            │
                                   P11-WebSocket
                                            │
                                   P12-Analytics
                                            │
                                     P13-AI (Fraud/ETD)
```

---

## 📦 Task Breakdown

---

### 🔴 PHASE A — Foundation Hardening (Thực Tế)
> **Mục tiêu**: Hệ thống không sập khi có load. Xử lý triệt để security và migration. Dừng lại đo tải thật trước khi làm Phase B.

---

#### TASK P0 — Schema Migration & Security Layer

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist`, `@security-auditor` |
| **Skill** | `database-design`, `clean-code` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 4 giờ |
| **Dependencies** | Không có |

**INPUT**: DB tạo tự động bằng `hibernate.ddl-auto=update`. Security lỏng lẻo.

**OUTPUT**:
- Tích hợp `Flyway` hoặc `Liquibase` để quản lý version schema DB.
- Đổi `spring.jpa.hibernate.ddl-auto=validate`.
- Áp dụng RBAC role checking.
- Cấu hình JWT Rotation/Expiration strategy an toàn.

**VERIFY**:
```bash
# Khởi động app thấy log migration
docker logs tracking-order-app | grep "Flyway Community Edition"
```

**ROLLBACK**: Revert về `update` và xóa schema migration table.

---

#### TASK P1 — Redis Cache & Rate Limiting

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `clean-code` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 4 giờ |
| **Dependencies** | Không có |

**INPUT**: `pom.xml` không có Redis. OrderService query DB cho mọi request. Không có rate limit.

**OUTPUT**:
- Thêm `spring-boot-starter-data-redis` vào `pom.xml`.
- Tạo `RedisConfig.java` với TTL config.
- Cache `getOrders()`, `getOrderDetail()` trong `OrderServiceImpl`.
- Thêm **Rate Limiting Filter** dựa trên Redis (Limit theo IP hoặc UserID) để chống brute-force và DDoS.
- `docker-compose.infra.yml` thêm Redis Stack service.

**VERIFY**:
```bash
# Redis running
docker exec -it redis redis-cli ping  # → PONG

# Cache hit in logs
curl GET /api/orders/1 (lần 2 → log: "cache HIT order:1")

# Rate limit trigger
for i in {1..101}; do curl -X GET /api/orders/1; done # Lần 101 -> 429 Too Many Requests
```

**ROLLBACK**: Xóa `@Cacheable` annotations và Rate Limiting Filter, giữ nguyên logic gốc.

---

#### TASK P2 — MySQL High Availability + Read/Write Split An Toàn

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `database-design` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 3 giờ |
| **Dependencies** | P0 |

**INPUT**: Single MySQL container. Không backup. Có rủi ro Replication Lag nếu setup HA sai.

**OUTPUT**:
- `docker-compose.infra.yml` thêm MySQL Primary + Replica (binlog replication).
- `mysql-cron-backup` container: backup lúc 2h sáng → `./backup/`.
- **CRITICAL FIX**: Core flows (Order API) phải **đọc/ghi 100% trên Primary** (Read-After-Write consistency) để user không thấy order "biến mất" do lag.
- Replica **chỉ** được cấu hình để phục vụ các module Reporting, Analytics hoặc Admin Dashboard.
- Script `scripts/restore-backup.sh`.

**VERIFY**:
```bash
docker logs mysql-backup | grep "Backup completed"
docker exec mysql-replica mysql -e "SHOW SLAVE STATUS\G" | grep Seconds_Behind
```

**ROLLBACK**: Revert về single MySQL.

---

#### TASK P3 — Resilience4j Circuit Breakers

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `clean-code` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 3 giờ |
| **Dependencies** | P1 (Redis dùng làm state store) |

**INPUT**: Order gọi Notification trực tiếp. Notification chậm → Order chậm.

**OUTPUT**:
- `resilience4j-spring-boot3` dependency.
- `ResilienceConfig.java`: circuit breaker config cho các external calls.
- `@CircuitBreaker(name="notification", fallbackMethod="notifyFallback")` wrapper.
- Retry: 3 attempts, exponential backoff 1s→2s→4s.

**VERIFY**:
```bash
docker stop notification-app
curl POST /api/orders  # → 201 Created (không bị block)
curl /actuator/health | grep circuitBreaker  # → OPEN
```

**ROLLBACK**: Gỡ dependency, bỏ annotation.

---

#### TASK P4 — Spring Actuator + OpenTelemetry

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `clean-code` |
| **Priority** | 🟠 P1 |
| **Effort** | 3 giờ |
| **Dependencies** | Không có |

**INPUT**: Không có health endpoints. Không có distributed tracing.

**OUTPUT**:
- `spring-boot-starter-actuator` + `opentelemetry-spring-boot-starter` dependency.
- `management.endpoints.web.exposure.include=health,metrics,prometheus`.
- `otel.exporter.otlp.endpoint=http://jaeger:4317`.
- `docker-compose.infra.yml`: Jaeger + Prometheus + Grafana.

**VERIFY**:
```bash
curl http://localhost:8080/actuator/health  # → {"status":"UP"}
open http://localhost:16686  # Jaeger UI → traces visible
```

**ROLLBACK**: Xóa properties và dependencies.

---

#### TASK P5 — Idempotency Keys Chuẩn Production

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟠 P1 |
| **Effort** | 2 giờ |
| **Dependencies** | P1 (Redis) |

**INPUT**: `POST /orders` không idempotent. Double-submit = duplicate order.

**OUTPUT**:
- `IdempotencyFilter.java`: check header.
- **CRITICAL FIX**: Key phải có scope chặt chẽ: `idempotency:{userId}:{hash(requestBody)}`.
- Lưu key vào Redis trong 24h.
- Nếu trùng key -> return cached response.

**VERIFY**:
```bash
# 2 requests cùng nội dung, cùng userId → chỉ 1 order vào DB
SELECT COUNT(*) FROM orders WHERE user_id='U123';
```

**ROLLBACK**: Gỡ bỏ filter.

---

#### TASK P6 — CI/CD Pipeline

| Field | Value |
|-------|-------|
| **Agent** | `@devops-engineer` |
| **Skill** | `deployment-procedures` |
| **Priority** | 🟠 P1 |
| **Effort** | 3 giờ |
| **Dependencies** | P0 |

**INPUT**: Deploy thủ công, nguy cơ lỗi cao.

**OUTPUT**:
- Tạo file `.github/workflows/deploy.yml`.
- Các steps: Lint code -> Run JUnit Tests -> Build Docker Image -> Push to Registry.

**VERIFY**:
```bash
# Push commit lên nhánh main và theo dõi GitHub Actions xanh
```

**ROLLBACK**: Xóa folder `.github`.

---
> 🛑 **ĐIỂM DỪNG (STOP & MEASURE)**
> Cần chạy load test với `k6` để lấy baseline metrics. Nếu hệ thống đã handle tốt traffic hiện tại (đáp ứng RPS đề ra với lỗi < 1%), **KHÔNG** chuyển sang Phase B.
---

### 🟠 PHASE B — Advanced Decoupling & Auto-scaling (Khi có Bottleneck)
> **Mục tiêu**: Vượt ngưỡng vật lý của Monolith thông qua Decoupling và Kubernetes. (Chỉ triển khai khi Phase A đuối sức).

---

#### TASK P7 & P8 — Kafka Infrastructure & Event Publishing

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟡 P2 |
| **Effort** | 6 giờ |
| **Dependencies** | Chờ đo đạc Phase A |

**OUTPUT**:
- Bitnami Kafka KRaft mode trong infra.
- `OrderEventPublisher.java`: Bắn `order.created` event thay vì gọi Notification trực tiếp.

**VERIFY**:
```bash
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

**ROLLBACK**: Xóa Kafka khỏi Docker compose, revert lại sync call.

---

#### TASK P9 — Notification Kafka Consumer (Idempotent)

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟡 P2 |
| **Effort** | 4 giờ |
| **Dependencies** | P7, P8 |

**OUTPUT**:
- `@KafkaListener` xử lý `order.created`.
- **CRITICAL FIX (Consumer Idempotency)**: Lưu `eventId` đã xử lý vào DB/Redis. Phải check tồn tại trước khi gửi notification (tránh việc Kafka retry gửi mail 2 lần).

**VERIFY**:
```bash
# Bắn 2 Kafka messages cùng eventId -> Hệ thống chỉ ghi log xử lý 1 lần, bỏ qua lần 2
```

**ROLLBACK**: Bỏ `@KafkaListener`, revert config.

---

#### TASK P10 — Kubernetes Manifests & Custom Metrics HPA

| Field | Value |
|-------|-------|
| **Agent** | `@devops-engineer` |
| **Skill** | `deployment-procedures` |
| **Priority** | 🟡 P2 |
| **Effort** | 6 giờ |
| **Dependencies** | P9 |

**OUTPUT**:
- Đưa app lên k3d.
- **CRITICAL FIX**: HPA (Horizontal Pod Autoscaler) không dùng CPU thuần. Cài đặt Prometheus Adapter/KEDA để scale dựa trên **Kafka Lag** và **Request Rate**.

**VERIFY**:
```bash
kubectl get hpa -n tracking -w  # HPA kích hoạt khi Kafka lag tăng lên
```

**ROLLBACK**: Revert k8s cluster, quay lại Docker Compose.

---

#### TASK P11 — Real-Time Tracking WebSocket

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🔵 P3 |
| **Effort** | 8 giờ |
| **Dependencies** | P10 |

**OUTPUT**:
- Redis Pub/Sub + Spring WebSocket cho location updates.
- **CRITICAL FIX**: Giới hạn max connection per pod và implement backpressure để tránh Pod OOM. Tách riêng WebSocket Gateway nếu cần.

**VERIFY**:
```bash
wscat -c ws://localhost:8080/ws/tracking
```

**ROLLBACK**: Xóa properties config websocket.

---

#### TASK P12 & P13 — Analytics & Product-Driven AI

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `database-design`, `api-patterns` |
| **Priority** | 🟣 P4 |
| **Effort** | 10+ giờ |
| **Dependencies** | P10 |

**OUTPUT**:
- Debezium MySQL CDC đẩy dữ liệu vào OpenSearch.
- **CRITICAL FIX (AI Use-case)**: Tập trung vào use-case thực tế có metric rõ ràng (VD: Fraud Detection, ETD Prediction), KHÔNG làm Recommendation viển vông.

**VERIFY**:
```bash
curl -X GET "localhost:9200/orders/_search" # Check OpenSearch CDC index
```

**ROLLBACK**: Tắt Debezium CDC container.

---

## 🚨 Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Replication Lag | Low | High | Bắt buộc đọc/ghi Read-After-Write qua Primary. |
| Kafka Duplicate Delivery | Medium | High | Idempotent Consumer check `eventId`. |
| Schema Migration fail | Low | High | Luôn viết migration script dạng backward-compatible. |
| HPA Scale chậm | Medium | Medium | KEDA based on Kafka lag/RPS. |
| WebSocket OOM | High | High | Max connection limits per pod. |

---

## 📋 Phase X — Final Verification Checklist

- [ ] Schema Migration chạy trơn tru, không lock table lâu.
- [ ] Rate limit chặn thành công spammer (HTTP 429).
- [ ] Tạo Order xong, gọi GET ngay lập tức trả về đúng Order (Primary routing).
- [ ] Gửi 2 request có cùng `{userId}:{hashBody}` trả về kết quả cached.
- [ ] Mọi HTTP request đều có Trace ID trong Grafana/Jaeger.
- [ ] K6 test rate limit và performance đạt ngưỡng yêu cầu của Phase A.
- [ ] (Phase B) Consumer nhận duplicate event nhưng chỉ gửi 1 SMS/Email.
- [ ] (Phase B) HPA scale theo Kafka lag thay vì CPU.

---

**[OK] Plan file updated (v3) with full details, tables, and commands retained.**
