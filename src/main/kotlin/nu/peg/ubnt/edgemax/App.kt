package nu.peg.ubnt.edgemax

import javafx.application.Application
import nu.peg.ubnt.edgemax.ui.TornadoDhcpMain
import nu.peg.ubnt.edgemax.util.KotlinResourceUtil
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JOptionPane

fun main(args: Array<String>) {
    val secretsPath = Paths.get("secrets.properties")
    if (!Files.exists(secretsPath)) {
        val templateStream = KotlinResourceUtil.getResourceStream("secrets.properties.template")
        Files.copy(templateStream, secretsPath)

        JOptionPane.showMessageDialog(null, "A secrets.properties file has been created in the current directory. Please fill it with the needed data")
        return
    }

    Application.launch(TornadoDhcpMain::class.java)
}