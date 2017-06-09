package nu.peg.ubnt.edgemax

import nu.peg.ubnt.edgemax.api.EdgeMaxApi
import nu.peg.ubnt.edgemax.api.EdgeMaxCredentials
import nu.peg.ubnt.edgemax.util.KotlinResourceUtil
import java.util.*

fun main(args: Array<String>) {
    val properties = Properties()
    properties.load(KotlinResourceUtil.getResourceStream("secrets.properties"))

    val credentials = EdgeMaxCredentials(properties.getProperty("username"), properties.getProperty("password"))
    val api = EdgeMaxApi("https://rt-2.in.peg.nu", credentials)
    println(api.getDhcpData())
}