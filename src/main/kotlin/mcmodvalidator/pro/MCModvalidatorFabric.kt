package mcmodvalidator.pro

import mcmodvalidator.pro.command.ValidatorCommands
import mcmodvalidator.pro.scanner.ModScanner
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object MCModvalidatorFabric : ModInitializer {
    private val logger = LoggerFactory.getLogger("mc-modvalidator-fabric")

    override fun onInitialize() {
        logger.info("MC-Modvalidator initialized.")
        ValidatorCommands.register()
        ModScanner.scan()
    }
}