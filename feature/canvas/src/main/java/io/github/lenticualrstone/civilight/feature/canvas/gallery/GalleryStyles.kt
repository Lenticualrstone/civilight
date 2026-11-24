package io.github.lenticualrstone.civilight.feature.canvas.gallery

import io.github.lenticualrstone.civilight.model.app.AppSettings
import io.github.lenticualrstone.civilight.designsystem.icon.IconId
import io.github.lenticualrstone.civilight.model.app.ViewMode
import io.github.lenticualrstone.civilight.model.gallery.GalleryOwnerKind

/** The picker's groups (board `VpGal*`): the seven r3 views, then the three views the gallery already had. */
enum class GalleryStyleGroup(val label: String, val short: String) {
    NEW(GalleryStyleCopy.GROUP_NEW, GalleryStyleCopy.GROUP_NEW_SHORT),
    BASIC(GalleryStyleCopy.GROUP_BASIC, GalleryStyleCopy.GROUP_BASIC),
}

/**
 * Every gallery view (0.2.3, r3 page `galviews` + `viewpick`). [id] is stored and never renamed (board keys
 * `film` · `mood` · `pola` · `exhibit` · `tone` · `just` · `line`). [base] is the old three-way kind of the
 * existing bodies (`목록` · `격자` · `카드`); null = an r3 view drawn by [GalleryStyleBody]. [iconPath] is the
 * board's 24 × 24 outline (`scr_v_gal.py` `I`).
 */
enum class GalleryStyle(
    val id: String,
    val label: String,
    val fragment: String,
    val group: GalleryStyleGroup,
    val base: GalleryViewKind?,
    val iconPath: String,
) {
    FILM("film", GalleryStyleCopy.FILM, GalleryStyleCopy.FILM_SUB, GalleryStyleGroup.NEW, null,
        "M5 3h14v18H5zM5 7h14M5 17h14M8 3v4M12 3v4M16 3v4M8 17v4M12 17v4M16 17v4"),
    MOOD("mood", GalleryStyleCopy.MOOD, GalleryStyleCopy.MOOD_SUB, GalleryStyleGroup.NEW, null,
        "M4 5h8v7H4zM14 4h6v6h-6zM6 14h6v6H6zM14 13h6v7h-6zM6 4l3 2M16 3l2 2"),
    CLOTHESLINE("pola", GalleryStyleCopy.CLOTHESLINE, GalleryStyleCopy.CLOTHESLINE_SUB, GalleryStyleGroup.NEW, null,
        "M2 5c5 3 15 3 20 0M5 7h6v8H5zM13 7h6v8h-6zM8 5v2M16 5v2"),
    EXHIBIT("exhibit", GalleryStyleCopy.EXHIBIT, GalleryStyleCopy.EXHIBIT_SUB, GalleryStyleGroup.NEW, null,
        "M3 20h18M5 5h6v8H5zM13 5h6v6h-6zM8 2v3M16 2v3"),
    TONE("tone", GalleryStyleCopy.TONE, GalleryStyleCopy.TONE_SUB, GalleryStyleGroup.NEW, null,
        "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z"),
    TAG_ROWS("just", GalleryStyleCopy.TAG_ROWS, GalleryStyleCopy.TAG_ROWS_SUB, GalleryStyleGroup.NEW, null,
        "M3 4v16M6.5 4h6v6h-6zM14.5 4h6v6h-6zM6.5 12h9v8h-9zM17.5 12h3v8h-3z"),
    SCENE_ORDER("line", GalleryStyleCopy.SCENE_ORDER, GalleryStyleCopy.SCENE_ORDER_SUB, GalleryStyleGroup.NEW, null,
        "M7 3v18M7 6h.01M7 12h.01M7 18h.01M10 5h10v3H10zM10 11h7v3h-7zM10 17h9v3h-9z"),
    LIST("list", GalleryStyleCopy.LIST, GalleryStyleCopy.LIST_SUB, GalleryStyleGroup.BASIC, GalleryViewKind.LIST,
        IconId.List.pathData),
    GRID("grid", GalleryStyleCopy.GRID, GalleryStyleCopy.GRID_SUB, GalleryStyleGroup.BASIC, GalleryViewKind.GRID,
        IconId.GridTall.pathData),
    CARD("card", GalleryStyleCopy.CARD, GalleryStyleCopy.CARD_SUB, GalleryStyleGroup.BASIC, GalleryViewKind.CARD,
        IconId.Grid.pathData),
    ;

    /** An r3 view (drawn by [GalleryStyleBody]). */
    val isNew: Boolean get() = base == null

    /** The old kind the rest of the screen keys on (layout, scroll save): the r3 views count as `카드`. */
    val kind: GalleryViewKind get() = base ?: GalleryViewKind.CARD

    companion object {
        /** Picker order: 새 보기 7, then 기본 3 (board `VpGal*`, without the 3D group). */
        val ORDER: List<GalleryStyle> = entries.sortedBy { it.group.ordinal }

        fun ofId(id: String?): GalleryStyle? = entries.firstOrNull { it.id == id }

        fun of(kind: GalleryViewKind): GalleryStyle = when (kind) {
            GalleryViewKind.LIST -> LIST
            GalleryViewKind.GRID -> GRID
            GalleryViewKind.CARD -> CARD
        }
    }
}

