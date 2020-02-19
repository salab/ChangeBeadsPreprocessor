package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer

import com.github.difflib.algorithm.Change
import com.github.difflib.algorithm.jgit.HistogramDiff
import com.github.difflib.patch.Patch
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.ArgOption.Companion.argOption
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.*
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RenameDetector
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.TreeFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class AnalyzeRepository(private val orgPath: String) {
    private val repository: FileRepository

    init {
        val gitDir = Paths.get(orgPath, ".git")
        repository = FileRepository(File(gitDir.toString()))
    }

    fun run(): MutableList<CommitData> {
        val commitDataList = mutableListOf<CommitData>()

        val orgRevWalk = createRevWalk()
        var beforeOrgCommit: RevCommit? = null
        val beforeFileContentMap: MutableMap<String, String> = mutableMapOf()

        // RevWalkで指定範囲内のRevCommitを取得
        orgRevWalk.forEach {
            parseRevCommit(it, beforeOrgCommit, beforeFileContentMap, commitDataList)

            beforeOrgCommit = it
        }

        return commitDataList
    }

    private fun parseRevCommit(
        revCommit: RevCommit,
        beforeOrgCommit: RevCommit?,
        beforeFileContentMap: MutableMap<String, String>,
        commitDataList: MutableList<CommitData>
    ) {
        val date = revCommit.authorIdent.`when`
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        // Originalのリポジトリを解析し前のコミットから変更が発生したファイルを特定
        val editFilePathList = getChangedFiles(beforeOrgCommit, revCommit)

        val commitData = getCommitData(revCommit, date, editFilePathList, beforeFileContentMap)
        commitDataList.add(commitData)

        if (argOption.log) {
            println(revCommit)
            println(commitData.toString())
            println("------------------------------------------------------------------")
        }
    }

    private fun getCommitData(
        revCommit: RevCommit,
        date: Date,
        editFilePathList: Set<String>,
        beforeFileContentMap: MutableMap<String, String>
    ): CommitData {
        val commitData = CommitData(commitHash = revCommit.name, editTime = date, message = revCommit.fullMessage)

        editFilePathList.forEach { editFilePath ->
            commitData.editFile[editFilePath] = FileData()

            val beforeFileContent = beforeFileContentMap[editFilePath] ?: ""
            val editFileContent = showFile(revCommit.tree, editFilePath)

            val patch = getPatchData(beforeFileContent, editFileContent, editFilePath)
            commitData.editFile[editFilePath]?.patches?.add(patch)

            // diff-match-patchライブラリによりword-diffを取得
            val diffMatchPatch = DiffMatchPatch()
            val diffList: LinkedList<DiffMatchPatch.Diff> = diffMatchPatch.diffMain(beforeFileContent, editFileContent)
            diffMatchPatch.diffCleanupSemantic(diffList)
            // Diffのオフセット情報を取得
            val diffData = DiffData(Paths.get(editFilePath).fileName.toString(), editFilePath, diffList)
            // クラス・メソッドの範囲情報を取得
            val sourceASTData = getMethodData(editFileContent)
            commitData.setEditClass(editFilePath, diffData, sourceASTData)

            beforeFileContentMap[editFilePath] = editFileContent
        }
        return commitData
    }

    private fun getPatchData(
        beforeFileContent: String,
        editFileContent: String,
        editFilePath: String
    ): PatchData {
        // java-diff-utilsによりline-diffを取得、パースして個々のコミットのDiffを取得
        val beforeListContent = beforeFileContent.split("\n")
        val editListContent = editFileContent.split("\n")
        val changeList: List<Change> = HistogramDiff<String>().computeDiff(beforeListContent, editListContent, null)
        val diffUtilsPatch = Patch.generate(beforeListContent, editListContent, changeList)
        println(diffUtilsPatch)
        val patch = diffUtilsDeltasToPatch(diffUtilsPatch.deltas, editFilePath, editFilePath, editListContent)
        return patch
    }

    private fun createRevWalk(): RevWalk {
        val revWalk = RevWalk(repository)
        revWalk.sort(RevSort.COMMIT_TIME_DESC, true)
        revWalk.sort(RevSort.REVERSE, true)
        val orgRootId = repository.resolve("HEAD")
        val orgRoot = revWalk.parseCommit(orgRootId)
        revWalk.markStart(orgRoot)
        return revWalk
    }

    private fun getChangedFiles(oldRevCommit: RevCommit?, newRevCommit: RevCommit): Set<String> {
        val changedFiles = HashSet<String>()
        val treeWalk = TreeWalk(repository)

        treeWalk.filter = TreeFilter.ANY_DIFF
        treeWalk.isRecursive = true

        // 引数のオーバーロードにより別のメソッドを呼び出しているためエルビス演算子で1行にはまとめないこと
        if (oldRevCommit == null) {
            treeWalk.addTree(EmptyTreeIterator())
        } else {
            treeWalk.addTree(oldRevCommit.tree)
        }
        treeWalk.addTree(newRevCommit.tree)

        val renameDetector = RenameDetector(repository)
        // 基本的にDELETEとADDが同時に発生することはないのでRENAMEの判定用閾値を最低にしている
        renameDetector.renameScore = 1
        renameDetector.addAll(DiffEntry.scan(treeWalk))
        for (diff in renameDetector.compute(treeWalk.objectReader, null)) {
            /*println("id:${newRevCommit.name} message:${newRevCommit.fullMessage}")
            println("old:${diff.oldPath} new:${diff.newPath} type:${diff.changeType.name}")
            println("score:${diff.score} renemedetectorscore:${renameDetector.renameScore}")
            println("${diff.newPath} -> ${diff.oldPath}")
            println("-------------------------------------")*/
            when (diff.changeType) {
                DiffEntry.ChangeType.COPY, DiffEntry.ChangeType.RENAME,
                DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.MODIFY -> {
                    changedFiles.add(diff.newPath)
                }
                DiffEntry.ChangeType.DELETE -> {
                    changedFiles.add(diff.oldPath)
                }
                null -> {
                    throw java.lang.IllegalStateException("${newRevCommit.name} don't have changeType")
                }
            }
        }

        return changedFiles
    }

    // gitコマンドにおける git show sha1:file を実行するメソッド
    private fun showFile(revTree: RevTree, filePath: String): String {
        val treeWalk = TreeWalk(repository)
        treeWalk.addTree(revTree)
        treeWalk.isRecursive = true
        treeWalk.filter = PathFilter.create(filePath)
        check(treeWalk.next()) { "Did not find expected file $filePath" }

        val objectId = treeWalk.getObjectId(0)
        val oLoader = repository.open(objectId)

        val contentToBytes = ByteArrayOutputStream()
        oLoader.copyTo(contentToBytes)

        return String(contentToBytes.toByteArray(), StandardCharsets.UTF_8)
    }
}

