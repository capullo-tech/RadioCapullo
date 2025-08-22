package tech.capullo.radio.snapcast

data class SnapcastServer(
    val serviceName: String,
    val host: String,
    val port: Int
) {
    override fun toString(): String = "$serviceName ($host:$port)"
}