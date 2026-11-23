package io.github.lenticualrstone.civilight.feature.cast.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.lenticualrstone.civilight.designsystem.component.ChipFlow
import io.github.lenticualrstone.civilight.designsystem.component.CivilightText
import io.github.lenticualrstone.civilight.designsystem.component.CivilightTextField
import io.github.lenticualrstone.civilight.designsystem.component.CreateCard
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardOverlay
import io.github.lenticualrstone.civilight.designsystem.component.CreateCardState
import io.github.lenticualrstone.civilight.designsystem.component.CreateField
import io.github.lenticualrstone.civilight.designsystem.component.CreateFieldType
import io.github.lenticualrstone.civilight.designsystem.component.CreateFields
import io.github.lenticualrstone.civilight.designsystem.component.FilterChip
import io.github.lenticualrstone.civilight.designsystem.theme.CivilightTheme
import io.github.lenticualrstone.civilight.model.cast.Tag

/**
 * The DS-72 create card with the cast's field set: text fields (name focused on open, caret at its
 * end so a picker's query is ready to edit) and the `태그` chips of the live project tags (several
 * may be checked; no tag is created from inside this card, CST-66 / §8.3 Q43). Fields without a
 * working control are not drawn ([CastCreate.fields]).
 */
@Composable
fun CastCreateCard(
    state: CreateCardState,
    onChange: (CreateCardState) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
    onCreateAndOpen: () -> Unit = {},
    canCreate: Boolean = CastCreate.canCreate(state),
    tags: List<Tag> = emptyList(),
    fields: List<CreateField> = CastCreate.fields(state.kind),
) {
    CreateCardOverlay(
        title = CreateCard.title(state.kind),
        mode = state.mode,
        canCreate = canCreate,
        onCancel = onCancel,
        onCreate = onCreate,
        onCreateAndOpen = onCreateAndOpen,
        dismissOnScrim = CreateCard.dismissOnScrim(state),
    ) {
        CastCardFields(state, fields, onChange, tags)
    }
}

@Composable
private fun ColumnScope.CastCardFields(state: CreateCardState, fields: List<CreateField>, onChange: (CreateCardState) -> Unit, tags: List<Tag>) {
    val focus = remember { FocusRequester() }
    for (field in fields) {
        when (field.type) {
            CreateFieldType.TEXT -> CivilightTextField(
                value = state.values[field.key].orEmpty(),
                onValueChange = { onChange(state.with(field.key, it)) },
                label = field.label,
                modifier = if (field.key == CreateFields.NAME) Modifier.focusRequester(focus) else Modifier,
            )
            CreateFieldType.MULTI_PICK -> if (field.key == CreateFields.tags.key && tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CivilightText(field.label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
                    val picked = CastCreate.tagKeys(state)
                    ChipFlow {
                        for (tag in tags) FilterChip(tag.name, tag.key in picked, { onChange(CastCreate.toggleTag(state, tag.key)) }, tagColor = tag.color)
                    }
                }
            }
            // CST-75 `색`: `없음` and the palette swatches.
            CreateFieldType.COLOR -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CivilightText(field.label, CivilightTheme.type.label, color = CivilightTheme.colors.ink2)
                io.github.lenticualrstone.civilight.designsystem.component.TagColorRow(
                    selected = CastCreate.tagColor(state),
                    onPick = { onChange(state.with(CreateFields.color.key, it.orEmpty())) },
                )
            }
            else -> Unit
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}
