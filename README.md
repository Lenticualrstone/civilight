# Civilight

시나리오 작성 앱 (Android · 폴더블). 이야기 · 세계관 중심 게임의 스크립트, 인물, 세계, 연표를 한곳에서 쓴다.

- 플랫폼: Android 10 (API 29) 이상, 폴더블 커버 · 안쪽 화면
- 언어: Kotlin · Jetpack Compose
- 상태: 개발 중. 기능 목록과 진행 상황은 [docs/FEATURES.md](docs/FEATURES.md)

## 빌드

```bash
./gradlew :app:assembleDebug
```

JDK 21이 필요하다 (Gradle 데몬 툴체인으로 지정됨). Android SDK 경로는 `local.properties`의 `sdk.dir`.

릴리즈 서명은 저장소 밖 `~/.civilight-signing/signing.properties` (또는 `CIVILIGHT_SIGNING` 환경 변수가 가리키는 파일)에서 읽는다. 없으면 서명 없는 릴리즈가 만들어진다.
