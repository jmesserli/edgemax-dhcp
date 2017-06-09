package nu.peg.ubnt.edgemax.api

import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import javax.net.ssl.HostnameVerifier

object HttpClientProvider {
    /** TODO Ignores SSL errors: yes, this is very unsafe */
    private val sslSocketFactory: SSLConnectionSocketFactory by lazy {
        val sslContext = SSLContexts.custom().loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build()
        val socketFactory = SSLConnectionSocketFactory(sslContext, HostnameVerifier { _, _ -> true })
        return@lazy socketFactory
    }

    val cookieStore = BasicCookieStore()

    fun createClient(): CloseableHttpClient = HttpClients.custom()
            .useSystemProperties()
            .setSSLSocketFactory(sslSocketFactory)
            .setDefaultCookieStore(cookieStore)
            .build()
}