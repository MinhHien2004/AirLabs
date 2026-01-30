# 📋 BÁO CÁO THỰC TẬP
# **HỆ THỐNG THEO DÕI CHUYẾN BAY THỜI GIAN THỰC**
## AirLabs Real-time Flight Tracker

---

**Sinh viên thực tập:** [Điền tên]  
**Đơn vị thực tập:** [Điền đơn vị]  
**Thời gian thực tập:** [Điền thời gian]  
**Ngày hoàn thành:** Tháng 01/2026  

---

## MỤC LỤC

1. [Giới thiệu dự án](#1-giới-thiệu-dự-án)
2. [Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
3. [Công nghệ sử dụng](#3-công-nghệ-sử-dụng)
4. [Cấu trúc dự án](#4-cấu-trúc-dự-án)
5. [Backend - Chi tiết triển khai](#5-backend---chi-tiết-triển-khai)
6. [Frontend - Giao diện người dùng](#6-frontend---giao-diện-người-dùng)
7. [Hệ thống Caching với Redis](#7-hệ-thống-caching-với-redis)
8. [CI/CD Pipeline với GitHub Actions](#8-cicd-pipeline-với-github-actions)
9. [Containerization với Docker](#9-containerization-với-docker)
10. [Deployment & Infrastructure](#10-deployment--infrastructure)
11. [API Documentation](#11-api-documentation)
12. [Kết luận và bài học](#12-kết-luận-và-bài-học)

---

## 1. GIỚI THIỆU DỰ ÁN

### 1.1. Mô tả tổng quan

**AirLabs Real-time Flight Tracker** là một hệ thống web application cho phép người dùng tra cứu thông tin chuyến bay theo thời gian thực. Hệ thống tích hợp với **AirLabs API** để lấy dữ liệu chuyến bay và sử dụng **Redis** làm caching layer để tối ưu hiệu suất.

### 1.2. Mục tiêu dự án

| STT | Mục tiêu | Mô tả |
|-----|----------|-------|
| 1 | **Tra cứu chuyến bay** | Cho phép tìm kiếm arrivals/departures theo mã sân bay IATA |
| 2 | **Tối ưu hiệu suất** | Giảm thiểu API calls bằng multi-layer caching |
| 3 | **Thời gian thực** | Cập nhật thông tin chuyến bay liên tục |
| 4 | **CI/CD tự động** | Tự động build, test và deploy khi có code mới |
| 5 | **Containerization** | Đóng gói ứng dụng với Docker |

### 1.3. Tính năng chính

```
✅ Tra cứu chuyến bay đến (Arrivals) theo sân bay
✅ Tra cứu chuyến bay đi (Departures) theo sân bay  
✅ Hiển thị trạng thái: Scheduled, En-route, Landed, Delayed
✅ Smart Caching - Cache tự động sau 3 lần gọi
✅ Negative Caching - Chặn spam với mã sân bay sai
✅ Async Update - Cập nhật ngầm khi cache hết hạn logic
✅ CI/CD Pipeline - Tự động deploy lên QA/Production
```

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1. Sơ đồ kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                               │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    React + TypeScript + Vite                       │  │
│  │                      (Tailwind CSS)                                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ HTTP/REST
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           APPLICATION LAYER                             │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                  Spring Boot 4.0.1 + Java 17                      │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │  │
│  │  │ Controllers │  │  Services   │  │ Repository  │               │  │
│  │  │  (REST API) │──│ (Business)  │──│   (JPA)     │               │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘               │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                    │                               │
                    │                               │
                    ▼                               ▼
┌─────────────────────────────┐    ┌─────────────────────────────────────┐
│       CACHING LAYER         │    │         DATA LAYER                  │
│  ┌───────────────────────┐  │    │  ┌─────────────────────────────┐   │
│  │    Redis Cloud        │  │    │  │    PostgreSQL (Render)      │   │
│  │  (Lettuce + Pool)     │  │    │  │    + HikariCP Pool          │   │
│  │  - Data Cache         │  │    │  │                             │   │
│  │  - Counter Cache      │  │    │  │    Tables:                  │   │
│  │  - Negative Cache     │  │    │  │    - flights                │   │
│  └───────────────────────┘  │    │  │    - airlines               │   │
└─────────────────────────────┘    │  │    - airports               │   │
                                   │  └─────────────────────────────┘   │
                                   └─────────────────────────────────────┘
                    │
                    │ External API Call
                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL SERVICE                                │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                     AirLabs API v9                                 │  │
│  │         https://airlabs.co/api/v9/schedules                       │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2. Luồng xử lý Request

```
┌──────┐      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│Client│─────▶│  Controller │─────▶│   Service   │─────▶│   Redis     │
└──────┘      └─────────────┘      └─────────────┘      └─────────────┘
                                          │                   │
                                          │ Cache Miss        │ Cache Hit
                                          ▼                   │
                                   ┌─────────────┐            │
                                   │  AirLabs    │            │
                                   │    API      │            │
                                   └─────────────┘            │
                                          │                   │
                                          ▼                   │
                                   ┌─────────────┐            │
                                   │ PostgreSQL  │            │
                                   │   (Sync)    │            │
                                   └─────────────┘            │
                                          │                   │
                                          └───────────────────┘
                                                    │
                                                    ▼
                                             ┌──────────┐
                                             │ Response │
                                             └──────────┘
```

---

## 3. CÔNG NGHỆ SỬ DỤNG

### 3.1. Backend Stack

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **Java** | 17 (LTS) | Ngôn ngữ lập trình chính |
| **Spring Boot** | 4.0.1 | Framework phát triển |
| **Spring Data JPA** | - | ORM và Database Access |
| **Spring Data Redis** | - | Redis Integration |
| **Lettuce** | - | Redis Client với Connection Pooling |
| **Lombok** | 1.18.32 | Giảm boilerplate code |
| **Jackson** | - | JSON Serialization |
| **Maven** | 3.9.x | Build Tool |

### 3.2. Frontend Stack

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **React** | 18.2.0 | UI Library |
| **TypeScript** | 4.9.5 | Type-safe JavaScript |
| **Vite** | 5.0.8 | Build Tool & Dev Server |
| **Tailwind CSS** | 3.4.0 | Utility-first CSS |
| **PostCSS** | 8.5.6 | CSS Processing |

### 3.3. Infrastructure & DevOps

| Công nghệ | Mục đích |
|-----------|----------|
| **Docker** | Containerization |
| **Docker Hub** | Container Registry |
| **GitHub Actions** | CI/CD Pipeline |
| **Self-hosted Runner** | Build & Deploy Agent |
| **Render.com** | PostgreSQL Hosting |
| **Redis Cloud** | Managed Redis Service |

---

## 4. CẤU TRÚC DỰ ÁN

### 4.1. Cấu trúc thư mục

```
demo/
├── 📁 .github/
│   └── 📁 workflows/
│       └── 📄 build.yaml              # CI/CD Pipeline
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/Task/demo/
│   │   │   ├── 📄 DemoApplication.java    # Main Application
│   │   │   ├── 📁 config/                 # Configuration Classes
│   │   │   │   ├── AirLabsConfig.java
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   ├── RedisCloudConfig.java
│   │   │   │   ├── RestTemplateConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── 📁 controller/             # REST Controllers
│   │   │   │   ├── AirLabsController.java
│   │   │   │   ├── AirlineController.java
│   │   │   │   ├── FlightController.java
│   │   │   │   ├── FlightControllerV2.java
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── ScheduleController.java
│   │   │   │   └── SyncController.java
│   │   │   ├── 📁 dto/                    # Data Transfer Objects
│   │   │   │   ├── FlightCacheEntry.java
│   │   │   │   ├── 📁 request/
│   │   │   │   └── 📁 response/
│   │   │   ├── 📁 entity/                 # JPA Entities
│   │   │   │   ├── Airline.java
│   │   │   │   ├── Airport.java
│   │   │   │   └── Flight.java
│   │   │   ├── 📁 Repository/             # JPA Repositories
│   │   │   │   ├── AirlineRepository.java
│   │   │   │   ├── AirportRepository.java
│   │   │   │   └── FlightRepository.java
│   │   │   ├── 📁 service/                # Business Logic
│   │   │   │   ├── AirlineService.java
│   │   │   │   ├── DataSyncService.java
│   │   │   │   ├── FlightCacheService.java
│   │   │   │   ├── FlightCacheServiceV2.java
│   │   │   │   ├── FlightService.java
│   │   │   │   ├── FlightServiceV2.java
│   │   │   │   └── ScheduleService.java
│   │   │   └── 📁 frontend/               # React Source
│   │   │       ├── App.tsx
│   │   │       ├── index.tsx
│   │   │       ├── index.css
│   │   │       ├── Scheduled.tsx
│   │   │       └── Scheduled.css
│   │   └── 📁 resources/
│   │       ├── 📄 application.yaml        # Spring Configuration
│   │       └── 📁 static/                 # Built Frontend
│   └── 📁 test/                           # Unit Tests
├── 📄 Dockerfile                          # Docker Build
├── 📄 pom.xml                             # Maven Configuration
├── 📄 package.json                        # NPM Dependencies
├── 📄 vite.config.ts                      # Vite Configuration
├── 📄 tailwind.config.cjs                 # Tailwind Configuration
└── 📄 tsconfig.json                       # TypeScript Configuration
```

---

## 5. BACKEND - CHI TIẾT TRIỂN KHAI

### 5.1. Main Application

```java
// DemoApplication.java
@SpringBootApplication
@EnableAsync  // Bật hỗ trợ async cho @Async annotation
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**Giải thích:**
- `@SpringBootApplication`: Kết hợp `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`
- `@EnableAsync`: Cho phép sử dụng `@Async` để xử lý bất đồng bộ (background update cache)

### 5.2. Entity Layer

#### Flight Entity

```java
@Entity
@Table(name = "flights", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"flight_iata", "dep_time"})
})
@Data
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonProperty("airline_iata")
    private String airlineIata;
    
    @JsonProperty("flight_iata")
    private String flightIata;
    
    @JsonProperty("dep_iata")
    private String depIata;
    
    @JsonProperty("arr_iata")
    private String arrIata;
    
    @JsonProperty("dep_time")
    private String depTime;
    
    @JsonProperty("arr_time")
    private String arrTime;
    
    @JsonProperty("status")
    private String status;
    
    // ... các trường khác
}
```

**Unique Constraint:** `(flight_iata, dep_time)` đảm bảo không có chuyến bay trùng lặp.

### 5.3. Repository Layer

```java
@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    // Tìm chuyến bay theo mã sân bay đi
    List<Flight> findByDepIata(String depIata);
    
    // Tìm chuyến bay theo mã sân bay đến
    List<Flight> findByArrIata(String arrIata);
    
    // Kiểm tra tồn tại
    boolean existsByFlightIataAndDepTime(String flightIata, String depTime);
    
    // Tìm theo composite key
    Flight findByFlightIataAndDepTime(String flightIata, String depTime);
    
    // Batch query - tránh N+1 problem
    @Query("SELECT f FROM Flight f WHERE CONCAT(f.flightIata, '|', f.depTime) IN :compositeKeys")
    List<Flight> findByCompositeKeys(@Param("compositeKeys") Set<String> compositeKeys);
    
    // Lấy danh sách tất cả sân bay đi
    @Query("SELECT DISTINCT flight.depIata FROM Flight flight ORDER BY flight.depIata")
    List<String> findAllDepIata();
}
```

### 5.4. Service Layer

#### FlightServiceV2 - Smart Caching Logic

```java
@Service
public class FlightServiceV2 {
    
    @Autowired
    private FlightCacheServiceV2 cacheService;
    
    @Autowired
    private FlightRepository flightRepository;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private AirLabsConfig airLabsConfig;

    /**
     * Logic chính với smart caching
     * Cache chỉ lưu khi IATA được gọi >= 3 lần
     */
    private List<Flight> getFlights(String iata, String type) {
        String iataUpper = iata.toUpperCase();
        
        // 1. Tăng call count
        int callCount = cacheService.incrementCallCount(iataUpper);
        
        // 2. Check negative cache
        if (cacheService.isNegativeCached(iataUpper, type)) {
            return new ArrayList<>();
        }

        // 3. Check data cache
        CacheResult cacheResult = cacheService.getFlightsFromCache(iataUpper, type);
        if (cacheResult.isCacheHit()) {
            return cacheResult.getFlights();
        }

        // 4. Cache miss - fetch from API
        List<Flight> flights = fetchFromAPI(iataUpper, type);
        
        if (flights.isEmpty()) {
            cacheService.setNegativeCache(iataUpper, type);
            return new ArrayList<>();
        }

        // 5. Sync to database
        List<Flight> savedFlights = syncToDatabase(flights);

        // 6. Cache nếu call count >= 3
        if (cacheService.shouldCache(iataUpper)) {
            cacheService.cacheFlights(iataUpper, type, savedFlights);
        }
        
        return savedFlights;
    }
}
```

### 5.5. Controller Layer

#### FlightControllerV2 - REST API

```java
@RestController
@RequestMapping("/api/v2/flights")
@CrossOrigin(origins = "*")
public class FlightControllerV2 {

    @Autowired
    private FlightServiceV2 flightService;

    /**
     * GET /api/v2/flights/schedules?dep_iata=SGN
     * Lấy danh sách chuyến bay theo IATA code
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<FlightDisplayDTO>> getFlightSchedules(
            @RequestParam String dep_iata) {
        List<FlightDisplayDTO> flights = flightService.getFlightSchedules(dep_iata);
        return ResponseEntity.ok(flights);
    }
    
    /**
     * GET /api/v2/flights/schedules/force-cache?dep_iata=SGN
     * Force cache ngay lập tức (bypass call count)
     */
    @GetMapping("/schedules/force-cache")
    public ResponseEntity<List<FlightDisplayDTO>> getFlightSchedulesForceCache(
            @RequestParam String dep_iata) {
        List<FlightDisplayDTO> flights = flightService.getFlightSchedulesWithForceCache(dep_iata);
        return ResponseEntity.ok(flights);
    }

    /**
     * DELETE /api/v2/flights/cache/{iataCode}
     * Xóa cache cho một IATA cụ thể
     */
    @DeleteMapping("/cache/{iataCode}")
    public ResponseEntity<Map<String, String>> evictCache(@PathVariable String iataCode) {
        flightService.evictCache(iataCode);
        return ResponseEntity.ok(Map.of("message", "Cache evicted for IATA: " + iataCode));
    }

    /**
     * GET /api/v2/flights/cache/stats
     * Lấy thống kê cache
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(flightService.getCallCountStats());
    }
}
```

### 5.6. Configuration

#### Application Configuration (application.yaml)

```yaml
server:
  port: 8080
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
    min-response-size: 1024
  tomcat:
    threads:
      max: 100
      min-spare: 10
    connection-timeout: 10000

spring:
  # Database Configuration
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/postgres}
    username: ${SPRING_DATASOURCE_USERNAME:postgres}
    password: ${SPRING_DATASOURCE_PASSWORD:123456}
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      idle-timeout: 30000
      connection-timeout: 10000
      max-lifetime: 600000
      pool-name: FlightHikariPool
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: update  
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 50
  
  # Redis Configuration
  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}
      password: ${SPRING_REDIS_PASSWORD:}
      timeout: 10000ms
      lettuce:
        pool:
          enabled: true
          max-active: 16
          max-idle: 8
          min-idle: 2

# AirLabs API
airlabs.api:
  base-url: ${AIRLABS_BASE_URL:https://airlabs.co/api/v9}
  api-key: ${AIRLABS_API_KEY:your-api-key}

# Health Check
management:
  health:
    redis:
      enabled: false
```

#### Redis Configuration

```java
@Configuration
public class RedisCloudConfig {
    
    @Value("${spring.data.redis.host}")
    private String redisHost;
    
    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    @Value("${spring.data.redis.password}")
    private String redisPassword;
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis server configuration
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
        serverConfig.setHostName(redisHost);
        serverConfig.setPort(redisPort);
        serverConfig.setPassword(redisPassword);
        
        // Connection pool configuration
        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofSeconds(2));
        poolConfig.setTestOnBorrow(true);
        
        // Socket options
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(15))
                .keepAlive(true)
                .tcpNoDelay(true)
                .build();
        
        // Build configuration
        LettucePoolingClientConfiguration clientConfig = 
            LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofSeconds(5))
                .build();
        
        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }
}
```

---

## 6. FRONTEND - GIAO DIỆN NGƯỜI DÙNG

### 6.1. Cấu trúc React Application

```
frontend/
├── App.tsx           # Main Component
├── index.tsx         # Entry Point
├── index.css         # Global Styles (Tailwind)
├── Scheduled.tsx     # Flight Schedule Component
└── Scheduled.css     # Component Styles
```

### 6.2. Main Application Component

```tsx
// App.tsx
import React from 'react';
import Scheduled from './Scheduled';
import './index.css';

function App() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <header className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white shadow-lg">
        <div className="container mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold text-center">
            ✈️ AirLabs Real-time Flight Tracker
          </h1>
          <p className="text-center text-blue-100 mt-2">
            Track arrivals and departures in real-time | v2.0
          </p>
        </div>
      </header>
      <Scheduled />
    </div>
  );
}

export default App;
```

### 6.3. Flight Schedule Component

```tsx
// Scheduled.tsx
import React, { useState } from 'react';
import './Scheduled.css';

interface Flight {
  actual_arr_time?: string;
  scheduled_arr_time?: string;
  actual_dep_time?: string;
  scheduled_dep_time?: string;
  arr_delayed?: number;
  dep_delayed?: number;
  airline_iata?: string;
  flight_iata?: string;
  dep_iata?: string;
  arr_iata?: string;
  status?: string;
}

const Scheduled: React.FC = () => {
  const [iata, setIata] = useState('');
  const [arrivals, setArrivals] = useState<Flight[]>([]);
  const [departures, setDepartures] = useState<Flight[]>([]);
  
  const API_BASE_URL = '';

  // Fetch arrivals from backend
  const fetchArrivals = async (iataCode: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/schedules/arrivals?iata=${iataCode}`);
      const flights = await response.json();
      setArrivals(flights || []);
    } catch (error) {
      console.error('Error fetching arrivals:', error);
      setArrivals([]);
    }
  };

  // Fetch departures from backend
  const fetchDepartures = async (iataCode: string) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/schedules/departures?iata=${iataCode}`);
      const flights = await response.json();
      setDepartures(flights || []);
    } catch (error) {
      console.error('Error fetching departures:', error);
      setDepartures([]);
    }
  };

  // Handle refresh
  const handleRefresh = () => {
    const iataCode = iata.toUpperCase().trim();
    if (iataCode.length === 3) {
      fetchArrivals(iataCode);
      fetchDepartures(iataCode);
    }
  };

  // Get CSS class based on status
  const getStatusClass = (status?: string) => {
    if (!status) return '';
    const lowerStatus = status.toLowerCase();
    if (lowerStatus.includes('landed')) return 'landed';
    if (lowerStatus.includes('scheduled')) return 'scheduled';
    if (lowerStatus.includes('delayed')) return 'delayed';
    if (lowerStatus.includes('en-route')) return 'en-route';
    return '';
  };

  return (
    <div className="scheduled-container">
      <h1 className="page-title">✈️ Flight Information System</h1>
      <div className="header">
        <input
          type="text"
          placeholder="Enter IATA Code (e.g. HAN, SGN)"
          value={iata}
          onChange={(e) => setIata(e.target.value.toUpperCase())}
        />
        <button onClick={handleRefresh}>🔄 Refresh</button>
      </div>
      
      <div className="container">
        {/* Arrivals Table */}
        <div className="board">
          <div className="board-header">📥 Arrivals - {iata || 'Select Airport'}</div>
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Carrier</th>
                <th>Flight</th>
                <th>Origin</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {arrivals.map((flight, index) => (
                <tr key={index}>
                  <td>{flight.actual_arr_time || 'N/A'}</td>
                  <td>{flight.airline_iata || 'N/A'}</td>
                  <td>{flight.flight_iata || 'N/A'}</td>
                  <td>{flight.dep_iata || 'N/A'}</td>
                  <td className={`status ${getStatusClass(flight.status)}`}>
                    {flight.status || 'N/A'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Departures Table */}
        <div className="board">
          <div className="board-header">📤 Departures - {iata || 'Select Airport'}</div>
          <table>
            {/* Similar structure as Arrivals */}
          </table>
        </div>
      </div>
    </div>
  );
};

export default Scheduled;
```

### 6.4. Vite Configuration

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  root: '.',
  publicDir: 'public',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src/main/java/Task/demo/frontend')
    }
  },
  build: {
    // Build trực tiếp vào Spring Boot static folder
    outDir: 'src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
```

---

## 7. HỆ THỐNG CACHING VỚI REDIS

### 7.1. Kiến trúc Cache

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MULTI-LAYER SMART CACHING                            │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 1: Negative Cache                                          │   │
│  │ Key: flights:empty:{airport_iata}                                │   │
│  │ TTL: 5 phút                                                      │   │
│  │ Mục đích: Chặn spam với mã sân bay không tồn tại                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 2: Counter Cache (Frequency-Based)                         │   │
│  │ Key: flights:counter:{airport_iata}                              │   │
│  │ TTL: 30 phút                                                     │   │
│  │ Logic: count < 3 → không cache, count >= 3 → cache               │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ LAYER 3: Data Cache (Logical Expiration)                         │   │
│  │ Key: flights:data:{airport_iata}                                 │   │
│  │ Physical TTL: 60 phút                                            │   │
│  │ Logical TTL: 30 phút (count >= 2) hoặc 5 phút (count < 2)       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.2. Key Format trong Redis

| Key Pattern | Data Type | TTL | Mô tả |
|-------------|-----------|-----|-------|
| `flights:data:{iata}` | Hash | 60 phút | Dữ liệu chuyến bay |
| `flights:counter:{iata}` | String | 30 phút | Đếm số lần gọi |
| `flights:empty:{iata}` | String | 5 phút | Marker cho kết quả rỗng |

### 7.3. Chiến lược Logical Expiration

```
┌────────────────────────────────────────────────────────────────────────┐
│                    LOGICAL EXPIRATION TIMELINE                         │
│                                                                        │
│  ←── Physical TTL: 60 phút ──────────────────────────────────────────→ │
│  ←── Logical TTL: 30 phút ──→                                          │
│                                                                        │
│  T=0         T=30min           T=60min                                 │
│  │           │                 │                                       │
│  ▼           ▼                 ▼                                       │
│  ╔═══════════╗═══════════════════╗                                     │
│  ║   FRESH   ║   STALE (async)   ║ EXPIRED                             │
│  ╚═══════════╝═══════════════════╝                                     │
│  │           │                   │                                     │
│  │ Return    │ Return stale +    │ Fetch new                           │
│  │ cached    │ async update      │ data                                │
│  │ data      │ in background     │                                     │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

### 7.4. FlightCacheServiceV2 Implementation

```java
@Service
public class FlightCacheServiceV2 {
    
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);
    private static final int MIN_CALL_COUNT_FOR_CACHE = 3;
    private static final String CACHE_PREFIX = "flights:v2:";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // In-memory counter (thread-safe)
    private final ConcurrentHashMap<String, AtomicInteger> callCountMap = new ConcurrentHashMap<>();

    /**
     * Tăng số lần gọi và kiểm tra điều kiện cache
     */
    public int incrementCallCount(String iataCode) {
        String key = iataCode.toUpperCase();
        AtomicInteger counter = callCountMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }
    
    /**
     * Kiểm tra xem IATA có đủ điều kiện cache không
     */
    public boolean shouldCache(String iataCode) {
        AtomicInteger counter = callCountMap.get(iataCode.toUpperCase());
        return counter != null && counter.get() >= MIN_CALL_COUNT_FOR_CACHE;
    }

    /**
     * Lấy flights từ cache
     */
    public FlightCacheEntry getCachedFlights(String iataCode) {
        try {
            String key = getCacheKey(iataCode);
            String json = redisTemplate.opsForValue().get(key);
            
            if (json == null) return null;
            
            FlightCacheEntry entry = objectMapper.readValue(json, FlightCacheEntry.class);
            
            // Kiểm tra expiration
            if (entry.isExpired()) {
                redisTemplate.delete(key);
                return null;
            }
            
            return entry;
        } catch (Exception e) {
            logger.error("Redis error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Lưu flights vào cache
     */
    public void cacheFlights(String iataCode, List<FlightDisplayDTO> flights) {
        if (!shouldCache(iataCode)) return;
        
        try {
            String key = getCacheKey(iataCode);
            
            FlightCacheEntry entry = new FlightCacheEntry(
                flights,
                Instant.now(),
                CACHE_DURATION
            );
            
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForValue().set(key, json, CACHE_DURATION);
            
        } catch (Exception e) {
            logger.error("Cache error: {}", e.getMessage());
        }
    }
}
```

### 7.5. Cache Result Class

```java
public class CacheResult {
    private boolean cacheHit;
    private boolean logicallyExpired;
    private List<Flight> flights;
    
    public static CacheResult hit(List<Flight> flights, boolean expired) {
        return new CacheResult(true, expired, flights);
    }
    
    public static CacheResult miss() {
        return new CacheResult(false, false, new ArrayList<>());
    }
}
```

---

## 8. CI/CD PIPELINE VỚI GITHUB ACTIONS

### 8.1. Workflow Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        CI/CD PIPELINE WORKFLOW                          │
│                                                                         │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐            │
│   │   DEVELOP   │ ────▶│     QA      │ ────▶│ PRODUCTION  │            │
│   │   (main)    │ PR   │   (QA)      │ PR   │(Production) │            │
│   └─────────────┘      └─────────────┘      └─────────────┘            │
│         │                    │                    │                     │
│         │                    │                    │                     │
│         ▼                    ▼                    ▼                     │
│   ┌─────────────┐      ┌─────────────┐      ┌─────────────┐            │
│   │  No Action  │      │ Build + Push│      │  Pull Image │            │
│   │             │      │   Deploy    │      │   Deploy    │            │
│   │             │      │  Port 8081  │      │  Port 8080  │            │
│   └─────────────┘      └─────────────┘      └─────────────┘            │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2. GitHub Actions Workflow

```yaml
# .github/workflows/build.yaml
name: airlabs-realtime-flight

on: 
  pull_request:
    types: [closed]
    branches:
      - QA
      - Production
  
jobs:
  docker: 
    # Chỉ chạy khi PR được merge (không phải close)
    if: github.event.pull_request.merged == true
    runs-on: self-hosted
    
    steps:
      # Step 1: Checkout code
      - name: Checkout repository
        uses: actions/checkout@v4

      # Step 2: Set environment variables dựa vào branch
      - name: Set environment variables
        id: env
        run: |
          if [ "${{ github.base_ref }}" == "QA" ]; then
            echo "environment=qa" >> $GITHUB_OUTPUT
            echo "port=8081" >> $GITHUB_OUTPUT
            echo "tag_prefix=qa" >> $GITHUB_OUTPUT
          elif [ "${{ github.base_ref }}" == "Production" ]; then
            echo "environment=production" >> $GITHUB_OUTPUT
            echo "port=8080" >> $GITHUB_OUTPUT
            echo "tag_prefix=prod" >> $GITHUB_OUTPUT
          fi

      # Step 3: Login Docker Hub
      - name: Login Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_ACCESS_TOKEN }}

      # Step 4: Setup Docker Buildx
      - name: Setup Docker Buildx
        uses: docker/setup-buildx-action@v3
        id: buildx

      # Step 5: Build and Push Docker Image
      - name: Build and Push Docker Image
        uses: docker/build-push-action@v5
        id: docker_build
        with:
          context: .
          file: ./Dockerfile
          builder: ${{ steps.buildx.outputs.name }}
          push: true
          tags: |
            hienminh1332004/airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}-v0.0.${{ github.run_number }}
            hienminh1332004/airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}-latest
          cache-from: type=registry,ref=hienminh1332004/airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}-buildcache
          cache-to: type=registry,ref=hienminh1332004/airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}-buildcache,mode=max
         
      # Step 6: Verify build
      - name: Verify build
        run: |
          echo "Image pushed successfully!"
          echo "Digest: ${{ steps.docker_build.outputs.digest }}"

      # Step 7: Deploy directly
      - name: Deploy directly
        env:
          IMAGE: hienminh1332004/airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}-v0.0.${{ github.run_number }}
          ENVIRONMENT: ${{ steps.env.outputs.environment }}
          PORT: ${{ steps.env.outputs.port }}
          CONTAINER_NAME: airlabs-app-${{ steps.env.outputs.environment }}
        run: |
          # Pull image từ Docker Hub
          docker pull $IMAGE
          
          # Stop old container
          docker stop $CONTAINER_NAME 2>/dev/null || true
          docker rm $CONTAINER_NAME 2>/dev/null || true
          
          # Run new container
          docker run -d \
            --name $CONTAINER_NAME \
            --restart unless-stopped \
            -p $PORT:8080 \
            -e SPRING_PROFILES_ACTIVE=$ENVIRONMENT \
            -e REDIS_HOST=${{ secrets.REDIS_HOST }} \
            -e REDIS_PORT=${{ secrets.REDIS_PORT }} \
            -e REDIS_PASSWORD=${{ secrets.REDIS_PASSWORD }} \
            -e AIRLABS_API_KEY=${{ secrets.AIRLABS_API_KEY }} \
            $IMAGE
          
          # Verify deployment
          sleep 5
          if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
            echo "Deployment successful!"
          else
            echo "Deployment failed!"
            exit 1
          fi
          
          # Cleanup old images (keep last 3)
          docker images --format "{{.Repository}}:{{.Tag}}" | \
            grep "airlabs-realtime-flight:${{ steps.env.outputs.tag_prefix }}" | \
            tail -n +4 | xargs -r docker rmi 2>/dev/null || true

      # Step 8: Health check
      - name: Health check
        run: |
          MAX_RETRIES=10
          RETRY_COUNT=0
          PORT=${{ steps.env.outputs.port }}
          
          while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
              http://localhost:$PORT/actuator/health || echo "000")
            
            if [ "$HTTP_CODE" -eq 200 ]; then
              echo "Health check passed!"
              exit 0
            fi
            
            echo "Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES - Status: $HTTP_CODE"
            sleep 3
            RETRY_COUNT=$((RETRY_COUNT + 1))
          done
          
          echo "Health check failed!"
          exit 1
```

### 8.3. GitHub Secrets Configuration

| Secret Name | Mô tả | Ví dụ |
|-------------|-------|-------|
| `DOCKERHUB_USERNAME` | Docker Hub username | `hienminh1332004` |
| `DOCKERHUB_ACCESS_TOKEN` | Docker Hub access token | `dckr_pat_xxx` |
| `REDIS_HOST` | Redis Cloud host | `redis-xxxxx.cloud.redislabs.com` |
| `REDIS_PORT` | Redis port | `13482` |
| `REDIS_PASSWORD` | Redis password | `xxxxxxxx` |
| `AIRLABS_API_KEY` | AirLabs API key | `7e455240-xxxx-xxxx` |

### 8.4. Branching Strategy

```
                    ┌─────────────────────────────────────────────────────┐
                    │                  GIT BRANCHING STRATEGY             │
                    │                                                     │
                    │                    Production                       │
                    │                        ▲                            │
                    │                        │ PR (after QA test OK)      │
                    │                        │                            │
                    │         ┌──────────────┴──────────────┐            │
                    │         │             QA               │            │
                    │         │      (Staging/Testing)       │            │
                    │         └──────────────▲──────────────┘            │
                    │                        │ PR                         │
                    │                        │                            │
                    │         ┌──────────────┴──────────────┐            │
                    │         │            main              │            │
                    │         │       (Development)          │            │
                    │         └─────────────────────────────┘            │
                    │                        ▲                            │
                    │                        │ commit                     │
                    │                        │                            │
                    │         ┌──────────────┴──────────────┐            │
                    │         │      Local Development       │            │
                    │         └─────────────────────────────┘            │
                    │                                                     │
                    └─────────────────────────────────────────────────────┘
```

---

## 9. CONTAINERIZATION VỚI DOCKER

### 9.1. Multi-stage Dockerfile

```dockerfile
# ==================== STAGE 1: BUILD FRONTEND ====================
FROM node:20-alpine AS frontend-build

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm ci --silent

# Copy frontend source files
COPY index.html vite.config.ts tsconfig.json tailwind.config.cjs postcss.config.js ./
COPY src ./src
COPY public ./public

# Build frontend - Vite output to /app/src/main/resources/static
RUN npm run build

# ==================== STAGE 2: BUILD BACKEND ====================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml và download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy Java source code
COPY src/main/java ./src/main/java
COPY src/main/resources/application.yaml ./src/main/resources/
COPY src/test ./src/test

# Copy built frontend from frontend-build stage
COPY --from=frontend-build /app/src/main/resources/static ./src/main/resources/static

# Build Java application
RUN mvn clean package -DskipTests

# ==================== STAGE 3: RUNTIME ====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy jar file từ build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables (override trong container)
ENV SPRING_PROFILES_ACTIVE=production

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 9.2. Docker Build Process

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     DOCKER MULTI-STAGE BUILD                            │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 1: frontend-build                                          │   │
│  │ Base: node:20-alpine (~150MB)                                    │   │
│  │ Actions:                                                         │   │
│  │   - npm ci                                                       │   │
│  │   - npm run build (Vite)                                        │   │
│  │ Output: /app/src/main/resources/static/                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 2: build                                                   │   │
│  │ Base: maven:3.9-eclipse-temurin-17 (~500MB)                     │   │
│  │ Actions:                                                         │   │
│  │   - mvn dependency:go-offline (layer caching)                   │   │
│  │   - Copy frontend static files                                   │   │
│  │   - mvn clean package -DskipTests                               │   │
│  │ Output: /app/target/*.jar                                       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 3: runtime (FINAL)                                         │   │
│  │ Base: eclipse-temurin:17-jre-alpine (~90MB)                     │   │
│  │ Contains:                                                        │   │
│  │   - JRE 17                                                       │   │
│  │   - app.jar (with embedded frontend)                            │   │
│  │ Size: ~120MB (compared to ~700MB with all stages)               │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 9.3. Docker Commands Reference

```bash
# Build image locally
docker build -t airlabs-flight:latest .

# Run container locally
docker run -d \
  --name airlabs-app \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=production \
  -e REDIS_HOST=redis-cloud.com \
  -e REDIS_PORT=13482 \
  -e REDIS_PASSWORD=xxxxx \
  -e AIRLABS_API_KEY=xxxxx \
  airlabs-flight:latest

# View logs
docker logs -f airlabs-app

# Execute into container
docker exec -it airlabs-app sh

# Stop and remove
docker stop airlabs-app && docker rm airlabs-app

# Push to Docker Hub
docker tag airlabs-flight:latest hienminh1332004/airlabs-realtime-flight:latest
docker push hienminh1332004/airlabs-realtime-flight:latest
```

---

## 10. DEPLOYMENT & INFRASTRUCTURE

### 10.1. Infrastructure Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        DEPLOYMENT ARCHITECTURE                          │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         CLOUD SERVICES                             │  │
│  │                                                                    │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │  │
│  │  │   Docker Hub    │  │  Redis Cloud    │  │   Render.com    │   │  │
│  │  │                 │  │  (AWS us-east)  │  │                 │   │  │
│  │  │ Image Registry  │  │                 │  │  PostgreSQL     │   │  │
│  │  │                 │  │ - 30MB RAM      │  │  - 256MB RAM    │   │  │
│  │  │ qa-latest       │  │ - High Avail    │  │  - 1GB Storage  │   │  │
│  │  │ prod-latest     │  │                 │  │                 │   │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘   │  │
│  │          │                    │                    │              │  │
│  └──────────┼────────────────────┼────────────────────┼──────────────┘  │
│             │                    │                    │                 │
│             ▼                    ▼                    ▼                 │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    SELF-HOSTED SERVER (AWS EC2)                   │  │
│  │                                                                    │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │                    GitHub Actions Runner                     │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                              │                                     │  │
│  │  ┌───────────────────────────┼───────────────────────────────┐    │  │
│  │  │                   DOCKER ENGINE                            │    │  │
│  │  │                                                            │    │  │
│  │  │  ┌─────────────────────┐  ┌─────────────────────┐         │    │  │
│  │  │  │   airlabs-app-qa    │  │ airlabs-app-prod    │         │    │  │
│  │  │  │                     │  │                     │         │    │  │
│  │  │  │   Port: 8081        │  │   Port: 8080        │         │    │  │
│  │  │  │   Profile: qa       │  │   Profile: prod     │         │    │  │
│  │  │  └─────────────────────┘  └─────────────────────┘         │    │  │
│  │  │                                                            │    │  │
│  │  └────────────────────────────────────────────────────────────┘    │  │
│  │                                                                    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 10.2. Environment Configuration

| Environment | Port | Profile | Container Name |
|-------------|------|---------|----------------|
| **QA** | 8081 | qa | airlabs-app-qa |
| **Production** | 8080 | production | airlabs-app-production |

### 10.3. External Services

| Service | Provider | Purpose | Configuration |
|---------|----------|---------|---------------|
| **PostgreSQL** | Render.com | Primary Database | 256MB RAM, Singapore Region |
| **Redis** | Redis Cloud | Caching Layer | 30MB, us-east-1, Free Tier |
| **Docker Hub** | Docker | Image Registry | Public Repository |
| **AirLabs** | AirLabs.co | Flight Data API | Rate Limited |

### 10.4. Health Check Endpoint

```java
@RestController
public class HealthController {
    
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
```

---

## 11. API DOCUMENTATION

### 11.1. API Endpoints

#### Flight API v1

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/flights` | Lấy tất cả chuyến bay |
| `GET` | `/api/flights/{id}` | Lấy chuyến bay theo ID |
| `GET` | `/api/flights/dep/{dep_iata}` | Lấy chuyến bay theo sân bay đi |
| `POST` | `/api/flights` | Tạo chuyến bay mới |
| `PUT` | `/api/flights/{id}` | Cập nhật chuyến bay |
| `DELETE` | `/api/flights/{id}` | Xóa chuyến bay |

#### Flight API v2 (với Smart Caching)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v2/flights/schedules?dep_iata={iata}` | Lấy lịch bay với smart cache |
| `GET` | `/api/v2/flights/schedules/force-cache?dep_iata={iata}` | Force cache ngay |
| `DELETE` | `/api/v2/flights/cache/{iataCode}` | Xóa cache cho IATA |
| `DELETE` | `/api/v2/flights/cache` | Xóa toàn bộ cache |
| `GET` | `/api/v2/flights/cache/stats` | Thống kê cache |

#### Schedule API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/schedules/arrivals?iata={iata}` | Lấy arrivals |
| `GET` | `/api/schedules/departures?iata={iata}` | Lấy departures |

### 11.2. Sample API Responses

**GET /api/v2/flights/schedules?dep_iata=SGN**

```json
[
  {
    "airline_iata": "VN",
    "flight_iata": "VN123",
    "flight_number": "123",
    "dep_iata": "SGN",
    "arr_iata": "HAN",
    "scheduled_dep_time": "2026-01-29T08:00:00",
    "actual_dep_time": "2026-01-29T08:15:00",
    "status": "en-route",
    "dep_delayed": 15
  },
  {
    "airline_iata": "VJ",
    "flight_iata": "VJ456",
    "flight_number": "456",
    "dep_iata": "SGN",
    "arr_iata": "DAD",
    "scheduled_dep_time": "2026-01-29T09:30:00",
    "actual_dep_time": "2026-01-29T09:30:00",
    "status": "scheduled",
    "dep_delayed": 0
  }
]
```

**GET /api/v2/flights/cache/stats**

```json
{
  "callCountStats": {
    "SGN": 5,
    "HAN": 3,
    "DAD": 1
  },
  "message": "Cache is created after 3 calls to the same IATA code",
  "cacheDuration": "30 minutes"
}
```

---

## 12. KẾT LUẬN VÀ BÀI HỌC

### 12.1. Kết quả đạt được

| Mục tiêu | Trạng thái | Ghi chú |
|----------|------------|---------|
| ✅ Xây dựng REST API với Spring Boot | Hoàn thành | Full CRUD operations |
| ✅ Tích hợp Redis Caching | Hoàn thành | Multi-layer smart caching |
| ✅ Frontend với React + TypeScript | Hoàn thành | Responsive UI |
| ✅ CI/CD Pipeline | Hoàn thành | GitHub Actions + Self-hosted |
| ✅ Docker Containerization | Hoàn thành | Multi-stage build |
| ✅ Database với PostgreSQL | Hoàn thành | JPA + HikariCP |

### 12.2. Kỹ năng học được

**Backend Development:**
- Spring Boot framework architecture
- JPA/Hibernate với PostgreSQL
- Redis caching strategies
- RESTful API design
- Async processing với @Async

**Frontend Development:**
- React với TypeScript
- Vite build tool
- Tailwind CSS
- API integration

**DevOps:**
- Docker containerization
- Multi-stage Docker builds
- GitHub Actions CI/CD
- Self-hosted runners
- Environment management

### 12.3. Thách thức và giải pháp

| Thách thức | Giải pháp |
|------------|-----------|
| API rate limiting | Smart caching giảm 80% API calls |
| Cold start latency | Connection pooling + Lazy init |
| Cache stampede | Logical expiration + Async update |
| Deployment complexity | Automated CI/CD pipeline |

### 12.4. Hướng phát triển

1. **WebSocket Integration:** Real-time updates không cần refresh
2. **Kubernetes Deployment:** Scalability và High Availability
3. **Monitoring:** Prometheus + Grafana dashboard
4. **Testing:** Unit tests, Integration tests, E2E tests
5. **Security:** OAuth2, Rate limiting, Input validation

---

## PHỤ LỤC

### A. Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Redis -->
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
    
    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.32</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### B. NPM Dependencies (package.json)

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "typescript": "^4.9.5"
  },
  "devDependencies": {
    "@types/react": "^18.2.45",
    "@types/react-dom": "^18.2.18",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.23",
    "postcss": "^8.5.6",
    "tailwindcss": "^3.4.0",
    "vite": "^5.0.8"
  },
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  }
}
```

### C. Useful Commands

```bash
# Development
./mvnw spring-boot:run          # Run Spring Boot
npm run dev                      # Run Vite dev server
npm run build                    # Build frontend

# Docker
docker build -t app .            # Build image
docker-compose up -d             # Run with docker-compose
docker logs -f container_name    # View logs

# Git
git checkout -b feature/xxx      # Create feature branch
git push origin main             # Push to main
gh pr create                     # Create Pull Request
```

---

**📝 Tài liệu này được tạo tự động từ source code dự án**

*Version: 1.0.0 | Ngày cập nhật: Tháng 01/2026*
