# QLBH Điện tử — ứng dụng POS Android

Ứng dụng quản lý bán hàng cho cửa hàng linh kiện PC, điện thoại, phụ kiện và dịch vụ sửa chữa/bảo hành. MVP chạy offline-first, lưu dữ liệu trên thiết bị bằng Room/SQLite.

## Tính năng hiện có

- **Tổng quan:** doanh thu và số hóa đơn trong ngày, cảnh báo hàng sắp hết, phiếu dịch vụ đang xử lý.
- **Bán hàng:** tìm/chọn sản phẩm từ danh sách, giỏ hàng, gắn tên khách, thanh toán tiền mặt; giao dịch tạo hóa đơn và trừ tồn kho trong một transaction.
- **Kho hàng:** thêm sản phẩm (SKU duy nhất), danh mục, thương hiệu, giá vốn/giá bán, tồn kho; nhập nhanh số lượng.
- **Dịch vụ:** tiếp nhận máy tính/điện thoại, ghi nhận lỗi, phí dự kiến, thông tin khách và trạng thái sửa chữa.
- **Khách hàng:** lưu hồ sơ khách và ghi chú.
- **Báo cáo:** doanh thu ngày/tháng, số hóa đơn, chi phí, tồn kho thấp; ghi nhận khoản thu/chi.
- Có dữ liệu mẫu được tạo tự động ở lần chạy đầu để thử POS.

## Công nghệ

- Kotlin, Android SDK 35, min SDK 26, Java 17
- Jetpack Compose + Material 3
- Room (SQLite), Kotlin Coroutines/Flow, ViewModel

## Chạy ứng dụng

1. Mở repository bằng Android Studio (Hedgehog trở lên) và cài Android SDK 35.
2. Chọn **Sync Project with Gradle Files** để tải dependencies.
3. Chạy cấu hình `app` trên emulator hoặc thiết bị Android 8.0+.

Hoặc từ thư mục gốc, với Android SDK và Gradle cài đặt:

```bash
gradle assembleDebug
gradle test
```

APK debug nằm tại `app/build/outputs/apk/debug/app-debug.apk`.

Không cần Firebase, tài khoản hay API key cho bản MVP. Cơ sở dữ liệu cục bộ `qlbh-pos.db`; gỡ ứng dụng sẽ xóa dữ liệu. Trước khi dùng thực tế, cần bổ sung sao lưu/đồng bộ, quyền người dùng, in hóa đơn, quét mã vạch và kiểm thử nghiệp vụ.
