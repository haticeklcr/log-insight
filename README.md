# Log Analiz Uygulaması (log-insight)

## Amaç
Kullanıcının yüklediği `.log` veya `.txt` uzantılı uygulama loglarını analiz eden bir REST API ve bu API'yi kullanan bir web arayüzü. Log seviyelerini (INFO/WARN/ERROR), exception içeren satırları ve tekrar eden hata mesajlarını tespit ederek sonucu hem JSON olarak hem de görsel bir arayüzde sunar. V3 ile birlikte her başarılı analiz PostgreSQL'de kalıcı olarak saklanır; geçmiş analizler listelenebilir, aranabilir, filtrelenebilir, detayları görüntülenebilir ve silinebilir. V4 ile birlikte log analizi artık arka planda çalışan asenkron bir **job** olarak yürütülür; kullanıcı her analize bir ad verir, job'ın durumunu (bekliyor/çalışıyor/tamamlandı/başarısız/iptal edildi) canlı olarak izleyebilir, iptal edebilir ya da başarısız olanları tekrar deneyebilir. V5 ile birlikte uygulama artık tek tip düz metin log dosyasıyla sınırlı değil — Spring Boot, JSON, Nginx access log, Apache access log ve genel düz metin olmak üzere 5 farklı log formatını otomatik algılayabiliyor veya kullanıcı manuel olarak seçebiliyor; hata mesajlarını normalize ederek gruplayabiliyor, log zaman çizelgesi çıkarabiliyor, hassas verileri maskeleyebiliyor ve gelişmiş filtrelerle (tarih aralığı, level, logger, thread, HTTP alanları) analiz kapsamını daraltabiliyor. Uygulama Türkçe ve İngilizce dillerini destekler. V6.1 ile birlikte uygulama, üretim ortamından toplanmış (systemd/journald, Docker/Kubernetes CRI gibi bir toplayıcı öneki taşıyan) log dosyalarını da doğru analiz edebiliyor; envelope tespiti, CRI bölünmüş kayıt birleştirme, format güven skoru hesabındaki bir hatanın düzeltilmesi, kademeli zaman çizelgesi ve gigabayt ölçekli dosyalara hazır 64 bit sayaçlarla.

## V5 ile Eklenen Özellikler
- **Çoklu log formatı desteği** — Spring Boot standart log formatı, JSON log formatı, Nginx access log, Apache access log ve genel düz metin log; her biri ayrı bir `LogParser` implementasyonu (`SpringBootLogParser`, `JsonLogParser`, `NginxAccessLogParser`, `ApacheAccessLogParser`, `PlainTextLogParser`)
- **Otomatik format algılama (AUTO)** — dosyanın ilk 50 (yapılandırılabilir) anlamlı satırı örneklenerek, en yüksek eşleşme oranına sahip format seçilir; eşik altında kalırsa düz metne (PLAIN_TEXT) güvenli şekilde düşülür
- **Manuel parser seçimi** — kullanıcı formatı bizzat belirtebilir; seçilen format dosyayla uyumlu değilse job kontrollü şekilde `FAILED` olur (`SELECTED_PARSER_CANNOT_PARSE_FILE`)
- **Multiline stack trace desteği** — bir exception'a ait `at ...`/`Caused by:`/`Suppressed:`/`... N more` satırları tek bir kayıt olarak gruplanır, root cause tespit edilir
- **Hata mesajı normalizasyonu** — UUID, IP, sayısal ID, timestamp, port, hex gibi değişken değerler yer tutucularla değiştirilerek aynı hatanın farklı örnekleri tek grupta toplanır
- **Hassas veri maskeleme** — Authorization/Bearer/Basic auth, cookie, session ID, e-posta, kredi kartı benzeri numara, password/api-key/token alanları veritabanına yazılmadan/UI'a dönmeden önce maskelenir
- **Log zaman çizelgesi** — dakikalık (gerektiğinde otomatik saatliğe ölçeklenen) bucket'larla zaman bazlı INFO/WARN/ERROR/exception dağılımı
- **Parse kalite skoru ve format güven skoru** — her analiz için 0-100 arası iki ayrı, açıklayıcı skor
- **Gelişmiş filtreler** — tarih aralığı, log level, logger, thread, mesaj içeriği, HTTP status code/method, URL path; parser ile uyumsuz filtre kombinasyonları kontrollü şekilde reddedilir
- **Logger/thread/HTTP dağılım istatistikleri** — en sık log üreten logger/thread'ler, HTTP status code ve method dağılımları
- **5 yeni Liquibase migration'ı, 5 yeni istatistik tablosu**
- **Genişletilmiş parser mimarisi** — `LogParser` interface'i + Strategy/Factory Pattern ile yeni format eklemek mevcut kodda değişiklik gerektirmiyor
- **80+ yeni backend testi, 30+ yeni frontend testi**, tümü V1-V4 testleriyle birlikte yeşil

## V6.1 ile Eklenen Özellikler
- **Log envelope tespiti ve soyulması** — SYSLOG_RFC3164, SYSLOG_RFC5424 ve CONTAINER_CRI (Docker/Kubernetes) toplayıcı önekleri tespit edilip analiz zincirinin en başında soyuluyor; güven eşiğinin altında kalan dosyalarda soyma hiç uygulanmıyor, mevcut parser'lar hiç etkilenmiyor
- **CRI bölünmüş (partial) kayıt birleştirme** — ayrı bir bileşen (`CriPartialRecordAssembler`) ardışık `P` parçalarını sonraki `F` ile tek mantıksal kayda birleştiriyor; stdout/stderr karışmıyor, eksik/limit aşan kayıtlar sessizce yutulmuyor
- **Format güven skoru düzeltmesi** — devam satırları (stack trace) artık paydaya dahil edilmiyor; stack trace ağırlıklı dosyalarda doğru parser yanlışlıkla reddedilmiyor
- **Exception ve parse edilemeyen satır sayımı düzeltmesi** — multiline gruplamadaki, parse edilemeyen her satırı önceki kayda yutan bir hatanın giderilmesi; parse kalite skorunun artık gerçek durumu yansıtması
- **Kademeli zaman çizelgesi** — dakika → 5 dakika → 15 dakika → saat → 6 saat → gün → hafta kademeleri arasında, bucket sayısı sınırı aşıldıkça gerektiği kadar tekrar tekrar yükseltiliyor
- **Transactional, toplu analiz sonucu kaydetme** — analiz kaydı + tüm istatistikler tek bir veritabanı transaction'ında, `saveAll` ile toplu yazılıyor; yarım kalan analiz kaydı oluşmuyor
- **64 bit sayaçlar** — tüm satır/kayıt sayaçları `BIGINT`/`long`'a genişletildi, gigabayt ölçekli dosyalarda 32 bit taşma riski ortadan kalktı
- **4 yeni Liquibase migration'ı (018-021)**
- **34 yeni backend testi**, tümü V1-V5 testleriyle birlikte yeşil

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

## Desteklenen Log Formatları

V5, aşağıdaki 5 log formatını destekler. Her format için bir örnek fixture dosyası `backend/src/test/resources/fixtures/` altında bulunur.

