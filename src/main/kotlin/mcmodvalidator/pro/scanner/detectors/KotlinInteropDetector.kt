package mcmodvalidator.pro.scanner.detectors

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mcmodvalidator.pro.issue.Category
import mcmodvalidator.pro.issue.DetectedIssue
import mcmodvalidator.pro.issue.Severity
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

class KotlinInteropDetector {

    fun detect(): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        val loader = FabricLoader.getInstance()

        for (mod in loader.allMods) {
            val modId = mod.metadata.id
            val jsonText = readFabricModJson(mod) ?: continue
            val json = try {
                JsonParser.parseString(jsonText).asJsonObject
            } catch (_: Exception) {
                continue
            }

            val entrypointsEl = json.get("entrypoints") ?: continue
            if (!entrypointsEl.isJsonObject) continue
            val entrypointsObj = entrypointsEl.asJsonObject

            for ((type, epEl) in entrypointsObj.entrySet()) {
                if (!epEl.isJsonArray) continue
                val epArray = epEl.asJsonArray

                for (epEntry in epArray) {
                    val (value, adapter) = when {
                        epEntry.isJsonObject -> {
                            val epObj = epEntry.asJsonObject
                            val v = epObj.get("value")?.asString ?: continue
                            val a = epObj.get("adapter")?.asString
                            v to a
                        }
                        epEntry.isJsonPrimitive -> {
                            val v = epEntry.asString
                            v to null
                        }
                        else -> continue
                    }

                    val clazz = tryLoadClass(value)
                    if (clazz != null && isKotlinClass(clazz)) {
                        if (adapter != "kotlin") {
                            issues.add(
                                DetectedIssue(
                                    modId = modId,
                                    severity = Severity.WARN,
                                    category = Category.KOTLIN_INTEROP,
                                    targetClass = value,
                                    message = "Kotlin entrypoint '$value' (type: $type) is missing adapter: \"kotlin\" in fabric.mod.json",
                                    suggestedFix = "Add \"adapter\": \"kotlin\" to the entrypoint definition in fabric.mod.json",
                                    autoFixable = false
                                )
                            )
                        }

                        if ((type == "main" || type == "client") && !isKotlinObject(clazz)) {
                            val expectedType = when (type) {
                                "main" -> "ModInitializer"
                                "client" -> "ClientModInitializer"
                                else -> type
                            }
                            issues.add(
                                DetectedIssue(
                                    modId = modId,
                                    severity = Severity.ERROR,
                                    category = Category.KOTLIN_INTEROP,
                                    targetClass = value,
                                    message = "Kotlin class '$value' registered as '$expectedType' entrypoint is not an `object`. Entrypoints must be singletons.",
                                    suggestedFix = "Change `class $value` to `object $value`",
                                    autoFixable = false
                                )
                            )
                        }

                        if (isKotlinObject(clazz)) {
                            for (method in clazz.declaredMethods) {
                                val params = method.parameterTypes
                                if (params.isNotEmpty() && params[0].name.startsWith("net.fabricmc.fabric.api.event") && !method.isAnnotationPresent(JvmStatic::class.java)) {
                                    issues.add(
                                        DetectedIssue(
                                            modId = modId,
                                            severity = Severity.WARN,
                                            category = Category.KOTLIN_INTEROP,
                                            targetClass = value,
                                            message = "Event handler '${method.name}' in object '$value' is missing @JvmStatic",
                                            suggestedFix = "Add @JvmStatic annotation to '${method.name}'",
                                            autoFixable = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            val mainContainers = loader.getEntrypointContainers("main", ModInitializer::class.java)
            for (container in mainContainers) {
                val clazz = container.entrypoint.javaClass
                if (isKotlinObject(clazz)) {
                    for (method in clazz.declaredMethods) {
                        val params = method.parameterTypes
                        if (params.isNotEmpty() && params[0].name.startsWith("net.fabricmc.fabric.api.event") && !method.isAnnotationPresent(JvmStatic::class.java)) {
                            issues.add(
                                DetectedIssue(
                                    modId = container.provider.metadata.id,
                                    severity = Severity.WARN,
                                    category = Category.KOTLIN_INTEROP,
                                    targetClass = clazz.name,
                                    message = "Event handler '${method.name}' in object '${clazz.name}' is missing @JvmStatic",
                                    suggestedFix = "Add @JvmStatic annotation to '${method.name}'",
                                    autoFixable = false
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore
        }

        return issues
    }

    private fun readFabricModJson(mod: net.fabricmc.loader.api.ModContainer): String? {
        val path = mod.findPath("fabric.mod.json").orElse(null) ?: return null
        return try {
            Files.readString(path)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryLoadClass(name: String): Class<*>? {
        return try {
            Class.forName(name, false, Thread.currentThread().contextClassLoader)
        } catch (_: Throwable) {
            null
        }
    }

    private fun isKotlinClass(clazz: Class<*>): Boolean {
        return clazz.getAnnotation(kotlin.Metadata::class.java) != null
    }

    private fun isKotlinObject(clazz: Class<*>): Boolean {
        return clazz.declaredFields.any { it.name == "INSTANCE" && it.type == clazz && java.lang.reflect.Modifier.isStatic(it.modifiers) }
    }
}
