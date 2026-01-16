TÀI LIỆU THIẾT KẾ: HỆ THỐNG TỐI ƯU HÓA DỮ LIỆU CHUYẾN BAY (FLIGHT DATA OPTIMIZATION SYSTEM)
1. Tổng quan (Overview)
Hệ thống được thiết kế để xử lý việc tra cứu thông tin chuyến bay dựa trên mã sân bay (Airport IATA). Mục tiêu chính là giảm thiểu số lần gọi API trả phí (AirLabs), tăng tốc độ phản hồi (Latency thấp) và đảm bảo tính nhất quán dữ liệu giữa Cache và Database.

2. Kiến trúc dữ liệu trên Redis (Data Architecture)
Để phục vụ các chiến lược trên, chúng ta sẽ tổ chức Key-Value trong Redis như sau:

2.1. Data Key (Lưu dữ liệu chuyến bay)
Key Format: flights:data:{airport_iata} (Ví dụ: flights:data:SGN)

Data Type: Hash

Structure:

Field: {flight_iata}_{dep_time} (Unique ID của chuyến bay trong ngày)

Value: FlightDTO (JSON String - chứa cả mã Hash của object để so sánh)

Physical TTL (Redis): 60 phút (Theo chiến lược Logical Expiration).

2.2. Counter Key (Đếm tần suất)
Key Format: flights:counter:{airport_iata} (Ví dụ: flights:counter:SGN)

Data Type: String (Integer)

Value: Số lần request.

TTL: 30 phút.

2.3. Negative Cache Key (Chặn lỗi)
Key Format: flights:empty:{airport_iata}

Data Type: String (Marker)

TTL: 5 phút.

3. Chiến lược Caching (Caching Strategy)
Hệ thống áp dụng mô hình Multi-Layer Smart Caching với các lớp bảo vệ sau:

3.1. Chiến lược cập nhật dữ liệu (Update Policy)
Sử dụng kết hợp Frequency-Based Caching (Dựa trên tần suất) và Logical Expiration (Hết hạn ảo).

Logical Expiration (Hết hạn ảo):

Set TTL vật lý (trên Redis) = 60 phút.

Set TTL logic (trong Code/Object) = 30 phút.

Cơ chế:

Nếu Time_Current < Time_Logic_Expire: Trả về dữ liệu ngay.

Nếu Time_Logic_Expire < Time_Current < Time_Physical_Expire:

Vẫn trả về dữ liệu cũ cho User (để User không phải chờ).

Kích hoạt một Async Thread (Luồng ngầm) để gọi API update dữ liệu mới vào Cache và DB.

Frequency-Based (Dựa trên mức độ quan tâm):

Mỗi khi có request (cache miss hoặc logical expire), tăng biến đếm Counter Key.

Count < 2: Đây là sân bay ít người quan tâm. Set Logical TTL ngắn (5 phút).

Count >= 2: Đây là "Hot Data". Set Logical TTL chuẩn (30 phút).

Negative Caching (Cache kết quả rỗng):

Nếu gọi AirLabs API trả về danh sách rỗng hoặc lỗi 404 (sai mã sân bay) -> Lưu ngay một key đánh dấu vào Redis với TTL 5 phút.

Mục đích: Chặn việc spam request vào các mã sân bay không tồn tại gây tốn quota.

4. Quy trình xử lý trùng lặp & Đồng bộ Database (Duplicate Check & Sync)
Quy trình này xảy ra sau khi lấy được dữ liệu tươi từ API (bất kể là do Cache Miss hay do Async Update).

Thuật toán so sánh (Hashing Strategy): Mỗi Object chuyến bay sẽ được tính một mã Hash (MD5 hoặc SHA-256) dựa trên toàn bộ nội dung của nó.

Các bước thực hiện:

Lấy danh sách mới: Có List New_Flights từ API.

Lấy danh sách cũ: Đọc Hash flights:data:{iata} từ Redis (nếu có).

Vòng lặp kiểm tra: Duyệt từng chuyến bay trong New_Flights:

Tạo Composite Key: K = dep_iata + dep_time + flight_iata.

Case 1: Key K chưa tồn tại trong Redis:

-> Đây là chuyến bay mới.

-> Action: INSERT vào Database.

-> Action: PUT vào Redis Hash.

Case 2: Key K đã tồn tại:

Lấy mã Hash của chuyến bay cũ trong Redis (Old_Hash).

Tính mã Hash của chuyến bay mới (New_Hash).

So sánh:

Nếu Old_Hash == New_Hash: Dữ liệu y hệt -> Bỏ qua (Không làm gì cả).