### Spring Boot Standart Log Formatı
Örnek fixture: `fixtures/spring-boot-sample.log`
Ayrıştırılan alanlar: `timestamp`, `level`, `thread`, `logger`, `message`. (`processId` regex eşleşmesinde kullanılır ama ortak `ParsedLogEntry` modelinde saklanmaz — spec'in önerdiği ortak modelde bu alan yok.)

### JSON Log Formatı
Örnek fixture: `fixtures/json-sample.log` (her satır bağımsız bir JSON nesnesi — JSON Lines)
```json
{"timestamp": "2026-01-01T12:30:15Z", "level": "INFO", "logger": "com.example.Service", "message": "hello", "thread": "main"}
```
Alan adı esnekliği desteklenir: `timestamp`/`time`/`@timestamp`, `level`/`logLevel`/`severity`, `logger`/`class`/`source`, `message`/`msg`, `thread`/`threadName`. Timestamp hem ISO-8601 metin hem epoch milisaniye olarak kabul edilir.

### Nginx Access Log
Örnek fixture: `fixtures/nginx-access-sample.log` ("common" format — referrer/user-agent olmadan). Ayrıştırılan alanlar: `clientIp`, `timestamp`, `method`, `path`, `protocol`, `statusCode`, `responseSize`. **`userAgent` bu formatta hiç doldurulmaz** — çünkü gerçek Nginx "common" log formatının satırında bu bilgi zaten hiç bulunmaz (spec bunu Nginx için beklenen bir alan olarak listeliyor olsa da, seçtiğimiz "common" kalıbı fiziksel olarak bu veriyi içermiyor).

### Apache Access Log
Örnek fixture: `fixtures/apache-access-sample.log` ("combined" format — referrer/user-agent ile). Ayrıştırılan alanlar: `clientIp`, `timestamp`, `method`, `path`, `protocol`, `statusCode`, `responseSize`, `referrer`, `userAgent`.

**Nginx/Apache ayrımı:** İkisi de aynı temel regex yapısını kullanır (`HttpAccessLogFormat`); ayrım, satırın referrer/user-agent alanları İÇERİP İÇERMEDİĞİNE göre yapılır — "common" formatı sadece Nginx'e, "combined" formatı sadece Apache'ye atanmıştır. Bu, gerçek dünyada Nginx'in de combined format üretebileceği (ve bu durumda userAgent içerebileceği) gerçeğinin bilinçli bir basitleştirmesidir (bkz. [Bilinen Eksikler](#bilinen-eksikler)).

Her iki formatta da HTTP status code'undan bir log seviyesi türetilir: `5xx → ERROR`, `4xx → WARN`, diğerleri → `INFO` (spec'in bunu açıkça istemediği ama V1-V4'ten gelen level sayaçlarının HTTP loglarında da anlamlı kalması için alınmış bir tasarım kararı).

### Genel Düz Metin Log
Örnek fixture: `fixtures/plain-text-sample.log`

`INFO`/`WARN`/`WARNING`/`ERROR`/`DEBUG`/`TRACE`/`Exception`/`Caused by` ifadelerini tanır (`WARNING` seviyesi `WARN` ile aynı kategoride sayılır). Diğer hiçbir parser'ın kabul etmediği (`canParse()` sonucu `false` olan) her satırı kabul eden **fallback (son çare) parser**'dır — kendi `canParse()` metodu her zaman `true` döner. Satır başında `yyyy-MM-dd HH:mm:ss` (isteğe bağlı milisaniye/`T` ayracıyla) kalıbına uyan bir tarih varsa bunu da ayrıştırır.

## Parser Mimarisi

Parser yapısı tamamen interface tabanlıdır ve genişletilebilir olacak şekilde tasarlanmıştır (`backend/src/main/java/com/hatice/loginsight/parser/`):
parser/
├── LogFormat.java # AUTO, SPRING_BOOT, JSON, NGINX_ACCESS, APACHE_ACCESS, PLAIN_TEXT
├── ParsedLogEntry.java # Tüm parser'ların ortak çıktı modeli
├── LogParser.java # Interface: getFormat() / canParse() / parse()
├── LogParserFactory.java # Factory Pattern — LogFormat -> LogParser eşlemesi
├── SpringBootLogParser.java
├── JsonLogParser.java
├── HttpAccessLogFormat.java # Nginx/Apache'nin paylaştığı ortak regex/ayrıştırma yardımcı sınıfı
├── NginxAccessLogParser.java
├── ApacheAccessLogParser.java
├── PlainTextLogParser.java
├── LogFormatDetector.java # Otomatik format algılama
├── LogFormatDetectionResult.java
├── MultilineExceptionAggregator.java
├── LogRecordGroup.java
├── MultilineExceptionInfo.java
├── ExceptionInfoExtractor.java
├── LogMessageNormalizer.java
└── SensitiveDataMasker.java

**Strategy Pattern:** Her format, `LogParser` interface'inin arkasında ayrı bir strateji olarak duruyor — "bir satırı nasıl ayrıştırırım" sorusunun 5 farklı cevabı, 5 ayrı sınıfta izole. Hiçbir yerde (controller, service, job runner) `if (format == SPRING_BOOT) ... else if (format == JSON) ...` şeklinde bir dallanma yok.

**Factory Pattern:** `LogParserFactory`, hangi `LogFormat`'a hangi `LogParser`'ın karşılık geldiğine karar veren tek yer. Spring, `@Component` işaretli tüm `LogParser` implementasyonlarını otomatik olarak bir `List<LogParser>` halinde `LogParserFactory`'nin constructor'ına enjekte eder; factory bunları bir `Map<LogFormat, LogParser>`'a dönüştürür. `getParser(format)` O(1) sürede doğru parser'ı döner.

**Interface + Polymorphism:** `AnalysisJobRunner` ve `LogFormatDetector`, hangi somut sınıfla konuştuklarını hiç bilmeden, sadece `LogParser` interface'i üzerinden (`parser.parse(satır)`, `parser.canParse(satır)`) çalışır — çalışma zamanında (runtime) Java doğru sınıfın kodunu otomatik seçer.

**Ortak `ParsedLogEntry` modeli:** 5 farklı formatın ürettiği tamamen farklı alan kümeleri, tek bir ortak sınıfta (`timestamp`, `level`, `thread`, `logger`, `message`, `normalizedMessage`, `exceptionType`, `statusCode`, `method`, `path`, `sourceFormat`, `rawLine`) toplanır. Bir formatın kullanmadığı alan `null` bırakılır. Bu sayede maskeleme, normalizasyon, filtreleme, istatistik gibi tüm sonraki katmanlar formattan tamamen bağımsız çalışabilir.

**Parser sorumluluklarının ayrılması:** Her sınıfın tek bir sorumluluğu var — parser'lar SADECE ham metni `ParsedLogEntry`'e çevirir; maskeleme (`SensitiveDataMasker`), normalizasyon (`LogMessageNormalizer`), multiline gruplama (`MultilineExceptionAggregator`), istatistik toplama (`AnalysisResultAccumulator`) ve zaman çizelgesi (`LogTimelineAggregator`) ayrı sınıflarda yaşar. `AnalysisJobRunner` bunları doğru sırada çağıran orkestratördür, içlerindeki mantığı bilmez.

## Yeni Parser Ekleme Adımları

Mevcut parser implementasyonlarında **hiçbir değişiklik yapmadan** yeni bir format eklemek için:

1. `LogFormat` enum'una yeni bir değer ekle (örn. `LOG4J_XML`)
2. `LogParser` interface'ini implemente eden, `@Component` işaretli yeni bir sınıf yaz (`getFormat()`, `canParse()`, `parse()` metotlarını doldur)
3. Bu kadar.

`LogParserFactory`'ye dokunulmaz (Spring, yeni `@Component`'i otomatik bulup listeye ekler). `LogFormatDetector`'a dokunulmaz (o zaten `parserFactory.getAllParsers()` ile kayıtlı TÜM parser'ları geziyor). `AnalysisJobRunner`'a dokunulmaz (polymorphic çalıştığı için hangi parser'ın eklendiğinden habersiz). Test için: `backend/src/test/resources/fixtures/` altına yeni bir örnek dosya + `backend/src/test/java/.../parser/` altına parser'a özel bir test sınıfı eklenmesi yeterlidir (bkz. [Parser Testlerinin Çalıştırılması](#parser-testlerinin-çalıştırılması)).

## Otomatik Format Algılama

Kullanıcı `AUTO` seçtiğinde (veya hiçbir `parserType` göndermediğinde), sistem şu adımları izler:

1. Dosyanın **tamamı okunmaz** — `LogFormatDetector.collectSampleLines()` yalnızca ilk `LOG_FORMAT_DETECTION_SAMPLE_SIZE` (varsayılan 50) **anlamlı** (boş olmayan) satırı okur, bu sayıya ulaşır ulaşmaz okuma durur.
2. Bu örnek satırlar, PLAIN_TEXT hariç her parser'a (`SpringBootLogParser`, `JsonLogParser`, `NginxAccessLogParser`, `ApacheAccessLogParser`) tek tek `canParse()` ile denenir.
3. Her parser için "örneklerin kaçını anladığı" oranı hesaplanır — bu, **format güven skorudur** (`formatConfidence`).
4. En yüksek güven skoruna sahip format `LOG_FORMAT_CONFIDENCE_THRESHOLD` (varsayılan 60) eşiğini geçiyorsa, o format seçilir.
5. Eşiği geçemiyorsa, **PLAIN_TEXT'e güvenli şekilde düşülür** (fallback); bu durumda `detectedLogFormat` alanına yine `PLAIN_TEXT` yazılır, ama `formatConfidence`/`matchedSampleCount` alanlarına PLAIN_TEXT'in kendi (her zaman ~%100 çıkacak, dolayısıyla yanıltıcı olacak) skoru değil, **eşiği geçemeyen en iyi adayın** skoru kaydedilir — kullanıcı "sistem X formatını düşündü ama yeterince emin olamadığı için düz metne düştü" bilgisini görebilsin diye.
6. Örneklenen satırların TAMAMI boşsa (dosyada hiç anlamlı içerik yoksa), `LOG_FORMAT_COULD_NOT_BE_DETECTED` hatasıyla job kontrollü şekilde `FAILED` olur — bu, gerçek hayatta neredeyse hiç tetiklenmez çünkü PLAIN_TEXT her zaman bir güvenlik ağı sağlar; sadece dosyanın kendisi tamamen boşsa/anlamsızsa devreye girer.

### Format Güven Skoru Hesaplama Yaklaşımı

`formatConfidence = round((eşleşen örnek satır sayısı / toplam örnek satır sayısı) × 100)`, 0-100 arası tam sayı. Küçük örnek boyutlarında (örn. dosyada sadece 3-4 satır varsa) yuvarlama nedeniyle sezgisel olmayan değerler (örn. 3/3 eşleşme ama %75 gibi bir ara sonuç) görülebilir — bu, **istatistiksel bir kesinlik iddiası değil**, sadece "örneklemin ne kadarı bu formata benziyor" sorusuna bir yaklaşık cevaptır.

## Manuel Parser Seçimi

Kullanıcı `POST /api/v1/analysis-jobs` isteğinde `parserType` alanını (`SPRING_BOOT`/`JSON`/`NGINX_ACCESS`/`APACHE_ACCESS`/`PLAIN_TEXT`) doldurduğunda:

- Otomatik algılama **hiç çalıştırılmaz**.
- İki aşamada doğrulanır: (1) job OLUŞTURULURKEN, `parserType`'ın geçerli bir enum değeri olup olmadığı (`INVALID_PARSER_TYPE`) ve seçilen filtrelerin bu formatla uyumlu olup olmadığı (`UNSUPPORTED_FILTER_FOR_PARSER`) kontrol edilir; (2) job ÇALIŞIRKEN, dosyanın gerçek ilk 50 örnek satırıyla seçilen parser'ın uyumu TEKRAR test edilir — uyum eşiğin altındaysa `SELECTED_PARSER_CANNOT_PARSE_FILE` ile job kontrollü şekilde `FAILED` olur (rastgele/yanlış sonuç üretilmez).

## Parse Kalite Skoru Hesaplama Yaklaşımı

`parseQualityScore` (0-100), 5 kriterin ortalamasının 100 ile çarpımıdır:
1. Parse edilen kayıt oranı (`parsedEntryCount / (parsedEntryCount + unparsedLineCount)`)
2. Parse edilemeyen satır oranının tersi (`1 - unparsedLineCount / toplam`)
3. Timestamp bulunan kayıt oranı
4. Level bulunan kayıt oranı
5. Mesaj alanı bulunan kayıt oranı

