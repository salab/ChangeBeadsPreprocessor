package jp.ac.titech.cs.se.changeBeadsPreprocessor

import jp.ac.titech.cs.se.changeBeadsPreprocessor.model.ClassASTData
import jp.ac.titech.cs.se.changeBeadsPreprocessor.model.MethodASTData
import jp.ac.titech.cs.se.changeBeadsPreprocessor.model.SourceASTData
import org.eclipse.jdt.core.dom.ASTVisitor
import org.eclipse.jdt.core.dom.TypeDeclaration

class MyVisitor: ASTVisitor() {
    val sourceASTData = SourceASTData()

    /*override fun visit(node: MethodDeclaration): Boolean {
        methodMap[node.name.identifier] = node
        return super.visit(node)
    }*/

    override fun visit(node: TypeDeclaration): Boolean {
        val classData = ClassASTData(node.name.identifier, node.startPosition, node.length)
        node.methods.forEach {
            classData.methodData.add(MethodASTData(it.name.identifier, it.startPosition, it.length))
        }
        sourceASTData.classData.add(classData)
        return super.visit(node)
    }
}
