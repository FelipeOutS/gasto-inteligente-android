package com.gastointeligente.app

import java.io.File
import java.io.FileOutputStream

internal data class ReadyDownload(val file: File, val name: String, val mime: String)

/** Bounded streaming: the complete report is never decoded into native memory. */
internal class DownloadAssembler(private val directory: File) {
    companion object {
        const val MAX_BYTES = 25L * 1024 * 1024
        const val CHUNK_BYTES = 32768
        fun safeName(raw: String): String = raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\p{Cntrl}:*?\"<>|]"), "_").trim().trimStart('.')
            .take(160).ifBlank { "relatorio" }
    }

    private var stream: FileOutputStream? = null
    private var pending: ReadyDownload? = null
    private var id: String? = null
    private var expected = 0L
    private var received = 0L

    fun begin(requestId: String, name: String, mime: String, size: Long) {
        check(stream == null) { "Já existe uma exportação em andamento." }
        require(requestId.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        require(size in 0..MAX_BYTES) { "O relatório excede o limite de 25 MB." }
        directory.mkdirs()
        val file = File.createTempFile("report-", ".part", directory)
        try {
            stream = FileOutputStream(file)
            pending = ReadyDownload(file, safeName(name), mime.substringBefore(';')
                .takeIf { it.matches(Regex("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+")) } ?: "application/octet-stream")
            id = requestId
            expected = size
            received = 0
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }

    fun append(requestId: String, bytes: ByteArray) {
        check(id == requestId && stream != null) { "Exportação inválida ou encerrada." }
        require(bytes.size in 1..CHUNK_BYTES && received + bytes.size <= expected) { "Tamanho de relatório inválido." }
        stream!!.write(bytes)
        received += bytes.size
    }

    fun finish(requestId: String): ReadyDownload {
        check(id == requestId && stream != null && received == expected) { "O relatório não foi recebido por completo." }
        stream!!.close()
        stream = null
        id = null
        return requireNotNull(pending).also { pending = null }
    }

    fun cancel() {
        try { stream?.close() } finally {
            stream = null
            pending?.file?.delete()
            pending = null
            id = null
        }
    }
}