**Önemli:** Bu skor de, format güven skoru gibi, **bilimsel bir kesinlik ifade etmez** — sadece "bu analiz sonucuna ne kadar güvenilebileceğine" dair kaba bir gösterge sunar. Örneğin bir düz metin (plain text) dosyasında logger/thread/statusCode gibi alanlar doğası gereği hiç yoktur; bu, o alanların "eksik" olduğu anlamına gelmez, sadece o formatın onları desteklemediği anlamına gelir — skor hesaplamasında bu alanlar kritere dahil edilmemiştir (sadece timestamp/level/message dahildir), yine de skor "mükemmel ayrıştırma" garantisi vermez.

## Multiline Stack Trace Desteği

`MultilineExceptionAggregator`, satır satır okuma döngüsünün üstünde çalışan durum bilgili (stateful) bir gruplayıcıdır. Bir satırın "yeni bir kayıt" mı yoksa "önceki kaydın devamı" mı olduğuna şu kalıplara bakarak karar verir: `at ` ile başlıyor mu, `Caused by:`/`Suppressed:` ile başlıyor mu, `... N more` kalıbına uyuyor mu, ya da aktif parser'ın kendi deseniyle hiç eşleşmiyor mu (son çare kontrolü). Bir grup tamamlandığında (`LogRecordGroup`), `ExceptionInfoExtractor` bu grubu tarayıp exception tipini, mesajını ve **root cause**'u (zincirdeki en son `Caused by:`) çıkarır. Çok uzun stack trace'lerde devam satırları `MAX_STACK_TRACE_LINES` (varsayılan 500) ile sınırlanır — limit aşılırsa fazla satırlar sessizce atlanır (`LogRecordGroup.isTruncated()`), ama kayıt yine tek bir exception olayı olarak sayılmaya devam eder.

## Hata Mesajı Normalizasyonu

`LogMessageNormalizer`, tüm normalizasyon kurallarının tutulduğu **tek merkezi sınıftır** — hiçbir parser kendi regex'ini kopyalamaz. Sıralı olarak (en özelden en genele: UUID → IPv6 → IPv4 → timestamp → request/trace ID → port → hex → genel sayı) çalışan kurallar, "User 12345 not found" gibi mesajları "User `<NUMBER>` not found" haline getirir. Orijinal ham mesaj **kaybolmaz** — `ParsedLogEntry`'de hem `message` (ham/maskelenmiş) hem `normalizedMessage` (normalize edilmiş) ayrı alanlar olarak taşınır; gruplama `normalizedMessage` üzerinden yapılır, kullanıcıya örnek bir ham mesaj da gösterilir.

## Hassas Veri Maskeleme

`SensitiveDataMasker`, Authorization header, Bearer/Basic auth, cookie, session ID, e-posta, kredi kartı benzeri numara, password/passwd/secret, API key, access/refresh token kalıplarını `****` ile değiştirir. **Pipeline sırası kritiktir:** her kayıt önce maskelenir, SONRA normalize edilir (`AnalysisJobRunner.processGroup()`) — böylece maskelenmemiş hassas veri hiçbir zaman, normalize edilmiş haliyle bile, veritabanına yazılmaz veya UI'a dönmez.

**Yanlış pozitif riskleri:** Maskeleme kuralları regex tabanlı olduğu için bazı meşru veriler de yanlışlıkla maskelenebilir — örneğin `password_reset_enabled=true` gibi bir alan adı "password" kelimesini içerdiği için değeri maskelenebilir, ya da 16 haneli bir sipariş numarası kredi kartı deseniyle eşleşip maskelenebilir. Bu, "hassas veriyi kaçırmaktansa fazla maskele" prensibiyle bilinçli olarak kabul edilmiş bir ödünleşimdir (trade-off).

## Log Zaman Çizelgesi

