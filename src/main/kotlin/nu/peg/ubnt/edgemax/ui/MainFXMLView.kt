package nu.peg.ubnt.edgemax.ui

import javafx.event.EventHandler
import javafx.scene.control.ListView
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.Pane
import javafx.util.Duration.millis
import nu.peg.ubnt.edgemax.api.EdgeMaxApi
import nu.peg.ubnt.edgemax.api.EdgeMaxCredentials
import nu.peg.ubnt.edgemax.api.model.DhcpLease
import nu.peg.ubnt.edgemax.api.model.DhcpNetwork
import nu.peg.ubnt.edgemax.util.KotlinResourceUtil
import tornadofx.*
import java.util.*

class MainFXMLView : View() {
    private val api: EdgeMaxApi by lazy {
        val properties = Properties()
        properties.load(KotlinResourceUtil.getResourceStream("secrets.properties"))

        val credentials = EdgeMaxCredentials(properties.getProperty("username"), properties.getProperty("password"))
        EdgeMaxApi(properties.getProperty("baseUrl"), credentials)
    }

    override val root: Pane by fxml("/main.fxml")

    val networkTree by fxid<TreeView<Any?>>()
    val progress by fxid<ProgressIndicator>()
    val propertyList by fxid<ListView<String>>("properties")

    init {
        networkTree.root = TreeItem("DHCP Networks")
        networkTree.root.isExpanded = true
        networkTree.cellFormat {
            text = when (it) {
                is String -> it
                is DhcpNetwork -> "${it.name}: ${it.subnet}"
                is DhcpLease -> "${it.hostname}: ${it.ip}"

                else -> throw IllegalStateException("Type not allowed")
            }
        }

        networkTree.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            val value = newValue.value
            when (value) {
                is DhcpNetwork -> updateForNetwork(value)
                is DhcpLease -> updateForLease(value)
            }
        }

        runAsync {
            api.getDhcpData()
        } ui { (dhcpNetworks) ->
            networkTree.populate { parent ->
                val value = parent.value
                if (parent == networkTree.root) {
                    println("Populating networks")
                    dhcpNetworks
                } else if (value is DhcpNetwork) {
                    parent.isExpanded = true
                    println("Populating leases of ${value.name}")
                    value.leases
                } else null
            }

            progress.fade(millis(500.0), 0.0).onFinished = EventHandler { progress.hide() }
        }
    }

    private fun updateForNetwork(network: DhcpNetwork) {
        propertyList.items = listOf(
                "Name: ${network.name}",
                "Subnet: ${network.subnet}",
                "Range: ${network.range.start} - ${network.range.end}",
                "Gateway: ${network.gateway}",
                "DNS Servers: ${network.dnsServers.joinToString(", ")}",
                "Lease Time: ${network.leaseTime} s"
        ).observable()
    }

    private fun updateForLease(lease: DhcpLease) {
        propertyList.items = listOf(
                "Hostname: ${lease.hostname}",
                "IP: ${lease.ip}",
                "MAC: ${lease.mac}",
                "${if (lease.static) "Static" else "Expiration: ${lease.expiration}"}"
        ).observable()
    }
}