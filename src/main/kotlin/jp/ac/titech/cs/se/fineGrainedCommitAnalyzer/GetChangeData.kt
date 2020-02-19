package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer

import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.SourceASTData
import jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model.SourceFile
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom.AST
import org.eclipse.jdt.core.dom.ASTParser

fun getMethodDataFromPath(path: String): SourceASTData {
    val source = SourceFile(path)
    return getMethodData(source.code)
}

fun getMethodData(code: String): SourceASTData {
    val parser = ASTParser.newParser(AST.JLS10)
    val options = JavaCore.getOptions()
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options)
    parser.setCompilerOptions(options)
    parser.setSource(code.toCharArray())
    // val unit: CompilationUnit = parser.createAST(NullProgressMonitor()) as CompilationUnit
    // parser.createAST(null)
    val unit = parser.createAST(NullProgressMonitor())
    val visitor = MyVisitor()
    unit.accept(visitor)
    return visitor.sourceASTData
}
