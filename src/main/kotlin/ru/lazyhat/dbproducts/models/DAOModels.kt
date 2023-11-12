package ru.lazyhat.dbproducts.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface DAO {
    @Serializable
    sealed class Product {
        abstract val id: ULong
        abstract val name: String
        abstract val valueInPiece: Float //in products by kilogram valueInPiece = 1
        abstract val valueUnit: ValueUnit
        abstract val isLatest: Boolean

        @Serializable
        @SerialName("NonBarcode")
        data class NonBarcode(
            override val id: ULong,
            override val name: String,
            override val valueInPiece: Float,
            override val valueUnit: ValueUnit,
            override val isLatest: Boolean
        ) : Product()

        @Serializable
        @SerialName("Barcode")
        data class Barcode(
            override val id: ULong,
            val barcode: ULong,
            override val name: String,
            override val valueInPiece: Float,
            override val valueUnit: ValueUnit,
            override val isLatest: Boolean
        ) : Product()
    }

    @Serializable
    data class ProductCreate(
        val barcode: ULong,
        val name: String,
        val valueInPiece: Float,
        val valueUnit: ValueUnit
    )

    @Serializable
    data class Price(
        val id: ULong,
        val value: Float,
        val createdAt: LocalDateTime
    )

    @Serializable
    data class Shop(
        val id: ULong,
        val name: String,
        val address: Address
    )

    @Serializable
    data class ShopCreate(
        val name: String,
        val address: Address
    )

    @Serializable
    data class ProductRecord(
        val id: ULong,
        val product: Product,
        val price: Price,
        val amount: Float,
        val cost: Float
    )

    @Serializable
    data class Cart(
        val id: ULong,
        val shop: Shop,
        val total: Float,
        val buyDate: LocalDateTime,
        val products: List<ProductRecord>
    )

    @Serializable
    data class CartCreate(
        val shopId: ULong,
        val products: List<CartProduct>
    )

    @Serializable
    data class CartProduct(
        val productId: ULong,
        val price: Float,
        val amount: Float
    )
}
