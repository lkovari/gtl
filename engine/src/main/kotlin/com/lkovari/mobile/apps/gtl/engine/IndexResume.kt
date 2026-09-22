package com.lkovari.mobile.apps.gtl.engine

data class IndexProgress(
    val done: Boolean,
    val truncated: Boolean,
    val nextAttemptAtMillis: Long
)

enum class IndexResumeAction {
    Ready,
    Resume,
    Backoff,
    Truncated
}

object IndexResume {
    fun action(progress: IndexProgress?, nowMillis: Long): IndexResumeAction {
        if (progress == null) {
            return IndexResumeAction.Resume
        }
        if (progress.done) {
            return IndexResumeAction.Ready
        }
        if (progress.truncated) {
            return IndexResumeAction.Truncated
        }
        if (progress.nextAttemptAtMillis > nowMillis) {
            return IndexResumeAction.Backoff
        }
        return IndexResumeAction.Resume
    }
}
