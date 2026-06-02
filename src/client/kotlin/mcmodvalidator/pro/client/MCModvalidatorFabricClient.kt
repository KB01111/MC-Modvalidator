package mcmodvalidator.pro.client

import com.mojang.brigadier.CommandDispatcher
import mcmodvalidator.pro.client.keybind.ModValidatorKeybinds
import mcmodvalidator.pro.client.ui.ModValidatorScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object MCModvalidatorFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        // Trigger lazy initialization of keybind object
        ModValidatorKeybinds.openScreen

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (ModValidatorKeybinds.openScreen.consumeClick()) {
                client.setScreen(ModValidatorScreen(client.screen))
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerClientCommands(dispatcher)
        }
    }

    private fun registerClientCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommands.literal("modvalidator")
                .then(
                    ClientCommands.literal("ui")
                        .executes { ctx ->
                            Minecraft.getInstance().execute {
                                Minecraft.getInstance().setScreen(ModValidatorScreen(Minecraft.getInstance().screen))
                            }
                            ctx.source.sendSuccess({ Component.literal("Opening ModValidator UI...") }, false)
                            1
                        }
                )
        )
    }
}