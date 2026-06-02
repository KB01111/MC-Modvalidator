package mcmodvalidator.pro.scanner.detectors

import mcmodvalidator.pro.issue.Category
import mcmodvalidator.pro.issue.DetectedIssue
import mcmodvalidator.pro.issue.Severity
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader

class PortingDetector {

    private val obsoleteClassPatterns = listOf(
        Regex("""net\.minecraft\.class_\d+"""),
        Regex("""net\.minecraft\.class_\d+${'$'}class_\d+""")
    )

    fun detect(): List<DetectedIssue> {
        val issues = mutableListOf<DetectedIssue>()
        val loader = FabricLoader.getInstance()

        try {
            val mainContainers = loader.getEntrypointContainers("main", ModInitializer::class.java)
            for (container in mainContainers) {
                val clazz = container.entrypoint.javaClass
                scanClassForObsoleteReferences(clazz, container.provider.metadata.id, issues)
            }
        } catch (_: Exception) {
            // ignore
        }

        return issues
    }

    private fun scanClassForObsoleteReferences(clazz: Class<*>, modId: String, issues: MutableList<DetectedIssue>) {
        val superclass = clazz.superclass
        if (superclass != null && superclass.name.contains("class_")) {
            issues.add(
                DetectedIssue(
                    modId = modId,
                    severity = Severity.ERROR,
                    category = Category.PORTING_OBSOLETE_CLASS,
                    targetClass = clazz.name,
                    message = "Class ${clazz.name} extends obfuscated superclass ${superclass.name}",
                    suggestedFix = "Update superclass reference to unobfuscated Minecraft class name",
                    autoFixable = false
                )
            )
        }

        for (iface in clazz.interfaces) {
            if (iface.name.contains("class_")) {
                issues.add(
                    DetectedIssue(
                        modId = modId,
                        severity = Severity.ERROR,
                        category = Category.PORTING_OBSOLETE_CLASS,
                        targetClass = clazz.name,
                        message = "Class ${clazz.name} implements obfuscated interface ${iface.name}",
                        suggestedFix = "Update interface reference to unobfuscated Minecraft class name",
                        autoFixable = false
                    )
                )
            }
        }

        for (field in clazz.declaredFields) {
            val typeName = field.type.name
            for (pattern in obsoleteClassPatterns) {
                if (pattern.containsMatchIn(typeName)) {
                    issues.add(
                        DetectedIssue(
                            modId = modId,
                            severity = Severity.ERROR,
                            category = Category.PORTING_OBSOLETE_CLASS,
                            targetClass = clazz.name,
                            message = "Field '${field.name}' in ${clazz.name} uses obfuscated type $typeName",
                            suggestedFix = "Replace with unobfuscated class reference",
                            autoFixable = false
                        )
                    )
                    break
                }
            }
        }

        for (method in clazz.declaredMethods) {
            val returnType = method.returnType.name
            for (pattern in obsoleteClassPatterns) {
                if (pattern.containsMatchIn(returnType)) {
                    issues.add(
                        DetectedIssue(
                            modId = modId,
                            severity = Severity.ERROR,
                            category = Category.PORTING_OBSOLETE_CLASS,
                            targetClass = clazz.name,
                            message = "Method '${method.name}' in ${clazz.name} returns obfuscated type $returnType",
                            suggestedFix = "Replace with unobfuscated class reference",
                            autoFixable = false
                        )
                    )
                    break
                }
            }
            for (paramType in method.parameterTypes) {
                for (pattern in obsoleteClassPatterns) {
                    if (pattern.containsMatchIn(paramType.name)) {
                        issues.add(
                            DetectedIssue(
                                modId = modId,
                                severity = Severity.ERROR,
                                category = Category.PORTING_OBSOLETE_CLASS,
                                targetClass = clazz.name,
                                message = "Method '${method.name}' in ${clazz.name} uses obfuscated parameter type ${paramType.name}",
                                suggestedFix = "Replace with unobfuscated class reference",
                                autoFixable = false
                            )
                        )
                        break
                    }
                }
            }
        }
    }

    private fun tryLoadClass(name: String): Class<*>? {
        return try {
            Class.forName(name, false, Thread.currentThread().contextClassLoader)
        } catch (_: Throwable) {
            null
        }
    }
}
