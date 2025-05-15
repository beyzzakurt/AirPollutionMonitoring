# 🌍  Real-Time Air Pollution Monitoring Platform

Bu proje, dünya genelinden hava kirliliği verilerini toplayan, analiz eden ve anomali tespiti yaparak bildirimler oluşturan web tabanlı bir platformdur. Spring Boot kullanılarak geliştirilen bu sistem, verileri Apache Kafka ile kuyruğa alır, InfluxDB'ye kaydeder ve istatistiksel yöntemlerle anomali analizi yapar.

## 🧭 Projenin Amacı ve Kapsamı

Bu platformun temel amacı, [https://waqi.info](https://waqi.info) sitesinden alınan hava kirliliği verilerini merkezi bir sistemde işleyerek:
- Belirli konumlar için hava kirliliği analizleri yapmak
- Anomali tespitleri ile hava kirliliği risklerine karşı erken uyarı sistemleri oluşturmak
- Manuel ve otomatik veri girişleriyle sistemin güvenilirliğini artırmak
- WHO standartları doğrultusunda veri eşiklerini değerlendirip bildirimler üretmektir.

## 🧱 Sistem Mimarisi

```
+-------------------+       +-------------------+      
| Air Pollution API |  -->  |     Kafka Queue   |  
+-------------------+       +-------------------+       
                                             |
                                             v
             +-------------------+      +--------------------------+
             | InfluxDB (Time DB)|<--->|  Spring Boot Backend API  |
             +-------------------+      +--------------------------+
                                                  |
                                +-----------------+-------------------+
                                |       REST API & Endpoints         |
                                +------------------------------------+
```

### 📦 Komponentler
- **Spring Boot Backend**: REST API, veri işleme ve analiz işlemleri burada yapılır.
- **InfluxDB**: Zaman serisi verileri (hava kirlilik değerleri) burada saklanır.
- **Apache Kafka & Zookeeper**: API’lerden alınan veriler kuyruğa alınır ve burada dağıtılır.
- **Kafka UI**: Kuyruktaki verileri izlemek için kullanılır.
- **Docker Compose**: Sistemi tüm bileşenleriyle ayağa kaldırmak için kullanılır.

## 🛠️ Teknoloji Seçimleri ve Gerekçeleri

| Teknoloji     | Amacı / Gerekçesi                                                                |
|---------------|----------------------------------------------------------------------------------|
| **Spring Boot** | REST API geliştirmek                                                             |
| **InfluxDB** | Zaman serisi veriler (hava kirliliği ölçümleri) için optimize edilmiş veritabanı |
| **Apache Kafka** | Yüksek hacimli veri akışını yönetmek ve ayrıştırmak                              |
| **Docker Compose** | Tüm servisleri tek komutla ayağa kaldırmak                                       |


## ⚙️ Kurulum Adımları

### 1. Gereksinimler
- Docker & Docker Compose
- Java 17+
- Git

### 2. Repositoryi Klonlayın
```bash
git clone https://github.com/beyzzakurt/AirPollutionMonitoring.git
cd AirPollutionMonitoring
```

### 3. Servisleri Başlatın
```bash
docker-compose up -d
```

Docker, aşağıdaki container'ları başlatır:
- Kafka
- Zookeeper
- Kafka UI (http://localhost:8081)
- InfluxDB (http://localhost:8086)

### 4. Projeyi Çalıştırın
```bash
mvn spring-boot:run
```

## 🚀 Kullanım Rehberi

### Manuel Veri Girişi

```bash
./manual-input.sh 41.38 2.16 148.2 72.1 45.3 36.5 90.2
```
Parametreler:
- Enlem, Boylam, PM2.5, PM10, NO2, SO2, O3

### Otomatik Test Scripti

```bash
./auto-test.sh --duration=30 --rate=2 --anomaly-chance=20
```

Parametreler:
- `--duration`: Script’in çalışma süresi
- `--rate`: Saniyede kaç istek atılacağı
- `--anomaly-chance`: Anomali oluşturma olasılığı

## 📡 API Dökümantasyonu

Postman'da endpoint'leri kolaylıkla kullanabilirsiniz.

### 1. Hava Kirliliği Verisini InfluxDB'ye Kaydet

```
POST http://localhost:8080/rest/api/air-pollution/add
```

**Body:**
```json
{
  "status": "ok",
  "data": {
    "aqi": 29,
    "idx": 6669,
    "attributions": [
      {
        "url": "http://www20.gencat.cat/portal/site/mediambient/",
        "name": "Medi Ambient. Generalitat de Catalunya",
        "logo": "Spain-Catalunya.png"
      },
      {
        "url": "http://www.eea.europa.eu/themes/air/",
        "name": "European Environment Agency",
        "logo": "Europe-EEA.png"
      },
      {
        "url": "https://waqi.info/",
        "name": "World Air Quality Index Project",
        "logo": null
      }
    ],
    "city": {
      "name": "Barcelona (Eixample), Catalunya, Spain",
      "url": "https://aqicn.org/city/spain/catalunya/barcelona-eixample",
      "geo": [
        41.385343283956,
        2.1538219595817
      ],
      "lat": 41.385343283956,
      "lon": 2.1538219595817
    },
    "dominentpol": "o3",
    "time": {
      "s": "2025-05-12 19:00:00",
      "tz": "+02:00",
      "v": 1747076400
    },
    "iaqi": {
      "co": {
        "v": 0.1
      },
      "no2": {
        "v": 14.7
      },
      "o3": {
        "v": 29.3
      },
      "pm10": {
        "v": 12.0
      },
      "pm25": {
        "v": 25.0
      },
      "so2": {
        "v": 0.6
      }
    }
  }
}
```

### 2. Belirli Konum İçin Hava Kirliliği Verilerini Getir

```
GET http://localhost:8080/rest/api/air-pollution/barcelona
```


### 3. Belirli Konum İçin Anomalileri Listele

```
GET http://localhost:8080/rest/api/air-pollution/anomalies/lima
```


### 4. Belirli Konum İçin Geçmişteki Anomalileri Listele

```
GET http://localhost:8080/rest/api/air-pollution/anomalies/history/lima?start=2025-05-11T01:00:00Z&end=2025-05-11T20:30:00Z
```

Parametreler:
- `--start`: Tarih ve saat ile başlangıç belirtilir.
- `--end`: Tarih ve saat ile bitiş belirtilir.


### 5. Kafka Üzerinden Hava Kirliliği Verisini Kaydet

```
POST http://localhost:8080/rest/api/air-pollution/send-to-kafka
```


**Body:**
```json
{
  "status": "ok",
  "data": {
    "aqi": 24,
    "idx": 6669,
    "attributions": [
      {
        "url": "http://www20.gencat.cat/portal/site/mediambient/",
        "name": "Medi Ambient. Generalitat de Catalunya",
        "logo": "Spain-Catalunya.png"
      },
      {
        "url": "http://www.eea.europa.eu/themes/air/",
        "name": "European Environment Agency",
        "logo": "Europe-EEA.png"
      },
      {
        "url": "https://waqi.info/",
        "name": "World Air Quality Index Project",
        "logo": null
      }
    ],
    "city": {
      "name": "Barcelona (Eixample), Catalunya, Spain",
      "url": "https://aqicn.org/city/spain/catalunya/barcelona-eixample",
      "geo": [
        41.385343283956,
        2.1538219595817
      ],
      "lon": 2.1538219595817,
      "lat": 41.385343283956
    },
    "dominentpol": "o3",
    "time": {
      "s": "2025-05-07 05:00:00",
      "tz": "+02:00",
      "v": 1746594000
    },
    "iaqi": {
      "co": {
        "v": 0.1
      },
      "no2": {
        "v": 5.5
      },
      "o3": {
        "v": 24.0
      },
      "pm10": {
        "v": 13.0
      },
      "pm25": {
        "v": 25.0
      },
      "so2": {
        "v": 0.6
      }
    }
  }
}
```

 
## 🧪 Anomali Tespit Yöntemleri

Anomali olarak işaretlenen durumlar:
- Son 24 saatlik ortalamaya göre %50’den fazla artış
- WHO limitlerini aşan değerler
- 25 km yarıçap içinde tespit edilen anomaliler

Kullanılan yöntemler:
- Z-score
- Interquartile Range (IQR)
- Sabit threshold (WHO standartları)


## 🧯 Sorun Giderme (Troubleshooting)

| Problem | Çözüm |
|--------|-------|
| `Kafka bağlantısı reddedildi` | Kafka container’larının çalıştığını kontrol edin: `docker ps` |
| `InfluxDB erişilemiyor` | Port 8086'nın açık olduğundan emin olun. |
| API çağrıları başarısız | Spring Boot loglarını inceleyin |

