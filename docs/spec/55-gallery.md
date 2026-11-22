# 55 · 갤러리 (image galleries of characters and projects) — feature prefix `GAL`

Implementation spec for the gallery: many images per character (`인물`) and per project, browsed as a list, a grid or a snapping card row, opened full screen, and each carrying its own text (title, description, tags, a link to a scene). Engineers implement from this file together with `50-canvas-pages.md` (CNV) for every image part it reuses. Every Korean string in `code` is UI copy and must be used verbatim.

**Sources (2026-11-23).** User requests of 2026-11-23: a gallery on character cards and projects, viewed as a list and as a scrolling card display, fused with the existing image and card features; then "each card's description text is shown when the card is picked". Existing specs: `50-canvas-pages.md` CNV-21…31 and CNV-45 (card view, images, holo, motion), `30-cast.md` CST-04, CST-07, CST-08, CST-13, CST-45, `20-story-time.md` STO-01, `60-library-security-files.md` LIB-36, LIB-38, LIB-40, LIB-47, LIB-50, `00-domain-model.md` §2.10, §5, §7, §8, `docs/status/DECISIONS.md` §1, §2. Memory rules: terse copy, no guessed interactions and no hidden gestures that change things, one-word vocabulary (`고르기`, `지우기`, `되살리기`, `바꾸기`, `고치기`, `씬`). Boards: canvas page `갤러리` (generator `design/night-manuscript-r1/gen/scr_gallery.py`). Review round 2026-11-23 (rules and implementation reviews) is folded in.

**Conventions.** As in `50-canvas-pages.md`: `[PROPOSED]` = behaviour chosen here because no source states it; each is listed in §6 with its default. "cover" = 390 × 844 boards (Compact width), "inner" = 800 × 800 boards (Medium/Expanded). Board names drop the `NmDark`/`NmLight` prefix; every board exists in dark and light. Separators in copy are ` · `. Counters are unspaced like the rest of the app (`3/8`, `7/10`). Type roles are the design system's (Pretendard everywhere in the app, domain model C-30; boards still draw the older board fonts). Colour and size tokens are the DS tokens (`bg raise ink ink2 ink3 acc pri onpri sel line edge tint glass shade`, `hit-min 44`, `card-w 222`).

