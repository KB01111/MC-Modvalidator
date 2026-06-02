package mcmodvalidator.pro.client.ui

import mcmodvalidator.pro.issue.IssueRegistry
import mcmodvalidator.pro.issue.Severity
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ModValidatorScreen(private val parent: Screen?) : Screen(Component.literal("ModValidator")) {

    private val mods = FabricLoader.getInstance().allMods.map { it.metadata.id to IssueRegistry.countByModId(it.metadata.id) }
    private var selectedMod: String? = null
    private var scrollOffset = 0
    private val lineHeight = 14
    private val leftPanelWidth = 170

    override fun init() {
        super.init()
        addRenderableWidget(
            Button.builder(Component.literal("Close")) {
                minecraft.setScreen(parent)
            }.pos(width / 2 - 50, height - 28).size(100, 20).build()
        )
    }

    override fun extractRenderState(guiGraphicsExtractor: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        extractBackground(guiGraphicsExtractor, mouseX, mouseY, partialTick)

        guiGraphicsExtractor.textRenderer().accept(width / 2, 8, title)

        val leftX = 10
        val leftY = 24
        val leftHeight = height - 60
        guiGraphicsExtractor.fill(leftX - 2, leftY - 2, leftX + leftPanelWidth, leftY + leftHeight, 0xAA000000.toInt())

        val maxVisible = (leftHeight / lineHeight).coerceAtLeast(1)
        val visibleCount = maxVisible.coerceAtMost(mods.size)
        scrollOffset = scrollOffset.coerceIn(0, (mods.size - maxVisible).coerceAtLeast(0))

        for (i in 0 until visibleCount) {
            val index = scrollOffset + i
            if (index >= mods.size) break
            val (modId, count) = mods[index]
            val isSelected = modId == selectedMod
            val bgColor = if (isSelected) 0xFF888888.toInt() else 0xFF444444.toInt()
            val textColor = when {
                count == 0 -> 0xAAAAAA
                IssueRegistry.getByModId(modId).any { it.severity == Severity.ERROR } -> 0xFF5555
                IssueRegistry.getByModId(modId).any { it.severity == Severity.WARN } -> 0xFFAA55
                else -> 0x55FF55
            }
            val y = leftY + i * lineHeight
            guiGraphicsExtractor.fill(leftX, y, leftX + leftPanelWidth - 4, y + lineHeight, bgColor)
            guiGraphicsExtractor.textRenderer().accept(leftX + 2, y + 2, Component.literal("$modId ($count)").withColor(textColor))
        }

        val rightX = leftX + leftPanelWidth + 8
        val rightY = 24
        val rightWidth = width - rightX - 10
        val rightHeight = height - 60
        guiGraphicsExtractor.fill(rightX - 2, rightY - 2, rightX + rightWidth, rightY + rightHeight, 0xAA000000.toInt())

        val selected = selectedMod
        if (selected != null) {
            val issues = IssueRegistry.getByModId(selected)
            if (issues.isEmpty()) {
                guiGraphicsExtractor.textRenderer().accept(rightX + 5, rightY + 5, Component.literal("No issues found for $selected").withColor(0xAAAAAA))
            } else {
                var y = rightY + 5
                for (issue in issues) {
                    val severityColor = when (issue.severity) {
                        Severity.INFO -> 0x55AAFF
                        Severity.WARN -> 0xFFAA55
                        Severity.ERROR -> 0xFF5555
                    }
                    val fixTag = if (issue.autoFixable) " [Autofixable]" else ""
                    guiGraphicsExtractor.textRenderer().accept(rightX + 5, y, Component.literal("[${issue.severity}] ${issue.message}$fixTag").withColor(severityColor))
                    y += lineHeight
                    guiGraphicsExtractor.textRenderer().accept(rightX + 15, y, Component.literal("Fix: ${issue.suggestedFix}").withColor(0xAAAAAA))
                    y += lineHeight + 4
                    if (y > rightY + rightHeight - lineHeight) break
                }
            }
        } else {
            guiGraphicsExtractor.textRenderer().accept(rightX + 5, rightY + 5, Component.literal("Select a mod to view issues").withColor(0xAAAAAA))
        }

        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        val leftX = 10
        val leftY = 24
        val leftHeight = height - 60
        val maxVisible = (leftHeight / lineHeight).coerceAtLeast(1)
        val visibleCount = maxVisible.coerceAtMost(mods.size)

        if (mouseX >= leftX && mouseX <= leftX + leftPanelWidth && mouseY >= leftY && mouseY <= leftY + visibleCount * lineHeight) {
            val index = ((mouseY - leftY) / lineHeight).toInt()
            if (index in 0 until visibleCount) {
                val modIndex = scrollOffset + index
                if (modIndex in mods.indices) {
                    selectedMod = mods[modIndex].first
                    return true
                }
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val leftX = 10
        val leftY = 24
        val leftHeight = height - 60

        if (mouseX >= leftX && mouseX <= leftX + leftPanelWidth && mouseY >= leftY && mouseY <= leftY + leftHeight) {
            val maxVisible = (leftHeight / lineHeight).coerceAtLeast(1)
            scrollOffset = (scrollOffset - scrollY.toInt()).coerceIn(0, (mods.size - maxVisible).coerceAtLeast(0))
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }
}
