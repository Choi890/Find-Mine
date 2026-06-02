# Find Mine

온디바이스 생활 기억 보조 앱입니다. 물건을 어디에 두었는지 사진, 음성, 텍스트로 기록하고 나중에 로컬 DB에서 검색합니다.

## v3 구현

- 음성 입력: Android 음성 인식 인텐트에 오프라인 선호 옵션 적용
- OCR: ML Kit Text Recognition bundled Korean/Latin 모델
- 이미지 라벨링: ML Kit bundled Image Labeling 모델
- 자동 추출: 음성/OCR 텍스트에서 물건명, 장소, 태그 후보 추출
- 검색: Room FTS 인덱스 + 로컬 n-gram 임베딩 유사도 + 동의어 확장
- 사진: 촬영/갤러리 첨부 후 OCR + 이미지 라벨링 자동 실행
- 사진 속 물건과 과거 기록 연결 추천
- 히스토리: 같은 물건의 과거 위치 표시
- 마지막으로 본 장소 자동 추천
- 습관 학습: 즐겨찾기, 검색 횟수, 반복 기록, 최근성을 기반으로 우선순위 계산
- 외출 전 알림: 일정/비/외출 컨텍스트를 로컬에서 입력받아 빠뜨릴 가능성이 높은 물건 추천
- 서버/API 호출 없음, `INTERNET` 권한 없음

예시 흐름:

```text
오늘 일정: 학교
비: 켬
외출: 켬

오늘 학교 일정이 있습니다. 비 컨텍스트가 켜져 있습니다.
외출 전 확인 기준으로 최근 우산은 현관 오른쪽 신발장에 기록되어 있습니다.
```

## 기술 메모

현재 안정 빌드는 Room 2.8.4의 FTS4 엔티티를 사용합니다. SQLite FTS5는 Room3 `@Fts5`와 `BundledSQLiteDriver` 조합으로 갈 수 있지만 아직 alpha 계열이라, 이 MVP는 기기 호환성과 안정성을 우선했습니다.

음성 엔진은 현재 Android 시스템 인식기를 사용합니다. `OnDeviceSpeechInput`으로 분리해 두었기 때문에 이후 sherpa-onnx 또는 whisper.cpp 엔진으로 교체하기 쉽습니다.

이미지 라벨링은 ML Kit bundled 모델을 사용합니다. 앱 크기는 늘지만 모델이 앱에 포함되어 첫 실행부터 사용할 수 있습니다.

## 실행

Android Studio에서 이 폴더를 열거나 터미널에서 실행합니다.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_SDK_ROOT='C:\Users\rolot\AppData\Local\Android\Sdk'
$env:ANDROID_HOME='C:\Users\rolot\AppData\Local\Android\Sdk'
.\gradlew.bat :app:assembleDebug
```

디버그 APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 다음 확장

- sherpa-onnx/whisper.cpp 완전 로컬 STT 엔진 탑재
- TFLite/LiteRT 이미지 라벨링 모델 추가
- Room3 + BundledSQLiteDriver 기반 FTS5 전환
- 실제 소형 임베딩 TFLite 모델로 유사 검색 교체
- Android Calendar 권한 기반 로컬 일정 연동
- 날씨 API 없이 사용자 입력/센서/일정 기반 컨텍스트 자동화
