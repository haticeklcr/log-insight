# Log Analiz Uygulaması (log-insight)

## Amaç
Kullanıcının yüklediği `.log` veya `.txt` uzantılı uygulama loglarını analiz eden bir REST API ve bu API'yi kullanan bir web arayüzü. Log seviyelerini (INFO/WARN/ERROR), exception içeren satırları ve tekrar eden hata mesajlarını tespit ederek sonucu hem JSON olarak hem de görsel bir arayüzde sunar.

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

**Backend:** Java 21, Spring Boot 4.1.0, Maven, Spring Web, Spring Validation, Spring Boot Actuator, JUnit 5, AssertJ

**Frontend:** Node.js LTS, npm, React, TypeScript, Vite, Fetch API, CSS Modules, Vitest, React Testing Library

**Container:** Docker, Docker Compose, Nginx

## Proje Klasör Yapısı
log-insight/
├── backend/ # Spring Boot REST API
│ ├── src/main/java/com/hatice/loginsight/
│ │ ├── controller/ # HTTP endpoint'leri
│ │ ├── service/ # İş mantığı (log analizi)
│ │ ├── dto/ # Veri transfer nesneleri
│ │ ├── exception/ # Özel exception'lar + merkezi hata yönetimi
│ │ └── config/ # CORS gibi uygulama genelinde ayarlar
│ ├── src/test/java/ # Backend testleri
│ ├── sample.log # Örnek log dosyası
│ ├── pom.xml
│ └── Dockerfile
├── frontend/ # React + TypeScript web arayüzü
│ ├── src/
│ │ ├── components/ # Header, FileUpload, AnalysisSummary, vb.
│ │ ├── services/ # Backend ile iletişim (logAnalysisApi.ts)
│ │ ├── types/ # TypeScript tip tanımları
│ │ └── App.tsx
│ ├── nginx.conf # Nginx reverse proxy yapılandırması
│ ├── package.json
│ └── Dockerfile
├── screenshots/ # Uygulama ekran görüntüleri
├── docker-compose.yml
└── README.md

## Development Ortamında Backend'i Çalıştırma

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

## Environment Variable Açıklamaları

| Değişken | Nerede Kullanılır | Varsayılan | Açıklama |
|---|---|---|---|
| `VITE_API_BASE_URL` | Frontend (development) | `http://localhost:8080` | Frontend'in backend'e istek atarken kullandığı taban adres. `frontend/.env` dosyasında tanımlanır, Docker ortamında kullanılmaz (Nginx proxy devrede olduğu için boş bırakılır). |
| `APP_LOG_ANALYSIS_MAX_FILE_SIZE` | Backend | `10MB` | Yüklenebilecek maksimum dosya boyutu. |
| `APP_CORS_ALLOWED_ORIGINS` | Backend | `http://localhost:5173` | Backend'e istek atmasına izin verilen frontend adresi (CORS). Docker Compose'da `http://localhost:3000` olarak ayarlanır. |

`frontend/.env.example` dosyası, gerçek değerler olmadan hangi değişkenlerin gerektiğini gösterir; gerçek `.env` dosyası git'e dahil edilmez.

## Docker Image Oluşturma ve Docker Compose ile Çalıştırma

Projeyi Docker ile ayağa kaldırmak için kök dizinde:

```bash
docker compose up --build
```

Bu komut:
- `backend/Dockerfile`'ı kullanarak backend image'ını (multi-stage: Maven build + JRE runtime) inşa eder.
- `frontend/Dockerfile`'ı kullanarak frontend image'ını (multi-stage: npm build + Nginx runtime) inşa eder.
- İki container'ı birbirine bağlı şekilde başlatır; frontend, backend'in sağlıklı (`healthy`) olmasını bekler.

Ortamı kapatmak için:
```bash
docker compose down
```

## Uygulama Erişim Adresleri

| Servis | Adres |
|---|---|
| Frontend (Docker) | http://localhost:3000 |
| Backend (Docker/Development) | http://localhost:8080 |
| Backend Health Check | http://localhost:8080/actuator/health |
| Frontend (Development, `npm run dev`) | http://localhost:5173 |

