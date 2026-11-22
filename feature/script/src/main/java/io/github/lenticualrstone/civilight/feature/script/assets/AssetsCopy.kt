package io.github.lenticualrstone.civilight.feature.script.assets

/**
 * Copy of `에셋` (SCR-61, board `Assets`) and its `[PROPOSED]` add / change form (not drawn). Words of
 * the form are the spec's field names in the vocabulary's terms; see the status file's Defaults.
 */
object AssetsCopy {
    /** Screen title (board `Assets`) and the command that opens it (SCR-68 `[PROPOSED]`). */
    const val TITLE = "에셋"

    /** Command-field hint on this screen (SCR-61). */
    const val SEARCH_HINT = "에셋 이름이나 파일 이름으로 찾기"

    /** The `+` icon's label and the add form's title (as `블록 추가`). */
    const val ADD_TITLE = "에셋 추가"

    /** Under a cue kind in the `연출 지시` sheet: opens the add form for that kind (SCR-08 · SCR-61). */
    const val ADD_FOR_CUE = "+ 에셋"

    const val FIELD_KIND = "종류"
    const val FIELD_NAME = "이름"
    const val FIELD_FILE = "파일 이름"
    const val FIELD_IMAGE = "이미지"
    const val FIELD_LENGTH = "길이"
    const val FIELD_LOOP = "반복"
    const val FIELD_PLACE = "장소"

    /** Unit of the length field. */
    const val SECONDS = "초"

    /** No image yet / no place (the asset list's and the scene sheets' words). */
    const val NO_IMAGE = "아직 이미지 없음"
    const val NO_PLACE = "장소 없음"

    /** Buttons. */
    const val PICK_IMAGE = "이미지 고르기"
    const val REMOVE_IMAGE = "빼기"
    const val PICK_PLACE = "장소 고르기"
    const val ADD = "추가"
    const val SAVE = "저장"
    const val CANCEL = "취소"
    const val CHANGE = "바꾸기 ›"

    /** Read-only projects (the add sheet's word). */
    const val READ_ONLY = "읽기 전용"

    /** No asset matches the filter or the search (the empty-search word). */
    const val NOTHING_FOUND = "결과 없음"

    /** The list of a project without assets (SCR-61 gives no copy). */
    const val NO_ASSETS = "TODO_COPY_SCRIPT_ASSETS_EMPTY"

    val all: List<String> = listOf(
        TITLE, SEARCH_HINT, ADD_TITLE, ADD_FOR_CUE, FIELD_KIND, FIELD_NAME, FIELD_FILE, FIELD_IMAGE, FIELD_LENGTH,
        FIELD_LOOP, FIELD_PLACE, SECONDS, NO_IMAGE, NO_PLACE, READ_ONLY, NOTHING_FOUND, NO_ASSETS,
    )
    val buttons: List<String> = listOf(PICK_IMAGE, REMOVE_IMAGE, PICK_PLACE, ADD, SAVE, CANCEL, CHANGE)
}
