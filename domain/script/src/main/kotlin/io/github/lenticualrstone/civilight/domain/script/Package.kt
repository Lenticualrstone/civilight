/**
 * Script domain (10-script.md, rule 12 and the script side of rule 22): block numbers, voice IDs,
 * block operations, anchor relocation, display layers, scene text, script checks, exports.
 *
 * Every operation is a pure function `ProjectDoc -> result` (§8.2): the caller keeps the previous
 * doc for undo. Korean copy is produced by small formatter functions next to each operation.
 */
package io.github.lenticualrstone.civilight.domain.script
