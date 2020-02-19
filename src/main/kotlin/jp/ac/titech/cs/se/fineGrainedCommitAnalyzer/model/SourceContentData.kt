package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model

class SourceASTData () {
    val classData: MutableList<ClassASTData> = mutableListOf()

    override fun toString(): String {
        return "class$classData"
    }
}

data class ClassASTData (
    val name: String, val start: Int, val length: Int, val methodData: MutableList<MethodASTData> = mutableListOf()
) {
    override fun toString(): String {
        return "($name: start=$start length=$length method=${methodData})"
    }
}

data class MethodASTData (val name: String, val start: Int, val length: Int) {
    override fun toString(): String {
        return "($name: start=$start length=$length)"
    }
}
