# PLAN-system-scale-upgrade.md
## Nâng cấp `tracking_order` lên Production-Grade Scalable System

> **Agent**: `@project-planner` + `@backend-specialist` + `@orchestrator`
> **Mode**: PLANNING ONLY — No code in this file
> **Created**: 2026-05-05
> **Stack**: Spring Boot 3.5 / Java 17 / MySQL / Docker → k3s/EKS

---

## 📋 Overview

Hệ thống `tracking_order` hiện là một **Modular Monolith** chạy Docker Compose, MySQL đơn lẻ,
không có cache, không có async messaging, không có auto-scaling. Mục tiêu là nâng cấp từng bước
để đạt được:

- **1,000 rps** cho Order processing
- **3,000 rps** cho Notification delivery
- **Auto-scaling** theo seasonal spike
- **Sẵn sàng** cho WebSocket tracking, Analytics, AI recommendations
- **100% dùng free/open-source** — đổi endpoint khi có tiền → chạy AWS

---

## ✅ Success Criteria

| Criteria | Đo lường |
|----------|---------|
| Order API chịu 1,000 rps | k6 load test: < 200ms p95, < 1% error |
| Notification async | Order vẫn trả 200 kể cả khi Notification service down |
| Zero-downtime deploy | Rolling update không drop request |
| DB failover < 30s | MySQL replica promotion tự động |
| Cache hit rate > 80% | Prometheus metric: `cache.hit_ratio` |
| Auto-scale triggered | HPA scale out khi CPU > 70% |
| Observability | Mọi request có trace ID trong Grafana |

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
| **DB Primary** | MySQL Docker | RDS MySQL Multi-AZ | Same JDBC URL, diff host |
| **DB Backup** | `mysql-cron-backup` image | RDS automated snapshots | Script identical |
| **Cache** | Redis Stack Docker | ElastiCache Redis | Same Spring config |
| **Messaging** | Kafka (Bitnami) Docker | AWS MSK | Same bootstrap-servers |
| **API Gateway** | Traefik OSS | Kong Gateway / AWS ALB | Same routing rules |
| **Auth** | Keycloak Docker | AWS Cognito | Same OAuth2 endpoints |
| **Monitoring** | Prometheus + Grafana OSS | Same (OSS on EKS) | 100% same |
| **Tracing** | Jaeger | AWS X-Ray / Tempo | OpenTelemetry abstraction |
| **Secret Mgmt** | Vault Dev | AWS Secrets Manager + ESO | Same env var injection |
| **WebSocket** | Spring WebSocket | Same + Redis Pub/Sub | Same code |
| **Analytics** | OpenSearch Docker | Amazon OpenSearch | Same REST API |
| **AI** | Ollama local | AWS SageMaker | API abstraction layer |

---

## 📁 File Structure (Target)

```
tracking_order/
├── src/main/java/com/example/tracking_order/
│   ├── modules/
│   │   ├── order/          ✅ Exists
│   │   ├── notification/   ✅ Exists → extract to service
│   │   ├── tracking/       📅 NEW — WebSocket real-time
│   │   ├── analytics/      📅 NEW — Event consumers
│   │   └── recommendation/ 📅 NEW — AI integration
│   ├── config/
│   │   ├── RedisConfig.java         📅 NEW
│   │   ├── KafkaConfig.java         📅 NEW
│   │   ├── ResilienceConfig.java    📅 NEW
│   │   └── OpenTelemetryConfig.java 📅 NEW
│   └── security/
│       └── JwtUtils.java   ✅ Exists → upgrade to JWKS
├── k8s/
│   ├── deployments/
│   │   ├── order-service.yaml       📅 NEW
│   │   └── notification-service.yaml 📅 NEW
│   ├── hpa/
│   │   ├── order-hpa.yaml           📅 NEW
│   │   └── notification-hpa.yaml   📅 NEW
│   └── configmaps/
│       └── app-config.yaml          📅 NEW
├── docker-compose.infra.yml   📅 NEW — Redis, Kafka, Monitoring only
├── docker-compose.app.yml     📅 NEW — App services
└── prometheus.yml             📅 NEW
```

---

## 🗺️ Dependency Graph

