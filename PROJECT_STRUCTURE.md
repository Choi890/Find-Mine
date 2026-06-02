# Find Mine 프로젝트 구조 설명

## 프로젝트 한줄 설명

Android Kotlin 앱입니다. 사진 OCR, 음성 입력, 로컬 텍스트 임베딩, Room 데이터베이스를 사용해 사용자의 기록을 저장하고 검색하는 개인 메모리 검색 앱입니다.

## 기본 작동 흐름

- MainActivity가 Compose 화면을 띄우고 FindMineViewModel이 화면 상태와 사용자 동작을 관리합니다.
- MemoryRepository가 OCR/음성/텍스트 파서/임베딩/Room DAO를 연결해 기록 저장과 검색을 처리합니다.
- Room 데이터베이스와 FTS 테이블이 기록 본문을 로컬에서 보관하고 빠르게 검색합니다.

## 문서 기준

- 아래 목록은 `git ls-files`로 확인되는 Git 추적 파일을 기준으로 작성했습니다.
- `.git`, `node_modules`, `build`, `.gradle`, 임시 업로드/출력물처럼 Git이 관리하지 않는 폴더는 제외했습니다.
- 폴더 표는 코드와 자산이 어떤 책임으로 나뉘는지, 파일 표는 각 파일이 실제로 무엇을 담당하는지 설명합니다.

## 폴더별 설명 (16개)

| 폴더 | 설명 |
| --- | --- |
| `.` | 프로젝트 루트입니다. 실행/빌드 설정, README, 전체 구조 문서, 최상위 진입 파일이 모여 있습니다. |
| `app` | Android 앱 모듈입니다. 앱 전용 빌드 설정, 소스 코드, 리소스, ProGuard 설정이 이 아래에 있습니다. |
| `app/src` | Android 소스 세트가 들어 있는 상위 폴더입니다. main, test 같은 빌드 대상별 파일을 구분합니다. |
| `app/src/main` | 실제 앱에 포함되는 AndroidManifest, Kotlin/Java 소스, 리소스, 에셋을 담는 기본 소스 세트입니다. |
| `app/src/main/java` | 앱의 Kotlin/Java 패키지 루트입니다. 패키지명에 맞춰 실제 클래스 파일이 하위 폴더에 배치됩니다. |
| `app/src/main/java/com` | Kotlin 패키지 네임스페이스의 `com` 단계입니다. 실제 앱 패키지는 이 아래 `findmine`, `focussound`, `ownlifeos` 같은 이름으로 이어집니다. |
| `app/src/main/java/com/findmine` | Find Mine 앱의 최상위 Kotlin 패키지입니다. 화면 진입점과 주요 기능 패키지가 이 아래에서 갈라집니다. |
| `app/src/main/java/com/findmine/data` | 앱 데이터 계층입니다. 로컬 DB, DAO, Entity, Repository처럼 저장소와 데이터 변환 코드를 담당합니다. |
| `app/src/main/java/com/findmine/ui` | 화면, ViewModel, UI 상태처럼 사용자 인터페이스와 직접 연결되는 Kotlin 파일을 담습니다. |
| `app/src/main/res` | Android XML 리소스 루트입니다. 문자열, 색상, 스타일, 아이콘, XML 설정처럼 코드가 참조하는 리소스를 보관합니다. |
| `app/src/main/res/drawable` | Android 벡터/드로어블 이미지 리소스 폴더입니다. 아이콘이나 그래픽 XML을 보관합니다. |
| `app/src/main/res/values` | 문자열, 색상, 테마, 스타일 등 앱 전역 XML 값을 정의하는 리소스 폴더입니다. |
| `app/src/main/res/xml` | 백업 규칙, 파일 공유 경로, 데이터 추출 규칙처럼 Android 시스템에 전달하는 XML 설정을 보관합니다. |
| `gradle` | Gradle Wrapper와 데몬 설정처럼 Android/Kotlin 빌드 도구가 사용하는 파일을 보관합니다. |
| `gradle/wrapper` | 개발 PC에 Gradle이 없어도 동일한 버전으로 빌드할 수 있게 하는 Wrapper 실행 파일과 속성 파일을 보관합니다. |
| `screenshots` | 앱 화면을 설명하거나 README에서 보여주기 위한 스크린샷 이미지를 보관합니다. |

