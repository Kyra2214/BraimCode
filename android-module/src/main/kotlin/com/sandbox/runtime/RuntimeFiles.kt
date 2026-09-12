package com.sandbox.runtime

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

/** Operações de arquivo que não seguem symlinks. */
internal object RuntimeFiles {
    fun existsNoFollow(file: File): Boolean = try {
        Os.lstat(file.absolutePath)
        true
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }

    fun isDirectoryNoFollow(file: File): Boolean = try {
        OsConstants.S_ISDIR(Os.lstat(file.absolutePath).st_mode)
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }
}
