package nu.peg.ubnt.edgemax.api

import com.beust.klaxon.*
import nu.peg.ubnt.edgemax.api.model.DhcpData
import nu.peg.ubnt.edgemax.api.model.DhcpLease
import nu.peg.ubnt.edgemax.api.model.DhcpNetwork
import nu.peg.ubnt.edgemax.api.model.IPRange
import nu.peg.ubnt.edgemax.util.withFormParams
import nu.peg.ubnt.edgemax.util.withReferer
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.CloseableHttpClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EdgeMaxApi(val baseUrl: String, private val credentials: EdgeMaxCredentials) {
    companion object {
        private val expirationFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    }

    private val httpClient: CloseableHttpClient = HttpClientProvider.createClient()

    init {
        login()
    }

    private fun login() {
        val post = HttpPost(baseUrl).withFormParams(
                "username" to credentials.username,
                "password" to credentials.password
        )

        val response = httpClient.execute(post)
        // Returns a 200 if the login is invalid and 302 Found if the login was successful
        if (response.statusLine.statusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw InvalidCredentialsException()
        }
    }

    fun getDhcpData(): DhcpData {
        val dynamicLeasesJson = getDynamicLeases() ?: throw EdgeMaxApiException("Could not load dynamic lease data")
        val poolGroupedDynamicLeases = extractDhcpLeasesFromDynamicLeases(dynamicLeasesJson)

        val dhcpInitialDataJson = getDhcpInitialData() ?: throw EdgeMaxApiException("Could not load dhcp initial data")
        val poolGroupedStaticLeases = extractDhcpLeasesFromStaticMappings(dhcpInitialDataJson)

        val poolGroupedLeases = mutableMapOf<String, List<DhcpLease>>()
        poolGroupedLeases.putAll(poolGroupedDynamicLeases)
        poolGroupedStaticLeases.entries.forEach {
            poolGroupedLeases.merge(it.key, it.value, { existingList, newList ->
                val leaseList = mutableListOf<DhcpLease>()
                leaseList.addAll(existingList)
                leaseList.addAll(newList)

                return@merge leaseList
            })
        }

        return DhcpData(extractDhcpNetworksAndMerge(dhcpInitialDataJson, poolGroupedLeases))
    }

    private fun parseExpiration(timeString: String) =
            LocalDateTime.parse(timeString, expirationFormatter)

    private fun getDhcpInitialData(): JsonObject? {
        val get = HttpGet("$baseUrl/api/edge/get.json").withReferer(baseUrl)
        val response = httpClient.execute(get)
        val responseObject = Parser().parse(response.entity.content) as? JsonObject

        return responseObject?.obj("GET")?.obj("service")?.obj("dhcp-server")?.obj("shared-network-name")
    }

    private fun extractDhcpNetworksAndMerge(dhcpInitialDataJson: JsonObject, leases: Map<String, List<DhcpLease>>): List<DhcpNetwork> {
        return dhcpInitialDataJson.entries.map { it as MutableMap.MutableEntry<String, JsonObject> }
                .map {
                    val subnetObj = (it.value.obj("subnet")?.entries?.first()!! as MutableMap.MutableEntry<String, JsonObject>)
                    val gateway = subnetObj.value.string("default-router")!!
                    val leaseTime = subnetObj.value.string("lease")!!.toInt()
                    val dnsServers = subnetObj.value.array<String>("dns-server")!!.value

                    val rangeStartEntry = subnetObj.value.obj("start")?.entries?.first()!! as MutableMap.MutableEntry<String, JsonObject>
                    val rangeStop = rangeStartEntry.value.string("stop")!!

                    val ipRange = IPRange(rangeStartEntry.key, rangeStop)

                    DhcpNetwork(it.key, subnetObj.key, gateway, leaseTime, dnsServers, ipRange, leases[it.key]!!)
                }
    }

    private fun extractDhcpLeasesFromStaticMappings(staticMappingsJson: JsonObject): Map<String, List<DhcpLease>> {
        fun getStaticMappings(outerJson: JsonObject): JsonObject {
            return (outerJson.obj("subnet")?.entries?.first()?.value as JsonObject?)?.obj("static-mapping")!!
        }

        return staticMappingsJson.entries
                .map { it.key to getStaticMappings(it.value as JsonObject) }
                .flatMap {
                    it.second.entries
                            .map { it as MutableMap.MutableEntry<String, JsonObject> }
                            .map { DhcpLease(it.key, it.value.string("ip-address")!!, it.value.string("mac-address")!!, null) }
                            .map { lease -> it.first to lease }
                }.groupBy({ it.first }, { it.second })
    }


    private fun getDynamicLeases(): JsonObject? {
        val get = HttpGet("$baseUrl/api/edge/data.json?data=dhcp_leases").withReferer(baseUrl)
        val response = httpClient.execute(get)
        val responseObject = Parser().parse(response.entity.content) as? JsonObject

        return responseObject?.obj("output")?.obj("dhcp-server-leases")
    }

    private fun extractDhcpLeasesFromDynamicLeases(dynamicLeasesJson: JsonObject): Map<String, List<DhcpLease>> {
        return dynamicLeasesJson.entries
                .filter { (it.value as? JsonObject) != null }
                .map { it as MutableMap.MutableEntry<String, JsonObject> }
                .flatMap { it.value.entries }
                .map { it as MutableMap.MutableEntry<String, JsonObject> }
                .map {
                    val value = it.value
                    value.string("pool")!! to DhcpLease(
                            value.string("client-hostname")!!,
                            it.key,
                            value.string("mac")!!,
                            parseExpiration(value.string("expiration")!!)
                    )
                }.groupBy({ it.first }, { it.second })
    }
}

data class EdgeMaxCredentials(val username: String, val password: String)

open class EdgeMaxApiException(message: String, throwable: Throwable? = null) : Exception(message, throwable)
class InvalidCredentialsException : EdgeMaxApiException("The supplied credentials were invalid")