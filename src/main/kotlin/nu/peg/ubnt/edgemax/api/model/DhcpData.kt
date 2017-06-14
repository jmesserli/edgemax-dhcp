package nu.peg.ubnt.edgemax.api.model

import java.time.LocalDateTime

data class DhcpData(
        val dhcpNetworks: List<DhcpNetwork>
)

data class DhcpNetwork(
        val name: String,
        val subnet: String,
        val gateway: String,
        val leaseTime: Int,
        val dnsServers: List<String>,
        val range: IPRange,
        val leases: List<DhcpLease>,
        val stats: DhcpStatistics
)

data class IPRange(
        val start: String,
        val end: String
)

data class DhcpLease(
        val hostname: String,
        val ip: String,
        val mac: String,
        val expiration: LocalDateTime?
) {
    val static: Boolean
        get() {
            return expiration == null
        }
}

data class DhcpStatistics(
        val poolSize: Int,
        val leased: Int,
        val available: Int
)
