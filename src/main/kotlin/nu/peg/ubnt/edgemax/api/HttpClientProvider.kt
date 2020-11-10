package nu.peg.ubnt.edgemax.api

import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts

object HttpClientProvider {
    /** TODO Ignores SSL errors: yes, this is very unsafe */
    private val sslSocketFactory: SSLConnectionSocketFactory by lazy {
        val sslContext = SSLContexts.custom().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build()
        SSLConnectionSocketFactory(sslContext) { _, _ -> true }
    }

    private fun createCookieStore() = BasicCookieStore()

    fun createClient(): CloseableHttpClient = HttpClients.custom()
            .useSystemProperties()
            .setSSLSocketFactory(sslSocketFactory)
            .setDefaultCookieStore(createCookieStore())
            .build()
}
