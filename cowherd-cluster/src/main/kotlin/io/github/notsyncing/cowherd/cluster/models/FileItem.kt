package io.github.notsyncing.cowherd.cluster.models

import io.github.notsyncing.cowherd.cluster.enums.FileItemType
import java.nio.file.Files
import java.nio.file.Path

data class FileItem(var path: String,
                    var type: FileItemType,
                    var checksum: String) {
    constructor() : this("", FileItemType.Unknown, "")

    constructor(path: Path, checksum: String) :
            this(path.toAbsolutePath().toString(),
                    if (Files.isDirectory(path)) FileItemType.Directory else FileItemType.File,
                    checksum)
}