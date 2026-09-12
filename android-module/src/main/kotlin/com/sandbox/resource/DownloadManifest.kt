package com.sandbox.resource

/** Metadados mínimos de qualquer artefato baixado com retomada e verificação. */
interface DownloadManifest {
    val url: String
    val sizeBytes: Long
    val sha256: String
}

data class LocalModelManifest(
    val id: String,
    val version: String,
    val format: String,
    val architecture: String,
    override val url: String,
    override val sizeBytes: Long,
    override val sha256: String,
    val license: String,
    val minAppVersion: String
) : DownloadManifest