`LogTimelineAggregator`, timestamp'i olan her kaydı zaman bazlı "bucket"lara topluyor. **Bucket büyüklüğü otomatik seçilir:** dakikalık başlanır; bucket sayısı `MAX_TIMELINE_BUCKETS` (varsayılan 500) sınırını aşacaksa, mevcut tüm dakikalık bucket'lar saatlik bucket'lara birleştirilir (`rebucketToHourly()`) ve o andan itibaren saatlik devam edilir — bu, hem kısa hem uzun zaman aralıklarını makul sayıda bucket'la temsil edebilmeyi sağlar. Her bucket; başlangıç zamanı, toplam sayı, INFO/WARN/ERROR sayısı ve exception sayısını tutar; sonuçlar `analysis_timeline_stat` tablosunda saklanır. Timestamp içermeyen kayıtlar (veya timestamp'i hiç ayrıştırılamayan formatlar) timeline'a hiç dahil edilmez — bu durumda timeline sonucu boş olabilir.

## Gelişmiş Filtreler

Kullanıcı yeni analiz oluştururken 9 isteğe bağlı filtre belirleyebilir: başlangıç/bitiş tarihi, log level(ler), logger, thread, mesaj içeriği, HTTP status code(lar), HTTP method(lar), URL path içeriği. `JobFilterCriteria`, ayrıştırılan her kaydı bu filtrelere göre değerlendirir — uymayan kayıtlar **sanki dosyada hiç yokmuş gibi** analiz dışı bırakılır (ne "parse edildi" ne "parse edilemedi" sayılır). Filtreler sadece seçilen (veya algılanan) formatın desteklediği alanlara uygulanabilir — `AnalysisFilterSupport`, örneğin Spring Boot/JSON formatında HTTP status code filtresi, ya da Nginx/Apache formatında logger filtresi gibi uyumsuz kombinasyonları hem job oluşturulurken (manuel seçimde) hem job çalışırken (AUTO dahil, format kesinleştikten sonra) `UNSUPPORTED_FILTER_FOR_PARSER` ile kontrollü şekilde reddeder.

## Yeni Analiz Sonucu Alanları

V5 ile birlikte hem job detay (`GET /api/v1/analysis-jobs/{id}`) hem analiz detay (`GET /api/v1/analyses/{id}`) response'ları aşağıdaki yeni alanları içerir:

| Alan | Nerede | Açıklama |
|---|---|---|
| `requestedParserType` | Job + Analiz | Kullanıcının istediği parser (`null`/boşsa `AUTO`) |
| `detectedLogFormat` | Job + Analiz | Sistemin kesinleştirdiği format |
| `parsedEntryCount` / `unparsedLineCount` | Analiz | Başarıyla ayrıştırılan / ayrıştırılamayan satır sayısı |
| `unparsedLinePercentage` | Analiz | Veritabanında saklanmaz, `unparsedLineCount / (parsedEntryCount + unparsedLineCount)` olarak istek anında hesaplanır |
| `firstLogTimestamp` / `lastLogTimestamp` | Analiz | Dosyadaki en erken/en geç log zamanı |
| `multilineExceptionCount` | Analiz | Birden fazla satırdan oluşan (stack trace'li) exception sayısı |
| `mostFrequentLoggers` / `mostFrequentThreads` | Analiz | `analysis_logger_stat`/`analysis_thread_stat` tablolarından, ayrı sorgularla çekilir |
| `statusCodeDistribution` / `httpMethodDistribution` | Analiz | `analysis_status_code_stat`/`analysis_http_method_stat` tablolarından |
| `timeline` | Analiz | `analysis_timeline_stat` tablosundan, bucket başlangıç zamanına göre sıralı |
| `parseQualityScore` / `formatConfidence` | Analiz | 0-100 arası, bkz. [Parse Kalite Skoru](#parse-kalite-skoru-hesaplama-yaklaşımı) / [Format Güven Skoru](#format-güven-skoru-hesaplama-yaklaşımı) |
| `formatDetectionSampleSize` / `matchedSampleCount` | Analiz | Format algılamada kaç örnek satır kullanıldığı / kaçının eşleştiği |
| `appliedFilters` | Job + Analiz | Job'a girilen filtrelerin tamamı (bir analiz sonucu, onu üreten job'a geri bakılarak doldurulur — bkz. [Veritabanı Yapısı](#veritabanı-yapısı)) |

Formatın desteklemediği alanlar (örn. plain text'te `mostFrequentLoggers`) boş liste veya `null` olarak döner; frontend bu durumda "Bu log formatında veri bulunamadı" empty state'ini gösterir.

## Kullanılan Teknolojiler

**Geliştirme ortamı:** Windows 10/11, WSL2, Ubuntu 22.04, VS Code + WSL eklentisi, Git, GitHub

**Backend:** Java 21, Spring Boot 4.1.0, Maven, Spring Web, Spring Validation, Spring Data JPA, Hibernate, Liquibase, Spring Boot Actuator, Spring Async, Jackson (Jackson 3.x — bkz. [Karşılaşılan Sorunlar](#v5e-özgü-sorunlar)), JUnit 5, Mockito, AssertJ, Testcontainers

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
| `requested_parser_type` / `detected_log_format` | VARCHAR(30) | (V5) kullanıcının istediği / sistemin algıladığı format |
| `parsed_entry_count` / `unparsed_line_count` | INT | (V5) |
| `first_log_timestamp` / `last_log_timestamp` | TIMESTAMP | (V5) |
| `multiline_exception_count` | INT | (V5) |
| `parse_quality_score` / `format_confidence` | INT | (V5) 0-100 arası |
| `format_detection_sample_size` / `matched_sample_count` | INT | (V5) |

**`frequent_error`** — bir analize ait en sık tekrar eden hata mesajları (V5: `normalized_message` sütunu eklendi, `message` artık örnek ham/maskelenmiş mesajı taşıyor)

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
| `requested_parser_type` / `detected_log_format` | VARCHAR(30) | (V5) |
| `filter_start_time` / `filter_end_time` | TIMESTAMP | (V5) |
| `filter_levels` / `filter_status_codes` / `filter_http_methods` | VARCHAR(200) | (V5) virgülle ayrılmış |
| `filter_logger` / `filter_thread` | VARCHAR(255) | (V5) |
| `filter_message_contains` / `filter_path_contains` | VARCHAR(500) | (V5) |

**Yeni V5 tabloları** — her biri `log_analysis.id`'ye `ON DELETE CASCADE` foreign key ile bağlı, `log_analysis_id` üzerinde index'li:

| Tablo | Alanlar |
|---|---|
| `analysis_logger_stat` | `id`, `log_analysis_id`, `logger_name`, `entry_count` |
| `analysis_thread_stat` | `id`, `log_analysis_id`, `thread_name`, `entry_count` |
| `analysis_status_code_stat` | `id`, `log_analysis_id`, `status_code`, `entry_count` |
| `analysis_http_method_stat` | `id`, `log_analysis_id`, `http_method`, `entry_count` |
| `analysis_timeline_stat` | `id`, `log_analysis_id`, `bucket_start`, `total_count`, `info_count`, `warn_count`, `error_count`, `exception_count` |

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
├── 008-fix-analysis-job-fk-on-delete.yaml # foreign key'i ON DELETE SET NULL yapar (V4)
├── 009-add-parser-fields-to-log-analysis.yaml # log_analysis'e parser/format alanları (V5)
├── 010-create-analysis-logger-stat-table.yaml # analysis_logger_stat tablosu (V5)
├── 011-create-analysis-thread-stat-table.yaml # analysis_thread_stat tablosu (V5)
├── 012-create-http-stat-tables.yaml # analysis_status_code_stat + analysis_http_method_stat (V5)
├── 013-create-analysis-timeline-stat-table.yaml # analysis_timeline_stat tablosu (V5)
├── 014-add-parse-quality-fields.yaml # parse_quality_score / format_confidence vb. (V5)
├── 015-add-parser-and-filter-fields-to-analysis-job.yaml # analysis_job'a parser/filtre alanları (V5)
├── 016-add-normalized-message-to-frequent-error.yaml # frequent_error.normalized_message (V5)
├── 017-add-indexes-to-stat-tables.yaml # yeni istatistik tablolarına index (V5)
├── 018-add-detected-envelope-to-analysis-job.yaml # analysis_job.detected_envelope (V6.1)
├── 019-add-detected-envelope-to-log-analysis.yaml # log_analysis.detected_envelope (V6.1)
├── 020-add-timeline-granularity-to-log-analysis.yaml # log_analysis.timeline_granularity (V6.1)
└── 021-widen-analysis-counters-to-bigint.yaml # tüm sayaç kolonlarını BIGINT'e genişletir (V6.1)
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
| `LOG_FORMAT_DETECTION_SAMPLE_SIZE` | Backend | `50` | (V5) Otomatik format algılamada okunacak örnek satır sayısı. |
| `LOG_FORMAT_CONFIDENCE_THRESHOLD` | Backend | `60` | (V5) Format güven skoru bu değerin altındaysa PLAIN_TEXT'e düşülür. |
| `MAX_STACK_TRACE_LINES` | Backend | `500` | (V5) Bir multiline exception kaydında tutulacak maksimum devam satırı sayısı. |
| `MAX_LOG_LINE_LENGTH` | Backend | `10000` | (V5) İşlenmeden önce her log satırının kırpılacağı maksimum karakter sayısı (aşırı regex backtracking riskine karşı). |
| `MAX_DISTINCT_LOGGERS` | Backend | `200` | (V5) Logger/thread/HTTP method dağılımlarında tutulacak maksimum benzersiz değer sayısı. |
| `MAX_DISTINCT_ERROR_GROUPS` | Backend | `500` | (V5) Normalize edilmiş hata gruplarının maksimum sayısı. |
| `MAX_TIMELINE_BUCKETS` | Backend | `500` | (V5) Bu sayı aşılırsa dakikalık bucket'lar otomatik olarak saatliğe birleştirilir. |
| `MAX_UNPARSED_LINE_PERCENTAGE` | Backend | `50` | (V5) Parse edilemeyen satır oranı bu yüzdeyi aşarsa job `TOO_MANY_UNPARSED_LINES` ile `FAILED` olur. |
| `ENVELOPE_DETECTION_ENABLED` | Backend | `true` | (V6.1) Envelope (toplayıcı öneki) tespitinin açık/kapalı olması. |
| `ENVELOPE_DETECTION_CONFIDENCE_THRESHOLD` | Backend | `80` | (V6.1) Bu değerin altında kalan envelope tespiti uygulanmaz, dosya değiştirilmeden işlenir. |
| `LOG_FORMAT_DETECTION_MAX_SAMPLE_SIZE` | Backend | `2000` | (V6.1) Format örneklemesi için (devam satırı olmayan yeterli satır bulana kadar) taranacak maksimum ham satır sayısı. |

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
| `analysisName` | hayır | analiz adına göre (büyük/küçük harf duyarsız, kısmi eşleşme) arama |
| `minErrorCount` | hayır | bu sayı ve üzerinde hata içeren kayıtları filtreleme |

```bash
curl "http://localhost:8080/api/v1/analyses?page=0&size=20&sort=analyzedAt,desc&fileName=app&analysisName=odeme&minErrorCount=5"
```

Cevap `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` alanlarını içerir.

**GET** `/api/v1/analyses/{id}` — tek bir analizin detayını döner. Kayıt yoksa `404 Not Found`.

**DELETE** `/api/v1/analyses/{id}` — analiz kaydını ve ilişkili en-sık-hata kayıtlarını siler. Başarılı silmede `204 No Content`, kayıt yoksa `404 Not Found`.

### Analiz Job Endpoint'leri (V4)

**POST** `/api/v1/analysis-jobs` — yeni bir asenkron analiz job'ı oluşturur, hemen (analiz beklemeden) cevap döner.
Content-Type: `multipart/form-data`

| Form alanı | Zorunlu mu | Açıklama |
|---|---|---|
| `file` | evet | |
| `analysisName` | evet | 3-100 karakter, trim edilir |
| `parserType` (V5) | hayır | `SPRING_BOOT`/`JSON`/`NGINX_ACCESS`/`APACHE_ACCESS`/`PLAIN_TEXT`; boş bırakılırsa `AUTO` |
| `startTime` / `endTime` (V5) | hayır | ISO-8601 tarih-saat |
| `levels` (V5) | hayır | virgülle ayrılmış (örn. `INFO,ERROR`) |
| `logger` / `thread` (V5) | hayır | içerik araması, yalnızca Spring Boot/JSON'da desteklenir |
| `messageContains` (V5) | hayır | içerik araması |
| `statusCodes` / `httpMethods` (V5) | hayır | virgülle ayrılmış, yalnızca Nginx/Apache'de desteklenir |
| `pathContains` (V5) | hayır | içerik araması, yalnızca Nginx/Apache'de desteklenir |

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

**Manuel parser seçimi ile örnek:**
```bash
curl -X POST -F "file=@backend/src/test/resources/fixtures/spring-boot-sample.log" \
  -F "analysisName=Manuel Parser Testi" -F "parserType=SPRING_BOOT" -F "levels=ERROR" \
  http://localhost:8080/api/v1/analysis-jobs
```

**GET** `/api/v1/analysis-jobs/{id}` — job'ın güncel durumunu ve tüm detaylarını döner. Kayıt yoksa `404 Not Found` (`JOB_NOT_FOUND`). (V5) Response artık `requestedParserType`, `detectedLogFormat`, `appliedFilters` alanlarını da içerir.

### V5 Parser Hata Kodları

| errorCode | HTTP Status | Ne zaman |
|---|---|---|
| `INVALID_PARSER_TYPE` | 400 | `parserType` geçerli bir `LogFormat` değeri değil |
| `INVALID_DATE_RANGE` | 400 | `endTime`, `startTime`'dan önce/aynı, ya da tarih formatı geçersiz |
| `UNSUPPORTED_FILTER_FOR_PARSER` | 400 | Seçilen (veya algılanan) formatın desteklemediği bir filtre girildi |
| `SELECTED_PARSER_CANNOT_PARSE_FILE` | job `FAILED` | Manuel seçilen parser, dosyanın gerçek içeriğiyle uyumlu değil |
| `LOG_FORMAT_COULD_NOT_BE_DETECTED` | job `FAILED` | `AUTO` modunda dosyada hiç anlamlı satır bulunamadı |
| `TOO_MANY_UNPARSED_LINES` | job `FAILED` | Parse edilemeyen satır oranı `MAX_UNPARSED_LINE_PERCENTAGE`'ı aştı |

`UNSUPPORTED_LOG_FORMAT`, `INVALID_JSON_LOG_ENTRY`, `SENSITIVE_DATA_MASKING_FAILED`, `LOG_LINE_TOO_LONG`, `STACK_TRACE_LIMIT_EXCEEDED` spec'in errorCode listesinde önerilmiş ama mimarimizde bilinçli olarak kullanılmamıştır — bkz. [Bilinen Eksikler](#bilinen-eksikler).

**GET** `/api/v1/analysis-jobs` — sayfalı job listesi. Parametreler: `page`, `size`, `sort` (varsayılan `createdAt,desc`), `analysisName`, `fileName`, `status` (`PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`/`CANCELLED`).

**POST** `/api/v1/analysis-jobs/{id}/cancel` — `PENDING` bir job'ı anında iptal eder; `RUNNING` bir job için iptal talebi oluşturur (bir sonraki kontrol noktasında işlenir). Geçersiz durumdaki bir job için `409 Conflict` (`INVALID_JOB_STATE`).

**POST** `/api/v1/analysis-jobs/{id}/retry` — yalnızca `FAILED` bir job'ı tekrar `PENDING`'e alır (aynı job ID'siyle). Retry limiti aşılmışsa `409 Conflict` (`RETRY_LIMIT_EXCEEDED`).

## Frontend Geçmiş ve Detay Ekranları

- **Analiz Geçmişi** — kayıtlı tüm analizlerin listesi. Her satırda analiz adı (V4), dosya adı, analiz tarihi, dosya boyutu, toplam satır, ERROR sayısı, exception sayısı, işlem süresi ile birlikte bir **Detay** ve bir **Sil** butonu bulunur. Sayfa altında önceki/sonraki sayfa butonları ve mevcut/toplam sayfa bilgisi gösterilir. Üstteki arama çubuğuyla analiz adına göre (V4) ve dosya adına göre arama, minimum ERROR sayısına göre filtreleme yapılabilir.
- **Detay butonuna** basıldığında, o analizin tüm bilgileri (dosya metadata'sı, tüm sayaçlar, en sık hata mesajları tablosu) ayrı bir ekranda gösterilir.
- **Sil butonuna** basıldığında önce bir onay penceresi açılır; onaylanırsa kayıt silinir, başarı mesajı gösterilir ve liste otomatik olarak yenilenir.
- Liste; yükleniyor (loading), boş (empty state), hata (backend erişilemiyor veya beklenmeyen sunucu hatası) durumlarını ayrı ayrı, kullanıcı dostu şekilde ele alır.

## Frontend Analiz İşleri Ekranları (V4)

Uygulama artık üç ana görünüme sahip: **Yeni Analiz**, **Analiz İşleri**, **Analiz Geçmişi**.

- **Yeni Analiz** artık senkron değil — kullanıcı bir analiz adı girip dosya seçtiğinde, bir job oluşturulur ve kullanıcı otomatik olarak **Analiz İşleri** sekmesindeki job takip ekranına yönlendirilir.
- **Analiz İşleri listesi** — analiz adı, dosya adı, durum, progress, oluşturulma/başlama/tamamlanma zamanı, retry sayısı ve **Detay/İptal/Retry/Sonuç** butonlarını gösterir; butonlar job durumuna göre aktif/pasif olur (örn. yalnızca `FAILED` job'da Retry aktif, "Sonuç" yalnızca `SUCCEEDED` **ve** bağlı analiz sonucu hâlâ mevcutsa aktif). Analiz adına, dosya adına ve duruma göre filtrelenebilir.
- **Job Detay ekranı** — tüm job bilgileri, `PENDING`/`RUNNING` durumunda bir progress bar, geçen süre, hata bilgisi (varsa, kullanıcı dostu çevrilmiş biçimde) ve `SUCCEEDED` job için "Sonucu Görüntüle" butonu (Analiz Geçmişi'ndeki detay ekranına yönlendirir; analiz sonucu ayrıca silinmişse bu buton görünmez).
- **Polling** — job takip ekranı, job'ın durumunu her **2 saniyede bir** sorgular; job terminal bir duruma (`SUCCEEDED`/`FAILED`/`CANCELLED`) ulaşınca polling otomatik durur, retry ile job yeniden aktif hale gelirse otomatik yeniden başlar; ekran kapatılınca (component unmount) da temizlenir; backend'e ulaşılamazsa ya da job artık bulunamıyorsa (silinmişse) kullanıcıya ayrı, anlaşılır mesajlar gösterilir. Geçersiz bir durumda iptal/retry denenirse (örn. zaten tamamlanmış bir job, ya da retry limiti dolmuş bir job), backend'in döndüğü hata kodu çevrilip ekranda gösterilir.

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
- Gerçek bir analiz hatasının (I/O hatası) `RUNNING → FAILED` geçişini tetiklediğinin ve analiz kaydı oluşturmadığının doğrulanması
- Başarılı bir job'ın geçici dosyasının gerçekten silindiğinin doğrulanması
- Retry limiti kesin olarak dolduğunda geçici dosyanın da silindiğinin doğrulanması
- Liquibase migration testinin `analysis_job` tablosunu ve `log_analysis.analysis_name` sütununu da kapsaması

V5 ile eklenen test senaryoları (80 yeni birim testi + 5 entegrasyon testi, 14 test dosyası):
- Her parser için doğru ayrıştırma (Spring Boot, JSON — standart + alternatif alan adları + epoch millis, Nginx, Apache, plain text — seviye tespiti + exception tipi + timestamp)
- Format algılamanın doğru çalışması, güven skorunun hesaplanması, eşik altında PLAIN_TEXT'e düşülmesi, hiç örnek satır olmadığında kontrollü hata
- Manuel parser seçiminin hem başarılı hem başarısız (uyumsuz dosya) senaryoları
- Multiline stack trace'in tek kayıt olarak gruplanması, devam satırı limitinin uygulanması, root cause'un (zincirdeki en son `Caused by`) doğru çıkarılması
- UUID/IP/sayısal ID/timestamp/hex normalizasyonu, aynı normalize mesaja sahip farklı ham mesajların tek grupta toplanması
- Authorization/Bearer/e-posta/kredi kartı/password/api-key maskeleme
- Parse kalite skorunun hem yüksek hem düşük (parse edilemeyen satır olan) senaryolarda doğru hesaplanması
- Timeline bucket'larının doğru oluşturulması, dakikalık→saatlik otomatik ölçeklenmesi, timestamp'siz kayıtların görmezden gelinmesi
- Logger/thread/status-code/http-method dağılımlarının doğru sayılması ve `MAX_DISTINCT_LOGGERS` ile sınırlandırılması
- Tarih aralığı/level/logger/thread/mesaj/status-code/http-method/path filtrelerinin her birinin ayrı ayrı doğru çalışması
- Parser ile uyumsuz filtre kombinasyonunda kontrollü validation hatası
- Yeni tüm alanların (parser/format/skor/istatistik) gerçek PostgreSQL'e kalıcı olarak yazılması (Testcontainers)
- V5 migration'larının (yeni tablolar + yeni sütunlar) gerçekten uygulandığının doğrulanması
- 5.000 satırlık bir dosyanın streaming ile hatasız işlenmesi

Her parser için ayrı bir fixture dosyası `backend/src/test/resources/fixtures/` altında bulunur (bkz. [Parser Testlerinin Çalıştırılması](#parser-testlerinin-çalıştırılması)).

V6.1 ile eklenen test senaryoları (34 yeni test):
- 3 envelope biçiminin (RFC3164, RFC5424, CRI) tespiti, güven skoru, `strip()` ile doğru ayrıştırılması, eşik altı karışık dosyada soyma yapılmaması, `ENVELOPE_DETECTION_ENABLED=false` ile kapatılabilirlik
- CRI ardışık P parçalarının F ile birleştirilmesi, stdout/stderr karışmaması, beklenmeyen stream'de grup sonlandırma, eksik/limit aşan kayıtların sessizce yutulmaması, dış zaman damgasının ilk P parçasından alınması
- Format güven skoru hesabının devam satırlarını dışlaması, `LOG_FORMAT_DETECTION_MAX_SAMPLE_SIZE` üst sınırının çalışması, stack-trace ağırlıklı bir dosyada doğru parser'ın reddedilmemesi
- Exception sınıf adının mesajın ortasında tespiti
- Zaman çizelgesinin birden fazla kademe üst üste yükselmesi
- journald/CRI önekli dosyaların uçtan uca doğru analiz edilmesi (gerçek job akışı üzerinden): envelope+format tespiti, multiline exception'ın tek kayıt sayılması, dış zaman damgası fallback'i, timeline oluşumu, gerçekten parse edilemeyen satırların sayılması
- Analiz sonucunun (kayıt + istatistikler) tek transaction'da kaydedilmesi ve bir istatistik hata verdiğinde TÜMÜNÜN rollback edilmesi
- `Integer.MAX_VALUE`'yu aşan bir sayacın doğru saklanıp okunması

Envelope biçimlerinin her biri için ayrı bir fixture dosyası bulunur; `envelope-real-world-sample.log`, elle yazılmamış, gerçek bir sistemden (`journalctl -o short`, WSL sistem journal'ı) alınmış çıktıdır — hassas veri içermediği için maskeleme gerekmedi.

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

Testlerde gerçek backend yerine API mock'ları kullanılır. Ayrıca: Türkçe/İngilizce dil seçimi ve kalıcılığı (`LanguageSwitcher.test.tsx`, `i18n/index.test.ts`), job durumlarının iki dilde gösterimi (`JobStatusBadge.test.tsx`) ve API hata kodlarının iki dilde çevrilmesi/fallback davranışı (`utils/apiErrorMessage.test.ts`) ayrı, otomatik testlerle doğrulanmıştır.

V5 ile eklenen senaryolar (28 yeni test, 3 yeni test dosyası):
- Parser seçeneklerinin gösterilmesi, varsayılan seçimin `AUTO` (Otomatik Algıla) olması, manuel seçimin `onParserTypeChange`'i doğru değerle tetiklemesi
- Gelişmiş filtre panelinin varsayılan olarak kapalı olması, aç/kapa butonunun çalışması
- Seçilen parser'a göre alakasız filtre alanlarının (Spring Boot'ta HTTP alanları, Nginx'te logger/thread) gizlenmesi
- Log seviyesi checkbox'larının işaretlenip filtre state'ine yansıması
- Doldurulan parser/filtre alanlarının gerçekten `createAnalysisJob` çağrısına iletilmesi
- Backend'den dönen `INVALID_DATE_RANGE` gibi bir hatanın ekranda çevrilmiş olarak gösterilmesi
- Algılanan formatın, parse kalite skorunun, format güven skorunun, ilk/son log zamanının, parse başarı yüzdesinin sonuç ekranında gösterilmesi
- V5 alanları olmayan (V5 öncesi) bir analiz için ilgili bölümlerin (Format Bilgisi, Timeline, Logger/Thread, HTTP dağılımı) hiç render edilmemesi — geriye dönük uyumluluk
- Log zaman çizelgesinin (INFO/WARN/ERROR ayrımlı) gösterilmesi
- Normalize edilmiş hata grubunun (normalize mesaj + sayı + örnek ham mesaj) ve maskelenmiş bir mesajın olduğu gibi gösterilmesi + maskeleme notunun görünmesi
- Logger/thread istatistiklerinin, HTTP status/method dağılımının gösterilmesi; veri olmayan durumlarda "Bu log formatında veri bulunamadı" empty state'i
- Yeni 6 V5 hata kodunun (`INVALID_PARSER_TYPE`, `INVALID_DATE_RANGE`, `UNSUPPORTED_FILTER_FOR_PARSER`, `SELECTED_PARSER_CANNOT_PARSE_FILE`, `LOG_FORMAT_COULD_NOT_BE_DETECTED`, `TOO_MANY_UNPARSED_LINES`) hem Türkçe hem İngilizce çevrildiğinin ve ham backend mesajına düşmediğinin doğrulanması
- V5 metinlerinin (parser seçimi, gelişmiş filtreler, format bilgisi başlıkları) hem Türkçe hem İngilizce'de doğru render edildiğinin dedike bir testle (`i18n/v5Texts.test.tsx`) doğrulanması

## Parser Testlerinin Çalıştırılması

```bash
cd backend
./mvnw test -Dtest="com.hatice.loginsight.parser.*"
```

Bu komut, `parser` paketindeki 10 test sınıfını (her parser + detector + aggregator + extractor + normalizer + masker için) izole olarak çalıştırır — veritabanı/Docker gerektirmez, saniyeler içinde biter. Tüm parser testleri ve entegrasyon testi dahil olmak üzere V5'in tam kapsamını görmek için `./mvnw clean test` (bkz. [Backend Testleri](#backend-testleri)) kullanılmalıdır.

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

### Parser Seçim Ekranı (V5)
![Parser seçim ekranı](screenshots/v5-parser-selection.png)

### Gelişmiş Filtre Ekranı (V5)
![Gelişmiş filtre ekranı](screenshots/v5-advanced-filters.png)

### Log Zaman Çizelgesi (V5)
![Log zaman çizelgesi](screenshots/v5-timeline.png)

### Parse Kalite Skoru ve Format Bilgisi (V5)
![Parse kalite skoru](screenshots/v5-parse-quality-score.png)

### Normalize Edilmiş Hata Grupları (V5)
![Normalize edilmiş hata grupları](screenshots/v5-normalized-errors.png)

### Türkçe Arayüz (V5)
![Türkçe arayüz V5](screenshots/v5-ui-turkish.png)

### İngilizce Arayüz (V5)
![İngilizce arayüz V5](screenshots/v5-ui-english.png)

## Bilinen Eksikler
- Sürükle-bırak (drag-and-drop) desteği eklendi ancak farklı tarayıcılarda kapsamlı test edilmedi.
- `mostFrequentErrors` listesinde üst sınır (örn. ilk 10) uygulanmıyor; çok sayıda benzersiz hata mesajı olan büyük dosyalarda liste uzun olabilir.
- Frontend, backend health check'i sadece sayfa ilk yüklendiğinde kontrol ediyor; periyodik otomatik yenileme yapmıyor.
- Analiz geçmişi ve analiz işleri listelerinde toplu (birden fazla kaydı aynı anda) silme/iptal/retry desteği yok; kayıtlar tek tek işlenebiliyor.
- `useJobPolling` hook'u, polling zaten bir hata mesajı gösteriyorken kullanıcı dil değiştirirse, ekrandaki mesajı hemen değil bir sonraki başarısız denemede yeni dile çevirir (bilinçli, küçük bir basitleştirme — bkz. kod içi yorum).
- Job geçmişinde otomatik arşivleme/eskimiş kayıtları temizleme mekanizması yok; `analysis_job` tablosu süresiz büyür.

**V5'e Özgü Bilinen Eksikler:**
- `UNSUPPORTED_LOG_FORMAT`, `INVALID_JSON_LOG_ENTRY`, `SENSITIVE_DATA_MASKING_FAILED` errorCode'ları spec'in önerdiği listede var ama bilinçli olarak hiç kullanılmıyor: `PlainTextLogParser` her zaman bir fallback sağladığı için "desteklenmeyen format" durumu hiç oluşmuyor; geçersiz bir JSON satırı zaten `unparsedLineCount`'a düşüp `TOO_MANY_UNPARSED_LINES` güvenlik ağıyla korunuyor; `SensitiveDataMasker` saf regex tabanlı olduğu için hiçbir zaman exception fırlatmıyor.
- `LOG_LINE_TOO_LONG` ve `STACK_TRACE_LIMIT_EXCEEDED` job'u `FAILED` yapmıyor — bilinçli olarak sessiz kırpma (truncate) ile "güvenli tarafta kal" yaklaşımı seçildi; limitlerin amacı zaten kaynak korumak, dosyayı reddetmek değil.
- Nginx/Apache ayrımı, "common" (referrer'sız) formatı Nginx'e, "combined" (referrer'lı) formatı Apache'ye atayarak yapılıyor — ama gerçek dünyada Nginx da combined format üretebilir. Böyle bir Nginx dosyası yanlışlıkla Apache olarak algılanabilir (fonksiyonel olarak zararsız, çünkü iki parser'ın ayrıştırdığı alanlar neredeyse aynı, ama `detectedLogFormat` etiketi yanıltıcı olabilir).
- `MAX_UNPARSED_LINE_SAMPLES` environment variable'ı spec'in örnek listesinde var ama kullanılmıyor — sistem hiçbir yerde parse edilemeyen satırların ham örneklerini saklamıyor, sadece sayısını (`unparsedLineCount`) tutuyor.
- Log Zaman Çizelgesi grafiği harici bir kütüphane olmadan, saf CSS ile çizilen basit bir sütun grafiği — yakınlaştırma (zoom), belirli bir zaman aralığını seçme gibi etkileşimli özellikler yok, sadece görsel bir özet.
- Format algılama yalnızca dosyanın ilk 50 satırına bakıyor; teorik olarak bir dosyanın ortasında format değişirse (örn. rotasyon sırasında iki farklı uygulamanın loglarının birleşmesi gibi son derece nadir bir senaryo), bu değişiklik hiç fark edilmez.
- `mostFrequentLoggers`/`mostFrequentThreads`/`mostFrequentErrors` listelerinde üst sınır UI'da uygulanmıyor (V4'ten devam eden bilinen eksikliğin V5'teki yeni listeler için de geçerli hali) — backend `MAX_DISTINCT_*` ile veri toplamayı sınırlıyor ama frontend tüm listeyi tek seferde render ediyor.

**V6.1'e Özgü Bilinen Eksikler:**
- `DOCKER_JSON` envelope biçimi (Docker'ın `json-file` log sürücüsü) desteklenmiyor — spec bunu "isteğe bağlı" olarak işaretlediği için kapsam dışı bırakıldı; yalnızca SYSLOG_RFC3164, SYSLOG_RFC5424 ve CONTAINER_CRI (containerd/CRI-O metin biçimi) destekleniyor.
- RFC3164 syslog zaman damgasında yıl bilgisi yok (`Jul 30 06:55:07`); içinde bulunulan yıl varsayılarak tamamlanıyor — yıl sonunda/başında analiz edilen, önceki yıla ait bir dosyada bu yanlış yıl üretebilir.
- Envelope sınıflandırması sezgiseldir, kesin değildir — toplayıcı önekleri birbirine çok benzeyebilir, güven eşiği altında kalan durumlarda dosya değiştirilmeden (düz metin gibi) işlenir.
- CRI parça birleştirmede içerik bütünlüğü (checksum) doğrulaması yok — bu V6 kapsamı dışında bırakıldı, sadece parça sırası ve stream ayrımı garanti ediliyor.

## Karşılaşılan Sorunlar ve Çözümleri

### V6.1'e Özgü Sorunlar

- **`int`→`long` genişletmesinde art arda birkaç eksik/kısmi diff:** Sayaçları 64 bit'e genişletirken (Faz 9), bazı dosyalarda gerçek "eski hali/yeni hali" kod bloğu yerine "ilgili getter/setter'lar da değişti" gibi düz yazı özetler verildi — bu, birden fazla ardışık derleme hatasına (`AnalysisSummaryDto`, `AnalysisDetailDto`, stat entity'leri, `AnalysisHistoryService`'teki gözden kaçan bir yardımcı metot) yol açtı. Ayrıca bir kopyalama sırasında `AnalysisSummaryDto` içeriği yanlışlıkla `AnalysisJobSummaryDto.java` dosyasına yazıldı (duplicate class hatası). Hepsi, gerçek derleme çıktısı paylaşılıp eksik kalan HER dosyanın tam/literal içeriği verilerek çözüldü; bu olay, "her değişiklik literal olarak verilmeli, düz yazıyla özetlenmemeli" kuralının önemini bir kez daha gösterdi.
- **`@Transactional`'ın aynı sınıf içinden çağrıldığında sessizce atlanması:** Analiz sonucu + istatistiklerin tek transaction'da kaydedilmesi gerekiyordu; `AnalysisJobRunner.handleSuccess()`'e doğrudan `@Transactional` eklemek çalışmazdı (self-invocation, V4'teki `@Async` sorununun kardeşi). Kaydetme mantığı ayrı bir bean'e (`AnalysisResultPersister`) taşınarak, dışarıdan çağrılan bir metotta gerçek transaction sınırı sağlandı.

### V5'e Özgü Sorunlar

- **Spring Boot 4.1'in Jackson'ı otomatik getirmemesi:** `spring-boot-starter-webmvc`, V4'te Liquibase'de yaşadığımız modülerleşme sorununun bir benzeriyle, artık JSON (Jackson) desteğini otomatik getirmiyor — `JsonLogParser` yazılınca derleme hatası (`package com.fasterxml.jackson.databind does not exist`) alındı. `spring-boot-starter-json` bağımlılığı ayrıca eklenerek çözüldü.
- **Jackson 3.x'in paket adını (namespace) değiştirmesi:** Yukarıdaki düzeltmeden sonra AYNI hata farklı bir şekilde tekrar çıktı. Kanıt (`./mvnw dependency:tree`) incelendiğinde, Spring Boot 4.1'in aslında Jackson 3.x kullandığı görüldü — Jackson 3.0 ile birlikte proje `com.fasterxml.jackson.databind` yerine `tools.jackson.databind` paket adını kullanmaya başlamış (sadece `jackson-annotations` eski adında kalmış). `JsonLogParser`'daki import satırları güncellenerek çözüldü.
- **`016` numaralı migration'ın (frequent_error.normalized_message) uygulanmamış olması:** Bir önceki mesajda "ekledim" denilmiş ama migration dosyasının içeriği ile master changelog diff'i hiç paylaşılmamıştı — sonraki bir fazda Testcontainers testi `SchemaManagementException: Schema validation: missing column [normalized_message]` hatasıyla bunu ortaya çıkardı. Migration dosyası ve master changelog güncellemesi gerçekten paylaşılarak çözüldü; bu olay, her adımda gerçek dosya içeriğinin verilmesi gerektiğinin önemini gösterdi.
- **`@Transactional` test metodu + `@Async` servis kombinasyonunun klasik tuzağı:** `frequentErrors` koleksiyonundaki `LazyInitializationException`'ı çözmek için bir Testcontainers testine `@Transactional` eklenince, testin TAMAMI tek bir veritabanı transaction'ında çalışmaya başladı; bu transaction commit olmadığı için, ayrı bir thread'de (ayrı bağlantıyla) çalışan `AnalysisJobRunner` henüz "görünmeyen" job satırını bulamadı, sessizce hiçbir şey yapmadan döndü, job sonsuza kadar `PENDING` kaldı. Kanıt (test süresinin tam olarak zaman aşımı süresine denk gelmesi, sonra job'ın hâlâ `PENDING` olduğunun görülmesi) izlenerek teşhis edildi. Çözüm: `@Transactional`'ı testten kaldırıp, `LogAnalysisRepository`'ye `JOIN FETCH` kullanan bir `findByIdWithFrequentErrors()` sorgu metodu eklemek — açık bir Hibernate oturumuna hiç gerek kalmadı.
- **`PlainTextLogParser`'ın satır başındaki timestamp'i hiç okumaması:** Manuel UI testinde (`huge-test.log`, 150.000 satır), timestamp'i olan satırlarda bile Log Zaman Çizelgesi'nin boş kaldığı ve parse kalite skorunun gereksiz düşük (80) çıktığı fark edildi. Kanıt (dosyanın gerçek ilk satırlarının paylaşılması) toplandıktan sonra, `PlainTextLogParser`'ın timestamp alanını hiçbir zaman doldurmadığı görüldü — parser sadece seviye/mesaj ayrıştırıyordu. Satır başında `yyyy-MM-dd HH:mm:ss` kalıbına uyan bir tarih varsa ayrıştıran bir yardımcı metot eklenerek çözüldü.
- **V1-V4'ten kalma testlerin yeni V5 alanlarıyla kırılması (birkaç kez):** (1) `createJob(file, analysisName)` imzasına yeni zorunlu parametreler eklenince mevcut testler derlenemez oldu — eski imza, yeni metodu `null` filtrelerle çağıran bir overload olarak geri eklenerek çözüldü. (2) Yeni "En Sık Hatalar" tablosunun normalize+ham mesajı ayrı sütunlarda göstermesi, ham mesajın (normalize edilmemiş bir test verisinde) iki kez görünmesine ve eski bir testin (`getByText` ile tekil eşleşme bekleyen) kırılmasına yol açtı — `getAllByText` ile düzeltildi. (3) `AnalysisJobDetail` tipine eklenen yeni zorunlu alanlar, mevcut test mock nesnelerini geçersiz kıldı — alanlar isteğe bağlı (`?:`) yapılarak çözüldü.
- **Frontend'de yanlış alan adıyla test verisi kurulması:** Yeni bir frontend testinde `LogAnalysisApiError`'a `errorCode` adında bir alan verilmişti, ama gerçek `ApiError` interface'i bu alanı `error` olarak adlandırıyordu (`LogAnalysisApiError`'ın kendi `errorCode` property'si zaten bunu constructor içinde otomatik dolduruyor). TypeScript derleme hatası (`Object literal may only specify known properties`) sayesinde derleme aşamasında yakalandı.

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

---

**V5'te yapay zekâdan hangi konularda destek alındığı:**
- Çoklu parser mimarisinin baştan tasarımı (Strategy + Factory Pattern, ortak `ParsedLogEntry` modeli, `LogParserFactory`'nin Spring'in otomatik bean toplama mekanizmasıyla kurulması)
- 5 parser'ın (Spring Boot, JSON, Nginx, Apache, plain text) her birinin regex/ayrıştırma mantığının yazılması
- Otomatik format algılama algoritmasının (örnekleme, güven skoru, eşik, fallback) tasarımı
- Multiline stack trace gruplama durum makinesinin (state machine) ve root cause çıkarımının tasarımı
- Hata mesajı normalizasyonu ve hassas veri maskeleme için merkezi, sıralı regex kural setlerinin tasarımı
- Zaman çizelgesi (timeline) bucket'lama ve otomatik dakika→saat ölçeklendirme mantığının tasarımı
- Gelişmiş filtreleme mimarisinin (`JobFilterCriteria`, `AnalysisFilterSupport`) ve parser-filtre uyumluluk kontrolünün tasarımı
- Yeni veritabanı şemasının (11 yeni sütun, 5 yeni tablo) ve Liquibase migration'larının yazılması
- Backend'in (`AnalysisJobRunner`) tamamen yeniden yazılıp tüm bu parçaların gerçek analiz akışına bağlanması
- Frontend'de parser seçimi/gelişmiş filtre UI'ı ve sonuç ekranının yeni bölümlerinin (format bilgisi, timeline grafiği, istatistik tabloları) tasarımı
- 80+ backend, 30+ frontend testinin yazımı (Testcontainers entegrasyon testi dahil)
- Ortam/kod sorunlarının (Jackson paket adı değişikliği, eksik migration, `@Transactional`+`@Async` çakışması, eksik timestamp ayrıştırma) kanıta dayalı olarak debug edilmesi

**Parser mimarisi için alınan destek:**
- "Parser seçimi controller veya service içerisinde uzun if-else veya switch bloklarıyla yapılmamalıdır" gereksinimi, `LogParser` interface'i + Strategy Pattern + `LogParserFactory` (Factory Pattern) ile karşılandı; Spring'in `List<LogParser>` constructor injection'ı sayesinde yeni bir parser eklemenin `LogParserFactory`'ye hiç dokunmadan yapılabilmesi özellikle vurgulandı.
- "Yeni parser eklemek için mevcut parser implementasyonlarında mümkün olduğunca değişiklik yapılmamalıdır" gereksinimi, ortak `ParsedLogEntry` modeli + polymorphic çağrılarla (`AnalysisJobRunner`'ın hiçbir zaman somut parser sınıflarını bilmemesi) yapısal olarak sağlandı.

**Regex üretimi için kullanılan promptlar:**
- Her formatın spec'teki örnek satırı verilip, o satırı tam olarak eşleştirecek + ilgili alanları (timestamp/level/thread/logger/message gibi) named gruplarla çıkaracak regex istendi.
- "Regex kullanımlarında aşırı backtracking riskine dikkat edilmelidir" gereksinimi doğrultusunda, her satırın işlenmeden önce `MAX_LOG_LINE_LENGTH` ile kırpılması istendi — regex'in kendisini "felaket geri izlemeye" (catastrophic backtracking) dayanıklı hale getirmek yerine, girdi boyutunu sınırlayarak riski azaltan bir yaklaşım tercih edildi.

**JSON log desteği için kullanılan promptlar:**
- "En az aşağıdaki yaygın alan adları desteklenmelidir" listesi (timestamp/time/@timestamp gibi) doğrudan `JsonLogParser`'daki aday-alan-isimleri listelerine yansıtıldı.
- Jackson'ın Spring Boot 4.1'de otomatik gelmemesi ve ardından Jackson 3.x'in paket adı değişikliği, gerçek derleme hatalarının paylaşılıp kanıta dayalı çözülmesiyle giderildi (bkz. Karşılaşılan Sorunlar).

**Nginx ve Apache log desteği için kullanılan promptlar:**
- "En az common veya combined formatlarından biri desteklenmelidir" gereksinimi karşısında, iki formatın ayırt edilmesi için bilinçli bir tasarım kararı (common→Nginx, combined→Apache) istendi ve kullanıcıya bu kararın gerekçesi ve sınırlaması (gerçek Nginx'in de combined üretebileceği) açıkça sorulup onaylatıldı.

**Multiline stack trace için kullanılan promptlar:**
- "Exception başlangıç satırı, at ile başlayan satırlar, Caused by, Suppressed, ... n more satırları aynı hata kaydının parçası olarak ele alınmalı" listesi, `MultilineExceptionAggregator`'daki `isContinuationLine()` kontrol sırasına birebir yansıtıldı.
- "Root cause mümkünse tespit edilmelidir" gereksinimi, `ExceptionInfoExtractor`'ın `Caused by:` zincirindeki EN SON eşleşmeyi root cause kabul etmesiyle karşılandı.

**Format detection için kullanılan promptlar:**
- "İlk belirli sayıdaki anlamlı satır incelenebilir", "boş satırlar dikkate alınmamalı", "format güven skoru eşik değerin altındaysa PLAIN_TEXT fallback kullanılmalı" maddeleri `LogFormatDetector`'daki `collectSampleLines`/`detect` metotlarına doğrudan yansıtıldı.

**Hata normalizasyonu için kullanılan promptlar:**
- "En az UUID, IPv4, IPv6, sayısal ID, timestamp, port, request ID, trace ID, hexadecimal değerlerin normalize edilmesi değerlendirilmelidir" listesi, `LogMessageNormalizer`'daki sıralı kural listesine (en özelden en genele) birebir yansıtıldı; sıralamanın neden önemli olduğu (UUID'nin içindeki rakamların genel sayı kuralına düşmemesi gerektiği) ayrıca sorgulanıp gerekçelendirildi.

**Hassas veri maskeleme için kullanılan promptlar:**
- "Maskeleme, mesaj veritabanına yazılmadan ve UI response'una eklenmeden önce uygulanmalıdır" gereksinimi, pipeline sırasının (önce maskele, sonra normalize et) `AnalysisJobRunner.processGroup()` içinde açıkça bu sırayla kodlanmasıyla karşılandı.

**Kalite skoru ve timeline için kullanılan promptlar:**
- "Skorun bilimsel kesinlik ifade etmediği açıkça belirtilmelidir" gereksinimi hem kod yorumlarında (spec gereği artık kaldırıldı, bkz. aşağıdaki not) hem bu README'de açıkça belirtildi.
- "Sistem dosyanın zaman aralığına göre uygun bucket büyüklüğünü otomatik seçebilir" önerisi, `LogTimelineAggregator`'ın dakikalık başlayıp `MAX_TIMELINE_BUCKETS` aşılınca saatliğe otomatik geçmesiyle karşılandı.

**Filtre yapısı için kullanılan promptlar:**
- "Filtreler yalnızca seçilen log formatının desteklediği alanlara uygulanmalıdır" gereksinimi, `AnalysisFilterSupport`'un hem job oluşturulurken (manuel seçimde) hem job çalışırken (AUTO dahil) çağrılmasıyla iki katmanlı olarak karşılandı — bu ikinci katmanın (AUTO için runtime kontrolü) ilk taslakta eksik olduğu, kullanıcının "hiçbir şeyi ertelemeyelim" talebi üzerine tekrar gözden geçirilirken fark edildi ve eklendi.

**Yapay zekânın ürettiği regex ve parser kodlarının nasıl test edildiği:**
- Her parser için hem doğrudan (inline string) hem fixture dosyası okuyarak çalışan birim testleri yazıldı; `./mvnw test -Dtest=<ParserAdı>Test` ile izole çalıştırıldı.
- Testcontainers entegrasyon testiyle gerçek bir PostgreSQL'e karşı uçtan uca (job oluşturma → asenkron çalışma → veritabanına yazma) doğrulandı.
- Ayrıca gerçek, büyük bir dosya (`huge-test.log`, 150.000 satır) tarayıcıda manuel olarak yüklenip sonuç ekranı satır satır incelendi — bu manuel test, otomatik testlerin kaçırdığı gerçek bir eksikliği (`PlainTextLogParser`'ın timestamp ayrıştırmaması) ortaya çıkardı.

**Yapay zekânın ürettiği kodlarda yapılan manuel değişiklikler:**
- İlk üretilen `AnalysisDetailView.tsx`'te, algılanan format adını (bir string) `StatCard`'a `number` tipine zorlanarak (`as unknown as number`) geçiren "hileli" bir satır fark edilip, `StatCard`'ın `value` prop'u `number | string` kabul edecek şekilde düzeltilerek kaldırıldı.
- İlk üretilen `AnalysisJobV5PersistenceTest`'te test zaman aşımı 10 saniyeydi; gerçek çalıştırmalarda (özellikle Docker/Testcontainers'ın ilk ısınma süresi nedeniyle) yetersiz kaldığı görülüp 30 saniyeye çıkarıldı, ayrıca zaman aşımında sessizce yanlış veri dönmek yerine açık bir hata fırlatacak şekilde güçlendirildi.

**Reddedilen veya hatalı bulunan öneriler:**
- İlk yaklaşımda, format uyuşmazlığı sadece manuel parser seçiminde job oluşturma anında kontrol ediliyordu; `AUTO` modunda algılanan format ile uyumsuz bir filtre girildiğinde hiçbir uyarı verilmeden sessizce "0 sonuç" dönmesi, kullanıcının "hiçbir şeyi ertelemeyelim" talebiyle yeniden gözden geçirilirken eksik bulundu ve `AnalysisJobRunner` içine ikinci bir kontrol katmanı eklenerek düzeltildi.
- Testte `@Transactional` ekleyerek `LazyInitializationException`'ı "çözme" yaklaşımı, async job akışıyla çakıştığı (job'un hiç görünmez kalması) fark edilince reddedildi; yerine `JOIN FETCH` sorgusu tercih edildi.

**V5 sırasında öğrenilen konular:**
- Strategy ve Factory Pattern'in birlikte kullanımı, Spring'in `List<Interface>` constructor injection'ıyla "kayıt tabanlı" (registry-based) bir mimarinin nasıl kurulduğu
- Interface/polymorphism'in, yeni bir tür eklerken mevcut kodu değiştirmeden genişletebilme (Open/Closed Principle) için nasıl kullanıldığı
- Regex'te "felaket geri izleme" (catastrophic backtracking) riski ve girdi boyutu sınırlama ile azaltılması
- Örnekleme (sampling) ile format/tür tahmini yapmanın güçlü ve zayıf yönleri; bir "güven skoru"nun neyi ifade edip neyi ifade etmediği
- Durum makinesi (state machine) deseninin, sadece job lifecycle'da değil, satır-satır akan bir metin akışını gruplamak (multiline log kayıtları) için de kullanılabileceği
- Merkezi, sıralı kural setleriyle (normalizasyon, maskeleme) çalışmanın, dağınık/kopyalanmış mantığa göre hem test edilebilirliği hem bakımı nasıl kolaylaştırdığı
- Jackson 3.x'in Spring Boot 4.x ile birlikte getirdiği paket adı (namespace) değişikliği ve bunun bağımlılık ağacı (`dependency:tree`) incelenerek nasıl teşhis edildiği
- `@Transactional` test metotlarının, ayrı bir thread'de çalışan `@Async` servislerle nasıl çakışabileceği (commit edilmemiş veri görünürlüğü sorunu)

---

**V6.1'de yapay zekâdan hangi konularda destek alındığı:**
- Envelope (syslog RFC3164/RFC5424, CRI) tespiti ve soyma katmanının tasarımı, format tespitinden önce çalışacak şekilde pipeline'a yerleştirilmesi
- CRI bölünmüş kayıt birleştirmenin, taşıma katmanı sorunu olarak multiline stack trace gruplamasından (log içeriği katmanı) bilinçli olarak ayrı bir bileşene çıkarılması
- Format güven skoru ve devam satırı/exception/unparsed-satır sayımı hatalarının kök nedeninin (tek bir yanlış yedek kural) teşhisi ve ortak bir `ContinuationLineDetector`'a çıkarılması
- Kademeli zaman çizelgesi, transactional toplu kaydetme ve 64 bit sayaç genişletmesinin tasarımı ve uygulanması
- Gerçek bir `journalctl` çıktısının fixture olarak hazırlanması

**Envelope desenleri için alınan destek:**
- Spec'teki örnek satırlar (`Jul 30 06:55:07 dc05 java[2172]: ...`, `<134>1 2026-07-30T...`, `... stdout F ...`) birebir eşleşecek regex'ler istendi; her birinin V1-V5 fixture'larıyla (spring-boot, json, nginx, apache, plain-text) yanlış eşleşmediği ayrıca kontrol edildi.

**Atomik/yarış senaryoları için alınan destek:**
- CRI birleştirmede "aynı anda yalnızca bir açık kayıt" ve "stdout/stderr karışmaması" gereksinimleri, tek bir `StringBuilder` tutan ve stream değişiminde açık grubu kontrollü sonlandıran bir tasarımla karşılandı.

**Checkpoint/byte-offset promptları:** V6.1 kapsamında henüz yok (V6.3'te ele alınacak).

**Yapay zekânın ürettiği kodlarda yapılan manuel değişiklikler:**
- Yok — tüm düzeltmeler (bkz. Karşılaşılan Sorunlar) kullanıcının gerçek derleme/test çıktısını paylaşması üzerine yapay zeka tarafından yapıldı, kullanıcı kod satırı düzenlemedi.

**Reddedilen veya hatalı bulunan öneriler:**
- Format güven skoru hesabında devam satırı tespitine "girintili satır + önceki kaydın hata olması" nüansının eklenmesi, önce format tespiti bağlamında döngüsel bağımlılık gerekçesiyle atlanmıştı; kullanıcı "neden yapmıyoruz" diye sorunca, bu gerekçenin yalnızca örnekleme bağlamında geçerli olduğu, asıl multiline gruplamada (parser zaten belliyken) hiçbir engel olmadığı fark edildi ve eklendi.

**Yapay zekâdan alınan kodların nasıl test edildiği:**
- Her faz sonrası ilgili test sınıfları izole çalıştırıldı, önemli refactor'lardan (Faz 8, Faz 9) sonra tam `./mvnw clean test` suite'i çalıştırıldı.
- CRI/envelope davranışı hem izole birim testleriyle (`CriPartialRecordAssemblerTest`, `EnvelopeDetectorTest`) hem gerçek job akışı üzerinden uçtan uca (`AnalysisJobV6EnvelopeTest`) doğrulandı.
- Transaction rollback davranışı, gerçek bir DB constraint ihlali (aşırı uzun logger adı) tetiklenerek test edildi — varsayımla değil, gerçek bir hatayla.

**V6.1 sırasında öğrenilen konular:**
- Toplayıcı (syslog/CRI) önek biçimleri ve bunların log analiz pipeline'ında neden en başta ele alınması gerektiği
- Taşıma katmanı ile log içeriği katmanı arasındaki ayrım ve bunun bileşen tasarımına nasıl yansıtılması gerektiği
- Bir güven skoru hesabında paydaya neyin dahil edilip edilmeyeceğinin sonucu nasıl kökten değiştirebileceği
- `@Transactional`'ın self-invocation sınırlaması ve bunun etrafından dolaşmanın (ayrı bean'e çıkarma) V4'teki `@Async` deneyiminden nasıl genellenebildiği
- Kod değişikliklerini literal diff olarak vermenin (özetlemek yerine) neden kritik olduğu — bu dersin sert biçimde, gerçek derleme hatalarıyla öğrenildiği
- Otomatik testlerin (ne kadar kapsamlı olursa olsun) gerçek, büyük ve çeşitli veriyle yapılan MANUEL testlerin yerini tutamayacağı — bu projede en az bir gerçek, kullanıcı tarafından bulunan eksiklik (timestamp ayrıştırma) sadece manuel UI testiyle ortaya çıktı