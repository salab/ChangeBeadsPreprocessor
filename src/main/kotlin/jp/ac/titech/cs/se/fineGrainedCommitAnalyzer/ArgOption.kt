package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer

import org.kohsuke.args4j.Option

class ArgOption {
    companion object {
        lateinit var argOption: ArgOption
    }

    @Option(name = "-s", required = true, aliases = ["--src"], metaVar = "<path>", usage = "path to input repository directory")
    var srcPath: String = ""

    @Option(name = "-l", aliases = ["--log"], usage = "output log to stdout")
    var log: Boolean = false
}