/**
 * The remembered gallery view per screen (`characterGallery` · `projectGallery`). The old key in
 * `AppSettings.viewByScreen` keeps `LIST` · `GRID` · `CARD` (an r3 view writes `CARD` there, so an older build shows
 * `카드`); an r3 view is stored by id in `AppSettings.galleryStyle`. An absent or unknown id reads the old key's view.
 */
object GalleryStyleChoice {
    fun of(settings: AppSettings, kind: GalleryOwnerKind): GalleryStyle {
        val old = GalleryStyle.of(GalleryViewChoice.of(settings.viewByScreen, kind))
        val picked = GalleryStyle.ofId(settings.galleryStyle[GalleryViewChoice.screenOf(kind)])
        return picked?.takeIf { it.isNew } ?: old
    }

    /** [settings] with [style] chosen for [kind]'s screen; every other screen's entries stay. */
    fun write(settings: AppSettings, kind: GalleryOwnerKind, style: GalleryStyle): AppSettings {
        val screen = GalleryViewChoice.screenOf(kind)
        return settings.copy(
            viewByScreen = settings.viewByScreen + (screen to GalleryViewChoice.modeOf(style.kind)),
            galleryStyle = if (style.isNew) settings.galleryStyle + (screen to style.id) else settings.galleryStyle - screen,
        )
    }
}

/** Copy of the r3 gallery views and the view picker (terse fragments, DECISIONS §3). */
object GalleryStyleCopy {
    const val SEP = " · "
    const val VIEW = "보기"
    const val GROUP_NEW = "새 보기"
    const val GROUP_NEW_SHORT = "새"
    const val GROUP_BASIC = "기본"

    const val FILM = "필름"
    const val FILM_SUB = "한 장 크게 · 필름 띠"
    const val MOOD = "무드보드"
    const val MOOD_SUB = "자유 배치 · 테이프"
    const val CLOTHESLINE = "빨랫줄"
    const val CLOTHESLINE_SUB = "줄에 건 사진 · 집게"
    const val EXHIBIT = "전시실"
    const val EXHIBIT_SUB = "액자 · 전시 벽"
    const val TONE = "색 묶음"
    const val TONE_SUB = "색깔별 묶음 · 탭"
    const val TAG_ROWS = "태그 줄"
    const val TAG_ROWS_SUB = "태그별 줄 · 꽉 채움"
    /** Board `장면 순서`; the app says `씬` for a scene (vocabulary rule, CopyLint). */
    const val SCENE_ORDER = "씬 순서"
    const val SCENE_ORDER_SUB = "원고 순서 · 줄"
    const val LIST = "목록"
    const val LIST_SUB = "제목 · 태그 · 씬"
    const val GRID = "격자"
    const val GRID_SUB = "같은 크기 · 세 줄"
    const val CARD = "카드 진열"
    const val CARD_SUB = "카드 한 줄 · 설명 판"

    // Picker (board `VpGalC` · `VpGalI`).
    const val PICK_TITLE = "보기 고르기"
    const val NOW = "지금"
    const val BTN_APPLY = "이 보기로"
    const val BTN_CANCEL = "취소"
    const val PREVIEW = "미리 보기"
    const val SWIPE_HINT = "밀어서 바꾸기"
    const val BOTH_FORMS = "커버 · 내부 같이 바뀜"
    const val BTN_PREV_STYLE = "이전 보기"
    const val BTN_NEXT_STYLE = "다음 보기"

