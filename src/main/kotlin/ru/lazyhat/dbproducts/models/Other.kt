package ru.lazyhat.dbproducts.models

import kotlinx.serialization.Serializable
@Serializable
enum class ValueUnit {
    Kilograms,
    Liters,
    Pieces //Pieces in other piece
}
@Serializable
data class Address(
    val region: String,
    val town: String,
    val street: String,
    val house: String
)