package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.ArgOption.Companion.argOption
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.CommitData
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.MethodData
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.RepositoryData
import java.lang.System.exit
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import java.io.*
import java.nio.file.Paths
import java.util.*


fun main(args: Array<String>) {
    argOption = ArgOption()
    val cmdLineParser = CmdLineParser(argOption)
    try {
        cmdLineParser.parseArgument(args.toList())
    } catch (e: CmdLineException) {
        cmdLineParser.printUsage(System.err)
        exit(0)
    }

    if (argOption.log) {
        println("src directory : ${argOption.srcPath}")
    }

    val repositoryData = RepositoryData()
    repositoryData.commitDataList.addAll(AnalyzeRepository(argOption.srcPath).run())

    val srcPath = Paths.get(argOption.srcPath)
    val outputPath = Paths.get(srcPath.parent.toString(), "${srcPath.fileName}.json").toString()
    outputRepositoryData(repositoryData, outputPath)
}

private fun outputRepositoryData(repositoryData: RepositoryData, fileName: String) {
    val mapper = jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    val jsonString = mapper.writeValueAsString(repositoryData)
    writeTextToFile(jsonString, fileName)
}

private fun writeTextToFile(text: String, fileName: String) {
    val file = File(fileName)
    val fileWriter = FileWriter(file)
    val printWriter = PrintWriter(BufferedWriter(fileWriter))
    printWriter.println(text)
    printWriter.close()
}

