package mcmodvalidator.pro.scanner

import mcmodvalidator.pro.issue.IssueRegistry
import mcmodvalidator.pro.scanner.detectors.KotlinInteropDetector
import mcmodvalidator.pro.scanner.detectors.PortingDetector
import org.slf4j.LoggerFactory

object ModScanner {
    private val logger = LoggerFactory.getLogger("mc-modvalidator-fabric")

    private val kotlinInteropDetector = KotlinInteropDetector()
    private val portingDetector = PortingDetector()

    fun scan() {
        logger.info("[ModValidator] Starting mod scan...")
        IssueRegistry.clear()

        val kotlinIssues = kotlinInteropDetector.detect()
        IssueRegistry.registerAll(kotlinIssues)

        val portingIssues = portingDetector.detect()
        IssueRegistry.registerAll(portingIssues)

        val total = kotlinIssues.size + portingIssues.size
        logger.info("[ModValidator] Scan complete. Found $total issues (${kotlinIssues.size} Kotlin interop, ${portingIssues.size} porting).")

        for (issue in (kotlinIssues + portingIssues)) {
            val level = when (issue.severity) {
                mcmodvalidator.pro.issue.Severity.INFO -> "INFO"
                mcmodvalidator.pro.issue.Severity.WARN -> "WARN"
                mcmodvalidator.pro.issue.Severity.ERROR -> "ERROR"
            }
            logger.info("[ModValidator] [$level][${issue.modId}] ${issue.message} (${issue.targetClass})")
        }
    }
}
