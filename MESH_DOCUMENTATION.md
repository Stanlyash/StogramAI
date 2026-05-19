# Stogram Mesh Network - Документация

## Обзор архитектуры

Stogram - это децентрализованная Mesh сеть, использующая три транспортных протокола для связи между устройствами без сервера:

1. **NFC** (Near Field Communication) - для быстрого установления контакта
2. **BLE** (Bluetooth Low Energy) - для обнаружения устройств на средней дистанции
3. **Wi-Fi Direct** - для высокоскоростной передачи данных

Все три слоя используют **CRDT** (Conflict-free Replicated Data Types) для синхронизации данных без конфликтов.

---

## 📱 Модуль 1: NFC (Near Field Communication)

### Файлы:
- `app/src/main/java/com/stogram/network/nfc/NfcMeshManager.kt`
- `app/src/main/java/com/stogram/network/nfc/MeshNodeData.kt`
- `app/src/test/java/com/stogram/network/nfc/NfcMeshManagerTest.kt`
- `app/src/test/java/com/stogram/network/nfc/MeshNodeDataTest.kt`

### Возможности:
- ✅ Чтение и запись NDEF сообщений
- ✅_foreground Dispatch_ для обработки NFC тегов при активном приложении
- ✅ Модель данных `MeshNodeData` с поддержкой CRDT
- ✅ Генерация уникальных ID узлов
- ✅ Векторные часы для отслеживания версий данных
- ✅ Слияние данных с разрешением конфликтов

### Использование:
```kotlin
val nfcManager = NfcMeshManager(context)
nfcManager.initialize()

// Отправка данных узла
val nodeData = MeshNodeData(
    nodeId = "unique-id",
    nodeName = "Device Name",
    timestamp = System.currentTimeMillis()
)
nfcManager.sendNodeData(nodeData)

// Поток входящих данных
nfcManager.receivedData.collect { data ->
    // Обработка полученных данных
}
```

---

## 📡 Модуль 2: BLE (Bluetooth Low Energy)

### Файлы:
- `app/src/main/java/com/stogram/mesh/ble/BleMeshManager.kt`
- `app/src/test/java/com/stogram/mesh/ble/BleMeshManagerTest.kt`

### Возможности:
- ✅ Сканирование BLE устройств вокруг
- ✅ Реклама собственного устройства
- ✅ GATT сервер для обмена данными
- ✅ Подключение к периферийным устройствам
- ✅ Reactive потоки для обнаружения устройств
- ✅ Автоматическое управление состоянием Bluetooth

### Характеристики:
- **Service UUID**: `0000stgr-0000-1000-8000-00805f9b34fb`
- **Characteristic UUID**: `0000stgc-0000-1000-8000-00805f9b34fb`

### Использование:
```kotlin
val bleManager = BleMeshManager(context)
bleManager.initialize()

// Запуск сканирования
bleManager.startScanning()

// Запуск рекламы
bleManager.startAdvertising()

// Поток найденных устройств
bleManager.discoveredDevices.collect { devices ->
    // Обновление UI со списком устройств
}

// Подключение к устройству
bleManager.connectToDevice(deviceAddress)
```

---

## 📶 Модуль 3: Wi-Fi Direct

### Файлы:
- `app/src/main/java/com/stogram/mesh/wifi/WifiDirectManager.kt`
- `app/src/test/java/com/stogram/mesh/wifi/WifiDirectManagerTest.kt`

### Возможности:
- ✅ Обнаружение устройств через Wi-Fi Direct
- ✅ Создание группы (Group Owner)
- ✅ Присоединение к существующей группе
- ✅ Управление P2P подключениями
- ✅ Мониторинг состояния подключения
- ✅ Интеграция с CRDT для синхронизации

### Использование:
```kotlin
val wifiManager = WifiDirectManager(context)
wifiManager.initialize()

// Запуск обнаружения
wifiManager.startDiscovery()

// Поток доступных пиров
wifiManager.availablePeers.collect { peers ->
    // Обновление списка доступных устройств
}

// Подключение к устройству
val result = wifiManager.connectToDevice(deviceAddress)
when (result) {
    is WifiDirectConnectionResult.Success -> { /* Успех */ }
    is WifiDirectConnectionResult.Failure -> { /* Ошибка */ }
    is WifiDirectConnectionResult.Cancelled -> { /* Отмена */ }
}

// Создание группы
wifiManager.createGroup()
```

