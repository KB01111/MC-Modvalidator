package mcmodvalidator.pro.client.keybind

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object ModValidatorKeybinds {
    val category: KeyMapping.Category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("modvalidator", "category"))

    val openScreen: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping(
            "key.modvalidator.open",
            GLFW.GLFW_KEY_F12,
            category
        )
    )
}
