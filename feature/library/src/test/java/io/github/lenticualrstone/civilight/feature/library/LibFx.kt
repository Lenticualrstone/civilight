package io.github.lenticualrstone.civilight.feature.library

import io.github.lenticualrstone.civilight.domain.actions.ActionContext
import io.github.lenticualrstone.civilight.feature.library.newproject.NewProjectForm
import io.github.lenticualrstone.civilight.feature.library.newproject.ProjectCreator
import io.github.lenticualrstone.civilight.feature.library.shelf.ShelfProject
import io.github.lenticualrstone.civilight.model.Key
import io.github.lenticualrstone.civilight.model.app.RatingDefaults
import io.github.lenticualrstone.civilight.model.doc.ProjectDoc
import io.github.lenticualrstone.civilight.model.project.ProjectFormat
import io.github.lenticualrstone.civilight.model.project.Rating
import io.github.lenticualrstone.civilight.model.sample.ExampleClock
import io.github.lenticualrstone.civilight.model.sample.ExampleProject
import java.time.LocalDateTime

/**
 * Test fixtures: the spec §4 sample projects (잿빛 = the example project as an ordinary row, 유리
 * 정원, 검은 서약, 외전) made through the real `만들기` path, at the fixtures' clock.
 */
object LibFx {
    val NOW: Long = ExampleClock.NOW
    val ZONE = ExampleClock.ZONE

    /** Local `yyyy-MM-ddTHH:mm` in Asia/Seoul as epoch millis. */
    fun at(local: String): Long = LocalDateTime.parse(local).atZone(ZONE).toInstant().toEpochMilli()

    private var lastKey = 0

    /** Keys are unique across every fixture made in one test run. */
    fun ctx(): ActionContext =
        ActionContext(now = NOW, zone = ZONE, device = "test-device", newKey = { "01KTEST" + (++lastKey).toString().padStart(19, '0') })

    fun created(form: NewProjectForm, defaults: RatingDefaults = RatingDefaults()): ProjectDoc =
        ProjectCreator.build(form, defaults, ctx(), bundleSource = null).doc

    /** The example's document, listed as an ordinary project (not the bundled example). */
    fun ashen(): ProjectDoc = ExampleProject.build()

    fun glass(): ProjectDoc = created(NewProjectForm(name = "유리 정원", format = ProjectFormat.SHORT, rating = Rating.ALL))

    fun oath(): ProjectDoc = created(NewProjectForm(name = "검은 서약 (가제)", format = ProjectFormat.SETTING_ONLY, rating = Rating.R18, protagonist = "세라"))

    fun gaiden(): ProjectDoc = created(NewProjectForm(name = "외전 · 오웬의 시장", format = ProjectFormat.SHORT, rating = Rating.R15, step4Hide = true))

    /** A 서재 row for [doc] modified at [updatedAt]. */
    fun shelf(doc: ProjectDoc, updatedAt: Long, isExample: Boolean = false): ShelfProject = ShelfProject(
        entry = ExampleProject.libraryEntry().copy(
            key = doc.key,
            name = doc.project.name,
            rating = doc.project.rating,
            hidden = doc.project.protections.hidden,
            isExample = isExample,
            readOnly = isExample,
            updatedAt = updatedAt,
        ),
        doc = doc,
    )

    fun keyOf(doc: ProjectDoc): Key = doc.key
}
