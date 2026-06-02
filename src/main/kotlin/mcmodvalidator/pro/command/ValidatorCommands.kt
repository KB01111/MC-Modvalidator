package mcmodvalidator.pro.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import mcmodvalidator.pro.issue.IssueRegistry
import mcmodvalidator.pro.scanner.ModScanner
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object ValidatorCommands {

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("modvalidator")
                .then(
                    Commands.literal("scan")
                        .executes { ctx ->
                            ModScanner.scan()
                            ctx.source.sendSuccess({ Component.literal("ModValidator scan complete. Check logs for details.") }, false)
                            1
                        }
                )
                .then(
                    Commands.literal("list")
                        .then(
                            Commands.argument("modid", StringArgumentType.word())
                                .executes { ctx ->
                                    val modId = StringArgumentType.getString(ctx, "modid")
                                    listIssues(ctx, modId)
                                }
                        )
                        .executes { ctx ->
                            ctx.source.sendSuccess({ Component.literal("Usage: /modvalidator list <modid>") }, false)
                            1
                        }
                )
        )
    }

    private fun listIssues(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, modId: String): Int {
        val issues = IssueRegistry.getByModId(modId)
        if (issues.isEmpty()) {
            ctx.source.sendSuccess({ Component.literal("No issues found for mod '$modId'. Run /modvalidator scan first.") }, false)
            return 1
        }

        ctx.source.sendSuccess({ Component.literal("Issues for mod '$modId' (${issues.size}):") }, false)
        for (issue in issues) {
            val severityTag = "[${issue.severity}]"
            val component = Component.literal("$severityTag ${issue.message} [${issue.category}]")
            ctx.source.sendSuccess({ component }, false)
        }
        return 1
    }
}
