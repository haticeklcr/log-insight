# Log Analiz Uygulaması (log-insight)

## Amaç
Kullanıcının yüklediği `.log` veya `.txt` uzantılı uygulama loglarını analiz eden bir REST API ve bu API'yi kullanan bir web arayüzü. Log seviyelerini (INFO/WARN/ERROR), exception içeren satırları ve tekrar eden hata mesajlarını tespit ederek sonucu hem JSON olarak hem de görsel bir arayüzde sunar. V3 ile birlikte her başarılı analiz PostgreSQL'de kalıcı olarak saklanır; geçmiş analizler listelenebilir, aranabilir, filtrelenebilir, detayları görüntülenebilir ve silinebilir. V4 ile birlikte log analizi artık arka planda çalışan asenkron bir **job** olarak yürütülür; kullanıcı her analize bir ad verir, job'ın durumunu (bekliyor/çalışıyor/tamamlandı/başarısız/iptal edildi) canlı olarak izleyebilir, iptal edebilir ya da başarısız olanları tekrar deneyebilir. Uygulama Türkçe ve İngilizce dillerini destekler.

## V4 ile Eklenen Özellikler
- Log analizinin arka planda çalışan asenkron bir **job** olarak yürütülmesi (kullanıcı isteği hemen `PENDING` durumunda bir job ID'siyle cevap alır, gerçek analiz ayrı bir thread havuzunda ilerler)
- Her analiz için zorunlu bir **analiz adı** (3-100 karakter, trim edilir, dosya adından bağımsız)
- Job durumları: `PENDING` → `RUNNING` → `SUCCEEDED`/`FAILED`/`CANCELLED`, veritabanında saklanır
- Log dosyalarının **streaming** (satır satır, tamamı belleğe alınmadan) işlenmesi, belirli aralıklarla progress güncellemesi
- Job **iptal etme** (bekleyen job anında, çalışan job işbirlikçi/cooperative şekilde iptal edilir) ve **retry** (sınırlı sayıda, başarısız job'lar için) akışları
- Uygulama yeniden başladığında yarım kalan (`RUNNING`) job'ların tespit edilip `FAILED` olarak işaretlenmesi, sahipsiz geçici dosyaların temizlenmesi
- Job geçmişini sayfalı listeleme, analiz adı/dosya adı/durum filtreleme
- Frontend'e "Analiz İşleri" sekmesi: job listesi, job detay ekranı (progress bar, geçen süre, iptal/retry/sonuç butonları), 2 saniyelik polling
- Türkçe ve İngilizce dil desteği (`i18next`), Header'da dil seçici, tercih `localStorage`'da kalıcı
- Yeni Liquibase migration'ları (`analysis_job` tablosu ve ilişkileri)
- Gerçek asenkron job akışını test eden yeni Testcontainers entegrasyon testleri

## V3 ile Eklenen Özellikler
- Her başarılı analiz sonucunun PostgreSQL'e kalıcı olarak kaydedilmesi (analiz + en sık hata kayıtları tek transaction içinde)
- Liquibase ile veritabanı migration yönetimi (Hibernate `ddl-auto` **kullanılmıyor**, tablolar yalnızca Liquibase ile oluşturuluyor)
- Analiz geçmişini sayfalı (pagination) listeleme, dosya adına göre arama, minimum hata sayısına göre filtreleme
- Tek bir analiz kaydının detayını görüntüleme
- Analiz kaydını kullanıcı onayıyla silme (ilişkili en-sık-hata kayıtları cascade ile birlikte silinir)
- Frontend'e "Analiz Geçmişi" sekmesi, sayfalama kontrolleri, arama/filtre çubuğu, detay ekranı ve silme onay penceresi
- PostgreSQL servisiyle genişletilmiş Docker Compose (named volume ile veri kalıcılığı)
- Gerçek PostgreSQL container'ı üzerinde çalışan Testcontainers entegrasyon testleri

## V2 ile Eklenen Özellikler
- Spring Boot Actuator ile sağlık kontrolü (`/actuator/health`)
- Dosya boyutu limitinin configuration/environment variable üzerinden yönetilmesi
- Standart, makine-okunur hata JSON formatı (`timestamp`, `status`, `error`, `message`, `path`)
- CORS desteği (frontend ile backend arasındaki iletişim için)
- React + TypeScript ile geliştirilmiş web arayüzü
- Docker ve Docker Compose ile tek komutla ayağa kaldırılabilen backend + frontend
- Nginx üzerinden reverse proxy yapılandırması
- Genişletilmiş backend testleri + yeni frontend testleri

## Kullanılan Teknolojiler

**Geliştirme ortamı:** Windows 10/11, WSL2, Ubuntu 22.04, VS Code + WSL eklentisi, Git, GitHub

**Backend:** Java 21, Spring Boot 4.1.0, Maven, Spring Web, Spring Validation, Spring Data JPA, Hibernate, Liquibase, Spring Boot Actuator, Spring Async, JUnit 5, Mockito, AssertJ, Testcontainers

**Frontend:** Node.js LTS, npm, React, TypeScript, Vite, Fetch API, CSS Modules, i18next, react-i18next, Vitest, React Testing Library

**Veritabanı ve container:** PostgreSQL 16, Docker, Docker Compose, Nginx

## Proje Klasör Yapısı
```
log-insight/
├── backend/                                  # Spring Boot REST API
│   ├── src/main/java/com/hatice/loginsight/
│   │   ├── controller/                        # HTTP endpoint'leri (LogAnalysisController, AnalysisHistoryController, AnalysisJobController)
│   │   ├── service/                           # İş mantığı (LogAnalysisService, AnalysisHistoryService, AnalysisJobService, AnalysisJobRunner, JobStateMachine, TempFileStorageService, StartupRecoveryService)
│   │   ├── repository/                        # Spring Data JPA repository'leri
│   │   ├── entity/                             # JPA entity'leri (LogAnalysisEntity, FrequentErrorEntity, AnalysisJobEntity, JobStatus)
│   │   ├── dto/                                 # Veri transfer nesneleri (entity'ler dışa hiç açılmaz)
│   │   ├── exception/                          # Özel exception'lar + merkezi hata yönetimi
│   │   └── config/                              # CORS, async executor gibi uygulama genelinde ayarlar
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── db/changelog/                        # Liquibase migration dosyaları
│   ├── src/test/java/                          # Backend testleri (Testcontainers dahil)
│   ├── sample.log                               # Örnek log dosyası
│   ├── sample2.log                              # İkinci örnek log dosyası (büyük, karışık seviyeli)
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                                    # React + TypeScript web arayüzü
│   ├── src/
│   │   ├── components/                          # Header, FileUpload, NewAnalysisFlow, JobsListView, JobDetailView, HistoryView, AnalysisDetailView, LanguageSwitcher, vb.
│   │   ├── services/                            # Backend ile iletişim (logAnalysisApi.ts, analysisJobApi.ts)
│   │   ├── hooks/                                # useJobPolling.ts
│   │   ├── i18n/                                 # i18next kurulumu + locales/tr.json, locales/en.json
│   │   ├── utils/                                # apiErrorMessage.ts, format.ts
│   │   ├── types/                               # TypeScript tip tanımları
│   │   └── App.tsx
│   ├── nginx.conf                                # Nginx reverse proxy yapılandırması
│   ├── package.json
│   └── Dockerfile
├── screenshots/                                  # Uygulama ekran görüntüleri
├── docker-compose.yml
├── .env.example
└── README.md
```

## Veritabanı Yapısı

**`log_analysis`** — her analiz işleminin özet bilgisi

| Alan | Tip | Açıklama |
|---|---|---|
| `id` | BIGINT (PK, auto increment) | |
| `file_name` | VARCHAR(255) | |
| `file_size` | BIGINT | bayt cinsinden |
| `total_lines` | INT | |
| `info_count` / `warning_count` / `error_count` / `exception_count` | INT | |
| `analyzed_at` | TIMESTAMP | |
| `processing_duration_ms` | BIGINT | analiz süresi (milisaniye) |
| `analysis_name` | VARCHAR(100) | (V4) kullanıcının verdiği analiz adı |

**`frequent_error`** — bir analize ait en sık tekrar eden hata mesajları

| Alan | Tip | Açıklama |
|---|---|---|
| `id` | BIGINT (PK, auto increment) | |
| `analysis_id` | BIGINT (FK → `log_analysis.id`, `ON DELETE CASCADE`) | |
| `message` | VARCHAR(1000) | |
| `occurrence_count` | INT | |

**`analysis_job`** (V4) — her asenkron analiz işinin durumu ve geçmişi

| Alan | Tip | Açıklama |
|---|---|---|
| `id` | UUID (PK) | |
| `analysis_name` | VARCHAR(100) | |
| `file_name` | VARCHAR(255) | |
| `file_size` | BIGINT | |
| `status` | VARCHAR(20) | `PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`/`CANCELLED` |
| `progress` | INT | 0-100 |
| `retry_count` | INT | |
| `created_at` / `started_at` / `completed_at` | TIMESTAMP | |
| `error_code` / `error_message` | VARCHAR | |
| `analysis_id` | BIGINT (FK → `log_analysis.id`, `ON DELETE SET NULL`) | başarılı job'ı sonucuna bağlar |
| `cancel_requested` | BOOLEAN | |
| `version` | BIGINT | optimistic locking için |

Log dosyasının ham içeriği veritabanında saklanmaz — yalnızca analiz sonucu ve dosya metadata'sı kaydedilir.

**Index'ler:** `log_analysis.analyzed_at`, `log_analysis.file_name`, `log_analysis.error_count`, `analysis_job.status`, `analysis_job.created_at`.

### Entity İlişkilerinin Kısa Açıklaması
`LogAnalysisEntity` ile `FrequentErrorEntity` arasında bire-çok (`@OneToMany`) ilişki var; `cascade = CascadeType.ALL` ve `orphanRemoval = true` sayesinde bir analiz kaydı silindiğinde ilişkili `frequent_error` satırları da JPA seviyesinde otomatik olarak silinir. Aynı davranış veritabanı seviyesinde de `ON DELETE CASCADE` foreign key'i ile güvence altına alınmıştır.

`analysis_job.analysis_id` foreign key'i ise `ON DELETE SET NULL` — bir analiz sonucu "Analiz Geçmişi"nden silinse bile, onu üreten job kaydı (ne zaman çalıştığı, süresi, retry sayısı gibi geçmiş bilgisi) korunur; sadece artık var olmayan sonuca işaret eden `analysis_id` `NULL`'a düşer. Frontend'de job listesindeki "Sonuç" butonu, job listesi DTO'sundaki `analysisId` alanı `null` ise pasif hale gelir.

## Liquibase Migration Yapısı

```
backend/src/main/resources/db/changelog/
├── db.changelog-master.yaml                       # Diğer tüm changelog'ları include eder
├── 001-create-log-analysis-table.yaml             # log_analysis tablosu
├── 002-create-frequent-error-table.yaml           # frequent_error tablosu + foreign key
├── 003-add-analysis-indexes.yaml                  # analyzed_at / file_name / error_count index'leri
├── 004-create-analysis-job-table.yaml             # analysis_job tablosu (V4)
├── 005-add-analysis-job-indexes.yaml              # status / created_at index'leri (V4)
├── 006-add-job-analysis-relation.yaml             # analysis_job → log_analysis foreign key (V4)
├── 007-add-analysis-name-to-log-analysis.yaml     # log_analysis.analysis_name sütunu (V4)
└── 008-fix-analysis-job-fk-on-delete.yaml         # foreign key'i ON DELETE SET NULL yapar (V4)
```

Her changeSet için bir `rollback` bloğu tanımlıdır. `spring.jpa.hibernate.ddl-auto=validate` olarak ayarlanmıştır — Hibernate hiçbir zaman şema oluşturmaz, sadece entity'lerin Liquibase tarafından oluşturulan şemayla eşleştiğini doğrular. Uygulama her başlatıldığında Liquibase, henüz uygulanmamış migration'ları otomatik olarak çalıştırır.

## Asenkron Analiz Mimarisi

Log analizi artık HTTP isteğini karşılayan thread'de değil, ayrı bir **thread havuzunda** (`@Async`) yürütülüyor:

1. `POST /api/v1/analysis-jobs` çağrıldığında: dosya validasyonu + analiz adı validasyonu yapılır, `PENDING` durumunda bir `analysis_job` kaydı oluşturulur, dosya `ANALYSIS_TEMP_DIRECTORY`'e geçici olarak yazılır, arka plan işi tetiklenir ve **hemen** (analiz beklemeden) cevap dönülür.
2. Arka plandaki iş (`AnalysisJobRunner`), job'ı `RUNNING`'e çekip dosyayı **satır satır (streaming)** okur; belirli aralıklarla (`ANALYSIS_JOB_PROGRESS_INTERVAL` satırda bir) hem progress'i günceller hem iptal isteğini kontrol eder.
3. İş bitince duruma göre `SUCCEEDED` (bir `log_analysis` kaydı oluşturularak), `FAILED` ya da `CANCELLED` olarak sonuçlanır.

### Job Durumları ve Geçişleri

| Geçiş | Ne zaman |
|---|---|
| `PENDING → RUNNING` | Arka plan thread'i işi ele aldığında |
| `RUNNING → SUCCEEDED` | Analiz sorunsuz bitince |
| `RUNNING → FAILED` | Analiz sırasında hata oluşunca, ya da uygulama yeniden başlarsa (`APPLICATION_RESTARTED_DURING_ANALYSIS`) |
| `PENDING → CANCELLED` | Henüz başlamamış bir job iptal edilince (anında) |
| `RUNNING → CANCELLED` | Çalışan bir job'a iptal talebi ulaşıp bir sonraki kontrol noktasında işlenince |
| `FAILED → PENDING` | Retry ile (yalnızca `FAILED` job'lar, sınırlı sayıda) |

Tüm geçiş kuralları `JobStateMachine` sınıfında merkezi olarak tutulur — örneğin `SUCCEEDED` bir job iptal edilemez, `PENDING`/`RUNNING` bir job retry edilemez.

### Thread Pool Configuration

| Environment Variable | Varsayılan | Açıklama |
|---|---|---|
| `ANALYSIS_EXECUTOR_CORE_POOL_SIZE` | `2` | Havuzda her an hazır bekleyen minimum thread sayısı |
| `ANALYSIS_EXECUTOR_MAX_POOL_SIZE` | `4` | Yoğunlukta çıkılabilecek maksimum thread sayısı |
| `ANALYSIS_EXECUTOR_QUEUE_CAPACITY` | `50` | Tüm thread'ler doluyken bekleyebilecek maksimum iş sayısı |
| `ANALYSIS_EXECUTOR_THREAD_NAME_PREFIX` | `log-analysis-` | Oluşturulan thread'lerin isim öneki (loglarda ayırt etmek için) |
| `ANALYSIS_JOB_MAX_RETRY` | `3` | Bir job'ın en fazla kaç kez retry edilebileceği |
| `ANALYSIS_JOB_PROGRESS_INTERVAL` | `100` | Kaç satırda bir progress/iptal kontrolü yapılacağı |

### Geçici Dosya Yönetimi

Yüklenen dosya, analiz arka planda bitene kadar `ANALYSIS_TEMP_DIRECTORY` (varsayılan `/tmp/log-insight`) altında, **job ID'siyle** (kullanıcının gönderdiği dosya adıyla değil) adlandırılarak saklanır — bu, path traversal saldırılarına karşı bir önlemdir. Dosya, job `SUCCEEDED`/`CANCELLED` olunca ya da uygulama yeniden başlayıp yarım kalan job'ı `FAILED` yapınca silinir; `FAILED` bir job'ın dosyası retry ihtimaline karşı **silinmez**. Uygulama her başladığında, hiçbir `PENDING` job'a karşılık gelmeyen "sahipsiz" geçici dosyalar da otomatik temizlenir.

## Development Ortamında Backend'i Çalıştırma

PostgreSQL'in ayrıca ayakta olması gerekir (bkz. [PostgreSQL Bağlantı Bilgileri](#postgresql-bağlantı-bilgileri)):

```bash
cd backend
./mvnw spring-boot:run
```

Backend `http://localhost:8080` adresinde ayağa kalkar.

### Backend Testlerini Çalıştırma

```bash
cd backend
./mvnw clean test
```

Docker Desktop'ın çalışıyor olması gerekir (bkz. [Testcontainers Testlerinin Çalıştırılması](#testcontainers-testlerinin-çalıştırılması)).

## Development Ortamında Frontend'i Çalıştırma

```bash
cd frontend
npm install
npm run dev
```

Frontend `http://localhost:5173` adresinde ayağa kalkar. Backend'in de aynı anda `http://localhost:8080`'de çalışıyor olması gerekir (CORS ayarları bunun için yapılandırılmıştır).

### Frontend Testlerini Çalıştırma

```bash
cd frontend
npm test
```

## PostgreSQL Bağlantı Bilgileri

Development ortamında yerel bir PostgreSQL örneğine ihtiyaç var (Docker ile hızlıca ayağa kaldırılabilir):

```bash
docker run --name log-insight-postgres-manual -e POSTGRES_DB=loginsight \
  -e POSTGRES_USER=loginsight -e POSTGRES_PASSWORD=loginsight \
  -p 5432:5432 -d postgres:16-alpine
```

Backend, `application.properties`'teki şu varsayılanlarla `localhost:5432`'ye bağlanır (aşağıdaki environment variable'larla değiştirilebilir): `DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=loginsight`, `DB_USERNAME=loginsight`, `DB_PASSWORD=loginsight`.

## Environment Variable Açıklamaları

| Değişken | Nerede Kullanılır | Varsayılan | Açıklama |
|---|---|---|---|
| `VITE_API_BASE_URL` | Frontend (development) | `http://localhost:8080` | Frontend'in backend'e istek atarken kullandığı taban adres. `frontend/.env` dosyasında tanımlanır, Docker ortamında kullanılmaz (Nginx proxy devrede olduğu için boş bırakılır). |
| `APP_LOG_ANALYSIS_MAX_FILE_SIZE` | Backend | `10MB` | Yüklenebilecek maksimum dosya boyutu. |
| `APP_CORS_ALLOWED_ORIGINS` | Backend | `http://localhost:5173` | Backend'e istek atmasına izin verilen frontend adresi (CORS). Docker Compose'da `http://localhost:3000` olarak ayarlanır. |
| `DB_HOST` | Backend | `localhost` | PostgreSQL sunucu adresi. Docker Compose'da `postgres` (servis adı). |
| `DB_PORT` | Backend | `5432` | PostgreSQL portu. |
| `DB_NAME` | Backend, PostgreSQL | `loginsight` | Veritabanı adı. |
| `DB_USERNAME` | Backend, PostgreSQL | `loginsight` | Veritabanı kullanıcı adı. |
| `DB_PASSWORD` | Backend, PostgreSQL | — | Veritabanı şifresi. Repo'da gerçek değer bulunmaz; `.env.example` yalnızca örnek gösterir. |
| `ANALYSIS_EXECUTOR_CORE_POOL_SIZE` | Backend | `2` | Asenkron analiz thread havuzunun minimum boyutu. |
| `ANALYSIS_EXECUTOR_MAX_POOL_SIZE` | Backend | `4` | Asenkron analiz thread havuzunun maksimum boyutu. |
| `ANALYSIS_EXECUTOR_QUEUE_CAPACITY` | Backend | `50` | Thread havuzu dolduğunda bekleyebilecek maksimum iş sayısı. |
| `ANALYSIS_EXECUTOR_THREAD_NAME_PREFIX` | Backend | `log-analysis-` | Oluşturulan thread'lerin isim öneki. |
| `ANALYSIS_JOB_MAX_RETRY` | Backend | `3` | Bir job'ın maksimum retry sayısı. |
| `ANALYSIS_JOB_PROGRESS_INTERVAL` | Backend | `100` | Progress/iptal kontrolünün kaç satırda bir yapılacağı. |
| `ANALYSIS_TEMP_DIRECTORY` | Backend | `/tmp/log-insight` | Yüklenen dosyaların analiz tamamlanana kadar saklandığı geçici klasör. |

`.env.example` dosyası (proje kökünde), gerçek değerler olmadan hangi değişkenlerin gerektiğini gösterir; gerçek `.env` dosyası `.gitignore` ile git'e dahil edilmez.

## Docker Image Oluşturma ve Docker Compose ile Çalıştırma

Projeyi Docker ile ayağa kaldırmak için kök dizinde:

```bash
docker compose up --build
```

Bu komut:
- `backend/Dockerfile`'ı kullanarak backend image'ını (multi-stage: Maven build + JRE runtime) inşa eder.
- `frontend/Dockerfile`'ı kullanarak frontend image'ını (multi-stage: npm build + Nginx runtime) inşa eder.
- Resmi `postgres:16-alpine` image'ını kullanarak `postgres` servisini başlatır.
- Üç container'ı birbirine bağlı şekilde başlatır; backend, postgres'in sağlıklı olmasını, frontend de backend'in sağlıklı olmasını bekler.

Ortamı kapatmak için (veriler korunur):
```bash
docker compose down
```

### Named Volume Davranışı

PostgreSQL verisi `log-insight-postgres-data` adlı named volume'da saklanır. `docker compose down` ve ardından tekrar `docker compose up` yapıldığında veriler **korunur** (volume silinmez). Veritabanını tamamen sıfırlamak (tüm analiz geçmişini ve job kayıtlarını silmek) için:

```bash
docker compose down -v
```

## Uygulama Erişim Adresleri

| Servis | Adres |
|---|---|
| Frontend (Docker) | http://localhost:3000 |
| Backend (Docker/Development) | http://localhost:8080 |
| Backend Health Check | http://localhost:8080/actuator/health |
| Frontend (Development, `npm run dev`) | http://localhost:5173 |

## Endpoint'ler

### Analiz Yapma (senkron, V1'den beri)

**POST** `/api/v1/logs/analyze`
Content-Type: `multipart/form-data`
Form alanı: `file`

#### Örnek İstek
```bash
curl -X POST -F "file=@backend/sample.log" http://localhost:8080/api/v1/logs/analyze
```

#### Örnek Cevap
```json
{
  "id": 1,
  "fileName": "sample.log",
  "totalLines": 9,
  "infoCount": 3,
  "warningCount": 1,
  "errorCount": 4,
  "exceptionCount": 1,
  "mostFrequentErrors": [
    { "message": "Connection refused: database unreachable", "count": 2 },
    { "message": "Request timeout", "count": 1 },
    { "message": "NullPointerException at LogService.java:42", "count": 1 }
  ]
}
```

#### Örnek Hata Cevabı
```json
{
  "timestamp": "2026-07-22T15:04:23.000Z",
  "status": 400,
  "error": "EMPTY_FILE",
  "message": "Yüklenen dosya boş",
  "path": "/api/v1/logs/analyze"
}
```

### Analiz Geçmişi Endpoint'leri (V3)

**GET** `/api/v1/analyses` — sayfalı listeleme, arama ve filtreleme

| Parametre | Zorunlu mu | Açıklama |
|---|---|---|
| `page` | hayır (varsayılan `0`) | sayfa numarası |
| `size` | hayır (varsayılan `20`) | sayfa başına kayıt |
| `sort` | hayır (varsayılan `analyzedAt,desc`) | `alan,yön` formatında sıralama |
| `fileName` | hayır | dosya adına göre (büyük/küçük harf duyarsız, kısmi eşleşme) arama |
| `minErrorCount` | hayır | bu sayı ve üzerinde hata içeren kayıtları filtreleme |

```bash
curl "http://localhost:8080/api/v1/analyses?page=0&size=20&sort=analyzedAt,desc&fileName=app&minErrorCount=5"
```

Cevap `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` alanlarını içerir.

**GET** `/api/v1/analyses/{id}` — tek bir analizin detayını döner. Kayıt yoksa `404 Not Found`.

**DELETE** `/api/v1/analyses/{id}` — analiz kaydını ve ilişkili en-sık-hata kayıtlarını siler. Başarılı silmede `204 No Content`, kayıt yoksa `404 Not Found`.

### Analiz Job Endpoint'leri (V4)

**POST** `/api/v1/analysis-jobs` — yeni bir asenkron analiz job'ı oluşturur, hemen (analiz beklemeden) cevap döner.
Content-Type: `multipart/form-data`, form alanları: `file`, `analysisName` (zorunlu, 3-100 karakter, trim edilir)

```bash
curl -X POST -F "file=@backend/sample.log" -F "analysisName=Test Analizi" http://localhost:8080/api/v1/analysis-jobs
```
```json
{
  "jobId": "d1a86b2d-7747-4c58-9af5-c1b81c41b100",
  "analysisName": "Test Analizi",
  "status": "PENDING",
  "progress": 0,
  "createdAt": "2026-01-01T12:00:00Z"
}
```

**GET** `/api/v1/analysis-jobs/{id}` — job'ın güncel durumunu ve tüm detaylarını döner. Kayıt yoksa `404 Not Found` (`JOB_NOT_FOUND`).

**GET** `/api/v1/analysis-jobs` — sayfalı job listesi. Parametreler: `page`, `size`, `sort` (varsayılan `createdAt,desc`), `analysisName`, `fileName`, `status` (`PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`/`CANCELLED`).

**POST** `/api/v1/analysis-jobs/{id}/cancel` — `PENDING` bir job'ı anında iptal eder; `RUNNING` bir job için iptal talebi oluşturur (bir sonraki kontrol noktasında işlenir). Geçersiz durumdaki bir job için `409 Conflict` (`INVALID_JOB_STATE`).

**POST** `/api/v1/analysis-jobs/{id}/retry` — yalnızca `FAILED` bir job'ı tekrar `PENDING`'e alır (aynı job ID'siyle). Retry limiti aşılmışsa `409 Conflict` (`RETRY_LIMIT_EXCEEDED`).

## Frontend Geçmiş ve Detay Ekranları

- **Analiz Geçmişi** — kayıtlı tüm analizlerin listesi. Her satırda dosya adı, analiz tarihi, dosya boyutu, toplam satır, ERROR sayısı, exception sayısı, işlem süresi ile birlikte bir **Detay** ve bir **Sil** butonu bulunur. Sayfa altında önceki/sonraki sayfa butonları ve mevcut/toplam sayfa bilgisi gösterilir. Üstteki arama çubuğuyla dosya adına göre arama ve minimum ERROR sayısına göre filtreleme yapılabilir.
- **Detay butonuna** basıldığında, o analizin tüm bilgileri (dosya metadata'sı, tüm sayaçlar, en sık hata mesajları tablosu) ayrı bir ekranda gösterilir.
- **Sil butonuna** basıldığında önce bir onay penceresi açılır; onaylanırsa kayıt silinir, başarı mesajı gösterilir ve liste otomatik olarak yenilenir.
- Liste; yükleniyor (loading), boş (empty state), hata (backend erişilemiyor veya beklenmeyen sunucu hatası) durumlarını ayrı ayrı, kullanıcı dostu şekilde ele alır.

## Frontend Analiz İşleri Ekranları (V4)

Uygulama artık üç ana görünüme sahip: **Yeni Analiz**, **Analiz İşleri**, **Analiz Geçmişi**.

- **Yeni Analiz** artık senkron değil — kullanıcı bir analiz adı girip dosya seçtiğinde, bir job oluşturulur ve kullanıcı otomatik olarak **Analiz İşleri** sekmesindeki job takip ekranına yönlendirilir.
- **Analiz İşleri listesi** — analiz adı, dosya adı, durum, progress, oluşturulma/başlama/tamamlanma zamanı, retry sayısı ve **Detay/İptal/Retry/Sonuç** butonlarını gösterir; butonlar job durumuna göre aktif/pasif olur (örn. yalnızca `FAILED` job'da Retry aktif, "Sonuç" yalnızca `SUCCEEDED` **ve** bağlı analiz sonucu hâlâ mevcutsa aktif). Analiz adına, dosya adına ve duruma göre filtrelenebilir.
- **Job Detay ekranı** — tüm job bilgileri, `PENDING`/`RUNNING` durumunda bir progress bar, geçen süre, hata bilgisi (varsa, kullanıcı dostu çevrilmiş biçimde) ve `SUCCEEDED` job için "Sonucu Görüntüle" butonu (Analiz Geçmişi'ndeki detay ekranına yönlendirir; analiz sonucu ayrıca silinmişse bu buton görünmez).
- **Polling** — job takip ekranı, job'ın durumunu her **2 saniyede bir** sorgular; job terminal bir duruma (`SUCCEEDED`/`FAILED`/`CANCELLED`) ulaşınca polling otomatik durur; ekran kapatılınca (component unmount) da temizlenir; backend'e ulaşılamazsa kullanıcıya bilgi verilir.

## Backend Testleri

```bash
cd backend
./mvnw clean test
```

V1/V2/V3'ten gelen tüm testler (dosya validasyonu, log sayaçları, hata gruplama, Actuator health, standart hata formatı, analiz geçmişi CRUD'u, Liquibase migration'ları) korunmuştur. V4 ile eklenen test senaryoları:
- Analiz adı validasyonu (zorunlu, min/max uzunluk, trim edilerek kaydedilmesi)
- Job'ın `PENDING` durumunda kaydedilmesi, `PENDING → RUNNING → SUCCEEDED`/`FAILED` geçişleri
- Job progress değerinin arka planda güncellenmesi
- Başarılı job'ın analiz kaydı oluşturması; başarısız/iptal edilen job'ın oluşturmaması
- `PENDING` job'ın anında iptal edilmesi; `RUNNING` job için iptal talebinin oluşturulup arka planda işlenmesi
- `SUCCEEDED` job'ın iptal edilememesi
- `FAILED` job'ın retry edilmesi, retry limitinin uygulanması, `PENDING`/`RUNNING` job'ın retry edilememesi
- Job listesinin pagination ve durum filtresiyle dönmesi
- Uygulama başlangıcında `RUNNING` job'ların `FAILED`'a alınması ve sahipsiz geçici dosyaların temizlenmesi
- `JobStateMachine`'in tüm durum geçiş kurallarının izole (veritabanına dokunmadan) test edilmesi

### Testcontainers Testlerinin Çalıştırılması

Veritabanı testleri, gerçek bir PostgreSQL örneğini geçici bir Docker container'ında (Testcontainers ile) başlatarak çalışır — mock veya in-memory veritabanı kullanılmaz. Container, `AbstractIntegrationTest`'te "singleton container" deseniyle (JVM başına bir kez, elle) başlatılır ve tüm test sınıfları arasında paylaşılır. Bunun için Docker Desktop (ya da WSL2 üzerinde Docker) çalışır durumda olmalı; başka hiçbir manuel adım gerekmez.

## Frontend Testleri

```bash
cd frontend
npm test
```

V1/V2/V3'ten gelen tüm testler korunmuştur. V4 ile eklenen senaryolar:
- Analiz adı girilerek job oluşturma akışı, analiz adı boş/kısa/uzun bırakıldığında validation mesajı gösterilmesi, adın trim edilerek gönderilmesi
- Job listesinin yüklenmesi, boş liste ve backend erişilemezlik durumları
- Job durumuna göre İptal/Retry/Sonuç butonlarının aktif/pasif olması (`PENDING`, `FAILED`, `SUCCEEDED` senaryoları ayrı ayrı)
- Job detay ekranında analiz adının, durumun, `RUNNING`'de progress bar'ın gösterilmesi (`SUCCEEDED`'da gösterilmemesi)
- Yalnızca `FAILED` durumda "Tekrar Dene" butonunun gösterilmesi, `SUCCEEDED` job'dan "Sonucu Görüntüle" ile analiz detayına geçiş
- Polling'in 2 saniyede bir tekrarlanması, terminal durumda durması, component unmount olunca temizlenmesi (`vi.useFakeTimers` ile)

Testlerde gerçek backend yerine API mock'ları kullanılır. Not: Türkçe/İngilizce dil geçişi ve kalıcılığı yalnızca tarayıcıda elle doğrulanmıştır, ayrı otomatik testler yazılmamıştır (bkz. Bilinen Eksikler).

## Nginx Proxy Yapısının Kısa Açıklaması

Development ortamında frontend (`5173`) ve backend (`8080`) farklı portlarda çalıştığı için, tarayıcı bunları farklı origin olarak görüyor ve CORS izni gerekiyor. Docker/production ortamında bu ihtiyacı ortadan kaldırmak için Nginx, hem frontend'in statik dosyalarını (`dist/` çıktısı) sunuyor hem de `/api/` ve `/actuator/` ile başlayan istekleri arka planda backend container'ına yönlendiriyor (reverse proxy). Böylece tarayıcı, tek bir origin'e (`localhost:3000`) konuşuyormuş gibi davranıyor, CORS'a gerek kalmıyor.

## Türkçe ve İngilizce Dil Desteği

Uygulama yalnızca Türkçe (`tr`, varsayılan) ve İngilizce (`en`) dillerini destekler; `i18next` + `react-i18next` ile yönetilir.

### Çeviri Dosyalarının Yapısı
```
frontend/src/i18n/
├── index.ts               # i18next kurulumu, localStorage'dan/varsayılan dilden başlatma
└── locales/
    ├── tr.json
    └── en.json
```

- Kullanıcının seçtiği dil (Header'daki TR/EN butonları) `localStorage`'a (`log-insight-language`) yazılır; sayfa yenilendiğinde oradan okunur. Geçersiz/desteklenmeyen bir değer varsa Türkçe'ye düşülür.
- Job durumları (`jobStatus` namespace'i) ve backend'in döndürdüğü tüm `errorCode`'lar (`errors` namespace'i) iki dilde de çevrilidir; frontend önce `errorCode`'a göre çeviri arar, bulamazsa backend'in ham `message` alanına düşer (`utils/apiErrorMessage.ts`).
- Menü başlıkları, form etiketleri/validation mesajları, buton metinleri, tablo başlıkları, loading/empty-state mesajları, onay pencereleri dahil kullanıcıya görünen hiçbir metin component içine sabit yazılmaz — hepsi `t("namespace.key")` üzerinden gelir.

## Ekran Görüntüleri

### Başarılı Analiz Ekranı
![Başarılı analiz](screenshots/successful-analysis.png)

### Hata Durumu — Desteklenmeyen Dosya Türü
![Desteklenmeyen dosya türü](screenshots/error-unsupported-file.png)

### Hata Durumu — Backend Servisine Erişilememesi
![Backend erişilemiyor](screenshots/error-backend-down.png)

### Analiz Geçmişi Listesi
![Analiz geçmişi](screenshots/analysis-history.png)

### Analiz Detay Ekranı
![Analiz detayı](screenshots/analysis-detail.png)

### Silme Onay Penceresi
![Silme onayı](screenshots/delete-confirm.png)

### Analiz İşleri Listesi (V4)
![Analiz işleri listesi](screenshots/jobs-list.png)

### Çalışan Job — Progress Ekranı (V4)
![Çalışan job progress](screenshots/job-running-progress.png)

### Başarısız Job Ekranı (V4)
![Başarısız job](screenshots/job-failed.png)

### Retry Akışı (V4)
![Retry akışı](screenshots/job-retry.png)

### Türkçe Arayüz (V4)
![Türkçe arayüz](screenshots/ui-turkish.png)

### İngilizce Arayüz (V4)
![İngilizce arayüz](screenshots/ui-english.png)

## Bilinen Eksikler
- Sürükle-bırak (drag-and-drop) desteği eklendi ancak farklı tarayıcılarda kapsamlı test edilmedi.
- `mostFrequentErrors` listesinde üst sınır (örn. ilk 10) uygulanmıyor; çok sayıda benzersiz hata mesajı olan büyük dosyalarda liste uzun olabilir.
- Frontend, backend health check'i sadece sayfa ilk yüklendiğinde kontrol ediyor; periyodik otomatik yenileme yapmıyor.
- Analiz geçmişi ve analiz işleri listelerinde toplu (birden fazla kaydı aynı anda) silme/iptal/retry desteği yok; kayıtlar tek tek işlenebiliyor.
- Türkçe/İngilizce dil geçişi ve kalıcılığı için ayrı otomatik testler yazılmadı, yalnızca tarayıcıda elle doğrulandı.
- `useJobPolling` hook'u, polling zaten bir hata mesajı gösteriyorken kullanıcı dil değiştirirse, ekrandaki mesajı hemen değil bir sonraki başarısız denemede yeni dile çevirir (bilinçli, küçük bir basitleştirme — bkz. kod içi yorum).
- Job geçmişinde otomatik arşivleme/eskimiş kayıtları temizleme mekanizması yok; `analysis_job` tablosu süresiz büyür.

## Karşılaşılan Sorunlar ve Çözümleri

### V4'e Özgü Sorunlar

- **`AnalysisJobRunner`'da ilk çalıştırmada `ObjectOptimisticLockingFailureException`:** Job `RUNNING`'e çekilip kaydedildikten sonra, `save()`'in döndürdüğü güncel nesne bir değişkene geri atanmadığı için, sonraki her kaydetme denemesi "bayat" (eski version numaralı) bir kopyayla yapılıyordu. `job = analysisJobRepository.save(job);` şeklinde sonucun her zaman geri atanmasıyla çözüldü.
- **Retry sonrası analiz tekrar çalıştırılamıyordu:** İlk tasarımda her bitiş senaryosunda (başarı/hata/iptal) geçici dosya siliniyordu; ama bu, `FAILED` bir job'ın retry'ı için gereken dosyayı da yok ediyordu. Çözüm: `FAILED` durumunda dosya silinmiyor, yalnızca `SUCCEEDED`/`CANCELLED` durumunda ve uygulama yeniden başlayınca (restart recovery) siliniyor.
- **`cancelJob()` çağrısı `RUNNING` bir job için bazen sessizce etkisiz kalıyordu (job hiç `CANCELLED` olmuyordu):** Kanıt: kaydedilmiş loglarda `ObjectOptimisticLockingFailureException` görüldü — iptal isteği ile `AnalysisJobRunner`'ın periyodik progress kaydı aynı satırı aynı anda güncellemeye çalışıyordu. Çözüm: hem `cancelJob` hem `AnalysisJobRunner`'ın checkpoint'i, optimistic locking çakışmasında **kendi taze transaction'ında yeniden deneme** yapacak şekilde güncellendi (`@Lazy` self-injection ile proxy üzerinden çağrı, `@Transactional`'ın tek metoda hapsedilmemesi).
- **Test sınıfının `@BeforeEach` temizliğinde de aynı türden optimistic locking hatası:** `deleteAll()`, hâlâ arka planda çalışan bir job'ın satırını "bayat version" ile silmeye çalışıyordu. `deleteAllInBatch()` (version kontrolü yapmayan toplu SQL `DELETE`) ile çözüldü.
- **`JobsListView`'de "Specification must not be null" / "Other specification must not be null" hataları:** Üç filtrenin de boş olduğu durumda `Specification.where(null)` ve `.and(null)` bu Spring Data sürümünde reddediliyordu. Her zaman doğru (`cb.conjunction()`) bir başlangıç `Specification`'ı kullanıp, her filtreyi yalnızca `null` değilse `.and(...)` ile eklemek şeklinde çözüldü.
- **Analiz kaydı silinirken `500 Internal Server Error` / foreign key ihlali:** `analysis_job.analysis_id` foreign key'ine V4'te herhangi bir `ON DELETE` kuralı tanımlanmamıştı — bir analiz sonucuna bağlı job varken o sonucu silmek PostgreSQL tarafından reddediliyordu. Yeni bir migration (`008`) ile foreign key `ON DELETE SET NULL` olarak yeniden oluşturuldu; job kaydı korunuyor, yalnızca `analysis_id` `NULL`'a düşüyor.
- **Job listesindeki "Sonuç" butonu, analizi silinmiş `SUCCEEDED` job'lar için de aktif görünüyordu:** Liste özet DTO'sunda `analysisId` alanı hiç yoktu, buton yalnızca `status`'e bakıyordu. DTO'ya `analysisId` eklenip butonun `disabled` koşuluna `analysisId !== null` kontrolü de eklenerek çözüldü.
- **`AnalysisJobService`'te derleme hataları (eksik `package` bildirimi, eksik alan tanımları, kendi kendine atanan `null` alan):** Elle yapılan düzenlemeler sırasında dosyanın başına `package` satırı eklenmeyi unutulmuş, bazı constructor parametreleri (`jobStateMachine`, `maxRetry`) alan olarak tanımlanmadan kullanılmış, bir parametre de (`analysisJobRunner`) constructor imzasından düşürülmüş ama gövdede hâlâ kullanılıyordu. Dosyanın ilgili kısımları tekrar gözden geçirilip tamamlanarak çözüldü.

### V1/V2/V3'ten Devam Eden Sorunlar

- **Liquibase migration'ları hiç çalışmıyordu:** Spring Boot 4.x, Liquibase autoconfiguration'ını `liquibase-core`'dan ayrı, kendi modülüne (`spring-boot-liquibase`) taşımış; bu bağımlılık eksikti. Eklenerek çözüldü.
- **Testcontainers, WSL2'de Docker'ı bulamıyordu:** Docker Desktop'ın yeni sürümü eski, versiyon-önekli API yollarını (`/v1.24/...`) desteklemiyor. `docker-java.properties` içine `api.version=1.55` eklenerek çözüldü.
- **`./mvnw clean test` tüm testler birlikte çalıştırıldığında bozuluyordu:** `@Testcontainers`/`@Container`, paylaşılan container'ı her test sınıfı bitince durduruyordu. "Singleton container" desenine geçilerek çözüldü.
- **Analiz kaydı silinirken `403 Forbidden`:** CORS ayarında `DELETE` metodu eksikti. Eklenerek çözüldü.
- **WSL'de npm'in Windows sürümüne yönlenmesi:** `hash -r` ile bash'in komut önbelleği temizlenerek çözüldü.
- **Frontend ↔ Backend CORS hatası (Actuator için ayrı):** `management.endpoints.web.cors.*` ayarları eklenerek çözüldü.
- **React Testing Library'de testler arası veri sızıntısı:** `src/test/setup.ts` içine `afterEach(() => cleanup())` eklenerek çözüldü.
- **Postman Desktop Agent'ın sürekli çökmesi:** Insomnia'ya geçilerek atlatıldı.

## Yapay Zekâ Kullanım Açıklaması

**Kullanılan AI aracı:** Claude (Anthropic)

**Yapay zekâdan hangi konularda destek alındığı:**
- Backend'e Actuator, CORS, standart hata formatı, PostgreSQL/Liquibase entegrasyonu, analiz geçmişi endpoint'leri eklenmesi
- V4'te: asenkron job mimarisinin tasarımı (thread pool, job lifecycle, streaming dosya okuma, iptal/retry, geçici dosya güvenliği, uygulama yeniden başlama davranışı)
- React + TypeScript proje mimarisinin tasarımı, V4'te job takip arayüzü (polling dahil) ve TR/EN çeviri altyapısının kurulması
- Docker multi-stage build, Nginx reverse proxy, Docker Compose environment variable yönetimi
- Backend/frontend test senaryolarının yazımı (Testcontainers dahil)
- Ortam/konfigürasyon sorunlarının (WSL/npm/Docker Desktop/Liquibase/Testcontainers/optimistic locking) kanıta dayalı olarak debug edilmesi

**Async yapı için alınan destek:**
- "Log analizi doğrudan HTTP request thread'i içerisinde tamamlanmamalı" gereksinimi, `@Async` + ayrı bir `AnalysisJobRunner` sınıfı ile karşılandı; `AnalysisJobService` içine `@Async` metod eklemek yerine ayrı sınıfa çıkarıldı çünkü Spring'in proxy mekanizması, aynı sınıf içinden yapılan `this.metod()` çağrılarında `@Async`'i sessizce atlıyor.

**Thread pool configuration için kullanılan promptlar:**
- "En az aşağıdaki değerler environment variable üzerinden değiştirilebilir olmalıdır" listesi doğrudan `AsyncConfig`'teki `@Value("${app.analysis-executor...}")` enjeksiyonlarına ve `docker-compose.yml`/`.env.example`'a yansıtıldı.

**Job lifecycle için kullanılan promptlar:**
- "Job durum geçişleri tek bir merkezi yapı üzerinden kontrol edilmelidir" gereksinimi `JobStateMachine` sınıfıyla karşılandı — durum kontrolü veritabanına hiç dokunmadan, izole test edilebilir saf mantık olarak tasarlandı.

**Retry ve cancellation için kullanılan promptlar:**
- "Aynı job iki kez eş zamanlı çalıştırılmamalıdır" gereksinimi, `JobStateMachine`'in yalnızca `FAILED` job'ların retry'ına izin vermesiyle yapısal olarak karşılandı.
- "RUNNING job için iptal talebi oluşturulabilmelidir" gereksinimi `cancel_requested` bayrağı + `AnalysisJobRunner`'ın periyodik kontrolüyle (işbirlikçi/cooperative iptal) karşılandı; bu tasarımın arka plan thread'iyle aynı satırı aynı anda güncelleme riski taşıdığı, gerçek test sırasında (bkz. Karşılaşılan Sorunlar) ortaya çıktı ve optimistic-locking-retry deseniyle düzeltildi.

**Streaming dosya işleme için kullanılan promptlar:**
- "Dosyanın tamamı gereksiz şekilde belleğe alınmamalıdır" gereksinimi, `BufferedReader` ile satır satır okuma ve `CountingInputStream` (dekoratör deseni) ile byte bazlı progress hesaplamasıyla karşılandı.

**Polling için kullanılan promptlar:**
- "Önerilen polling aralığı: 2 saniye", "Job terminal duruma geldiğinde polling durmalıdır", "Component unmount olduğunda polling temizlenmelidir" maddeleri `useJobPolling` custom hook'unda, `setInterval`/`clearInterval` ve bir `ref` ile jobId takibi kullanılarak karşılandı.

**Türkçe ve İngilizce çeviri yapısı için kullanılan promptlar:**
- "Kullanıcıya görünen metinler component içerisine sabit yazılmamalıdır" gereksinimi doğrultusunda `i18next`/`react-i18next` kuruldu, tüm component'ler `t("namespace.key")` çağrılarına geçirildi.
- "Frontend kullanıcıya gösterilecek hata mesajını mümkün olduğunca backend'in errorCode alanına göre seçmelidir" gereksinimi, `utils/apiErrorMessage.ts`'teki merkezi `translateApiError` fonksiyonuyla karşılandı.

**Yapay zekânın ürettiği kodlarda yapılan manuel değişiklikler:**
- `AnalysisJobService`'e daha önce eksik bırakılan `package` bildirimi, alan tanımları ve constructor parametresi, sonraki mesajlarda tamamlandı.
- `NewAnalysisFlow.tsx`'te ilk üretilen halde `useTranslation` hook'u çağrılmadan `t` kullanılmış; sonraki düzenlemede eklendi ve `validateAnalysisName` fonksiyonu `t`'ye erişebilmesi için component içine taşındı.
- `DeleteConfirmDialog`'daki dosya adı vurgusu (`<strong>`), TR/EN cümle yapısındaki kelime sırası farkı nedeniyle kaldırıldı, interpolasyonlu tek bir çeviri cümlesine geçildi.

**Reddedilen veya hatalı bulunan öneriler:**
- İlk üretilen `AnalysisJobRunner`'ın her bitiş senaryosunda geçici dosyayı silmesi, retry akışını imkansız kıldığı fark edilince reddedildi; `FAILED` durumunda dosya saklanacak şekilde değiştirildi.
- İlk denenen basit `catch { setErrorMessage(sabit metin) }` yaklaşımı, spec'in "errorCode'a göre mesaj seçilmeli" gereksinimini karşılamadığı için, merkezi `translateApiError` fonksiyonuna geçilerek reddedildi.

**Yapay zekâdan alınan kodların nasıl test edildiği:**
- Her fazdan sonra `./mvnw clean test` / `npm test` ile otomatik testler; ayrıca `curl` ile job oluşturma/iptal/retry/listeleme endpoint'leri, tarayıcıda uçtan uca (analiz adı → job → progress → sonuç, iptal, retry, dil değişimi) manuel olarak denendi.
- Concurrency (iptal ile progress güncellemesinin çakışması) gibi zamanlamaya duyarlı davranışlar, gerçek loglar incelenerek (tahminle değil, `ObjectOptimisticLockingFailureException` stack trace'i görülerek) teşhis edildi.
- `docker compose up --build` ile tam ortamda, hem veri kalıcılığı (`down`/`up` sonrası) hem veri sıfırlama (`down -v`) davranışları ayrı ayrı doğrulandı.

**V4 sırasında öğrenilen konular:**
- Asenkron işlem, HTTP request thread'i ile worker thread farkı, thread pool ve queue capacity kavramları
- Spring'in `@Async`/`@Transactional` proxy mekanizması ve neden aynı sınıf içinden `this.metod()` çağrısının bu anotasyonları atladığı (self-invocation problemi), `@Lazy` self-injection ile çözümü
- Optimistic locking (`@Version`), race condition'lar ve "yeniden dene" (retry-on-conflict) deseni
- Job lifecycle / durum makinesi tasarımı, idempotency'nin retry ile ilişkisi
- Cooperative cancellation (bir işi zorla değil, bayrakla nazikçe durdurma) mantığı
- Graceful/relaunch senaryolarında yarım kalan işlerin ele alınması (restart recovery)
- Streaming dosya okuma ve dekoratör deseni (`CountingInputStream`)
- i18next ile çok dilli frontend mimarisi, çeviri anahtarı organizasyonu, backend hata kodlarının frontend'de merkezi çevirisi