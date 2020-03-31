package jp.ac.titech.cs.se.changeBeadsPreprocessor.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.difflib.patch.AbstractDelta
import java.lang.StringBuilder
import java.util.*

class CommitData(
    @JsonProperty("commitHash") val commitHash: String,
    @JsonFormat(
        shape = JsonFormat.Shape.STRING,
        pattern = "yyyy-MM-dd HH:mm:ss"
    )
    @JsonProperty("editTime") val editTime: Date,
    @JsonProperty("message") val message: String?,
    @JsonProperty("editFile") val editFile: MutableMap<String,FileData> = mutableMapOf()
) {
    fun setEditClass(filePath: String, diffData: DiffData, sourceASTData: SourceASTData) {
        diffData.diffMatchPatchChunkList.forEach {
            when (it.type) {
                ChunkType.INSERT -> {
                    setEditClassMethod(filePath, it.newStart, it.length, sourceASTData.classData)
                }
                ChunkType.DELETE -> {
                    setEditClassMethod(filePath, it.oldStart, it.length, sourceASTData.classData)
                }
            }
        }
    }

    // 変更範囲の情報とASTから変更されたクラス・メソッドを抽出しset
    private fun setEditClassMethod(filePath: String, chunkStart: Int, chunkLength: Int, classData: MutableList<ClassASTData>){
        classData.forEach {
            val classEnd = it.start + it.length - 1
            val chunkEnd = chunkStart + chunkLength - 1
            if (chunkStart <= classEnd && it.start <= chunkEnd) {
                val methodData = MethodData()
                it.methodData.forEach{ method ->
                    val methodEnd = method.start + method.length - 1
                    if (chunkStart <= methodEnd && method.start <= chunkEnd) {
                        methodData.editMethod.add(method.name)
                    }
                }

                var fileData = this.editFile[filePath]
                if (fileData == null) {
                    fileData = FileData()
                }

                if (fileData.editClass[it.name] == null) {
                    fileData.editClass[it.name] = methodData
                } else {
                    fileData.editClass[it.name]?.editMethod?.addAll(methodData.editMethod)
                }
            }
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.appendln("┌----------------------commit data--------------------┐")
        builder.appendln("id: ${this.commitHash}")
        builder.appendln("author date: ${this.editTime}")
        builder.append("message: ${this.message}")
        builder.appendln("editFileList:")
        editFile.forEach{ (key, value) ->
            builder.appendln("[$key]")
            builder.appendln(value.toString())
        }
        builder.appendln("└-----------------------------------------------------┘")
        return builder.toString()
    }
}

data class FileData(
    @JsonProperty("editClass") val editClass: MutableMap<String, MethodData> = mutableMapOf(),
    @JsonProperty("patches") val patches: MutableList<PatchData> = mutableListOf()
) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.appendln("edit class method:$editClass")
        builder.appendln("-----diff-----")
        patches.forEach{
            builder.appendln(it.toString())
        }

        return builder.toString()
    }
}

class MethodData(
    @JsonProperty("editMethod") val editMethod: MutableSet<String> = mutableSetOf()
) {
    override fun toString(): String {
        return editMethod.toString()
    }
}

enum class DiffOrigin { Add, Remove, NoChange }

// Clustererで処理しやすいよう行ごとのデータ構造で保存
class PatchData(
    @JsonProperty("hunks") val hunks: MutableList<HunkData> = mutableListOf(),
    @JsonProperty("oldFile") val oldFile: String,
    @JsonProperty("newFile") val newFile: String
) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.appendln("old:$oldFile new:$newFile")
        hunks.forEach{
            it.lines.forEach { line ->
                val sign = when(line.origin) {
                    DiffOrigin.Add -> "+"
                    DiffOrigin.Remove -> "-"
                    DiffOrigin.NoChange -> " "
                }
                builder.appendln("$sign | ${line.content}")
            }
        }
        return builder.toString()
    }
}

class HunkData(
    @JsonProperty("lines") var lines: MutableList<LineDiffData> = mutableListOf(),
    @JsonProperty("newLines") val newLines: Int,
    @JsonProperty("newStart") val newStart: Int,
    @JsonProperty("oldLines") val oldLines: Int,
    @JsonProperty("oldStart") val oldStart: Int
)

/*
oldLineNo: origin = DiffOrigin.INSERT の場合，-1
newLineNo: origin = DiffOrigin.DELETE の場合，-1
 */