## 파일별 설명 (39개)

| 파일 | 설명 |
| --- | --- |
| `.gitignore` | Git에 올리지 않을 빌드 산출물, 캐시, 개인 환경 파일을 지정하는 설정 파일입니다. 저장소에는 필요한 소스/자산만 남기도록 도와줍니다. |
| `app/build.gradle.kts` | Android 앱 모듈의 Gradle 빌드 설정입니다. SDK 버전, 의존성, Kotlin/Compose/Room 같은 모듈별 빌드 옵션을 지정합니다. |
| `app/proguard-rules.pro` | 릴리스 빌드에서 코드 축소/난독화를 할 때 유지해야 할 클래스나 예외 규칙을 지정합니다. |
| `app/src/main/AndroidManifest.xml` | Android 앱의 패키지 구성, Activity/Service, 권한, 파일 provider 같은 시스템 등록 정보를 선언합니다. |
| `app/src/main/java/com/findmine/data/FindMineDatabase.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. FindMineDatabase Kotlin 소스입니다. 주 역할은 Room 데이터베이스 정의와 Entity/DAO 연결 입니다. |
| `app/src/main/java/com/findmine/data/ImageTextRecognizer.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. ImageTextRecognizer Kotlin 소스입니다. 주 역할은 음성/이름/패턴 인식 처리 입니다. |
| `app/src/main/java/com/findmine/data/LocalTextEmbedding.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. LocalTextEmbedding Kotlin 소스입니다. 주 역할은 기록 텍스트를 로컬 벡터로 변환해 유사도 검색과 의미 기반 검색에 사용할 임베딩 값 생성 입니다. |
| `app/src/main/java/com/findmine/data/MemoryDao.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. MemoryDao Kotlin 소스입니다. 주 역할은 데이터베이스 접근 쿼리 정의 입니다. |
| `app/src/main/java/com/findmine/data/MemoryRecord.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. MemoryRecord Kotlin 소스입니다. 주 역할은 저장 데이터 구조 정의 입니다. |
| `app/src/main/java/com/findmine/data/MemoryRecordFts.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. MemoryRecordFts Kotlin 소스입니다. 주 역할은 Room FTS 전문 검색 테이블을 정의해 기록 본문을 빠르게 검색할 수 있게 하는 인덱스 모델 구성 입니다. |
| `app/src/main/java/com/findmine/data/MemoryRepository.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. MemoryRepository Kotlin 소스입니다. 주 역할은 데이터 계층과 화면/도메인 계층 연결 입니다. |
| `app/src/main/java/com/findmine/data/MemoryTextParser.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. MemoryTextParser Kotlin 소스입니다. 주 역할은 입력 텍스트나 프롬프트를 앱 내부 모델로 해석 입니다. |
| `app/src/main/java/com/findmine/data/OnDeviceSpeechInput.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. OnDeviceSpeechInput Kotlin 소스입니다. 주 역할은 Android 기기 내 음성 입력을 받아 텍스트 기록으로 변환하는 음성 입력 처리 입니다. |
| `app/src/main/java/com/findmine/data/SmartMemoryEngine.kt` | Find Mine의 데이터 계층에서 기록 저장, OCR/음성 입력, 임베딩, 검색 처리를 담당합니다. SmartMemoryEngine Kotlin 소스입니다. 주 역할은 핵심 처리 엔진과 실행 흐름 제어 입니다. |
| `app/src/main/java/com/findmine/ui/FindMineViewModel.kt` | Find Mine의 화면 상태와 사용자 인터랙션을 담당하는 UI 계층 코드입니다. FindMineViewModel Kotlin 소스입니다. 주 역할은 화면 상태, 이벤트 처리, 비동기 작업 관리 입니다. |
| `app/src/main/java/com/findmine/ui/MainActivity.kt` | Find Mine의 화면 상태와 사용자 인터랙션을 담당하는 UI 계층 코드입니다. MainActivity Kotlin 소스입니다. 주 역할은 Android 화면 진입점과 UI 초기화 입니다. |
| `app/src/main/res/drawable/ic_launcher.xml` | Android 런처 아이콘 또는 적응형 아이콘 구성을 정의하는 XML 리소스입니다. |
| `app/src/main/res/values/strings.xml` | 앱에서 표시하는 문자열 리소스를 한 곳에 모아 다국어 처리와 재사용을 쉽게 합니다. |
| `app/src/main/res/values/styles.xml` | 앱 테마와 공통 스타일을 정의해 화면 전반의 색상/폰트/컴포넌트 모양을 통일합니다. |
| `app/src/main/res/xml/backup_rules.xml` | Android 자동 백업에 포함하거나 제외할 앱 데이터를 지정하는 XML 규칙입니다. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Android 데이터 추출/백업 정책에서 어떤 데이터를 이동 가능한지 지정하는 XML 규칙입니다. |
| `app/src/main/res/xml/file_paths.xml` | FileProvider가 외부 앱에 안전하게 공유할 수 있는 파일 경로를 지정합니다. |
| `build.gradle.kts` | 루트 Gradle 빌드 설정입니다. Android/Kotlin 플러그인과 전체 프로젝트 빌드 구성을 정의합니다. |
| `gradle.properties` | Gradle 빌드 성능, AndroidX 사용 여부, Kotlin/빌드 옵션 같은 공통 속성을 지정합니다. |
| `gradle/gradle-daemon-jvm.properties` | Gradle 데몬이 사용할 JVM 관련 속성을 지정해 빌드 환경을 일정하게 유지합니다. |
| `gradle/wrapper/gradle-wrapper.jar` | Gradle Wrapper가 지정된 Gradle 버전을 내려받고 실행하는 데 사용하는 바이너리 파일입니다. |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle Wrapper가 사용할 Gradle 배포판 버전과 다운로드 URL을 지정합니다. |
| `gradlew` | Unix/macOS/Linux에서 Gradle Wrapper를 실행하는 스크립트입니다. |
| `gradlew.bat` | Windows에서 Gradle Wrapper를 실행하는 배치 스크립트입니다. |
| `PROJECT_STRUCTURE.md` | 프로젝트의 모든 주요 폴더와 Git 추적 파일을 한글로 설명하는 구조 문서입니다. 처음 보는 사람이 경로별 역할을 빠르게 파악하기 위해 추가했습니다. |
| `README.md` | 프로젝트 개요, 실행 방법, 주요 기능을 설명하는 기본 안내 문서입니다. |
| `screenshots/add.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/alerts.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/alerts2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/metric_photos.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/metric_records.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/search.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `screenshots/search2.png` | 앱 화면 상태나 UI 변경 결과를 기록한 스크린샷 이미지입니다. 문서화와 화면 비교에 사용됩니다. |
| `settings.gradle.kts` | Gradle이 인식할 프로젝트 이름과 포함할 모듈을 지정하는 설정 파일입니다. |

## 읽는 방법

- 먼저 폴더별 설명에서 큰 기능 묶음을 확인한 다음, 파일별 설명에서 실제 구현 파일을 찾으면 됩니다.
- Android 프로젝트는 `app/src/main/java` 아래 Kotlin 파일이 핵심 코드이고, `app/src/main/res`와 `app/src/main/assets`는 화면/모델/오디오 자산입니다.
- 웹 프로젝트는 `index.html`, `styles.css`, `script.js` 또는 `app.js`가 화면 구조, 스타일, 동작을 나눠 담당합니다.
- Python 프로젝트는 루트의 실행 스크립트와 `src`, `backend`, `scripts`, `tests` 폴더를 함께 보면 처리 흐름을 이해할 수 있습니다.