    // Views.
    const val BTN_UNLOCK = "잠금 풀기"
    const val UNLOCK_SUB = "잠금 풀기 · 지문 · PIN"
    const val HIDDEN_IMAGE = "숨긴 이미지"
    const val HIDDEN_OWNERS = "숨긴 인물"
    const val LOCKED = "잠김"
    const val HIDDEN = "숨김"
    const val BTN_ENLARGE = "크게 보기"
    const val BTN_PREV_FRAME = "이전 액자"
    const val BTN_NEXT_FRAME = "다음 액자"
    const val BTN_PREV_PAGE = "이전 쪽"
    const val BTN_NEXT_PAGE = "다음 쪽"
    const val FILM_A11Y = "필름 · 모든 이미지 · 눌러서 고르기"
    const val MOOD_A11Y = "무드보드"
    const val CLOTHESLINE_A11Y = "빨랫줄"
    const val EXHIBIT_A11Y = "전시실"
    const val TONE_A11Y = "색 묶음"
    const val TAG_ROWS_A11Y = "태그 줄"
    const val SCENE_ORDER_A11Y = "원고 순서"
    const val PICKED_A11Y = "고른 이미지"
    const val WARM = "따뜻함"
    const val COOL = "차가움"
    const val GREY = "잿빛"
    const val NO_TAG = "태그 없음"
    const val NO_SCENE = "씬 없음"
    const val NO_SCENE_LANE = "씬 없는 이미지"
    const val NO_PLACED = "씬에 이은 이미지 없음"
    const val NONE = "없음"
    const val HOLO = "홀로"
    const val PLAIN = "보통"
    const val PRESET = "기본 이미지"
    const val FIRST_SHOW = "첫 전시"

    // Information pane keys (board `KV`).
    const val KEY_TAG = "태그"
    const val KEY_SCENE = "씬"
    const val KEY_TIME = "때"
    const val KEY_KIND = "종류"
    const val KEY_RATIO = "비율"
    const val KEY_SIZE = "크기"
    const val KEY_ADDED = "넣은 날"
    const val KEY_USE = "쓰인 곳"

    /** `보기 · 필름` (the header button's name). */
    fun viewButton(label: String): String = "$VIEW$SEP$label"

    /** `지금 · 필름`. */
    fun now(label: String): String = "$NOW$SEP$label"

    /** `미리 보기 · 필름`. */
    fun preview(label: String): String = "$PREVIEW$SEP$label"

    /** `보기 · 필름` toast after applying. */
    fun applied(label: String): String = "$VIEW$SEP$label"

    /** `필름 · 한 장 크게 · 필름 띠 · 새 보기` (a picker tile's name). */
    fun tile(style: GalleryStyle): String = listOf(style.label, style.fragment, style.group.label).joinToString(SEP)

    /** `새 보기 · 7`. */
    fun group(label: String, n: Int): String = "$label$SEP$n"

    /** `3 / 8` among the visible images; the hidden bundle reads `숨김 2`. */
    fun count(index1: Int, n: Int): String = "$index1 / $n"

    fun hiddenCount(h: Int): String = if (h > 0) "$HIDDEN $h" else HIDDEN

    /** `벽 1 / 3`. */
    fun wall(index1: Int, n: Int): String = "벽 $index1 / $n"

    /** `필름 · 3 / 8`. */
    fun filmLabel(count: String): String = "$FILM$SEP$count"

    /** `리엔 · 첫 전시`. */
    fun exhibitTitle(owner: String): String = "$owner$SEP$FIRST_SHOW"

    /** `씬 없음 · 3`. */
    fun lane(n: Int): String = "$NO_SCENE$SEP$n"

    /** `삽화 4` (a tag row's label). */
    fun tagRow(label: String, n: Int): String = "$label $n"

    /** `따뜻함 · 3장`. */
    fun toneTab(label: String, n: Int): String = "$label$SEP$n${GalleryUiCopy.UNIT_IMAGES}"

    /** `11월 2일`. */
    fun monthDay(month: Int, day: Int): String = "${month}월 ${day}일"

    /** `2400 × 3200`. */
    fun pixels(w: Int, h: Int): String = "$w × $h"

    /** `1-3 씬 2 · 1일째 저녁` style meta pieces joined. */
    fun join(parts: List<String>): String = parts.filter { it.isNotBlank() }.joinToString(SEP)
}