class LineDiffData(
    @JsonProperty("origin") val origin: DiffOrigin,
    @JsonProperty("content") val content: String,
    @JsonProperty("oldLineNo") val oldLineNo: Int,
    @JsonProperty("newLineNo") val newLineNo: Int
) {
    override fun toString(): String {
        return content
    }
}

fun diffUtilsDeltasToPatch(
    deltas: List<AbstractDelta<String>>,
    oldFileName: String,
    newFileName: String,
    newContentList: List<String>
): PatchData {
    val patch = PatchData(oldFile = oldFileName, newFile = newFileName)

    deltas.forEach { lineDelta ->
        val hunk = deltaToHunk(lineDelta)
        // Diffの前後の行を追加
        val allHunkLines = getBeforeLines(hunk.newStart-1, newContentList)
        allHunkLines.addAll(hunk.lines)
        allHunkLines.addAll(getAfterLines(hunk.newStart + hunk.newLines, newContentList))
        hunk.lines = allHunkLines
        patch.hunks.add(hunk)
    }

    return patch
}

fun getBeforeLines(beforeEndLineNo: Int, newContentList: List<String>): MutableList<LineDiffData> {
    val lines = mutableListOf<LineDiffData>()
    if (beforeEndLineNo <= 1) {
        return lines
    }
    val beforeLines = 3
    val beforeStartLineNo = if (beforeEndLineNo <= beforeLines) {
        1
    } else {
        beforeEndLineNo - beforeLines + 1
    }
    val rawLines = newContentList.slice((beforeStartLineNo-1) until beforeEndLineNo)
    var lineNo = beforeStartLineNo
    rawLines.forEach { rawLine ->
        lines.add(LineDiffData(DiffOrigin.NoChange, rawLine, lineNo, lineNo))
        lineNo++
    }

    println("before${beforeStartLineNo}-${beforeEndLineNo}:${lines}")
    return lines
}

fun getAfterLines(afterStartLineNo: Int, newContentList: List<String>): MutableList<LineDiffData> {
    val lines = mutableListOf<LineDiffData>()
    if (afterStartLineNo >= newContentList.size) {
        return lines
    }
    val afterLines = 3
    val afterEndLineNo = if (newContentList.size - afterStartLineNo + 1 <= afterLines) {
        newContentList.size
    } else {
        afterStartLineNo + afterLines - 1
    }
    val rawLines = newContentList.slice((afterStartLineNo-1) until afterEndLineNo)
    var lineNo = afterStartLineNo
    rawLines.forEach { rawLine ->
        lines.add(LineDiffData(DiffOrigin.NoChange, rawLine, lineNo, lineNo))
        lineNo++
    }

    println("after${afterStartLineNo}-${afterEndLineNo}:${lines}")
    return lines
}

fun deltaToHunk(delta: AbstractDelta<String>): HunkData {
    // positionはsource, targetの配列内の番号を返す（0始まり）ため1を足して行番号と一致させる
    val hunk = HunkData(oldStart = delta.source.position + 1,
        oldLines = delta.source.lines.size,
        newStart = delta.target.position + 1,
        newLines = delta.target.lines.size)
    val sourceIterator = delta.source.lines.iterator()
    val targetIterator = delta.target.lines.iterator()
    var sourceLine: String
    var targetLine: String
    var sourceLineNo = hunk.oldStart
    var targetLineNo = hunk.newStart
    while (sourceIterator.hasNext() && targetIterator.hasNext()) {
        sourceLine = sourceIterator.next()
        targetLine = targetIterator.next()
        hunk.lines.add(LineDiffData(DiffOrigin.Remove, sourceLine, sourceLineNo, -1))
        hunk.lines.add(LineDiffData(DiffOrigin.Add, targetLine, -1, targetLineNo))
        sourceLineNo++
        targetLineNo++
    }

    while (sourceIterator.hasNext()) {
        sourceLine = sourceIterator.next()
        hunk.lines.add(LineDiffData(DiffOrigin.Remove, sourceLine, sourceLineNo, -1))
        sourceLineNo++
    }

    while (targetIterator.hasNext()) {
        targetLine = targetIterator.next()
        hunk.lines.add(LineDiffData(DiffOrigin.Add, targetLine, -1, targetLineNo))
        targetLineNo++
    }

    return hunk
}