**Words.** `갤러리` = this feature (the user's word). `고른 카드` = the card the user tapped; its text is shown. `설명 판` = the panel that shows the picked card's text. `보기` = full-screen viewer. The CNV-27 system-picker button `갤러리` is shown as `사진` inside the gallery flow to avoid two meanings of one word (§6 Q1). In code the blob origin stays `BlobSource.GALLERY` (serial name `gallery`, file format); it means "system photo picker" and is shown as `사진` (§3.1).

---

## 1. Scope

### 1.1 What this area owns

1. **Gallery items**: an ordered set of images per owner (a character or the project), each with title, description, tags, an optional scene link, holo and hidden flags (GAL-13, §3).
2. **Entry points**: the gallery section in the character profile, the count chip on character cards, the project home shortcut (GAL-01…03).
3. **Gallery screen**: three views `목록` · `격자` · `카드`, picking a card and showing its text, the full-screen viewer, the project gallery that groups every owner (GAL-04…10).
4. **Adding and editing**: multi-add through the existing picker, crop, the text sheet, main-image actions, order, reuse, holo, several at once (GAL-11…18).
5. **Protection, deletion, files**: hiding and rating, trash and restore, storage, export (GAL-19…22).
6. **Layout, motion, states**: inner layouts, motion, empty and large galleries (GAL-23…25).

### 1.2 Hand-offs (reused, not redefined here)

| Area | Reused part | Used by |
| --- | --- | --- |
| CNV | `ViewToggle` (CNV-21), snapping rows `SnapCardRow` (CNV-22), `ProjectCard` / `ImageTile` anatomy (CNV-23, DS-51/52), no-image tile (CNV-24), slot registry (CNV-25), presets (CNV-26), picker `ImagePickSheet` (CNV-27), crop `ImageCrop` (CNV-28), storage by hash (CNV-29), holo (CNV-30), motion tokens and card motions (CNV-37, CNV-45), reduce motion (CNV-46), inner layouts (CNV-33) | GAL-04…25 |
| CST | Profile layout and tabs (CST-07, CST-08; tabs stay five), card view rows (CST-04), portrait and background slots (CST-13), display-name rule (CST-11), character hiding, deletion and merge (CST-39…45), tag editor | GAL-01, 02, 13, 14, 19, 20 |
| STO | Project home `작업` shortcuts (STO-01), scene codes and 때 short form (`1-7 씬 3 · 2일째 새벽`, STO-17…22), command bar (STO-36), search highlight (whole sentence, boxed match) | GAL-03, 13, 04 |
| LIB | Project cover slot and `표지로` target (LIB-04), item hiding contract (LIB-36), unlock (LIB-32), trash host (LIB-38), toast host (LIB-40), `.civilight` export (LIB-47), format versions (LIB-50, LIB-56), encryption default (LIB-51), capture blocking and name masking (LIB-26, LIB-27), project size (LIB-62) | GAL-14, 19…22 |
| 00 domain model | `Blob`, `ImageUse`, `Tag`, liveness, trash, merge, package layout, versioning; addendum §12 adds `GalleryItem` | §3 |

Changes other specs need (not edited here; the owners apply them): STO-01 `갤러리` shortcut · CST-04 `갤러리 N` chip · CNV-21 three-cell toggle · CNV-25 `갤러리` row in the slot table · CNV-27 `갤러리` button → `사진` (Q1) · LIB-36 images in the hideable list · LIB-38 trash kind `galleryItem` · LIB-47 size line on the export sheet (Q13) · LIB-50 feature id `gallery` (format 1.0; the gallery ships in 0.1.0, so there is no format 1.1 and no upgrade step).

### 1.3 needsExternal

No GAL feature is needsExternal. The system photo picker (`PickMultipleVisualMedia`), the document picker and the rotation sensor are ordinary app APIs.

---

## 2. Features

### A. Entry points

#### GAL-01 · 인물 개요의 갤러리 칸 (gallery section in the profile) — P1 · needsExternal: no
Board `GalProfile`.
- The profile keeps its five fixed tabs (CST-08). On `개요`, a section sits right after the field table and before the first long-text section (`Ⅰ 개요`).
- Section header (`.sec` style): left `갤러리 · <N>` (`갤러리 · 8`; N = live, visible items; hidden ones are not counted while locked). Right, two text buttons: `넣기` with a plus icon (`ink2`, opens GAL-11 for this character) and `전체 보기 ›` (`acc`, opens the gallery screen GAL-04).
- Below: one snapping row (CNV-22) of small gallery cards 132 × 176 (3:4), pitch 146, starting at the content inset; the third card peeks. Cards use the gallery card anatomy (GAL-07) without the `⋯` button. Order = the gallery order (GAL-15). At most 12 cards in this row `[PROPOSED]`; the rest is reached with `전체 보기 ›`.
- Tapping a card opens the gallery screen **in its remembered view** (GAL-04) with that item picked (GAL-08): `카드` scrolls the row so the card is at the row start and opens the panel under it; `격자` scrolls the tile into view and opens the panel under its grid row; `목록` scrolls to the row and expands it.
- No items: the section shows one outlined card 132 × 176 (1.5 dp `edge` outline, `+` and `이미지 넣기`, same look as the empty gallery, GAL-25) instead of the row; the header reads `갤러리 · 0` and has no `전체 보기 ›`. Dashed outlines stay reserved for things attached to something deleted.
- Inner: the same section in the record pane (409) or the 560 column; card size unchanged.

*Sources:* user request; CST-08 (tabs fixed); CNV-22, CNV-23; rules review 5, 16.

#### GAL-02 · 인물 카드의 갤러리 수 (count chip on character cards) — P2 · needsExternal: no
Board `GalCastCards`.
- On the 인물 card view (CST-04), a portrait card whose character has visible gallery items shows a chip at the bottom-right of the image: `갤러리 <N>` (same style as the `홀로` chip: height 24, radius 12, light text on `rgba(10,11,14,.5)` with blur 12). Hit area 44.
- Tapping the chip opens that character's gallery (GAL-04); tapping elsewhere on the card still opens the profile.
- No chip when N = 0 or when the character is hidden and locked (the card is not drawn then, LIB-36).

*Sources:* user request ("캐릭터 카드"); CST-04; CNV-23 chip.

#### GAL-03 · 프로젝트 갤러리 들어가는 곳 (project entry) — P1 · needsExternal: no
Board `GalHome`.
- Project home `작업` shortcuts (STO-01) become `스토리 · 인물 · 세계 · 연표 · 캔버스 · 갤러리 · 설정`, same three-column grid, same look as the other shortcuts (no count; `설정` keeps its own row and `ink3`).
- Tap → the project gallery (GAL-10), view remembered (GAL-04).
- Command bar: `갤러리` is also a command target (`/갤러리`, STO-36) `[PROPOSED]`.

*Sources:* STO-01; user request ("프로젝트에 갤러리 항목"); rules review 18.

### B. The gallery screen

#### GAL-04 · 갤러리 화면 틀 (header, views, filters, order line, search) — P1 · needsExternal: no
Boards `GalShelf`, `GalGrid`, `GalList`, `GalSearch`.
- **Header** (glass, 112): back; small line = owner name (`리엔`; project gallery: the project name); title `갤러리`; right: the view toggle and `+` (accessibility `이미지 넣기`, opens GAL-11).
- **View toggle**: the DS `ViewToggle` (CNV-21) with **three** cells: `목록` (three lines icon) · `격자` (three columns of tall 3:4 tiles, two rows) · `카드` (the existing 2 × 2 card icon). The `격자` glyph uses tall tiles so it does not read as the square `카드` glyph. Accessibility group `보기`, each cell labelled. Same height 36, radius 12, cells 44 wide. This is a new variant of an existing component (three cells instead of two); `카드` keeps its meaning and icon from 서재 and 인물.
- The view is remembered per screen in `AppSettings.viewByScreen` with keys `characterGallery` and `projectGallery` (not per owner, not in `.civilight`). Default `카드` `[PROPOSED]`, because the user asked for the card display.
- **Filter chips** (one choice, horizontal, scrolls when long): `전체 <N>` then one chip per tag used in this gallery with its count (`삽화 4` · `설정화 2` · `소품 1`). Project gallery adds `프로젝트 <N>` · `인물 <N>` after `전체` (GAL-10).
- **Order line** (under the chips), left to right:
  - `직접 순서` with a chevron (accessibility `정렬`): a small sheet with only the sort modes `직접 순서` · `넣은 날` · `씬 순서` (radio).
  - `여러 장 고르기` (text button, `ink2`; GAL-18).
  - Right slot: `카드` → the pager marks of CNV-22 (12 or fewer cards) or a mono readout `<first visible> · <N>` such as `41 · 128` (more than 12). `목록` and `격자` → mono `<N>장 · 숨김 <H>` (`8장 · 숨김 2`; `· 숨김 H` only while hidden items exist and are locked). Grouped rows (GAL-10): the pagers move to the group headers and this slot shows the hint (GAL-08).
  - `직접 순서` = the stored order (GAL-15). `넣은 날` = newest first. `씬 순서` = by linked scene in manuscript order; items whose scene is not live, or hidden while locked, count as unlinked; unlinked items last in stored order.
- **Search**: the command bar hint `갤러리에서 찾기 · 제목 · 설명 · 태그` (project: `모든 갤러리 · 제목 · 설명 · 태그`). Matches in title, description and tag names of visible items; results use the search-highlight rule (whole sentence shown, the match boxed with `mark.hit`), with a 44 thumbnail and the label `<owner> · 갤러리 · <title> · 설명` (or `제목`, `태그`). A result opens the gallery with that item picked. Hidden items while locked are not searched.

*Sources:* CNV-21, CNV-22; user request (list and card display); memory (search highlight); rules review 3, 11, 12, 14.

#### GAL-05 · 목록 (list view) — P1 · needsExternal: no
Board `GalList`.
- One row per item, separated by `line`: thumbnail 64 × 64 (the item's `목록 1:1` crop, radius 12, holo foil if on), then title (body title role 16/22 bold; untitled → `제목 없음` in `ink3`), the description clamped to 2 lines (13/19 `ink2`; none → `설명 없음` `ink3`), then a mono line with the scene code or the tags. Right: `⋯` (accessibility `메뉴`, GAL-08 menu).
- **Tap a row = pick it** (GAL-08): the row expands in place (only one at a time) on `sel` fill with a 2 dp `acc` bar on the left: full description (no clamp), meta line with the scene link and tag chips, position `<i>/<N>`, and the buttons `보기` · `고치기` · `⋯`. Tap the picked row again → it collapses (unpicked).
- Hidden and locked items: one row at the end, the LIB-36 `LockRow` (`숨긴 이미지 <H>` · `지문 · PIN으로 열림` · `지문으로 보기`).

*Sources:* CNV-21 list rows; LIB-36; user follow-up ("목록에서는 행에 글").

#### GAL-06 · 격자 (grid view) — P1 · needsExternal: no
Boards `GalGrid`, `GalUnlocked`.
- Three columns of 3:4 tiles (cover 110 × 147, gap 10; the picker's `ImageTile` look, radius 14), the item's `카드 3:4` crop. No text on tiles. A chip at the top-left shows `초상` · `배경` · `표지` · `홀로` when they apply (GAL-14, GAL-17), joined with ` · `.
- **Tap a tile = pick it**: the tile gets the selection ring (2 dp `bg` gap + 2 dp `acc`, like `ImageTile`, without the check badge, which stays reserved for choosing in pickers) and the **설명 판 opens right under the grid row that holds it**; the rows below move down. Description clamped to 2 lines + `더 보기`.
- Hidden items: one locked tile at the end, `tint` fill, `edge` outline, lock icon, `숨긴 이미지 <H>`, small `지문으로 보기` (the whole tile is the button). This tile is the only custom lock variant; rows use `LockRow`.

*Sources:* CNV-27 tile, DS-52; LIB-36; user follow-up; rules review 6, 8.

#### GAL-07 · 카드 (snapping card row, "카드 진열") — P1 · needsExternal: no
Boards `GalShelf`, `GalMany`.
- One horizontal row of gallery cards that snaps (CNV-22: starts at the content inset, next card peeks, snaps the nearest card's left edge, decelerate 320 ms, haptic `틱`, image parallax 8 %).
- **Gallery card** = `ProjectCard` anatomy (CNV-23), 3:4: cover 222 × 296 (pitch 236), inner 236 × 315 (pitch 252), radius 20, the item's `카드 3:4` crop. Title (title role 20/26, 800) and a meta line (scene code, else tags) on the bottom shade; an untitled item has no shade and no text. Top-left chip from `초상` · `배경` · `표지` · `홀로` joined with ` · ` (`초상 · 홀로`). Top-right `⋯` (44 hit, accessibility `메뉴`).
- **Tap a card = pick it** (GAL-08). The picked card lifts 10 dp with the selection ring and a soft shadow; the 설명 판 opens right under the row with an arrow pointing at the card.
- Text on a card face sits only on the dark shade, never on bare foil (CNV-30 contrast ≥ 4.5:1 holds as on project cards).

*Sources:* user request ("스크롤 메뉴 · 카드 진열 효과"); CNV-22, CNV-23, CNV-45.

#### GAL-08 · 고른 카드와 설명 판 (picking a card and showing its text) — P1 · needsExternal: no
The user's follow-up request. Boards `GalShelf` (long text), `GalShelfAway` (scrolled away, expanded), `GalMany` (nothing picked), `GalProject` (nothing picked, grouped), `GalGrid` (no text), `GalList` (row), `GalView`, `GalWide`, `GalShelfWide`.
- **Picking is only an explicit tap** on a card, tile or row (or arriving with an item named: profile card tap, search result, closing the viewer). Scrolling, flinging and snapping never pick and never change the picked item. Pager marks show the scroll position, not the pick.
- **State**: one picked item per gallery screen (UI state, kept across fold and process death, CNV-35; never exported). On first entry nothing is picked `[PROPOSED]`; returning to the same gallery restores the last pick.
- **Hint while nothing is picked** (only in `카드`, where tapping is the only way to see text): a single row shows `카드 누르기 · 설명 보기` (small, `ink3`, centred) in the place where the panel opens, right under the row; grouped rows (GAL-10) show it in the order-line slot. `목록` and `격자` show no hint (rows carry their text; the slot keeps the count). Inner: GAL-23.
- **Tap the picked card again** → unpicks it and closes the panel. Tap another card → the pick moves; the panel content changes in place and its arrow moves to the new card.
- **Panel content** (`raise` fill, radius 20, 1 dp `line`, padding 14 × 16, width = content width):
  1. Title (title role 19/26, 800); untitled → `제목 없음` in `ink3`. Right: mono `<i>/<N>` (position among the visible items in the current order and filter).
  2. Meta: scene link in `acc` (`1-7 씬 3 · 2일째 새벽`; tap → opens the script at that scene's head), then tag chips (height 22). No link and no tags → `연결 없음` in `ink3`. A scene that is hidden while locked reads `숨긴 씬` (no tap); a trashed or purged scene reads dotted `지운 씬 · <code>` (dotted underline, no tap; the code is the last known one, from the `NameTag` after a purge).
  3. Description (body role 15/24, `ink2`; `@` links in `acc`). Clamped: `카드` 3 lines, `격자` 2 lines; when clamped, a text button `더 보기` (`acc`) expands the panel in place to the full text and turns into `접기`. No description → `설명 없음` (`ink3`).
  4. Buttons: `보기` (secondary, expand icon; GAL-09) · `고치기` (secondary; GAL-13) · `⋯` (menu). When the item has neither title nor description, the first button is the primary `설명 쓰기` (opens GAL-13 with the cursor in `설명`), then `보기` · `⋯`.
- **Picked card off-screen** (board `GalShelfAway`; the user scrolled away): the panel stays where it is and its title row gains a text button `고른 카드로` (back chevron, `acc`) that scrolls the row back so the picked card is at the row start; the position `<i>/<N>` moves to the meta line.
- **Filter change** that hides the picked item → it is unpicked and the panel closes. **Locking** while a hidden item is picked → it is unpicked.
- **Where the panel sits**: cover `카드` and `격자` → directly under the row that holds the picked item (multi-row screens included, GAL-10); `목록` → the row itself expands; inner → see GAL-23; `보기` → GAL-09.
- **Editing** never happens in the panel; `고치기` opens the text sheet (GAL-13) and the panel shows the saved text when it closes.
- `⋯` on a card that is not picked opens the menu for that card and picks it.

*Sources:* user follow-up 2026-11-23; memory (no guessing, explicit controls); CNV-22; rules review 3.

#### GAL-09 · 보기 (full-screen viewer) — P1 · needsExternal: no
Boards `GalView`, `GalViewOff`, `GalViewWide`.
- Opened from `보기` in the panel or the menu (GAL-14 menu, top buttons). Background `bg`. Top: `닫기` icon button (left), centre small mono `<owner> · 갤러리` over mono `<i>/<N>`, `⋯` (right).
- The image is shown **whole** (the original, fitted, not the card crop; a preset at its registry size), with holo when on. Pinch zooms and two-finger drag pans while zoomed (viewing only; nothing changes).
- **Buttons** at the bottom: `이전` · `설명` · `다음` (secondary, primary when on, secondary). `이전` · `다음` move through the visible items in the current order and filter; at the ends the button is drawn at 38 % opacity and does nothing (board `GalViewOff`: last item). No swipe paging `[PROPOSED]` (§6 Q6).
- **`설명` toggle** (accessibility pressed state): on → a `raise` panel slides up from the bottom (cover: from y 536 of 844) with title, `고치기` (text button), meta line and the **full description** in a scrolling area (fade at the bottom edge, scroll mark); the image area shrinks so the image stays **above** the panel and no text is ever drawn over the image or its foil. Off (board `GalViewOff`) → the image uses the full height above the buttons. The choice is remembered app-wide (`AppSettings.galleryTextOn`, default on `[PROPOSED]`).
- Closing returns to the gallery with the last viewed item picked.
- Inner (`GalViewWide`): image left (fit in 500 px), buttons under it, the 설명 area as a fixed right column 300 wide with title, meta, full description, `원본 <W> × <H> · <size>` (preset: `<W> × <H> · 기본 이미지`), `넣은 날 <MM-DD>`, and `고치기`. `설명` off hides the column and centres the image.

*Sources:* user request; CNV-30 (never behind text); memory (explicit prev/next buttons).

#### GAL-10 · 프로젝트 갤러리 (project gallery and grouping) — P1 · needsExternal: no
Board `GalProject`.
- Same screen as GAL-04 with the project as owner. Chips: `전체 <N>` · `프로젝트 <N>` · `인물 <N>` · tag chips.
- `프로젝트` = items owned by the project. `인물` = every visible character's items. `전체` = both.
- With `전체` or `인물`, items are grouped by owner: project first, then characters in the cast order. `카드`: one snapping row per group, header `<owner> · <N>` with pager or readout; project row cards 162 × 216, character rows 132 × 176 `[PROPOSED]`; a character header has a right text button `<name> 갤러리 ›` that opens that character's gallery. `목록` and `격자`: sections with the same headers.
- Picking works as GAL-08; the panel opens under the row of the picked card.
- Characters with no items get no group. Hidden characters (locked) collapse into one `LockRow` after the groups: `숨긴 인물 <H>` · `지문 · PIN으로 열림` · `지문으로 보기`. No image count is shown for them.
- Adding from the project gallery (`+`) adds to the project's own items; adding to a character happens in that character's gallery.

*Sources:* user request; CST-04 rows per group; LIB-36; reviews (hiding leaks).

### C. Adding and editing

#### GAL-11 · 이미지 넣기 (add several images) — P1 · needsExternal: no
Board `GalAdd`.
- The CNV-27 picker sheet (top 118) in a several-at-once mode: title `갤러리에 넣기` + `닫기`; subtitle `<owner> · 여러 장 · 고른 순서대로`; tabs `기본 이미지 8` · `내 이미지 <N>`.
- Tapping a tile toggles it; chosen tiles show the selection ring and a numbered badge (`1`, `2`, `3`) = the order in which they will be added. Under each tile: mono size of the original (`4.2 MB`; presets: the preset name). A tile whose source this gallery already holds in a **visible** item is dimmed and cannot be chosen: `이미 있음` (live) or `휴지통에 있음` (trashed; restore it from the trash instead).
- Note line `원본 그대로 · 같은 원본은 한 번만 저장`.
- Buttons: `사진` (system photo picker, several allowed) · `파일` (document picker, several allowed) · `넣기 · <k>` (primary; disabled at 0).
- `넣기` appends the chosen images at the end of the gallery in badge order; images from `사진` · `파일` are imported (CNV-29) and appended in the order the system returns them. **No crop step** on add: the card and list crops start as the centred largest frame (CNV-28 zoom 1.0) until the user crops (GAL-12). Holo starts as `홀로그램 효과 기본` (CNV-19).
- A chosen or imported image whose source the gallery already holds (live or trashed, §3.1) is skipped. The toast reads `<k>장 넣음` (k = added) and, when visible duplicates were skipped, `· 이미 있음 <m>`. Duplicates of hidden items while locked are skipped without being counted or named (GAL-19 principle).
- The whole add is **one undo step** (`되돌리기` removes every item it added and restores the previous pick).
- After adding: the sheet closes, the gallery scrolls to the first new item and picks it.
- `내 이미지` = images imported in this project (Q-IM-03 of CNV decides the scope; the gallery follows it). While locked it leaves out blobs that only hidden items, hidden owners or hidden projects use (GAL-19).

*Sources:* CNV-27, CNV-29; user request (reuse existing); reviews (hidden leak, uniqueness on restore).

#### GAL-12 · 자르기 (crop) — P1 · needsExternal: no
- Board `GalCrop`. The CNV-28 crop editor with the gallery slot (new `SlotSpec GALLERY`): ratio chips `갤러리 카드 3:4` · `목록 1:1`, `규격 고정`, holo row allowed. Header small line `<owner> · 갤러리` over `자르기`.
- Crops only affect the card face, tiles and list thumbnails; `보기` always shows the whole original.
- Entry: `⋯ › 자르기` (GAL-08 menu).
- Presets: cropping follows CNV Q-IM-03. Until it is decided `[PROPOSED]`, a preset item's crops are stored in the preset's own pixels (registry width and height), like an `ImageUse` with a preset source; if CNV decides presets cannot be cropped, the menu row reads `자르기` · `기본 이미지 · 자르기 없음` and is disabled (§6 Q14).
- The image-slot rules screen (`ImageSlots`) gains a row `갤러리` · `갤러리 카드 · 목록 썸네일 · 보기는 원본` · ratios 3:4, 1:1 · `홀로 선택` (hand-off to CNV-25).

*Sources:* CNV-25, CNV-28; implementation review 13.

#### GAL-13 · 이미지 정보 (title, description, tags, scene link) — P1 · needsExternal: no
Board `GalInfo`.
- Sheet (cover, top 118; inner: side panel) titled `이미지 정보` + `닫기`. Opened by `고치기` (panel, menu, viewer). Saves as you type (one undo step per field edit), like profile fields.
- Head: thumbnail 52, mono `<W> × <H> · <size> · <source>` (`3024 × 4032 · 4.2 MB · 사진`; source words `사진` · `파일`; a preset reads `<W> × <H> · 기본 이미지`, no size), small `<owner> · 갤러리 <i>/<N> · 원본 그대로`.
- Fields:
  - `제목`: one line, optional, no length limit shown; long titles end with an ellipsis on cards.
  - `설명`: multi-line rich text with `@` links (right hint `@ 인물 · 용어 · 장소 잇기`); the shared run contract (SCR / domain model C-05). Not game output: no term candidates or `▲` (rule 21 §4).
  - `태그`: chips of the project's tag list (domain `Tag`); tap toggles; `+ 태그` creates a project tag through the one write path `CharacterEdits.addTag` (domain:cast). The tag editor counts gallery use too (§3.4).
  - `연결`: one optional scene. Box shows `씬` · `<scene code> · <때 short form>` (`1-7 씬 3 · 2일째 새벽`) with `빼기` and `바꾸기`; empty → `씬 고르기` `[PROPOSED]` opening the existing scene picker (story structure picker, search by code and title). A non-live or hidden scene shows as in GAL-08.
- Empty states on the card face and panel follow GAL-08.

*Sources:* user follow-up ("이미지 설명 등의 문구"); CST-08 (`@` links in long text); rule 21 §4; STO time format.

#### GAL-14 · 메뉴 · 초상으로 · 배경으로 · 표지로 (menu and use as the main image) — P1 · needsExternal: no
Board `GalMore`.
- **Menu** (`⋯` on a card, tile, row, panel or viewer; sheet, cover top 104; inner popover): title = item title (or `제목 없음`) + `닫기`, small `<owner> · 갤러리 <i>/<N>`. First a button row `보기` · `고치기`, then rows: `초상으로` / `표지로` · `배경으로` (character only) · `자르기` · `다른 곳에도 넣기` · `순서` (GAL-15) · `순서 바꾸기` (`전체 순서 · 옮기기`, chevron) · `홀로그램 효과` · `숨기기` · `지우기`.
- Character items: `초상으로` (`인물 카드 · 아바타 · 카드 4:5 · 아바타 1:1`) and `배경으로` (`프로필 위 · 커버 9:16 · 펼친 화면 4:3`). Project items: `표지로` (`서재 카드 · 목록 썸네일 · 카드 3:4 · 목록 1:1`).
- The action writes the existing `ImageUse` of that slot with this item's source (same blob, no copy) and opens the crop editor for that slot's ratios (CNV-28). `취소` in the crop editor cancels the whole action.
- **Hidden item** (only reachable while unlocked): the row's sub line reads `숨긴 이미지 · 초상에서는 보임` (`배경에서는 보임` · `표지에서는 보임`) and the crop editor shows the same fragment above its buttons; applying it makes the image visible in that slot (the item itself stays hidden).
- **No stored "main" flag.** An item shows the chip `초상` · `배경` · `표지` (and the menu row shows `지금 초상` instead of the chevron) when the slot's `ImageUse.source` equals the item's source. Changing the portrait elsewhere moves the chip by itself.
- `배경으로` on a holo item: the background slot never has holo (CNV-25), so holo is off for that use only.

*Sources:* CST-13, CNV-25, LIB-04; user request (fuse with existing); reviews (menu rows, hidden reuse).

#### GAL-15 · 순서 (order) — P1 · needsExternal: no
Boards `GalMore` (row `순서`), `GalOrder`, `GalOrderMove`.
- Stored order = `GalleryItem.order` (OrderKey, per owner). Positions `<i>/<N>` count the **visible** items (hidden ones while locked are skipped), the same set everywhere.
- **Moves only in `직접 순서` with `전체` and no filter.** Otherwise the order controls are disabled and the menu row reads `<i>/<N> · 넣은 날` · `씬 순서` or `전체에서만`.
- Menu row `순서` · `<i>/<N> · 직접 순서` with buttons `맨 앞` · `앞으로` · `뒤로`. `앞으로` places the item directly before the previous **visible** item (`뒤로`: after the next visible one), so every tap changes what the user sees; hidden items keep their keys. One step per tap; each is one undo step.
- Screen `순서 바꾸기` (from the menu row `순서 바꾸기 ›`): header back, small `<owner> · 갤러리`, title `순서 바꾸기`; no `완료` (every change saves at once; back leaves). Section `직접 순서 · <N>`. Rows (64 high): grip (44 hit, accessibility `끌어서 옮기기`; drag works only on this screen), thumbnail 44, title (or `제목 없음`), position number, buttons `앞으로` · `뒤로` (44 hit, disabled at the ends). Tapping a row picks it (`sel`); the bottom bar shows `고른 이미지 · <title> · <i>/<N>` with `맨 앞` · `옮기기` · `맨 뒤`.
- **`옮기기`** (board `GalOrderMove`; works for 100+ items without dragging): a bar under the header shows the moving item: thumbnail 32, `옮기는 중 · <title>` over mono `<i>/<N>`, and `취소`; a row `맨 앞으로` sits above the first row; grips are hidden, and every row's buttons are replaced by one text button `이 뒤로` (the moving row reads `옮기는 중`). Tapping `이 뒤로` places the item right after that row, ends the mode and is one undo step. Scrolling and search narrow the target; nothing moves until a target is tapped.

*Sources:* memory (explicit controls, 100+ items); domain model §1.2; reviews (100+ order, hidden neighbours, filter).

#### GAL-16 · 다른 곳에도 넣기 (same original in several galleries) — P2 · needsExternal: no
- Menu row `다른 곳에도 넣기` · `원본 하나 · 프로젝트 · 다른 인물` opens an owner picker (project + characters, the character picker pattern with search and tags) and creates a new item in the chosen gallery, appended at the end, that **copies every field** except `key`, `ownerKind`, `ownerKey`, `order` and bookkeeping: `source`, `title`, `caption`, `tagKeys`, `sceneKey`, `crops`, `holo`, `hidden`. The original is stored once (CNV-29).
- Owners that already hold that source in a visible item (live or trashed) show `이미 있음` and cannot be chosen. An owner that holds it only in a hidden item while locked is not marked; choosing it adds nothing and the toast reads `0장 넣음` (GAL-19 principle).

*Sources:* user request; CNV-29, T-CNV-35; reviews (hidden flag dropped).

#### GAL-17 · 홀로그램 효과 (holo per item) — P1 · needsExternal: no
- Menu switch `홀로그램 효과` · `은빛 · 기울이면 흐름` (CNV-30 look, input, sweep, sensors off off-screen, reduce motion static). Default for new items = `홀로그램 효과 기본`.
- Drawn on card faces, tiles, list thumbnails and the viewer image; chip `홀로`. Never under text: the viewer panel sits outside the image (GAL-09); card face text sits on the shade.

*Sources:* CNV-30.

#### GAL-18 · 여러 장 고르기 (act on several) — P2 · needsExternal: no
- Entry: the text button `여러 장 고르기` in the order line (GAL-04), visible in every view. It turns tiles, cards and rows into checkboxes (count `<k>장 고름`, `닫기`); bottom actions `태그` · `숨기기` · `지우기` · `다른 곳에도 넣기`. Each action is one undo step. Leaving the mode does not change the picked item.

*Sources:* memory (scale to 100+); rules review 12.

### D. Protection, deletion, files

#### GAL-19 · 숨기기 · 등급 (hiding and rating) — P1 · needsExternal: no
Boards `GalGrid` (locked tile), `GalUnlocked`.
- **Item hiding** `[PROPOSED]` (extends LIB-36's list to gallery images, §6 Q3): menu switch `숨기기` · `지문 · PIN으로 열림`. Unhiding needs authentication (LIB-32).
- **Principle.** While locked, a hidden item is absent from every visible surface, yet it still holds its source for the uniqueness rule (§3.1). Anything that would surface it is skipped without naming it. From this:
  - It collapses into one locked tile, row or card at the end (`숨긴 이미지 <H>` · `지문으로 보기`) and is left out of counts (shown as `숨김 <H>` in the order line only), positions, search, the profile section, the cast chip and the picker's `내 이미지`.
  - Its source never marks a tile `이미 있음`; adding or copying that source is skipped and only the added count is reported (GAL-11, GAL-16).
  - It is never offered as a main image; `초상으로` and the others need the unlocked menu and say what becomes visible (GAL-14).
  - A hidden scene linked from a visible item reads `숨긴 씬`; a hidden character's group is one `LockRow` without an image count (GAL-10).
- **Unlocked** (board `GalUnlocked`): hidden items stay in their stored order with a chip `숨김` (lock icon) on the tile, card or row; counts include them (`전체 10`, positions `<i>/10`); a bar under the order line reads `숨긴 이미지 2 · 열림 · 앱을 떠나면 잠김` with `다시 잠그기` (as `LrUnlocked`). Relocking unpicks a hidden item and closes its panel.
- **Hidden character** (locked): its gallery is not reachable; in the project gallery it is the `LockRow` of GAL-10.
- **Hidden project**: everything is behind the project lock; no gallery image is used for `preview.webp` beyond the cover rules of LIB.
- **Rating**: items take the project rating; no per-image rating. R-18 or capture-blocked projects apply `화면 캡처 가리기` (LIB-26) to every gallery screen, including the viewer and the picker; `이름 가리기` (LIB-27) applies to owner names in headers, group headers, menus and trash rows. Nothing else changes on screen (not drawn).

*Sources:* LIB-26, LIB-27, LIB-36; rule 19 §6; reviews (picker leak, reuse leak, unlocked state).

#### GAL-20 · 지우기 · 되살리기 (delete and restore) — P1 · needsExternal: no
Board `GalList` (toast).
- Menu row `지우기` · `휴지통 · 30일 뒤 삭제`. No confirm sheet (an item has no references). The item moves to the trash and the toast shows `이미지 지움 · <title>` (untitled: `이미지 지움`) with `되돌리기` (LIB-40).
- Trash entry kind `galleryItem` (§3; filter `전체`, and `인물` when the owner is a character `[PROPOSED]`). Row title = item title or `제목 없음`, sub `<owner> · 갤러리`; a hidden item reads `숨긴 이미지` and needs authentication to restore. `되살리기` puts it back at its stored order position.
- **Owner deleted** (a character): its items become non-live with it; the delete impact sheet counts them as `이미지 <n>` (rule 20); restoring or undoing the character brings them back (domain model §5.1). **Owner purged**: its items are purged with it.
- **Owner merged** (CST-45, `MergeOps`): the source's items move to the target, appended after the target's items in the source's order; an item whose source the target already holds (live or trashed) stays with the merged character and returns with it only as part of `mergedOriginal`. `MergeRecord.movedRefs` lists every moved item (`{ ownerKind: galleryItem, ownerKey: <item key>, field: ownerKey }`); undo moves them back.
- The blob stays while any live or trashed row references it (§3.4, one reference function); deleting an item that is also the portrait source does not touch the portrait (separate `ImageUse`).

*Sources:* LIB-38, LIB-40; rule 20; vocabulary (`지우기` = trash); implementation review 2, 4.

#### GAL-21 · 저장과 용량 (storage and size) — P1 · needsExternal: no
- Originals are stored once by content hash and kept as they are (CNV-29); no resizing on import, no size cap `[PROPOSED]` (§6 Q5).
- Card, tile and list images are cached bitmaps from the crops (never exported). Large images are decoded subsampled; full decode above 4096 px long edge is not allowed (WLD-38). The viewer decodes to the screen size and tiles when zoomed.
- Sizes are shown where they help: picker tiles (`4.2 MB`), the info sheet, the inner viewer column. The project size on the device (LIB-62) includes gallery originals. A total for the export sheet and backups is a hand-off (§6 Q13): `galleryBytes(doc)` = sum of distinct blobs referenced by live gallery items.
- Presets take no storage and show no size.

*Sources:* CNV-29; WLD-38; LIB-62; implementation review 14.

#### GAL-22 · 내보내기 (export) — P1 · needsExternal: no
- `.civilight`: always contains every live gallery item (hidden ones too) in `gallery/<ownerKey>.json` and every original they reference in `gallery/files/<hash>.<ext>` (§3.3). Optional feature id `gallery`; part of **format 1.0** (correction 2026-11-23: 0.1.0 is not released yet and ships with the gallery, so there is no format 1.1 and no upgrade step).
- **Reader tolerance** (§6 Q16): a file without any `gallery/` entry opens with empty galleries; the reader finds `gallery/<ownerKey>.json` and `gallery/files/*` **by path**, whether or not the manifest lists `gallery`; gallery originals join the one blob store (the same bytes under `assets/` are stored once). Portraits and covers stay `ImageUse` rows in `assets/`.
- Encryption: the project counts as having hidden items when any gallery item is hidden, so the encryption switch defaults to on (rule 19 §5, LIB-51); the user can turn it off, and then gallery images are in the plain package.
- `휴지통도 포함`: trashed items go to `trash/gallery/<ownerKey>.json`.
- PDF, CSV and game-engine JSON carry no gallery content `[PROPOSED]` (§6 Q7).

*Sources:* LIB-47, LIB-50, LIB-51; domain model §7, §8; implementation review 6, 15.

### E. Layout, motion, states

#### GAL-23 · 펼친 화면 (inner layouts) — P1 · needsExternal: no
Boards `GalWide`, `GalShelfWide`, `GalViewWide`.
- **`목록` and `격자`** follow CNV-33 layout A `목록 + 상세`: left pane 390 = the cover screen (no inline panel); right pane 409 = the picked item: small mono `고른 이미지 · <i>/<N>`, title (21/28), `⋯`; the card crop at 261 × 348 with holo and chip; meta line; full description; `보기` · `고치기`. Nothing picked → the right pane shows the first item's image dimmed with `이미지 누르기 · 설명 보기` `[PROPOSED]` (§6 Q8). Layout B `가운데 한 칸`: the cover layout at 560 width with the inline panel.
- **`카드`** on inner: full width (side inset 32), cards 236 × 315, pitch 252; the panel opens under the row in two columns (text left; `보기` · `고치기` · `더 보기` right, 168 wide), description clamped to 3 lines. Nothing picked → the hint of GAL-08 under the row.
- **`보기`**: GAL-09 inner.
- Fold/unfold keeps the view, the pick and the scroll position (CNV-34, CNV-35).

*Sources:* CNV-33…35.

#### GAL-24 · 모션 (motion) — P1 · needsExternal: no
Tokens from CNV-37: standard (0.2, 0, 0, 1) 200 ms; enter (0.05, 0.7, 0.1, 1) 320 ms; exit (0.3, 0, 0.8, 0.15) 160 ms; enterLong 360 ms; reduced fade 120 ms.

| Motion | Spec | Reduce motion |
| --- | --- | --- |
| Pick a card | card lifts 10 dp + ring fades in, standard 200 | ring only, fade 120 |
| Panel opens under a row | height from 0 + fade, enter 320; rows below move with it; the hint fades out | fade 120, rows jump |
| Pick moves to another card | panel text crossfade (exit 160, then enter 200); arrow slides to the new card, standard 200 | fade 120 |
| `더 보기` · `접기` | height change, standard 200 | instant |
| Card → `보기` | the card's image grows into the viewer image (shared element), enterLong 360; panel slides up enter 320 | fade 120 |
| `이전` · `다음` | image slides 48 dp in the direction + fades, standard 200 | fade 120 |
| `설명` on · off | image resizes and panel slides, enter 320 · exit 160 | fade 120 |
| Switch view | `목록 ↔ 카드` thumbnail morph of CNV-45 (360); `격자` tiles morph the same way | fade 120 |
| Card row | CNV-22 snap 320, parallax 8 %, haptic `틱` | snap only |
| Add | new cards fade and rise 8 dp, enter 320, 40 ms stagger, max 6 | fade 120 |
| Delete | card collapses, exit 160; neighbours close the gap, standard 200 | fade 120 |
| `이 뒤로` (옮기기) | row slides to its new place, standard 200; list scrolls to keep it in view | instant |
| Holo on | CNV-30 sweep 480 | static |

*Sources:* CNV-37, CNV-45, CNV-46.

#### GAL-25 · 빈 갤러리 · 많은 이미지 (empty and large) — P1 · needsExternal: no
Boards `GalEmpty`, `GalMany`, `GalSearch`.
- **Empty**: header as usual; section `갤러리 · 0`; one outlined card 222 × 296 centred (1.5 dp `edge` outline, `+` icon, `이미지 넣기`; tap → GAL-11); small line `기본 이미지 8 · 사진 · 파일 · 원본 그대로`. If the character already has a portrait (or the project a cover), a row offers it: thumbnail, `초상 이미지도 넣기` (project: `표지 이미지도 넣기`), small `지금 초상 · <preset name or 사진> · 원본 하나`, button `넣기` (adds an item with the same source; nothing is copied).
- **Filter with no result** (the last item of a tag chip was deleted while that chip was on): the chip reads `<tag> 0`, the line `이 태그의 이미지 없음` `[PROPOSED]` with the button `전체 보기` (not drawn).
- **Many images** (board `GalMany`, 128 items): lists are lazy; `카드` shows the mono readout `<first visible> · <N>` instead of pager marks; order changes use buttons and `옮기기` (GAL-15); search and tag chips narrow the set; `여러 장 고르기` acts on several; nothing auto-picks.

*Sources:* CNV-24 (no-image look), memory (scale to 100+); rules review 9.

---

## 3. Data

Full definitions: `00-domain-model.md` §12 (addendum). Summary:

### 3.1 `GalleryItem` (new entity)

| Field | Type | Null | Meaning |
| --- | --- | --- | --- |
| key | ULID | no | |
| ownerKind | `project` · `character` | no | |
| ownerKey | Key | no | the project key or a character key |
| source | String | no | `preset:<id>` or a blob hash |
| order | OrderKey | no | `직접 순서` within the owner |
| title | String | no | `""` = untitled |
| caption | RichDoc | yes | description with `@` link runs |
| tagKeys | List<Key> | no | project `Tag`s |
| sceneKey | Key | yes | linked scene |
| crops | Map<Ratio, Crop> | no | only `r3x4`, `r1x1`; empty = centred largest frame; presets in registry pixels |
| holo | Boolean | no | |
| hidden | Boolean | no | GAL-19 |
| createdAt, updatedAt, updatedBy | | no | bookkeeping |
| deletedAt, trashEntryKey | | yes | trash (direct mark) |
| ext | Json | yes | |

Rules:
- **Uniqueness** `[PROPOSED]` (§6 Q15): at most one item per `(ownerKind, ownerKey, source)` among live **and trashed** items, so `되살리기` can never create a duplicate. Purge frees the source. Picker labels: `이미 있음` · `휴지통에 있음`.
- Live only while the owner is live. Main image is derived from `ImageUse` (GAL-14), never stored.
- `@` names in `caption` resolve at `최신` (§6 Q10); the scene link label and 때 are computed on read (base + interval, per episode).
- `BlobSource.GALLERY` (existing enum, serial `gallery`) means "system photo picker" and has nothing to do with this feature; the UI word for it is `사진`. No code rename.

### 3.2 App state (never exported)
`AppSettings.viewByScreen` keys `characterGallery`, `projectGallery` (`LIST` · `GRID` · `CARD`; `ViewMode.GRID` is new, the existing codec falls back to `LIST` for unknown values); `AppSettings.galleryTextOn` (Boolean, default true); saved UI state (process death, CNV-35) gains the picked item key, panel expanded flag and row scroll per gallery screen.

### 3.3 `.civilight` (format 1.0)

| Path | Content | When |
| --- | --- | --- |
| `gallery/<ownerKey>.json` | `{ ownerKind, ownerKey, items: GalleryItem[] }`, live items of one owner | when the owner has any; feature id `gallery` (optional) |
| `gallery/files/<hash>.<ext>` | every original referenced by a written gallery item (also written under `assets/` when another row uses it; the device stores it once) | with the items |
| `trash/gallery/<ownerKey>.json` | trashed items, same shape | `휴지통도 포함` |

- Manifest: `formatVersion: "1.0"`, `minReaderVersion: "1.0"` (unchanged), `features.optional` gains `gallery` when any gallery file is written. No upgrade step: the gallery is part of format 1.0 (0.1.0 ships with it). The format 1.0 regression file (LIB-56) gains G0 (request to the `app` owner).
- Every written blob keeps its metadata row in `assets/index.json › blobs`; the bytes of a gallery original sit under `gallery/files/`, and also under `assets/` only when a non-gallery row (portrait, cover …) uses the same blob.
- On read: `gallery/files/*` go into the one blob store (deduplicated by hash); items whose owner no longer exists in the file are dropped.

### 3.4 Write paths and one reference function

Gallery items join the existing delete, restore, purge and undo machinery; no per-feature copies.

| Piece (module) | Gallery change |
| --- | --- |
| `BlobReferences.counts` (core:store), `PurgeOps.referencedBlobs` (domain:trash), `PurgeActions.heldBlobs` (domain:actions) | all three call **one** function, `core:model` `BlobRefs`, which also counts `GalleryItem.source` hashes of live and trashed items (done 2026-11-23) |
| `DeleteOps`, `LiveRows`, `RestoreOps` (domain:trash) | item delete with trash entry `galleryItem`; live-row diff for restore reports; restore of an item and of an owner |
| `DocChange.revert` (domain:trash) | row-level undo needs a `revertList` branch for `galleryItems`, as it has for `imageUses` |
| `PurgeOps.removeImages` (domain:trash) | purging an owner also removes its gallery items |
| `Impact` (domain:trash) | new `OwnedKind.GALLERY_ITEM` → delete sheet row `이미지 <n>` |
| `TrashListing` (domain:trash) | rows of kind `galleryItem`, filters `전체` and `인물` |
| `MergeOps` (domain:trash) | moves gallery items per GAL-20 and records them in `movedRefs` |
| `DeletedItems` (domain:cast) | the character delete snapshot lists its gallery items |
| `CharacterActions` (domain:actions) | character delete, restore and purge carry the items through the pieces above |
| Tag counts (feature:cast tag settings, `CharacterEdits` in domain:cast) | `+ 태그` uses `CharacterEdits.addTag`; tag rows count `인물 <n> · 이미지 <m>`; removing a tag removes it from items' `tagKeys` in the same edit; rename needs nothing (keys) |
| `HideableKind` · `ItemSurfaces` (domain:library) | gallery image as a hideable kind and its surfaces (GAL-19) |
| `ExportRun` and the package reader and writer (domain:library) | §3.3 files, feature id, format 1.0 (no upgrade step) |
| `ImageOwners.myImages` (feature:canvas) | leaves out blobs only hidden items use while locked |

### 3.5 Module ownership (DECISIONS §2)

- **Lane `feature:canvas`** owns the gallery screen, viewer, add, crop, info sheet, menu, order screens and the pure seams of §4 (canvas has no domain module, so they live there with JVM tests).
- **`feature:cast`** draws the profile section (GAL-01) and the count chip (GAL-02); **`feature:story`** draws the home shortcut (GAL-03). Both only open the gallery route and use the shared parts below; neither depends on `feature:canvas`.
- **Requests to the orchestrator** (shared modules are frozen during lanes):
  - `core:model`: `GalleryItem`, `GalleryFile`, `ProjectDoc.gallery`, a `TrashKind` value `galleryItem`, `Features.GALLERY` in `OPTIONAL` (and so in `KNOWN`), `ViewMode.GRID`, `AppSettings.galleryTextOn`, the saved UI state fields (pick, panel expanded, row scroll).
  - `core:store`: file units `gallery/<ownerKey>.json` and the trash mirror; the one blob reference function.
  - `core:navigation`: route `Gallery(ownerKind, ownerKey, pickKey?)` and the gallery sheets (add, info, menu) registered by canvas.
  - `core:designsystem`: `ViewToggle` three-cell variant; `core:ui`: the shared `GalleryCard` and `SnapCardRow` (moved out of `feature:canvas`), so cast and story do not copy them (cast already copies canvas's `SlotImage`).
  - `domain:trash` (no lane), `domain:actions`, `domain:cast`, `domain:library`: the rows of §3.4.
  - DECISIONS §1 gains a row: gallery item add, edit, order, delete, restore, purge → `domain:actions` `GalleryActions` (called by `feature:canvas`), through `domain:trash` for trash work (done 2026-11-23).
  - The command-bar search index adds gallery titles, descriptions and tags.

---

## 4. Rules as test cases

Fixture **G0**: project 「잿빛 새벽의 등불」 (R-15); 리엔's gallery in stored order: 1 `결사에 들어온 날` (1-3 씬 2, tag 삽화), 2 `외출복 설정` (설정화), 3 `등불 앞의 리엔` (1-7 씬 3, 삽화, holo, same source as 리엔's portrait, description of 5 sentences that contains `손등의 그을음은 마지막까지 불을 지킨 흔적`), 4 untitled with no text, H1 (hidden), 5 `대정전의 밤` (1-4 씬 2, 삽화), 6 `등불 고리` (소품), 7 `하층 구역 계단` (1-6 씬 1, 삽화), 8 `겨울 옷 시안` (설정화), H2 (hidden). Visible numbering 1…8 skips H1 and H2. Suggested pure seams: `galleryRows(items, filter, sort, locked)`, `pickState(...)`, `panelModel(item, view)`, `mainChips(item, imageUses)`, `orderMove(...)`, `addPlan(gallery, chosen, locked)`, `galleryBytes(doc)`.

**T-GAL-01 · tap picks, scroll does not.** Given G0 in `카드`, nothing picked. When the row is flung to card 5. Then nothing is picked and the hint `카드 누르기 · 설명 보기` shows under the row. When card 3 is tapped. Then card 3 is picked and the panel reads `등불 앞의 리엔` · `3/8`. When the row is flung to card 7. Then card 3 stays picked and the panel shows `고른 카드로`.

**T-GAL-02 · tap again unpicks.** Given card 3 picked. When card 3 is tapped. Then nothing is picked and the panel closes. When card 5 is tapped instead. Then card 5 is picked and the panel shows `대정전의 밤` · `5/8`.

**T-GAL-03 · empty text.** Given item 4 picked. Then the panel shows `제목 없음`, `연결 없음`, `설명 없음` and the buttons `설명 쓰기` · `보기` · `⋯`.

**T-GAL-04 · clamp.** Given item 3 picked in `카드`. Then the description shows 3 lines and `더 보기`. When `더 보기` is tapped. Then the full text shows and the button reads `접기`. In `격자` the clamp is 2 lines; in `목록` the expanded row shows the full text.

**T-GAL-05 · panel under the row.** Given `격자` (3 columns) and item 4 tapped. Then the panel is inserted after row 2 (items 4–6) and row 3 moves down.

**T-GAL-06 · counts and hidden.** Given G0 locked. Then chips read `전체 8 · 삽화 4 · 설정화 2 · 소품 1`, the order line reads `8장 · 숨김 2` in `격자`, the grid ends with one tile `숨긴 이미지 2`, and the profile header reads `갤러리 · 8`.

**T-GAL-07 · main image is derived.** Given 리엔's portrait `ImageUse.source` = item 3's source. Then item 3 shows `초상 · 홀로` and its menu row `초상으로` reads `지금 초상`. When item 1 is used `초상으로` and the crop is applied. Then item 1 shows `초상`, item 3 shows `홀로` only, and no gallery row changed.

**T-GAL-08 · crop cancel.** When `배경으로` is chosen on item 3 and the crop editor is cancelled. Then the background `ImageUse` is unchanged.

**T-GAL-09 · add order and duplicates.** Given the picker with three tiles chosen in the order B, A, C, and item 3's source shown as `이미 있음`. When `넣기 · 3` is tapped. Then B, A, C are visible items 9, 10, 11 (numbered among visible items) at the end, the toast reads `3장 넣음`, the first new item is picked, and no second item with item 3's source exists.

**T-GAL-10 · same original stored once.** Given one photo added to 리엔's gallery and, through `다른 곳에도 넣기`, to the project gallery. When exported. Then the photo is one file `gallery/files/<hash>.<ext>` and the two `gallery/*.json` files hold one item each with the same `source`.

**T-GAL-11 · order buttons.** Given item 3. When `앞으로`. Then it is item 2 and the old item 2 is item 3. When `맨 앞`. Then it is item 1. Given item 1, `앞으로` is disabled. Each tap is one undo step.

**T-GAL-12 · sort does not reorder storage.** When `씬 순서` is chosen. Then the visible order is 1-3 씬 2, 1-4 씬 2, 1-6 씬 1, 1-7 씬 3, then unlinked items in stored order; stored `order` keys are unchanged and the menu order buttons are disabled.

**T-GAL-13 · viewer ends and text.** Given `보기` on item 8 (last). Then `다음` is disabled. When `설명` is turned off and the viewer is closed and reopened. Then `설명` is still off.

**T-GAL-14 · delete and restore.** When item 6 `등불 고리` is deleted. Then the toast reads `이미지 지움 · 등불 고리` with `되돌리기`, chips read `전체 7` and `소품` is gone. When `되돌리기`. Then it is back as item 6.

**T-GAL-15 · owner deleted.** When 리엔 is deleted. Then the impact sheet lists `이미지 10` (locked: `이미지 8 · 숨김 2`, the order-line form), her gallery items are not live (no own trash entries) and the blob of item 3 is still kept. When 리엔 is restored. Then all 10 items are back in order.

**T-GAL-16 · hidden character in the project gallery.** Given 하르트 hidden with 3 items, locked. Then the project gallery `전체` shows no 하르트 group and ends with the row `숨긴 인물 1` (no image count); the project counts exclude his items.

**T-GAL-17 · export.** Given G0 with one trashed item. When exported without `휴지통도 포함`. Then `gallery/<리엔 key>.json` has 9 items (7 visible + 2 hidden; the trashed one is left out), the manifest reads `formatVersion` 1.0 and lists `gallery` as optional, the encryption switch defaulted to on (project has hidden items), and gallery images are present in the payload. With the switch off, the same files are in the plain package. PDF export contains no gallery image.

**T-GAL-18 · reader tolerance.** Given a file with no `gallery/` entries. Then it opens with empty galleries and 리엔's portrait still shows. Given a G0 file whose manifest does not list `gallery` in `features.optional`. Then the reader finds `gallery/` by path and all items and their originals are back with the same bytes; no `gallery/*` path is kept as an unknown file. Gallery files whose owner is not in the file are dropped.

**T-GAL-19 · search.** Given the query `그을음`. Then one result: item 3, the sentence `손등의 그을음은 마지막까지 불을 지킨 흔적.` with `그을음` boxed; opening it shows the gallery with item 3 picked.

**T-GAL-20 · hidden source in the picker.** Given G0 locked and H1's source used by nothing else. Then `내 이미지` does not list it. When the same file is chosen through `사진` together with one new photo and `넣기` is tapped. Then one item is added, the toast reads `1장 넣음`, and no text names a hidden image.

**T-GAL-21 · uniqueness with the trash.** Given item 6 deleted. Then its tile reads `휴지통에 있음` and cannot be chosen. When the same file is added through `사진`. Then nothing is added and the toast reads `0장 넣음 · 이미 있음 1`. When item 6 is restored. Then exactly one item holds that source. When item 6 is purged instead. Then the source can be added again.

**T-GAL-22 · reuse copies every field.** Given unlocked and H1 (holo, caption, scene link, crops). When `다른 곳에도 넣기` → project. Then the new item has the same `source`, `title`, `caption`, `tagKeys`, `sceneKey`, `crops`, `holo` and `hidden = true`. When locked. Then the project gallery does not show it.

**T-GAL-23 · main image from a hidden item.** Given unlocked, H1's menu. Then `초상으로` reads `숨긴 이미지 · 초상에서는 보임`. When applied. Then the portrait source is H1's source and H1 stays hidden.

**T-GAL-24 · order skips hidden and needs 전체.** Given G0 locked (stored 4, H1, 5). When item 5 `앞으로`. Then the visible order is …3, 5, 4… and the position reads `4/8`. Given the chip `삽화` on. Then the order buttons are disabled and the row reads `<i>/<N> · 전체에서만`.

**T-GAL-25 · 옮기기 for 100+.** Given 128 items and item 3 picked on `순서 바꾸기`. When `옮기기`, then `이 뒤로` on item 80. Then item 3 sits right after the old item 80 at `80/128`, the mode ends, and one undo step restores it.

**T-GAL-26 · profile entry keeps the view.** Given the gallery view set to `목록`. When card 3 in the profile section is tapped. Then the gallery opens in `목록` with row 3 expanded.

**T-GAL-27 · one blob reference function.** Given a blob used only by item 2, and another used only by trashed item 6. When an unrelated character 오웬 is purged and `PurgeActions.runDue` runs. Then both blobs survive, and `BlobReferences.counts`, `PurgeOps.referencedBlobs` and `PurgeActions.heldBlobs` return the same set.

**T-GAL-28 · owner purge and undo.** When 리엔 is deleted and then the delete is undone. Then all 10 items are live again. When 리엔 is deleted and purged. Then no gallery item with her key remains and blobs only her items used are collected.

**T-GAL-29 · merge.** Given 세라 holds item 2's source in her gallery. When 리엔 is merged into 세라. Then 세라's gallery gains items 1, 3…8, H1, H2 after her own items in that order, item 2 stays with 리엔, and `MergeRecord.movedRefs` lists the 9 moved items. When the merge is undone. Then they are back with 리엔 in their old order.

**T-GAL-30 · scene link states.** Given item 5 linked to a hidden scene (locked) and item 7 linked to a trashed scene. Then item 5's panel reads `숨긴 씬`, item 7's reads dotted `지운 씬 · 1-6 씬 1`, and `씬 순서` sorts both as unlinked.

**T-GAL-31 · tags.** When `+ 태그` creates `회상` in the info sheet. Then the tag exists in the project list (via `CharacterEdits.addTag`) and the tag settings row reads `인물 0 · 이미지 1`. When it is renamed. Then the chip shows the new name. When it is removed. Then no item holds it and its chip is gone.

**T-GAL-32 · presets.** Given an item from preset `안개 숲`. Then the info head reads `<W> × <H> · 기본 이미지` with no size, its picker tile shows the preset name, and export writes no file for it.

**T-GAL-33 · add is one undo step.** Given `넣기 · 3` done. When `되돌리기`. Then all three items are gone and the previous pick is back.

**T-GAL-34 · unlock and relock.** Given G0 locked with item H1 not shown. When unlocked. Then chips read `전체 10`, H1 and H2 show in stored order with the chip `숨김`, and the bar reads `숨긴 이미지 2 · 열림`. When H1 is picked and `다시 잠그기` is tapped. Then nothing is picked, chips read `전체 8` and the lock tile is back.

**T-GAL-35 · trash rows.** When item 6 is deleted. Then the trash filter `인물` lists `등불 고리` · `리엔 · 갤러리`. When H1 is deleted while unlocked and the app relocks. Then its trash row reads `숨긴 이미지` and `되살리기` asks for 지문 · PIN.

**T-GAL-36 · process death.** Given item 3 picked with the panel expanded and the row scrolled. When the process is killed and the screen restored. Then item 3 is picked, the panel is expanded and the scroll is the same.

**T-GAL-37 · view setting.** When `격자` is chosen in a character gallery. Then `viewByScreen.characterGallery = GRID` round-trips through the settings codec; an unknown stored value reads as `LIST`.

**T-GAL-38 · size total.** Given items 2 and 3 share no source and item 3 shares its source with the portrait. Then `galleryBytes` counts each distinct blob once and presets as 0.

**T-GAL-39 · hint only in 카드.** Given nothing picked. Then `카드` shows `카드 누르기 · 설명 보기` under the row and the pager on the order line; `격자` shows `8장 · 숨김 2` and no hint; inner `목록` shows `이미지 누르기 · 설명 보기` in the right pane.

---

## 5. Screens

| Screen / state | Boards | Cover (390) | Inner (800) | Components |
| --- | --- | --- | --- | --- |
| 인물 개요 · 갤러리 칸 | `GalProfile` | section under the field table; row of 132 × 176 cards | record pane, same | ListRow `.row`, gallery card, `SnapCardRow` |
| 인물 카드 · 갤러리 수 | `GalCastCards` | `갤러리 8` chip on portrait cards | as CST-06 | portrait card, chip |
| 프로젝트 홈 · 작업 | `GalHome` | `갤러리` in the 작업 grid | as STO-43 | — |
| 갤러리 · 카드 | `GalShelf`, `GalShelfAway`, `GalMany`, `GalShelfWide` | 222 × 296 row, picked card lifted, panel under; picked card scrolled away with the panel expanded (`접기`, `고른 카드로`); 128 items, readout, nothing picked with the hint | 236 × 315 row, two-column panel | ViewToggle (3), chips, `SnapCardRow`, ProjectCard anatomy, panel |
| 갤러리 · 격자 | `GalGrid`, `GalUnlocked`, `GalWide` | 3 × 110 tiles, panel under the row (no-text state), locked tile; unlocked hidden items with `숨김` chips and `다시 잠그기` | list + detail | ImageTile anatomy, panel, lock tile |
| 갤러리 · 목록 | `GalList` | rows with 64 thumbnails, expanded picked row, delete toast | list + detail | ListRow, Toast |
| 갤러리 · 찾기 | `GalSearch` | command bar query, results with boxed match | same | SearchHit |
| 보기 | `GalView`, `GalViewOff`, `GalViewWide` | fitted original, `이전 · 설명 · 다음`, text panel below the image; `설명` off at the last item | image + 300 text column | Buttons, holo |
| 갤러리에 넣기 | `GalAdd` | picker sheet, numbered choices, `사진 · 파일 · 넣기 · 3` | side sheet (Q-IM-01) | Sheet, Tabs, ImageTile |
| 갤러리 · 자르기 | `GalCrop` | CNV-28 editor, chips `갤러리 카드 3:4 · 목록 1:1` | not drawn (Q-IM-01) | CropFrame |
| 이미지 정보 | `GalInfo` | sheet with 제목 · 설명 · 태그 · 연결 | side panel | Sheet, fields, Tag |
| 이미지 메뉴 | `GalMore` | `보기 · 고치기` buttons, rows and switches | popover | Sheet, ListRow, Controls |
| 순서 바꾸기 | `GalOrder`, `GalOrderMove` | rows with grip and `앞으로 · 뒤로`, bottom `맨 앞 · 옮기기 · 맨 뒤`; 옮기기 mode with `이 뒤로` | same at 560 | ListRow, Buttons |
| 프로젝트 갤러리 | `GalProject` | rows per owner with pagers in group headers, hint, LockRow | list + detail | as above, `LockRow` |
| 빈 갤러리 | `GalEmpty` | outlined card + portrait offer | same, centred | new-item card |

Not drawn (rules stated above): project gallery with a card picked, the `여러 장 고르기` mode, the sort sheet, the owner picker of `다른 곳에도 넣기`, inner add and crop sheets, filter with no result, the trash row, R-18 and capture-blocked screens (no visible change), the list `LockRow`.

---

## 6. 열린 질문 (each with the default this spec uses)

1. **이름 겹침.** The feature is `갤러리`; the CNV-27 system-picker button is also `갤러리` (boards `ImagePick`, map import, motion caption). Default: rename that button to `사진` everywhere (CNV-27 and those boards); until the user confirms, only the gallery boards use `사진`.
2. **기본 보기.** Default view of a gallery screen. Default: `카드`.
3. **이미지 숨기기.** Per-image hiding is not in LIB-36's list. Default: included (GAL-19), same lock and masking.
4. **처음 들어갈 때 고른 카드.** Default: nothing picked, the hint in `카드`; return restores the last pick.
5. **용량 제한.** No cap exists for images. Default: no cap, no resizing; sizes shown.
6. **보기에서 밀어 넘기기.** Default: no swipe paging; only `이전` · `다음` buttons (pinch zoom only).
7. **PDF 내보내기.** Default: no gallery images in PDF, CSV or engine JSON.
8. **펼친 화면에서 아무것도 안 고른 오른쪽 칸.** Default: the first item dimmed with `이미지 누르기 · 설명 보기`.
9. **태그 목록.** Gallery tags share the project `Tag` list with characters. Default: shared (one tag system).
10. **시점 렌즈.** Gallery screens have no lens chip. Default: names in `@` links resolve at `최신`, also for items linked to a scene; a scene link is shown even when it is beyond the 집필 선.
11. **프로필 칸의 카드 수.** Default: at most 12 cards in the profile row, then `전체 보기 ›`.
12. **휴지통 필터.** Default: gallery items appear under `전체` and, for character items, `인물`.
13. **내보내기 크기.** With no cap, 100+ originals grow the `.civilight` and backups. Default: the LIB-47 export sheet and the backup warning gain a row `갤러리 이미지 <n> · <MB>` from `galleryBytes` (hand-off to LIB).
14. **기본 이미지 자르기.** Depends on CNV Q-IM-03. Default: presets can be cropped in registry pixels; if CNV says no, `자르기` is disabled for preset items.
15. **같은 원본과 휴지통.** Default: a trashed item still holds its source (`휴지통에 있음`), so restore never duplicates; the alternative is refusing restore with a toast.
16. **파일 안 갤러리 자리.** Default: `gallery/<ownerKey>.json` plus originals under `gallery/files/` (a blob also used by a portrait or cover is in the package twice; the device stores it once), so the gallery part of a package is self-contained and read by path. (The earlier reason, a 0.1.0 round trip, no longer applies: the gallery ships in 0.1.0, format 1.0.) The alternative, originals only under `assets/`, would save those duplicate bytes.
