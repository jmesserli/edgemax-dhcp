package nu.peg.ubnt.edgemax

import javafx.application.Application
import nu.peg.ubnt.edgemax.ui.TornadoDhcpMain

fun main(args: Array<String>) {
    Application.launch(TornadoDhcpMain::class.java)

//    val networks = api.getDhcpData().dhcpNetworks
//    networks.forEach {
//        println("${it.name}: ${it.subnet}, DHCP ${it.range.start} - ${it.range.end}")
//        it.leases.forEach {
//            println("   ${if (it.hostname.isEmpty()) "<unknown>" else it.hostname}: ${it.ip}, ${it.mac}, ${if (it.static) "static" else "until ${it.expiration}"}")
//        }
//    }
}