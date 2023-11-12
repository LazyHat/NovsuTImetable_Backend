package ru.lazyhat.dbproducts.models

import kotlinx.datetime.LocalDateTime

interface Local {

    sealed interface Product {
        val id: ULong
        val name: String
        val valueInPiece: Float //in products by kilogram valueInPiece = 1
        val valueUnit: ValueUnit
        val isLatest: Boolean

        data class Barcode(
            override val id: ULong,
            val barcode: ULong,
            override val name: String,
            override val valueInPiece: Float,
            override val valueUnit: ValueUnit,
            override val isLatest: Boolean
        ) : Product

        data class NonBarcode(
            override val id: ULong,
            override val name: String,
            override val valueInPiece: Float,
            override val valueUnit: ValueUnit,
            override val isLatest: Boolean
        ) : Product
    }

    data class ProductCreate(
        val barcode: ULong,
        val name: String,
        val valueInPiece: Float,
        val valueUnit: ValueUnit
    )

    data class ProductFind(
        val barcode: ULong? = null,
        val name: String? = null,
        val valueInPiece: Float? = null,
        val valueUnit: ValueUnit? = null
    )

    data class Price(
        val id: ULong,
        val productId: ULong,
        val shopId: ULong,
        val value: Float,
        val createdAt: LocalDateTime
    )

    data class PriceCreate(
        val productId: ULong,
        val shopId: ULong,
        val value: Float
    )

    data class Shop(
        val id: ULong,
        val name: String,
        val address: Address
    )

    data class ShopCreate(
        val name: String,
        val address: Address
    )

    data class ProductRecord(
        val id: ULong,
        val productId: ULong,
        val cartId: ULong,
        val priceId: ULong,
        val amount: Float
    )

    data class ProductRecordCreate(
        val productId: ULong,
        val cartId: ULong,
        val priceId: ULong,
        val amount: Float
    )

    data class ProductRecordFind(
        val productId: ULong? = null,
        val cartId: ULong? = null,
        val priceId: ULong? = null,
        val amount: Float? = null
    )

    data class Cart(
        val id: ULong,
        val shopId: ULong,
        val buyDate: LocalDateTime
    )

    data class CartCreate(
        val shopId: ULong
    )
}

