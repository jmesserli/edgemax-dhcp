package nu.peg.ubnt.edgemax.util

import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.message.BasicNameValuePair

fun HttpPost.withFormParams(vararg params: Pair<String, String>): HttpPost {
    val nameValuePairList = params.map { BasicNameValuePair(it.first, it.second) }
    val formEntity = UrlEncodedFormEntity(nameValuePairList, Consts.UTF_8)
    entity = formEntity

    return this
}

fun <T : HttpRequestBase> T.withReferer(referer: String): T {
    addHeader("Referer", referer)
    return this
}