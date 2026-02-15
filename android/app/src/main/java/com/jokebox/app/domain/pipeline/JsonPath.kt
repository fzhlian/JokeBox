package fzhlian.JokeBox.app.domain.pipeline

import org.json.JSONArray
import org.json.JSONObject

object JsonPath {
    fun getNode(root: JSONObject, path: String?): Any? {
        if (path.isNullOrBlank()) return null
        if (path == "$") return root
        val tokens = path.split(".")
        var cursor: Any = root
        for (token in tokens) {
            cursor = when (cursor) {
                is JSONObject -> cursor.opt(token) ?: return null
                is JSONArray -> {
                    val idx = token.toIntOrNull() ?: return null
                    if (idx in 0 until cursor.length()) cursor.get(idx) else return null
                }
                else -> return null
            }
        }
        return cursor
    }

    fun getString(root: JSONObject, path: String?): String? {
        val node = getNode(root, path) ?: return null
        return when (node) {
            is String -> node
            else -> node.toString()
        }
    }

    fun getItems(root: JSONObject, path: String): List<JSONObject> {
        val node = getNode(root, path) ?: return emptyList()
        return when (node) {
            is JSONArray -> buildList {
                for (i in 0 until node.length()) {
                    val value = node.opt(i)
                    if (value is JSONObject) add(value)
                }
            }
            is JSONObject -> listOf(node)
            else -> emptyList()
        }
    }
}

