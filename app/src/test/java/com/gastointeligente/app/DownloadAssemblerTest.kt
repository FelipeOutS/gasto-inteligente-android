package com.gastointeligente.app

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadAssemblerTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun binaryReportArrivesWithoutChangingBytesOrName() {
        val assembler = DownloadAssembler(directory.root)
        val bytes = ByteArray(70000) { (it % 251).toByte() }
        assembler.begin("test", "gastos-setembro.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes.size.toLong())
        bytes.toList().chunked(32768).forEach { assembler.append("test", it.toByteArray()) }
        val file = assembler.finish("test")
        assertArrayEquals(bytes, file.file.readBytes())
        assertEquals("gastos-setembro.xlsx", file.name)
    }

    @Test fun incompleteReportCannotBePresentedAsSaved() {
        val assembler = DownloadAssembler(directory.root)
        assembler.begin("a", "relatorio.csv", "text/csv;charset=utf-8", 9)
        assembler.append("a", byteArrayOf(1, 2))
        assertThrows(IllegalStateException::class.java) { assembler.finish("a") }
        assembler.cancel()
        assertTrue(directory.root.listFiles()!!.isEmpty())
    }

    @Test fun wrongRequestIdAndOversizedChunkCannotModifyReport() {
        val assembler = DownloadAssembler(directory.root)
        assembler.begin("a", "arquivo.pdf", "application/pdf", 2)
        assertThrows(IllegalStateException::class.java) { assembler.append("b", byteArrayOf(1)) }
        assertThrows(IllegalArgumentException::class.java) { assembler.append("a", ByteArray(32769)) }
        assembler.append("a", byteArrayOf(1, 2))
        assertArrayEquals(byteArrayOf(1, 2), assembler.finish("a").file.readBytes())
    }

    @Test fun limitsAreCheckedBeforeCreatingAnyFile() {
        val assembler = DownloadAssembler(directory.root)
        assertThrows(IllegalArgumentException::class.java) { assembler.begin("a", "huge", "text/plain", DownloadAssembler.MAX_BYTES + 1) }
        assertTrue(directory.root.listFiles()!!.isEmpty())
    }

    @Test fun cancelAllowsANewDownloadAndZeroByteFilesAreValid() {
        val assembler = DownloadAssembler(directory.root)
        assembler.begin("a", "old.csv", "text/csv", 9)
        assembler.cancel()
        assembler.begin("b", "empty.csv", "text/csv;charset=utf-8", 0)
        val ready = assembler.finish("b")
        assertEquals(0, ready.file.length())
        assertEquals("text/csv", ready.mime)
    }

    @Test fun namesCannotEscapeDestinationOrContainControlCharacters() {
        assertEquals("relatorio.csv", DownloadAssembler.safeName("../../relatorio.csv"))
        assertEquals("relatorio.csv", DownloadAssembler.safeName("C:\\private\\relatorio.csv"))
        assertEquals("a_b.csv", DownloadAssembler.safeName("a\nb.csv"))
        assertEquals("relatorio", DownloadAssembler.safeName(".."))
    }
}
