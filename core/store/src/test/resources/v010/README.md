# v0.1.0 fixtures

Written by the code of the `v0.1.0` tag (not by this branch), for the 0.2.0 upgrade tests (scope §8, 00 §13.8).

- `store-rich/`: `ProjectStore.writeNew` of the example project with its gallery (`ExampleGallery.build()`), protections `captureBlock` and `recorderLinkMask` on, plus: a stage value on #043, a hand-typed voice ID, a hidden character (세라), one `아님` decision, a CSV and a PDF export record, and the lens at the end of 1-7. Holds game data (fixed and discarded voice IDs, cues, assets, voice lines, archive documents, a choice-option speaker), an R-15 rating, hidden items, holo images, a node map, travel overrides, acknowledgements, hand-set reveals and trashed rows.
- `store-plain/`: the same project with every piece of game data removed (automatic voice IDs only; only the PDF export record is kept).
- `settings.json`: `AppSettingsCodec.encode` of 0.1.0 settings with `viewByScreen = {library: list, cast: card}` and `lockScreenRecording = false`.

`domain/library/src/test/resources/v010/rich.civilight` is the same rich project written by the v0.1.0 `ProjectPackageWriter` with `includeTrash = true` and the example recorder library.

Do not regenerate these from the current branch.
