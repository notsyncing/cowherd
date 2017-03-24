package io.github.notsyncing.cowherd.deploy

import io.github.notsyncing.cowherd.deploy.commands.ServerCommands
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.completer.FileNameCompleter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

class CowherdDeployApp(val args: Array<String>) {
    companion object {
        private fun checkForDependencies(): Boolean {
            val rsync = ProcessBuilder()
                    .command("rsync", "--version")
                    .inheritIO()
                    .start()

            val r = rsync.waitFor()

            if (r != 0) {
                println("ERROR: rsync not found!")
                return false
            }

            return true
        }

        @JvmStatic
        fun main(args: Array<String>) {
            if (!checkForDependencies()) {
                System.exit(1)
                return
            }

            val app = CowherdDeployApp(args)
            app.run()
        }
    }

    private val commands = listOf(ServerCommands(this))

    private val terminal: Terminal
    private val reader: LineReader

    private var stop = false

    init {
        terminal = TerminalBuilder.builder()
                .build()

        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(FileNameCompleter())
                .history(DefaultHistory())
                .parser(DefaultParser())
                .build()
    }

    fun run() {
        while (!stop) {
            val rawLine = reader.readLine("> ")
            val cmdLine = reader.parsedLine ?: continue

            dispatchCommand(cmdLine.words())
        }
    }

    private fun dispatchCommand(words: List<String>) {
        if (words.isEmpty()) {
            return
        }

        val cmd = words[0]

        if (cmd.isNullOrBlank()) {
            return
        }

        val args = mutableListOf<String>()

        if (words.size > 1) {
            args.addAll(words.subList(1, words.lastIndex + 1))
        }

        for (c in commands) {
            val m = c::class.java.methods.firstOrNull { it.isAnnotationPresent(Command::class.java) && it.name == cmd } ?: continue

            try {
                m(c, *args.toTypedArray())
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return
        }

        println("Unknown command $cmd")
    }

    fun stop() {
        stop = true
    }
}