```
P1-Redis ─────────────────────────────────────────────────────┐
   │                                                           │
P2-MySQL-HA ────────────┐                                      │
   │                    │                                      ▼
P3-Resilience4j ────────┤                              P11-WebSocket
   │                    │                              (cần Redis)
   ▼                    ▼
P4-OpenTelemetry   P5-Idempotency
        │                │
        └────────────────┘
                 │
                 ▼
           P6-Kafka ──────────► P7-Events ──────► P8-NotifConsumer
                                                         │
                                                         ▼
                                                  P9-K8s Manifests
                                                         │
                                                         ▼
                                                  P10-HPA Testing
                                                         │
                                              ┌──────────┘
                                              │
                                     P11-WebSocket
                                              │
                                     P12-Analytics
                                              │
                                        P13-AI/Recs
```

---

## 📦 Task Breakdown

---

### 🔴 PHASE 1 — Foundation Hardening (Tuần 1–2)
> **Mục tiêu**: Hệ thống không sập khi có load. Đây là BLOCKER cho mọi thứ khác.

---

#### TASK P1 — Thêm Redis Cache

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `clean-code` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 4 giờ |
| **Dependencies** | Không có |

**INPUT**: `pom.xml` không có Redis. OrderService query DB cho mọi request.

**OUTPUT**:
- Thêm `spring-boot-starter-data-redis` vào `pom.xml`
- Tạo `RedisConfig.java` với TTL config
- Cache `getOrders()`, `getOrderDetail()` trong `OrderServiceImpl`
- Cache product catalog từ `CatalogService`
- `application.yml` thêm `spring.redis.*` config
- `docker-compose.infra.yml` thêm Redis Stack service

**VERIFY**:
```bash
# Redis running
docker exec -it redis redis-cli ping  # → PONG

# Cache hit in logs
curl GET /api/orders/1 (lần 2 → log: "cache HIT order:1")

# Prometheus metric
curl /actuator/prometheus | grep cache_hits
```

**ROLLBACK**: Xóa `@Cacheable` annotations, giữ nguyên logic gốc.

---

#### TASK P2 — MySQL High Availability + Backup

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `database-design` |
| **Priority** | 🔴 P0 — Blocker |
| **Effort** | 3 giờ |
| **Dependencies** | Không có |

**INPUT**: Single MySQL container. Không backup. SPOF.

**OUTPUT**:
- `docker-compose.infra.yml` thêm MySQL Primary + Replica (binlog replication)
- `application.yml` config read/write split:
  - Write → `mysql-primary:3306`
  - Read → `mysql-replica:3306`
- `mysql-cron-backup` container: backup lúc 2h sáng → `./backup/`
- Script `scripts/restore-backup.sh`

**VERIFY**:
```bash
docker logs mysql-backup | grep "Backup completed"
docker exec mysql-replica mysql -e "SHOW SLAVE STATUS\G" | grep Seconds_Behind
# → Seconds_Behind_Master: 0 (hoặc < 5)
```

**ROLLBACK**: Revert về single MySQL, file backup đã sẵn ở `./backup/`.

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
- `resilience4j-spring-boot3` dependency
- `ResilienceConfig.java`: circuit breaker config cho `notificationService`, `paymentService`
- `@CircuitBreaker(name="notification", fallbackMethod="notifyFallback")` wrapper
- Retry: 3 attempts, exponential backoff 1s→2s→4s

**VERIFY**:
```bash
docker stop notification-app
curl POST /api/orders  # → 201 Created (không bị block)
curl /actuator/health | grep circuitBreaker  # → OPEN
```

---

#### TASK P4 — Spring Actuator + OpenTelemetry

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `clean-code` |
| **Priority** | 🟠 P1 |
| **Effort** | 3 giờ |
| **Dependencies** | Không có |

**INPUT**: Không có health endpoints. Không có distributed tracing. Kubernetes probe sẽ fail.

**OUTPUT**:
- `spring-boot-starter-actuator` + `opentelemetry-spring-boot-starter` dependency
- `management.endpoints.web.exposure.include=health,metrics,prometheus`
- `otel.exporter.otlp.endpoint=http://jaeger:4317`
- `docker-compose.infra.yml`: Jaeger + Prometheus + Grafana + Loki
- `prometheus.yml`: scrape config

**VERIFY**:
```bash
curl http://localhost:8080/actuator/health  # → {"status":"UP"}
curl http://localhost:8080/actuator/prometheus | grep jvm_
open http://localhost:16686  # Jaeger UI → traces visible
```

---

#### TASK P5 — Idempotency Keys

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟠 P1 |
| **Effort** | 2 giờ |
| **Dependencies** | P1 (Redis) |

