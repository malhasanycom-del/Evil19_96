package com.evil19_96.tv

data class Channel(
    val name: String,
    val url: String,
    val group: String = "غير مصنف",
    val logo: String? = null
)
