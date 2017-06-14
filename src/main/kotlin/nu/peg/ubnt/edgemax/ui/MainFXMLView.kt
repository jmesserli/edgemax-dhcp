package nu.peg.ubnt.edgemax.ui

import javafx.event.EventHandler
import javafx.scene.chart.PieChart
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.util.Duration.millis
import nu.peg.ubnt.edgemax.api.EdgeMaxApi
import nu.peg.ubnt.edgemax.api.EdgeMaxCredentials
import nu.peg.ubnt.edgemax.api.model.DhcpLease
import nu.peg.ubnt.edgemax.api.model.DhcpNetwork
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.util.*

class MainFXMLView : View() {
    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }

    private val api: EdgeMaxApi by lazy {
        val properties = Properties()
        properties.load(Files.newInputStream(Paths.get("secrets.properties")))

        val credentials = EdgeMaxCredentials(properties.getProperty("username"), properties.getProperty("password"))
        EdgeMaxApi(properties.getProperty("baseUrl"), credentials)
    }

    override val root: Pane by fxml("/main.fxml")

    val networkTree by fxid<TreeView<Any?>>()
    val progress by fxid<ProgressIndicator>()
    val propertyList by fxid<ListView<String>>("properties")
    val statsPiePane by fxid<TitledPane>()
    val statsPie by fxid<PieChart>()

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

        statsPiePane.isDisable = false
        statsPiePane.isExpanded = true

        val leased = network.stats.leased
        val available = network.stats.available
        statsPie.data = listOf(
                PieChart.Data("Leased ($leased)", leased.toDouble()),
                PieChart.Data("Available ($available)", available.toDouble())
        ).observable()
    }

    private fun updateForLease(lease: DhcpLease) {
        statsPiePane.isDisable = true
        statsPiePane.isExpanded = false

        propertyList.items = listOf(
                "Hostname: ${lease.hostname}",
                "IP: ${lease.ip}",
                "MAC: ${lease.mac}",
                "${if (lease.static) "Static" else "Expiration: ${lease.expiration!!.format(timeFormatter)}"}"
        ).observable()
    }
}