<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/assets/icon-dark.svg">
  <img src="docs/assets/icon-light.svg" alt="Civilight" width="96">
</picture>

# Civilight

이야기와 세계관이 무거운 게임을 위한 시나리오 작성 앱. 폴더블 전용.

![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![version](https://img.shields.io/badge/version-0.1.0-blue)
![font license](https://img.shields.io/badge/font-Pretendard%20OFL%201.1-lightgrey)

</div>

## 소개

Civilight는 스크립트, 인물, 세계, 연표를 한 프로젝트 안에서 쓰는 앱이다.
혼자 쓰는 작가를 위해 만들었다. 커버 화면에서 빠르게 쓰고, 안쪽 화면에서 펼쳐 놓고 본다.
어디까지 썼는지를 기준으로 인물과 설정을 보여 주어, 아직 쓰지 않은 내용이 미리 새지 않게 한다.

## 주요 기능

기능 명세는 8개 영역, 462개 항목이다. 아래는 코드에 들어 있는 것을 영역별로 줄인 것이다.

### 스크립트

- 대사, 나레이션, 괄호 지문, 연출 지시, 선택지, 시스템 텍스트, 작가 메모 블록
- 화자 고르기와 줄바꿈 규칙을 갖춘 원고 편집
- 씬 단위 위치 표시(`이 씬 · 1-7`)와 씬 나누기, 합치기
- 커버 화면 한 칸 배치와 안쪽 화면 원고 + 옆 패널 배치

### 스토리 · 시점 · 때 · 점검

- 프로젝트 홈, 이어 쓰기, 새 프로젝트의 `시작해 볼 것`
- 시점 렌즈: 지금까지 쓴 곳을 기준으로 인물과 설정을 보여 준다
- 씬의 때를 기준과 간격으로 저장하고, 이동 거리와 시간을 점검한다
- 순서와 때가 어긋나거나 앞질러 쓴 용어를 점검 센터에 모은다

### 인물

- 인물 목록, 카드, 태그 필터, 찾기
- 프로젝트별 항목과 비밀 항목
- 두 사람 사이의 관계와 각자의 마음, 변화 기록
- 관계도

### 세계

- 장소, 용어, 세력 카드
- 용어의 첫 공개 기록과 후보 확인
- 세계 달력과 연표
- 지도와 인물 동선

### 캔버스 · 페이지

- 확대와 이동이 되는 캔버스, 노드와 선, 선 라벨
- 관계 선을 관계 기록으로 잇기
- 페이지, 카드, 이미지 보기

### 서재 · 보호 · 파일

- 프로젝트 목록, 최근 연 파일, 예시 프로젝트
- 잠금과 다시 잠그기, 등급과 숨기기
- 내보내기와 `.civilight` 파일 열기
- 지우기와 되살리기 휴지통

### 라이프 레코더

- 녹음 화면과 각 화면에서 들어가는 진입점
- 마이크 권한 안내와 글로 쓰기 대체
- 받아쓰기는 아직 자리만 있다 (아래 진행 상황 참고)

### 디자인 시스템

- 라이트와 다크 테마, Pretendard 글꼴
- 유리 막대, 명령 칸, 분할 버튼, 탭, 대화상자
- 원고 줄 세 개와 캐럿으로 만든 앱 마크

## 화면

<!-- SCREENSHOTS -->

## 설치

1. [Releases](../../releases)에서 `civilight-0.1.0.apk`를 받는다.
2. 설정에서 이 출처의 앱 설치를 허용한다 (출처를 알 수 없는 앱).
3. 받은 APK를 열어 설치한다.

Android 10 (API 29) 이상이 필요하다.

## 빌드

JDK 21이 필요하다 (Gradle 데몬 툴체인으로 지정됨). Android SDK 경로는 `local.properties`의 `sdk.dir`에 둔다.

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:assembleRelease
```

```bash
./gradlew test
```

릴리즈 서명은 저장소 밖 `~/.civilight-signing/signing.properties` (또는 `CIVILIGHT_SIGNING` 환경 변수가 가리키는 파일)에서 읽는다. 없으면 서명 없는 릴리즈가 만들어진다.

| 항목 | 값 |
| --- | --- |
| versionName | 0.1.0 |
| minSdk / targetSdk / compileSdk | 29 / 37 / 37 |
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.4.1 |
| Compose BOM | 2026.05.01 |
| Navigation3 | 1.1.2 |

## 구조

22개 Gradle 모듈.

| 묶음 | 모듈 | 역할 |
| --- | --- | --- |
| core | `model` `store` `designsystem` `navigation` `ui` | 데이터 모델, 저장, 디자인 시스템, 라우팅, 공용 화면 부품 |
| domain | `story` `script` `cast` `world` `trash` `library` `recorder` `checks` `actions` | 화면 없는 규칙과 계산 (JVM 테스트 대상) |
| feature | `library` `story` `script` `cast` `world` `map` `canvas` `recorder` | 영역별 화면 |
| app | `app` | 앱 진입점과 화면 연결 |

## 진행 상황

이 앱은 초기 버전이다. 기능 목록과 명세는 [docs/FEATURES.md](docs/FEATURES.md)에 있고, 모듈별 구현 상태는 `docs/status/`에 있다.

- 많은 화면이 부분 구현이다. 컴파일되고 저장소와 연결되어 있지만, 실제 기기에서 다듬어 보지 못한 것이 많다.
- 외부 서비스가 필요한 기능은 자리만 있다. Google Drive 백업, 음성 받아쓰기, 홈 화면 위젯과 빠른 설정 타일, 잠금 화면 녹음, 글로 설명하는 지형 만들기가 그렇다.
- `docs/FEATURES.md`의 상태 열은 아직 갱신되지 않았다.

## 글꼴

본문과 화면 글꼴은 [Pretendard](https://github.com/orioncactus/pretendard)를 쓴다.

## 라이선스

- Pretendard: SIL Open Font License 1.1. 전문은 [licenses/Pretendard-OFL.txt](licenses/Pretendard-OFL.txt).
- 앱 코드: 미정.
