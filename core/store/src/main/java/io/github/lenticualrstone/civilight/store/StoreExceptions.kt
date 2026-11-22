package io.github.lenticualrstone.civilight.store

import io.github.lenticualrstone.civilight.model.Key

/** A project directory that cannot be opened (no readable `project.json` or `story/index.json`). Nothing is rewritten. */
class ProjectLoadException(val projectKey: Key, message: String, cause: Throwable? = null) :
    IllegalStateException("project $projectKey: $message", cause)

/** An edit of a read-only project (the example project, LIB-17, test P-08). */
class ReadOnlyProjectException(val projectKey: Key) : IllegalStateException("project $projectKey is read-only")

/** No project with this key in the library (or not in the expected state). */
class NoSuchProjectException(val projectKey: Key, message: String = "no such project") :
    IllegalStateException("project $projectKey: $message")

/** A project key already used by a live or trashed project (§6: keys are unique among both). */
class ProjectKeyTakenException(val projectKey: Key) : IllegalStateException("project key $projectKey is taken")
