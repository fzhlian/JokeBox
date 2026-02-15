package fzhlian.JokeBox.app.data.model

data class OnlineSourceConfig(
    val request: RequestSpec,
    val response: ResponseSpec,
    val mapping: MappingSpec
)

data class RequestSpec(
    val method: String = "GET",
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val query: Map<String, String> = emptyMap(),
    val body: String? = null
)

data class ResponseSpec(
    val itemsPath: String,
    val cursorPath: String? = null
)

data class MappingSpec(
    val content: String,
    val title: String? = null,
    val ageHint: String? = null,
    val language: String? = null,
    val sourceUrl: String? = null
)