fun getChangeContent(repository: Repository, revCommit: RevCommit, changedFiles: Set<String>): HashMap<String, String> {
    val changeContentMap = HashMap<String, String>()

    for (changedFile in changedFiles) {
        val tree = revCommit.tree
        val treeWalk = TreeWalk(repository)
        treeWalk.addTree(tree)
        treeWalk.isRecursive = true

        treeWalk.filter = PathFilter.create(changedFile)
        if (!treeWalk.next()) {
            throw IllegalStateException("Did not find expected file '$changedFile' (commitHash : $revCommit)")
        }
        val objectId = treeWalk.getObjectId(0)
        val loader = repository.open(objectId)
        val contentToBytes = ByteArrayOutputStream()
        loader.copyTo(contentToBytes)

        val text = String(contentToBytes.toByteArray(), charset("utf-8"))
        changeContentMap[changedFile] = text

        if (argOption.log) {
            println("---$changedFile---")
            println(text)
            println("------------------")
        }
    }

    return changeContentMap
}

fun catFile(repository: Repository, objectId: ObjectId): String {
    val loader = repository.open(objectId)
    val contentToBytes = ByteArrayOutputStream()
    loader.copyTo(contentToBytes)
    val text = String(contentToBytes.toByteArray(), charset("utf-8"))
    if (argOption.log) {
        println("┌--------------------------------------------------------------------┐")
        println(text)
        println("└--------------------------------------------------------------------┘")
    }
    return text
}