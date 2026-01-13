# Hướng dẫn Test API

## ✅ Đã sửa các vấn đề:

1. **Endpoint chính xác**: `/api/products/add`
2. **Method**: POST
3. **Auto-generate ID**: Đã thêm `@GeneratedValue(strategy = GenerationType.IDENTITY)`

## 📋 Các bước test:

### Bước 1: Khởi động server
```powershell
.\mvnw spring-boot:run
```

Chờ đến khi thấy dòng:
```
Started DemoApplication in X.XXX seconds
```

### Bước 2: Test bằng Postman hoặc curl

**URL**: `http://localhost:8080/api/products/add`
**Method**: POST
**Headers**: 
```
Content-Type: application/json
```

**Body (JSON)**:
```json
{
    "title": "Test Product",
    "price": 99.99,
    "description": "This is a test product",
    "category": "electronics",
    "image": "https://example.com/image.jpg",
    "rating": {
        "rate": 4.5,
        "count": 100
    }
}
```

### Bước 3: Test bằng PowerShell
```powershell
$body = '{"title":"Test Product","price":99.99,"description":"This is a test product","category":"electronics","image":"https://example.com/image.jpg","rating":{"rate":4.5,"count":100}}'

Invoke-RestMethod -Uri "http://localhost:8080/api/products/add" -Method POST -Headers @{"Content-Type"="application/json"} -Body $body | ConvertTo-Json
```

## ✅ Kết quả mong đợi:

```json
{
    "id": 1,
    "title": "Test Product",
    "price": 99.99,
    "description": "This is a test product",
    "category": "electronics",
    "image": "https://example.com/image.jpg",
    "rating": {
        "rate": 4.5,
        "count": 100
    }
}
```

## 📝 Tổng kết:

**Controller**: `ProductController.java`
- `@RequestMapping("/api/products")` (class level)
- `@PostMapping("/add")` (method level)
- **Full path**: POST `/api/products/add`

**Logic thêm sản phẩm**:
1. Nhận ProductCreateRequest từ client
2. Tạo Product entity mới
3. Copy dữ liệu từ request → entity
4. Save vào database (PostgreSQL)
5. Trả về product đã lưu (có ID auto-generated)

✅ **Logic hoàn toàn ĐÚNG!**
