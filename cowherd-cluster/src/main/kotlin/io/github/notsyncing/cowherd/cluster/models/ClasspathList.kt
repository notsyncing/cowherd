package io.github.notsyncing.cowherd.cluster.models

class ClasspathList(var mainClassName: String,
                    val list: MutableList<FileItem>) {
    constructor() : this("", mutableListOf())

    fun isEmpty() = list.isEmpty()

    fun clear() {
        list.clear()
    }

    fun add(item: FileItem) = list.add(item)
}