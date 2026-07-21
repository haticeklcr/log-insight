# Log Analiz Uygulaması (log-insight)

## Amaç
Kullanıcının yüklediği `.log` veya `.txt` uzantılı uygulama loglarını analiz eden bir REST API. Log seviyelerini (INFO/WARN/ERROR), exception içeren satırları ve tekrar eden hata mesajlarını tespit ederek sonucu JSON olarak döndürür.

## Kullanılan Teknolojiler
- Java 21
- Spring Boot 4.1.0 (Spring Web → `spring-boot-starter-webmvc`, Spring Validation)
- Maven (proje `./mvnw` ile çalıştırılır)
- JUnit 5 + AssertJ + MockMvc

## Gereksinimler
- WSL2 üzerinde Ubuntu
- Java 21 (OpenJDK)
- Git

## WSL2 Ortamının Hazırlanması
1. WSL2 üzerinde Ubuntu dağıtımı kurulu ve varsayılan dağıtım olarak ayarlı olmalı.
2. `java -version` ile Java 21 kurulu olduğu doğrulanmalı.
3. Proje `~/projects/log-insight` altında tutulur.

## Kurulum
```bash
cd ~/projects/log-insight
./mvnw clean install
```

## Çalıştırma
```bash
./mvnw spring-boot:run
```
Uygulama varsayılan olarak `http://localhost:8080` üzerinde ayağa kalkar.

## Testlerin Çalıştırılması
```bash
./mvnw clean test
```

## Endpoint

**POST** `/api/v1/logs/analyze`
Content-Type: `multipart/form-data`
Form alanı: `file`

### Örnek İstek
```bash
curl -X POST -F "file=@sample.log" http://localhost:8080/api/v1/logs/analyze
```

### Örnek Cevap
```json
{
  "fileName": "sample.log",
  "totalLines": 9,
  "infoCount": 3,
  "warningCount": 2,
  "errorCount": 4,
  "exceptionCount": 1,
  "mostFrequentErrors": [
    { "message": "Connection refused", "count": 3 },
    { "message": "Request timeout", "count": 1 }
  ]
}
```

### Hata Durumları
| Durum | HTTP Status |
|---|---|
| Boş dosya | 400 Bad Request |
| Desteklenmeyen dosya uzantısı | 400 Bad Request |
| Dosya boyutu 5MB'ı aşıyor | 413 Payload Too Large |

## Bilinen Eksikler / Karşılaşılan Sorunlar

- **WSL diskinin taşınması:** C: sürücüsünde yer sorunu nedeniyle WSL sanal diski (`Ubuntu-22.04`) D: sürücüsüne taşındı (export/import yöntemiyle). Bu işlem Linux içindeki dosya yolu yapısını etkilemedi, proje geliştirmesine engel teşkil etmedi.
- **Spring Boot sürüm uyumsuzluğu:** Spec'te "Spring Boot" (3.x serisi kastediliyor) isteniyordu ancak Spring Initializr'da artık minimum desteklenen sürüm 4.0.0. Bu nedenle proje 4.1.0 ile oluşturuldu. Bu sürümde `spring-boot-starter-web` → `spring-boot-starter-webmvc` olarak, test starter'ı da `spring-boot-starter-webmvc-test` / `spring-boot-starter-validation-test` olarak ayrıştı; kod bu isimlendirmeye göre yazıldı.
- **Ortam sürüm farkı:** Spec'te Ubuntu 24.04 LTS isteniyor, WSL dağıtımı 22.04. İki sürüm arasında proje için işlevsel bir fark oluşmadı, göz ardı edildi.
- **`mostFrequentErrors` sıralaması:** Eşit sayıda tekrar eden hata mesajları arasında ek bir ikincil sıralama kriteri (alfabetik vb.) uygulanmadı; sadece sayıya göre azalan sıralama yapıldı.

## Yapay Zekâ Kullanım Açıklaması

**Kullanılan AI aracı:** Claude (Anthropic)

**Destek alınan konular:**
- Proje iskeletinin (Spring Boot + Maven yapısı) oluşturulması
- Controller/Service/DTO katmanlarının tasarımı
- Log dosyası okuma ve satır satır analiz mantığı
- Validasyon ve merkezi hata yönetimi (`@RestControllerAdvice`) tasarımı
- Unit ve controller testlerinin yazımı
- README içeriğinin hazırlanması

**Kullanılan önemli prompt örneği:**
"Log Analiz Uygulaması projesini, verilen ortam bilgileri ve mevcut ilerleme durumuna göre eksiksiz tamamla; validasyon, merkezi hata yönetimi, hata mesajı gruplama, testler ve README dahil."

**AI'ın ürettiği kodlarda yapılan değişiklikler:**
- Hata mesajı çıkarma mantığında (`ERROR` kelimesinden sonraki kısmın alınması) baştaki `:` karakterinin de temizlenmesi eklendi.
- Dosya boyutu limiti hem `LogAnalysisService` içinde hem de `application.properties`'de multipart ayarı olarak iki katmanlı kontrol edildi.

**Reddedilen/hatalı bulunan öneriler:**
- İlk aşamada tüm exception türleri için tek bir generic `RuntimeException` kullanılması önerilmişti; bunun yerine HTTP status kodlarının anlamlı şekilde ayrışabilmesi için üç ayrı custom exception sınıfı tercih edildi.

**Öğrenilen konular:**
- `@RestControllerAdvice` ile merkezi exception handling kurulumu
- `MultipartFile` ile dosya validasyonu ve Spring'in kendi multipart boyut limitinin ayarlanması
- `MockMvc` ile controller seviyesinde test yazımı
- Map üzerinden frekans sayımı ve `Stream` ile sıralama/dönüştürme

## Teslim Edilecekler
- [x] Git repository (kaynak kod + README.md)
- [x] Başarılı `mvn clean test` çıktısı
- [x] Örnek log dosyası (`sample.log`)
- [x] Örnek API isteği ve cevabı (yukarıda)