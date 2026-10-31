package com.gastointeligente.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class BackupRulesTest {
    private fun parse(path: String) = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }.newDocumentBuilder().parse(File("src/main/$path"))

    @Test fun manifestReferencesBothBackupPolicies() {
        val app = parse("AndroidManifest.xml").getElementsByTagName("application").item(0) as Element
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals("false", app.getAttributeNS(ns, "allowBackup"))
        assertEquals("@xml/backup_rules", app.getAttributeNS(ns, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(ns, "dataExtractionRules"))
    }

    @Test fun localDataExcludedFromCloudAndDeviceTransfers() {
        val expected = setOf("root", "file", "database", "sharedpref", "external", "device_root", "device_file", "device_database", "device_sharedpref")
        for ((file, tags) in listOf("backup_rules.xml" to listOf("full-backup-content"), "data_extraction_rules.xml" to listOf("cloud-backup", "device-transfer"))) {
            val doc = parse("res/xml/$file")
            for (tag in tags) {
                val sections = doc.getElementsByTagName(tag)
                assertEquals(1, sections.length)
                val section = sections.item(0) as Element
                assertEquals(0, section.getElementsByTagName("include").length)
                val rules = section.getElementsByTagName("exclude")
                val actual = (0 until rules.length).map { rules.item(it) as Element }
                assertEquals(expected, actual.map { it.getAttribute("domain") }.toSet())
                assertTrue(actual.all { it.getAttribute("path") == "." })
            }
        }
    }
}
