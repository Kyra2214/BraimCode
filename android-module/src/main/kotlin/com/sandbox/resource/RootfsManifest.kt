package com.sandbox.resource

/**
 * Descreve a versão do rootfs disponível para download.
 * Gerado a partir do rootfs-builder/build.sh (Fase 0.1) e hospedado
 * separadamente do APK (CDN, release do GitHub, etc).
 */
data class RootfsManifest(
    val version: String,
    val arch: String,
    val distro: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String,
    val minAppVersion: String
)