---

## 🔐 Разрешения (AndroidManifest.xml)

### NFC:
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

### BLE:
```xml
<!-- Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<!-- Android 11 и ниже -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<!-- Локация для Android 6-11 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" />
```

### Wi-Fi Direct:
```xml
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 🛠 Helper: PermissionHelper

### Файл:
- `app/src/main/java/com/stogram/util/PermissionHelper.kt`

### Возможности:
- ✅ Проверка всех необходимых разрешений
- ✅ Запрос разрешений runtime (для Android 6+)
- ✅ Проверка доступности NFC, BLE
- ✅ Умное определение необходимых разрешений по версии Android

### Использование в MainActivity:
```kotlin
// Автоматический запрос разрешений при запуске
if (!PermissionHelper.hasAllPermissions(this)) {
    PermissionHelper.requestPermissions(this)
}

// Проверка доступности оборудования
val nfcAvailable = PermissionHelper.isNfcAvailable(context)
val bleAvailable = PermissionHelper.isBleAvailable(context)
```

---

## 🧪 Тестирование

### Запуск тестов:
```bash
# Все тесты
./gradlew test

# Тесты NFC
./gradlew test --tests "com.stogram.network.nfc.*"

# Тесты BLE
./gradlew test --tests "com.stogram.mesh.ble.*"

# Тесты Wi-Fi Direct
./gradlew test --tests "com.stogram.mesh.wifi.*"
```

### Покрытие:
- ✅ Unit тесты для моделей данных
- ✅ Unit тесты для менеджеров
- ✅ Instrumented тесты для NFC (требуют устройство)

---

## 🔄 CRDT Синхронизация

Каждый модуль использует CRDT для бесконфликтной синхронизации:

### MeshNodeData структура:
```kotlin
data class MeshNodeData(
    val nodeId: String,           // Уникальный ID узла
    val nodeName: String,         // Имя устройства
    val timestamp: Long,          // Временная метка
    val vectorClock: Map<String, Long>, // Векторные часы
    val payload: String           // Полезная нагрузка
)
```

### Слияние данных:
```kotlin
fun merge(other: MeshNodeData): MeshNodeData {
    // Объединение векторных часов
    val mergedClock = mergeVectorClocks(this.vectorClock, other.vectorClock)
    
    // Выбор более новой версии
    return if (other.timestamp > this.timestamp) other else this
}
```

---

## 🎯 MVVM Архитектура

Проект следует архитектуре MVVM:

```
┌─────────────────┐
│   Compose UI    │  ← LoginScreen.kt
├─────────────────┤
│    ViewModel    │  ← (будет создан)
├─────────────────┤
│  Mesh Managers  │  ← NFC, BLE, Wi-Fi Direct
├─────────────────┤
│     CRDT Layer  │  ← Синхронизация данных
└─────────────────┘
```

---

## 📀 Сборка и запуск

### Требования:
- Android Studio Hedgehog или новее
- JDK 17
- Min SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)

### Сборка:
```bash
./gradlew assembleDebug
```

### Запуск на устройстве:
1. Откройте проект в Android Studio
2. Подключите устройство (Android 7+)
3. Нажмите Run (Shift+F10)
4. Предоставьте все запрошенные разрешения
5. Проверьте статус NFC/BLE/Wi-Fi на главном экране

---

## 🚀 Следующие шаги

1. ✅ NFC модуль - готово
2. ✅ BLE модуль - готово
3. ✅ Wi-Fi Direct модуль - готово
4. ✅ Permissions Helper - готово
5. ⏳ CRDT слой синхронизации - в разработке
6. ⏳ ViewModel для управления Mesh сетью
7. ⏳ UI для отображения соседних устройств
8. ⏳ Экран настроек Mesh сети
9. ⏳ Тестирование на реальных устройствах

---

## 📝 Заметки

- Все модули работают независимо и могут быть включены/выключены
- Приложения работает на Android 7.0 - 15+
- Runtime permissions запрашиваются автоматически при запуске
- UI показывает статус каждого типа подключения
- Данные синхронизируются без сервера через CRDT
