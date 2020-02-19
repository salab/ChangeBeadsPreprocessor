package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch
import java.lang.StringBuilder
import java.util.*

class DiffData (val fileName: String, val filePath: String) {
    val diffMatchPatchChunkList: MutableList<DiffMatchPatchChunk> = mutableListOf()
    constructor(fileName: String, filePath: String, diffList: LinkedList<DiffMatchPatch.Diff>)
            : this(fileName, filePath) {
        var oldOffset = 0
        var newOffset = 0

        diffList.forEach {
            when (it.operation) {
                // TODO: DELETEとINSERTが連続してる場合の処理を要検討
                // [(DELETE: oldStart=221 newStart=221 length=3 content=bar), (INSERT: oldStart=224 newStart=221 length=9 content=nextState)]
                // INSERTの方のoldStartは221の方が正確（多分今のままでも問題ないけど）
                DiffMatchPatch.Operation.DELETE -> {
                    diffMatchPatchChunkList.add(DiffMatchPatchChunk(ChunkType.DELETE, oldOffset, newOffset, it.text.length, it.text))
                    oldOffset += it.text.length
                }
                DiffMatchPatch.Operation.INSERT -> {
                    diffMatchPatchChunkList.add(DiffMatchPatchChunk(ChunkType.INSERT, oldOffset, newOffset, it.text.length, it.text))
                    newOffset += it.text.length
                }
                DiffMatchPatch.Operation.EQUAL -> {
                    oldOffset += it.text.length
                    newOffset += it.text.length
                }
            }
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.appendln("$filePath  $fileName")
        builder.append(diffMatchPatchChunkList)
        return builder.toString()
    }
}

enum class ChunkType { DELETE, INSERT }

data class DiffMatchPatchChunk (val type: ChunkType, val oldStart: Int, val newStart: Int, val length: Int, val content: String) {
    override fun toString(): String {
        return "($type: oldStart=$oldStart newStart=$newStart length=$length content=$content)"
    }
}