package your.app.package.runtime

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

/**
 * Checagens de existência que não seguem symlinks (lstat), usadas para não
 * ser enganado por links quebrados durante a preparação do runner.
 */
internal object RuntimeFiles {

    fun existsNoFollow(file: File): Boolean = try {
        Os.lstat(file.absolutePath)
        true
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }

    fun isDirectoryNoFollow(file: File): Boolean = try {
        val stat = Os.lstat(file.absolutePath)
        OsConstants.S_ISDIR(stat.st_mode)
    } catch (error: ErrnoException) {
        if (error.errno == OsConstants.ENOENT) false else throw error
    }
}
