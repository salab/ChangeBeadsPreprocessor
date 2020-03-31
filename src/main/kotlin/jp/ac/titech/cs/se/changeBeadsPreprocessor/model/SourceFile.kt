package jp.ac.titech.cs.se.changeBeadsPreprocessor.model

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.StringBuilder

class SourceFile(private val path: String) {
    val code: String
    init {
        val br = BufferedReader(InputStreamReader(FileInputStream(path)))
        val sb = StringBuilder()

        val file = File(path)
        val lines = file.absoluteFile.readLines().toList()
        sb.append(lines)
        code = sb.toString()
    }
}