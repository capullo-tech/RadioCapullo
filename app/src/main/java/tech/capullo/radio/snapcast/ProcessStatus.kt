package tech.capullo.radio.snapcast

/**
 * Health of a supervised native process (snapserver / shairport-sync) as
 * tracked by [tech.capullo.radio.services.RadioBroadcasterService]'s restart
 * supervisor. Surfaced to the broadcaster UI so a crashed or recovering
 * source is visible instead of log-only.
 */
enum class ProcessStatus {
    STARTING,
    RUNNING,
    RESTARTING,
    STOPPED,
}