Nếu Old_Hash != New_Hash: Có thông tin thay đổi (ví dụ delay, đổi cổng) -> Action: UPDATE Database. -> Action: PUT vào Redis Hash.

5. Luồng hoạt động chi tiết (Workflow Sequence)
Dưới đây là mô tả luồng đi của dữ liệu khi User request tìm kiếm SGN.

User Request: GET /api/flights?iata=SGN

Check Negative Cache:

Có key flights:empty:SGN? -> Nếu có, trả về rỗng ngay (Stop).

Check Data Cache:

Đọc flights:data:SGN từ Redis.

Xử lý Cache Hit (Có dữ liệu):

Kiểm tra Logical Expiration (trường refreshTime trong value).

Nếu còn hạn: Return Data cho User.

Nếu hết hạn (nhưng vẫn còn trong Redis):

Return Data cũ cho User ngay lập tức.

Async Task: Khởi chạy luồng cập nhật ngầm (Đi tới Bước 6).

Xử lý Cache Miss (Không có dữ liệu):

User phải chờ. Đi tới Bước 6.

Gọi API & Cập nhật (Update Strategy):

Gọi AirLabs API lấy dữ liệu mới.

Nếu API lỗi/rỗng -> Set Negative Cache (TTL 5p) -> Return.

Nếu có dữ liệu -> Tăng flights:counter:SGN.

Phân loại TTL (Frequency Check):

Lấy giá trị Counter.

Nếu Counter < 2: Set Logical TTL = 5 phút.

Nếu Counter >= 2: Set Logical TTL = 30 phút.

Đồng bộ Database (Dedup Logic):

Thực hiện so sánh Hash như mục 4.

Lưu thay đổi vào DB & Redis.

(Nếu là Async Task ở bước 4 thì kết thúc luồng ngầm).

(Nếu là Cache Miss ở bước 5 thì trả về dữ liệu mới cho User).

6. Sơ đồ tuần tự (Sequence Diagram)
sequenceDiagram
    participant User
    participant App as Spring Boot
    participant Redis
    participant DB as PostgreSQL
    participant API as AirLabs

    User->>App: Request info (SGN)
    
    %% STEP 1: Check Negative Cache
    App->>Redis: Check Negative Key (SGN)
    alt Is Negative?
        Redis-->>App: Yes
        App-->>User: Return Empty/Error
    end

    %% STEP 2: Check Data Cache
    App->>Redis: Get Hash Data (SGN)
    Redis-->>App: Data or Null

    alt Cache Hit (Data Exists)
        App->>App: Check Logical Expiration
        alt Not Expired
            App-->>User: Return Cached Data (FAST)
        else Logically Expired
            App-->>User: Return Stale Data (FAST)
            App->>App: Spawn Async Thread
            Note right of App: Background Update Starts
        end
    else Cache Miss
        Note right of App: Must fetch synchronously
    end

    %% STEP 3: Fetch & Update (Sync or Async)
    App->>API: Call AirLabs API
    API-->>App: Flight List JSON

    alt Data Empty?
        App->>Redis: Set Negative Cache (5 min)
    else Has Data
        App->>Redis: INCR Counter (SGN)
        Redis-->>App: Count Value
        
        App->>App: Determine TTL (Count < 2 ? 5m : 30m)
        
        loop Every Flight
            App->>Redis: Get Old Hash
            App->>App: Compare Old Hash vs New Hash
            alt Different Hash
                App->>DB: Upsert (Insert/Update)
                App->>Redis: Update Hash Field
            else Same Hash
                Note right of App: Ignore (Optimize DB IO)
            end
        end
        
        App->>Redis: Set Data Key with Physical TTL (60m)
    end
    
    alt Was Cache Miss?
        App-->>User: Return New Data
    end

7. Ghi chú triển khai (Implementation Notes)
Async Threading: Sử dụng @Async của Spring Boot hoặc CompletableFuture cho trường hợp cập nhật ngầm (Logical Expiration). Điều này cực kỳ quan trọng để User không cảm thấy bị lag khi cache "hết hạn ảo".

Hashing: Nên dùng SHA-256 của thư viện Apache Commons Codec hoặc Guava để tạo hash string cho object Flight. Đảm bảo implement phương thức toString() hoặc hàm tạo hash bao gồm tất cả các trường quan trọng.

Redis Pipelining: Khi update lại Cache (bước lưu Hash), nên dùng Redis Pipeline để đẩy một lúc nhiều lệnh HSET thay vì gọi lẻ tẻ từng lệnh để tối ưu hiệu năng mạng.

Database Batch: Tương tự, khi Insert/Update vào DB, hãy dùng JDBC Batch Update (JPA saveAll) để insert hàng loạt.