**INPUT**: `POST /orders` không idempotent. Double-submit = duplicate order.

**OUTPUT**:
- `IdempotencyFilter.java`: check `X-Idempotency-Key` header → Redis store 24h
- Nếu key tồn tại → return cached response
- Swagger doc: `@Parameter(name="X-Idempotency-Key")`

**VERIFY**:
```bash
# 2 requests cùng key → 1 order trong DB
SELECT COUNT(*) FROM orders WHERE idempotency_key='test-123';  # → 1
```

---

### 🟠 PHASE 2 — Async Decoupling via Kafka (Tuần 3–4)
> **Mục tiêu**: 3,000 rps notification không block order. Fault isolation.

---

#### TASK P6 — Kafka Infrastructure

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟠 P1 |
| **Effort** | 2 giờ |
| **Dependencies** | Phase 1 complete |

**OUTPUT**:
- Bitnami Kafka KRaft mode (không cần Zookeeper) trong `docker-compose.infra.yml`
- Topics: `order.created` (p6), `order.status.updated` (p3), `notification.push` (p6), `tracking.events` (p3)
- `KafkaConfig.java`: Producer + Consumer bean config
- Kafka UI tại `http://localhost:8090`

**VERIFY**:
```bash
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
open http://localhost:8090  # Kafka UI
```

---

#### TASK P7 — Order Event Publishing

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns`, `clean-code` |
| **Priority** | 🟠 P1 |
| **Effort** | 4 giờ |
| **Dependencies** | P6 |

**OUTPUT**:
- `OrderCreatedEvent.java` record
- `OrderEventPublisher.java`: `KafkaTemplate.send("order.created", event)`
- `OrderServiceImpl`: thay direct notification call → event publish
- Dead Letter Topic `order.created.DLT` nếu publish fail sau 3 retries

**VERIFY**:
```bash
curl POST /api/orders  # → 201 Created (< 50ms, không chờ notification)
# Kafka UI → topic "order.created" → message visible
```

---

#### TASK P8 — Notification Kafka Consumer

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟠 P1 |
| **Effort** | 4 giờ |
| **Dependencies** | P7 |

**OUTPUT**:
- `NotificationConsumer.java`: `@KafkaListener(topics="order.created", groupId="notification-group")`
- 6 concurrent consumers trong group
- `@RetryableTopic(attempts=3, backoff=@Backoff(delay=1000, multiplier=2))`
- Separate Spring profile `notification` cho Docker service riêng

**VERIFY**:
```bash
docker stop order-app
# notification-service vẫn consume từ Kafka backlog
docker logs notification-service | grep "Processing notification"
```

---

### 🟡 PHASE 3 — Kubernetes & Auto-scaling (Tuần 5–6)
> **Mục tiêu**: k3d local, K8s YAML ready to deploy EKS.

---

#### TASK P9 — Kubernetes Manifests

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `deployment-procedures` |
| **Priority** | 🟡 P2 |
| **Effort** | 6 giờ |
| **Dependencies** | Phase 1 + Phase 2 complete |

**OUTPUT** (`k8s/` folder):
- `namespaces/`, `configmaps/`, `secrets/` (Sealed Secrets)
- `deployments/order-service.yaml` + `notification-service.yaml` (với liveness + readiness probes)
- `hpa/order-hpa.yaml` (min:2, max:20, cpu:70%)
- `hpa/notification-hpa.yaml` (min:3, max:30, cpu:60%)
- `ingress/traefik-ingress.yaml`
- `infra/redis-statefulset.yaml` + `kafka-statefulset.yaml`

**VERIFY**:
```bash
k3d cluster create tracking --agents 2
kubectl apply -f k8s/ -R
kubectl get pods -n tracking  # → all Running
curl http://api.localhost/api/orders  # → 200
```

---

#### TASK P10 — HPA Load Testing với k6

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `performance-profiling` |
| **Priority** | 🟡 P2 |
| **Effort** | 3 giờ |
| **Dependencies** | P9 |

**OUTPUT**:
- `k6/order-load-test.js`: ramp to 1,000 rps
- `k6/notification-load-test.js`: ramp to 3,000 rps
- Document: baseline metrics + HPA trigger evidence

**VERIFY**:
```bash
k6 run --vus 100 --rps 1000 --duration 60s k6/order-load-test.js
# → p95 < 200ms, errors < 1%