## Endpoint

**POST** `/api/v1/logs/analyze`
Content-Type: `multipart/form-data`
Form alanı: `file`

### Örnek İstek
```bash
curl -X POST -F "file=@backend/sample.log" http://localhost:8080/api/v1/logs/analyze
```

### Örnek Cevap
```json
{
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

### Örnek Hata Cevabı
```json
{
  "timestamp": "2026-07-22T15:04:23.000Z",
  "status": 400,
  "error": "EMPTY_FILE",
  "message": "Yüklenen dosya boş",
  "path": "/api/v1/logs/analyze"
}
```

## Nginx Proxy Yapısının Kısa Açıklaması

Development ortamında frontend (`5173`) ve backend (`8080`) farklı portlarda çalıştığı için, tarayıcı bunları farklı origin olarak görüyor ve CORS izni gerekiyor. Docker/production ortamında bu ihtiyacı ortadan kaldırmak için Nginx, hem frontend'in statik dosyalarını (`dist/` çıktısı) sunuyor hem de `/api/` ve `/actuator/` ile başlayan istekleri arka planda backend container'ına yönlendiriyor (reverse proxy). Böylece tarayıcı, tek bir origin'e (`localhost:3000`) konuşuyormuş gibi davranıyor, CORS'a gerek kalmıyor.

## Ekran Görüntüleri

### Başarılı Analiz Ekranı
![Başarılı analiz](screenshots/successful-analysis.png)

### Hata Durumu — Desteklenmeyen Dosya Türü
![Desteklenmeyen dosya türü](screenshots/error-unsupported-file.png)

### Hata Durumu — Backend Servisine Erişilememesi
![Backend erişilemiyor](screenshots/error-backend-down.png)

## Bilinen Eksikler
- Sürükle-bırak (drag-and-drop) desteği eklendi ancak farklı tarayıcılarda kapsamlı test edilmedi.
- `mostFrequentErrors` listesinde üst sınır (örn. ilk 10) uygulanmıyor; çok sayıda benzersiz hata mesajı olan büyük dosyalarda liste uzun olabilir.
- Frontend, backend health check'i sadece sayfa ilk yüklendiğinde kontrol ediyor; periyodik otomatik yenileme yapmıyor.

## Karşılaşılan Sorunlar ve Çözümleri

- **WSL'de npm'in Windows sürümüne yönlenmesi:** `npm create vite` komutu `ERR_INVALID_URL` hatası veriyordu. Kök sebep, bash'in `npm` komutunu daha önce çalıştırılan Windows npm'ine (`/mnt/c/Program Files/nodejs/npm`) "hash"lemiş olmasıydı. `hash -r` ile bash'in komut önbelleği temizlenerek doğru (Linux) npm'e yönlendirildi.
- **Frontend ↔ Backend CORS hatası:** Tarayıcı konsolunda `No 'Access-Control-Allow-Origin' header` hatası alındı. Normal endpoint'ler için `WebConfig.java` ile CORS tanımlandı; ancak Spring Boot Actuator kendi ayrı CORS mekanizmasını kullandığından, `application.properties`'e ayrıca `management.endpoints.web.cors.*` ayarları eklenmesi gerekti.
- **React Testing Library'de testler arası veri sızıntısı:** Ardışık testlerde "Found multiple elements" hatası alındı. Sebep, `vitest.config.ts`'de `globals` açık olmadığı için Testing Library'nin otomatik `cleanup` mekanizmasının devreye girmemesiydi. `src/test/setup.ts` içine elle `afterEach(() => cleanup())` eklenerek çözüldü.
- **Postman Desktop Agent'ın sürekli çökmesi:** Windows Olay Görüntüleyicisi incelendiğinde `Postman Agent.exe`'nin `0x80000003` hata koduyla çöktüğü görüldü; güvenlik yazılımı/ağ ayarlarıyla ilgisiz olduğu anlaşıldı. Alternatif olarak Insomnia kullanılarak API manuel test edildi.

## Yapay Zekâ Kullanım Açıklaması

**Kullanılan AI aracı:** Claude (Anthropic)

**Yapay zekâdan hangi konularda destek alındığı:**
- Backend'e Actuator, configuration-tabanlı dosya boyutu limiti, standart hata formatı ve CORS eklenmesi
- React + TypeScript proje mimarisinin tasarımı (component ayrımı, servis katmanı, tip tanımları)
- Docker multi-stage build ve Nginx reverse proxy yapılandırması
- Frontend ve backend test senaryolarının yazımı
- WSL/npm/Postman ile ilgili ortam sorunlarının debug edilmesi

**UI tasarımı için kullanılan önemli promptlar:**
- "Frontend aşağıdaki bileşenlere ayrılabilir: App, Header, BackendStatus, FileUpload, AnalysisSummary, StatCard, FrequentErrorsTable, ErrorAlert, LoadingIndicator" şeklindeki spec maddesi doğrudan component yapısına dönüştürüldü.
- "Backend'den dönen teknik hata mesajı doğrudan ve kontrolsüz biçimde kullanıcıya gösterilmemeli" gereksinimi, `LogAnalysisApiError` sınıfı ve `App.tsx`'teki hata ayrımı (`instanceof` kontrolü) ile karşılandı.

**Docker yapılandırması için kullanılan önemli promptlar:**
- "Backend Dockerfile: Multi-stage build kullanılmalı, runtime image mümkün olduğunca küçük olmalı" — bu doğrultuda `eclipse-temurin:21-jdk` (build) → `eclipse-temurin:21-jre` (runtime) ayrımı yapıldı.
- "Nginx, /api ile başlayan istekleri backend servisine yönlendirebilir" — `nginx.conf`'taki `location /api/ { proxy_pass ... }` bloğu bu şekilde oluşturuldu.

**Yapay zekânın ürettiği kodlarda yapılan manuel değişiklikler:**
- Hata mesajı çıkarma mantığında baştaki `:` karakterinin temizlenmesi eklendi (V1'den beri süregelen bir düzeltme).
- `ActuatorHealthTest`, ilk üretilen haliyle `TestRestTemplate` kullanıyordu, bu sınıf mevcut Spring Boot sürümünde bulunamadığı için `HttpClient` (Java'nın kendi standart kütüphanesi) kullanan bir versiyonla değiştirildi.

**Hatalı veya projeye uygun olmadığı için reddedilen öneriler:**
- `LogAnalysisControllerTest` için önerilen `@AutoConfigureMockMvc` tabanlı yaklaşım, projedeki Spring Boot sürümüyle derleme hatası verdiği için reddedildi; bunun yerine mevcut `standaloneSetup` + `@BeforeEach` yaklaşımı korundu.

**Yapay zekâdan alınan kodların nasıl test edildiği:**
- Her backend değişikliğinden sonra `mvn clean test` ile otomatik testler, ayrıca `curl` ile manuel uçtan uca senaryolar (geçerli dosya, boş dosya, yanlış uzantı, boyut aşımı) çalıştırıldı.
- Her frontend değişikliğinden sonra `npm test` ile otomatik testler, ayrıca tarayıcıdan (`localhost:5173` ve Docker'da `localhost:3000`) manuel olarak dosya yükleme/analiz akışı denendi.
- Docker Compose kurulumu, `docker compose up --build` ile ayağa kaldırılıp hem `curl` hem tarayıcı üzerinden doğrulandı; backend container'ı kasıtlı olarak durdurularak hata senaryosu da test edildi.

**V2 sırasında öğrenilen konular:**
- Spring'de `@Value` ile configuration/environment variable enjeksiyonu
- CORS'un tarayıcı güvenlik mekanizması olarak çalışma mantığı ve Actuator'ın ayrı bir CORS yapılandırması gerektirdiği
- React'te `useState`, props ile component'ler arası veri akışı, CSS Modules ile stil izolasyonu
- Vitest/React Testing Library ile component testleri ve `vi.mock` ile API mock'lama
- Docker multi-stage build mantığı ve Nginx'in hem statik dosya sunucusu hem reverse proxy olarak kullanılması
- WSL ortamında npm/Node.js kurulumlarının Windows tarafıyla karışabileceği ve bunun nasıl teşhis edilip düzeltileceği
