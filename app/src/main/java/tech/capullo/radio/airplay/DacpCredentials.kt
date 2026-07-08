package tech.capullo.radio.airplay

/**
 * DACP remote-control credentials for the connected AirPlay sender, delivered
 * over shairport-sync's metadata pipe ([AirplayMetadataParser]):
 *
 * - [dacpId] (`ssnc/daid`) identifies the sender's `_dacp._tcp` control service,
 *   advertised as `iTunes_Ctrl_<dacpId>`, to be resolved to host:port.
 * - [activeRemote] (`ssnc/acre`) is the token sent in the `Active-Remote` header
 *   of each DACP request.
 */
data class DacpCredentials(val dacpId: String, val activeRemote: String)
