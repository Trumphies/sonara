package com.sonara.app.player

/**
 * Pure audio-focus decision policy (no Android imports — JVM-testable).
 *
 * Focus-change values are android.media.AudioManager's public constants
 * (GAIN=1, LOSS=-1, LOSS_TRANSIENT=-2, LOSS_TRANSIENT_CAN_DUCK=-3).
 * Behavior follows the platform convention for media apps:
 * permanent/transient loss pauses; can-duck only lowers volume;
 * gain restores volume and resumes ONLY playback that this loss paused
 * (a user pause is never overridden by a focus gain).
 */
object AudioFocusPolicy {
    const val FOCUS_GAIN = 1
    const val FOCUS_LOSS = -1
    const val FOCUS_LOSS_TRANSIENT = -2
    const val FOCUS_LOSS_TRANSIENT_CAN_DUCK = -3

    /** Volume multiplier while ducked (platform convention: clearly quieter, still audible). */
    const val DUCK_SCALE = 0.2f

    enum class Action {
        PAUSE_PERMANENT,
        PAUSE_TRANSIENT,
        DUCK,
        RESTORE_AND_RESUME_IF_PAUSED,
        IGNORE,
    }

    fun actionFor(change: Int, pausedByFocusLoss: Boolean, ducked: Boolean): Action =
        when (change) {
            FOCUS_LOSS -> Action.PAUSE_PERMANENT
            FOCUS_LOSS_TRANSIENT -> Action.PAUSE_TRANSIENT
            FOCUS_LOSS_TRANSIENT_CAN_DUCK -> Action.DUCK
            FOCUS_GAIN ->
                if (pausedByFocusLoss || ducked) Action.RESTORE_AND_RESUME_IF_PAUSED else Action.IGNORE
            else -> Action.IGNORE
        }
}
