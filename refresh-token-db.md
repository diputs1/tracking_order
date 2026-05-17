# 🧠 Task: Xây dựng cơ chế Refresh Token lưu Database (Có Rotation)

## 🎯 Mục tiêu
Implement cơ chế lưu trữ Refresh Token vào Database với khả năng hỗ trợ đăng nhập nhiều thiết bị và bảo mật xoay vòng (Rotation). Cơ chế này giúp thu hồi token khi người dùng logout và bảo vệ chống lại Replay Attack.

## 🛠 Các bước triển khai

### Bước 1: Database Migration
- Tạo file migration `V2__Create_RefreshToken_Table.sql` trong thư mục Flyway.
- Định nghĩa bảng `refresh_tokens` có liên kết khoá ngoại tới bảng `users`.

### Bước 2: Khởi tạo Entity & Repository
- Tạo `RefreshToken.java` mapping với bảng `refresh_tokens`.
- Tạo `RefreshTokenRepository.java` với các hàm `findByToken` và `deleteByToken`.

### Bước 3: Triển khai RefreshTokenService
- `RefreshTokenService` & `RefreshTokenServiceImpl`: Tạo mới token, kiểm tra hạn (verifyExpiration) và xoá token.

### Bước 4: Tích hợp vào Flow Xác thực (Auth)
- Bổ sung hàm `generateTokenFromUsername` vào `JwtUtils.java`.
- Sửa `AuthServiceImpl.java`: 
  - Khi login: Gọi `RefreshTokenService` để tạo refresh token thật.
  - Thêm phương thức `refreshToken`: Xoay vòng (tuỳ chọn xoá cũ, sinh mới) và trả về Access Token mới.
- Tạo API Endpoint `/refresh` tại `AuthController.java`.
- Xoá Refresh Token khỏi DB khi user chủ động gọi API logout.

### Bước 5: Kiểm tra và Finalize
- Đảm bảo Clean Code và SOLID.
