package jp.ac.titech.cs.se.fineGrainedCommitAnalyzer.model

import com.fasterxml.jackson.annotation.JsonProperty

data class RepositoryData (
    @JsonProperty("commitDataList") val commitDataList: MutableList<CommitData> = arrayListOf(),
    @JsonProperty("canRebaseMap") val canReBaseMap: HashMap<String, HashMap<String, Boolean>> = hashMapOf()
){
    fun setCanRebaseMap(firstKey: String, secondKey:String, value: Boolean) {
        val secondKeyValueMap = canReBaseMap[firstKey]
        if(secondKeyValueMap != null){
            secondKeyValueMap[secondKey] = value
        } else {
            canReBaseMap[firstKey] = hashMapOf(secondKey to value)
        }
    }

    fun getCanRebaseMap(firstKey: String, secondKey: String): Boolean? {
        return canReBaseMap[firstKey]?.get(secondKey)
    }
}