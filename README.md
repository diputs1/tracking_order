# 📦 Tracking Order System

[![Java Version](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.13-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-Cache-red?style=for-the-badge&logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)](https://www.docker.com/)

Hệ thống quản lý và theo dõi đơn hàng (Order Tracking) được xây dựng trên nền tảng **Spring Boot 3**. Dự án được thiết kế theo hướng hiện đại, sẵn sàng cho việc mở rộng (Scalable) và tối ưu hóa hiệu năng với Redis Caching.

---

## 🚀 Tech Stack

### Backend
- **Core:** Java 17, Spring Boot 3.5.13
- **Security:** Spring Security & JWT (Stateless Authentication)
- **Database:** MySQL 8.0 (Persistence)
- **Caching:** Redis (Performance Optimization)
- **Migrations:** Flyway (Database version control)
- **Mapping:** MapStruct & Lombok (Boilerplate reduction)
- **Documentation:** SpringDoc OpenAPI (Swagger UI)

### Infrastructure & DevOps
- **Containerization:** Docker & Docker Compose
- **CI/CD:** GitHub Actions
- **Logging:** SLF4J + Logback

---

## ✨ Key Features

- [x] **Authentication:** Đăng ký, đăng nhập với JWT, phân quyền chi tiết.
- [x] **Product Catalog:** Quản lý sản phẩm, danh mục và kho hàng.
- [x] **Shopping Cart:** Giỏ hàng realtime, tính toán giá trị đơn hàng.
- [x] **Order Management:** Quy trình đặt hàng, thanh toán và xử lý đơn.
- [x] **Tracking:** Theo dõi trạng thái đơn hàng theo thời gian thực.
- [x] **Shipping Integration:** Tích hợp các đơn vị vận chuyển (Shipping Carriers).

---

## 🏗️ System Architecture

Dự án hiện tại là một **Modular Monolith**, được thiết kế để dễ dàng tách rời thành Microservices trong tương lai:

1. **Persistence Layer:** Sử dụng MySQL cho các dữ liệu quan trọng cần tính nhất quán.
2. **Caching Layer:** Sử dụng Redis để giảm tải cho DB và tăng tốc độ phản hồi cho các API như Product List, Category.
3. **Security Layer:** JWT-based authentication đảm bảo tính stateless và khả năng scale ngang.

---

## 🛠️ Getting Started

### Prerequisites
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Setup Environment
Sao chép file `.env.example` thành `.env` và cấu hình các thông số (Database, Redis, JWT Secret).

### 2. Run with Docker (Recommended)
```bash
# Khởi chạy toàn bộ hạ tầng (MySQL, Redis) và Application
docker-compose up -d
```

### 3. Manual Run (Local Development)
Khởi chạy infrastructure trước:
```bash
docker-compose -f docker-compose.infra.yml up -d
```
Sau đó chạy ứng dụng Spring Boot:
```bash
./mvnw spring-boot:run
```

---

## 📖 API Documentation

Sau khi ứng dụng khởi chạy thành công, bạn có thể truy cập tài liệu API tại:
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

---

## 🚢 Roadmap

- [ ] Tích hợp **Kafka** để decoupling các service (Order -> Shipping).
- [ ] Triển khai **Redis Cluster** cho High Availability.
- [ ] Chuyển đổi sang kiến trúc **Microservices**.
- [ ] Tích hợp các cổng thanh toán (VNPay, Momo).

---

## 📄 License
Project này được phát triển bởi **Antigravity**. Vui lòng liên hệ nếu bạn muốn đóng góp!
