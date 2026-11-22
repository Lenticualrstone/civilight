# Release APK smoke test (emulator, 2026-11-23)

No crash, no FATAL in logcat. Screens reached: onboarding, 새 프로젝트, project home, 스크립트, 스토리, 인물, 세계, 연표, 지도, 캔버스, 설정, 휴지통, 레코더 main.

Not reachable: 페이지 (no entry point from project home, settings or library; ⋯ menu shows "페이지 0"), 점검 센터 (no entry point; the 용어 tab hint "공개 시점 전 사용 → 점검 센터" is not tappable).

| # | Area | Problem | Severity |
| --- | --- | --- | --- |
| 1 | library | 휴지통 empty state shows `TODO_COPY_LIB38_EMPTY` | copy, visible |
| 2 | canvas | node sheet button `TODO_COPY_CANVAS_REMOVE_NODE` | copy, visible |
| 3 | canvas | 선 라벨 sheet button `TODO_COPY_CANVAS_REMOVE_LINE` | copy, visible |
| 4 | canvas, library | content descriptions `TODO_COPY_CANVAS_ZOOM_OUT`, `TODO_COPY_LIB02_MORE_LABEL` | copy, a11y |
| 5 | script | Enter after "Line one." made a new block containing "Line one.\nLine two." while the old block kept "Line one." (adb input; verify with a unit test of the split-at-caret path and the IME composing path) | major |
| 6 | script | scene header shows "씬 1 · 장소 없음 · 때 없음" without the scene title that 스토리 and home show ("씬 1 · The Gate"); episode row "미정" and "—" | minor |
| 7 | cast, map, library | grids stop at ~90 % width (map style picker leaves ~214 px), library card view uses only the left half | minor layout |
| 8 | world/map | empty 지도 list: heading "지도 · 0" twice, empty card edge to edge without side margin | minor |
| 9 | map | map view opens at 296 % zoom; bottom bar "새 지도 / 되돌리기" is light in dark mode | minor |
| 10 | canvas | new nodes placed lower-left; view shifts when a sheet opens; new memo overlaps an existing node | minor |
| 11 | shell | command bar 명령 chip with empty query lists nothing | minor |
| 12 | library | example project shows "예시 프로젝트 삭제됨" after onboarding (check LIB-17 intent) | check |
| 13 | various | "Ⅰ" glyph renders as a bar; 인물 profile tab bar faint overlapped label behind "말투"; 스토리 selected-row highlight past margins; big gap between script top bar and first scene | minor |