kubectl get hpa -n tracking -w
# → REPLICAS tăng tự động
```

---

### 🔵 PHASE 4 — Real-Time Tracking WebSocket (Tuần 7–8)

---

#### TASK P11 — WebSocket + Redis Pub/Sub + TimescaleDB

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🔵 P3 |
| **Effort** | 8 giờ |
| **Dependencies** | Phase 3 complete |

**OUTPUT**:
- `spring-boot-starter-websocket` dependency
- `WebSocketConfig.java`: STOMP + SockJS `/ws/tracking`
- `TrackingController.java`: `@MessageMapping("/location")`
- Redis Pub/Sub: fan-out location events to subscribers
- Kafka topic `tracking.events` → TimescaleDB persist
- TimescaleDB container trong infra

**VERIFY**:
```bash
wscat -c ws://localhost:8080/ws/tracking
# Gửi location → client nhận < 500ms
```

---

### 🟣 PHASE 5 — Analytics + AI (Tuần 9–12)

---

#### TASK P12 — CDC Analytics Pipeline

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `database-design` |
| **Priority** | 🟣 P4 |
| **Effort** | 8 giờ |
| **Dependencies** | Phase 3 complete |

**OUTPUT**:
- Debezium MySQL CDC → Kafka Connect
- `AnalyticsConsumer.java`: index vào OpenSearch
- MinIO container: Parquet data lake
- Grafana dashboard: daily orders, revenue, top products

---

#### TASK P13 — AI Recommendation Engine

| Field | Value |
|-------|-------|
| **Agent** | `@backend-specialist` |
| **Skill** | `api-patterns` |
| **Priority** | 🟣 P4 |
| **Effort** | 10 giờ |
| **Dependencies** | P12 (data), P1 (Redis cache) |

**OUTPUT**:
- PostgreSQL + pgvector (DB riêng cho AI)
- `RecommendationService.java` interface
- `OllamaClient.java`: HTTP client → `http://ollama:11434`
- `/api/recommendations?userId=X` endpoint
- Cache 1h trong Redis
- Ollama container: model `nomic-embed-text`

---

## 📊 Timeline Summary

```
Tuần 1-2:  PHASE 1 — Foundation (P1 Redis → P2 MySQL → P3 Circuit → P4 OTel → P5 Idempotency)
Tuần 3-4:  PHASE 2 — Kafka Async (P6 Infra → P7 Publish → P8 Consumer)
Tuần 5-6:  PHASE 3 — Kubernetes  (P9 Manifests → P10 HPA Test)
Tuần 7-8:  PHASE 4 — WebSocket   (P11 Tracking)
Tuần 9-12: PHASE 5 — Analytics + AI (P12 CDC → P13 Recs)
```

---

## 🚨 Risk Register

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Redis OOM | Medium | High | Eviction policy `allkeys-lru`, limit 512MB |
| Kafka consumer lag spike | Medium | High | Monitor lag, scale consumer pods |
| MySQL replication delay | Low | High | Alert lag > 5s, fallback read → primary |
| WebSocket connection limit | Medium | Medium | Sticky sessions + Redis Pub/Sub |
| Ollama OOM local | High | Low | Dùng model nhỏ: `nomic-embed-text` (274MB) |
| k3d RAM không đủ | High | Medium | Cần tối thiểu 8GB RAM máy dev |

---

## 📋 Phase X — Final Verification Checklist

- [ ] `curl /actuator/health` → `{"status":"UP"}`
- [ ] Redis cache hit > 80%
- [ ] MySQL backup tồn tại trong `./backup/`
- [ ] Circuit breaker OPEN khi dependency down, main service vẫn 201
- [ ] Duplicate order với same idempotency key → 1 row
- [ ] Kafka topics created, Kafka UI accessible
- [ ] Order API < 100ms (không chờ notification)
- [ ] k3d: all pods Running
- [ ] HPA scale-out triggered dưới load
- [ ] k6: 1000 rps, p95 < 200ms, errors < 1%
- [ ] WebSocket: location update fan-out < 500ms
- [ ] OpenSearch indexed, Grafana dashboard populated
- [ ] AI recommendation endpoint trả results < 10ms (cached)

---

**[OK] Plan file created: `docs/PLAN-system-scale-upgrade.md`**

Next steps:
- Review plan này
- Chạy `/create PHASE 1 — P1 Redis Cache` để bắt đầu implement
- Hoặc chỉnh sửa plan theo nhu cầu
