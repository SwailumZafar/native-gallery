package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem

enum class LockedMediaImportNextStep {
    NothingImported,
    RequestOriginalRemoval
}

data class LockedMediaImportOutcome(
    val requestedIds: Set<String>,
    val importedIds: Set<String>,
    val failedIds: Set<String>,
    val nextStep: LockedMediaImportNextStep
)

enum class LockedMediaOriginalRemovalResult {
    Approved,
    CancelledOrFailed
}

data class LockedMediaOriginalRemovalResolution(
    val committedIds: Set<String>,
    val rollbackIds: Set<String>
)

/**
 * Pure decisions for the two-phase "encrypt first, remove the shared original second" operation.
 */
internal object LockedMediaOperationsPolicy {
    fun afterImport(
        requestedIds: Iterable<String>,
        importedIds: Iterable<String>
    ): LockedMediaImportOutcome {
        val requested = requestedIds.toStableSet()
        val imported = importedIds.toStableSet().intersect(requested)
        return LockedMediaImportOutcome(
            requestedIds = requested,
            importedIds = imported,
            failedIds = requested - imported,
            nextStep = if (imported.isEmpty()) {
                LockedMediaImportNextStep.NothingImported
            } else {
                LockedMediaImportNextStep.RequestOriginalRemoval
            }
        )
    }

    fun afterOriginalRemoval(
        importedIds: Iterable<String>,
        result: LockedMediaOriginalRemovalResult,
        originalsStillPresentIds: Iterable<String> = emptySet()
    ): LockedMediaOriginalRemovalResolution {
        val imported = importedIds.toStableSet()
        if (result == LockedMediaOriginalRemovalResult.Approved) {
            return LockedMediaOriginalRemovalResolution(
                committedIds = imported,
                rollbackIds = emptySet()
            )
        }

        // Only roll back a vault copy when its public original is still readable. If Android
        // removed an item despite an ambiguous/cancelled result, retaining the vault copy avoids
        // turning a UI failure into irreversible data loss.
        val rollback = originalsStillPresentIds.toStableSet().intersect(imported)
        return LockedMediaOriginalRemovalResolution(
            committedIds = imported - rollback,
            rollbackIds = rollback
        )
    }

    private fun Iterable<String>.toStableSet(): Set<String> {
        return filter(String::isNotBlank).toCollection(linkedSetOf())
    }
}

data class LockedMediaImportExecution(
    val outcome: LockedMediaImportOutcome,
    /**
     * The complete hidden-media state after a change, or null when no hidden state was changed.
     */
    val updatedHiddenMediaIds: Set<String>?
)

data class LockedMediaOriginalRemovalExecution(
    val resolution: LockedMediaOriginalRemovalResolution,
    /**
     * The complete hidden-media state after a rollback, or null when no rollback was necessary.
     */
    val updatedHiddenMediaIds: Set<String>?
)

/**
 * Synchronous domain service for vault import and safe rollback. Run these methods on an IO
 * dispatcher. The callback constructor is internal to keep the service locally unit-testable.
 */
class LockedMediaOperations internal constructor(
    private val importToVault: (MediaItem) -> Boolean,
    private val originalStillExists: (String) -> Boolean,
    private val deleteFromVault: (String) -> Unit,
    private val setHidden: (Set<String>, Boolean) -> Set<String>
) {
    constructor(
        vaultRepository: LockedMediaVaultRepository,
        hiddenMediaRepository: HiddenMediaRepository
    ) : this(
        importToVault = vaultRepository::importMedia,
        originalStillExists = vaultRepository::originalMediaExists,
        deleteFromVault = vaultRepository::delete,
        setHidden = hiddenMediaRepository::setMediaHidden
    )

    fun importIntoVault(mediaItems: List<MediaItem>): LockedMediaImportExecution {
        val uniqueItems = mediaItems.distinctBy(MediaItem::id)
        val importedIds = uniqueItems.mapNotNull { mediaItem ->
            mediaItem.id.takeIf {
                runCatching { importToVault(mediaItem) }.getOrDefault(false)
            }
        }
        val outcome = LockedMediaOperationsPolicy.afterImport(
            requestedIds = uniqueItems.map(MediaItem::id),
            importedIds = importedIds
        )
        val hiddenState = outcome.importedIds
            .takeIf(Set<String>::isNotEmpty)
            ?.let { setHidden(it, true) }
        return LockedMediaImportExecution(
            outcome = outcome,
            updatedHiddenMediaIds = hiddenState
        )
    }

    fun resolveOriginalRemoval(
        importedIds: Set<String>,
        result: LockedMediaOriginalRemovalResult
    ): LockedMediaOriginalRemovalExecution {
        val originalsStillPresent = if (result == LockedMediaOriginalRemovalResult.CancelledOrFailed) {
            importedIds.filterTo(linkedSetOf()) { mediaId ->
                runCatching { originalStillExists(mediaId) }.getOrDefault(false)
            }
        } else {
            emptySet()
        }
        val resolution = LockedMediaOperationsPolicy.afterOriginalRemoval(
            importedIds = importedIds,
            result = result,
            originalsStillPresentIds = originalsStillPresent
        )
        val hiddenState = resolution.rollbackIds
            .takeIf(Set<String>::isNotEmpty)
            ?.let { rollbackIds ->
                val nextHiddenState = setHidden(rollbackIds, false)
                rollbackIds.forEach { mediaId ->
                    runCatching { deleteFromVault(mediaId) }
                }
                nextHiddenState
            }
        return LockedMediaOriginalRemovalExecution(
            resolution = resolution,
            updatedHiddenMediaIds = hiddenState
        )
    }
}
