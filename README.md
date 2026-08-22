# 簡單羅盤

一個以 Kotlin 與 Jetpack Compose 開發的 Android 羅盤 APP。介面以即開即用為目標，提供磁北、地理正北、氣泡水平儀、座標、海拔、主題色盤及北向觸覺回饋。

## 功能

- 即時顯示方位角與八方位名稱
- 磁北與地理正北一鍵切換
- 羅盤中心氣泡水平儀
- 座標與海拔資訊
- 對準目前北向時提供細微觸覺回饋
- 感測器校正與磁場干擾提示
- 感測器狀態防抖，避免提示訊息頻繁切換
- 明亮／深色快捷切換
- 經典、海洋、森林、夕陽與高對比色盤
- Android 12 以上支援 Material You 動態色
- 導覽列與狀態列跟隨亮暗主題
- 保持螢幕常亮選項
- 所有設定使用 DataStore 保存在裝置上

## 系統需求

- Android 8.0（API 26）以上
- 建議使用具備磁力計的 Android 實機
- JDK 17
- Android SDK 36

沒有磁力計的裝置仍可安裝 APP，但無法提供羅盤方向。

## 權限與隱私

APP 只會在使用者切換正北或啟用位置資訊時要求位置權限：

- `ACCESS_COARSE_LOCATION`：取得約略座標並計算磁偏角
- `ACCESS_FINE_LOCATION`：取得較精確座標與 GPS 海拔

定位只在 APP 位於前景時低頻更新，每 15 秒或移動 10 公尺時取得新位置。APP 不需要網路權限，座標、海拔與設定不會上傳。

若位置權限被拒絕，羅盤會繼續以磁北模式運作。

## 方位計算

APP 優先使用 Android 旋轉向量感測器 `TYPE_ROTATION_VECTOR`。不支援時，會退回加速度計與磁力計組合計算方位。

方位資料採圓周低通濾波，避免跨越 359°／0° 時發生反向跳動。地理正北使用目前位置與 Android `GeomagneticField` 計算磁偏角：

```text
地理正北方位 = 磁北方位 + 磁偏角
```

感測器狀態會分別追蹤方位與磁力計準確度。警告需持續約 1.5 秒才顯示，恢復穩定需持續約 3 秒才清除，降低臨界值附近的視覺閃爍。

## 北向觸覺回饋

手機接近水平且方位進入北向 `±1°` 時，APP 會觸發一次輕微的系統時鐘刻度回饋。方位離開 `±4°` 後才會重新啟用，避免在 0° 附近連續震動。

觸覺回饋遵循 Android 系統設定，也可在 APP 設定中關閉。

## 建置

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS 或 Linux：

```bash
./gradlew assembleDebug
```

Debug APK 會產生在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 測試與檢查

執行單元測試、Debug 建置與 Android Lint：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

目前單元測試涵蓋：

- 角度正規化與八方位區間
- 359°／0° 的平滑處理
- 磁偏角與正北方位換算
- 北向觸覺回饋的進入與重新啟用門檻
- 感測器警告延遲、恢復延遲與磁場遲滯判定

## 專案結構

```text
app/src/main/kotlin/com/status/simplecompass/
├── MainActivity.kt                  # Activity、權限請求與狀態整合
├── data/
│   └── SettingsRepository.kt        # DataStore 設定保存
├── location/
│   └── CompassLocationManager.kt    # 前景定位與磁偏角
├── sensor/
│   ├── CompassMath.kt               # 方位計算、濾波與觸覺門檻
│   ├── CompassSensorManager.kt      # Android 感測器生命週期
│   └── SensorStatusStabilizer.kt    # 感測器狀態防抖
└── ui/
    ├── CompassApp.kt                # Compose 主畫面與設定面板
    └── theme/Theme.kt               # 色盤、動態色與系統列
```

## 實機驗證

Android 模擬器通常無法提供可信的磁力計、GPS 海拔與觸覺效果。發布前應使用實機確認：

- 羅盤方向與螢幕旋轉後的方向修正
- 磁場干擾與校正提示
- 正北磁偏角換算
- GPS 海拔是否由裝置提供
- 氣泡移動方向與水平判定
- 三鍵及手勢導覽列的亮暗效果
- 北向觸覺強度
