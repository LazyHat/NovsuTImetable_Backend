package ru.lazyhat.dbproducts.models

//LOCAL TO DAO

fun Local.Shop.toDAO(): DAO.Shop = DAO.Shop(
    id, name, address
)

fun Local.Price.toDAO(): DAO.Price = DAO.Price(
    id, value, createdAt
)

fun Local.Product.toDAO(): DAO.Product = when (this) {
    is Local.Product.Barcode -> DAO.Product.Barcode(id, barcode, name, valueInPiece, valueUnit, isLatest)
    is Local.Product.NonBarcode -> DAO.Product.NonBarcode(id, name, valueInPiece, valueUnit, isLatest)
}

fun Local.ProductRecord.toDAO(product: DAO.Product, price: DAO.Price): DAO.ProductRecord = DAO.ProductRecord(
    id, product, price, amount, price.value * amount
)

fun Local.Cart.toDAO(shop: DAO.Shop, productRecords: List<DAO.ProductRecord>): DAO.Cart = DAO.Cart(
    id, shop, productRecords.fold(0f) { acc, productRecord -> acc + productRecord.cost }, buyDate, productRecords
)

//DAO TO LOCAL

fun DAO.ShopCreate.toLocal(): Local.ShopCreate = Local.ShopCreate(
    name, address
)

fun DAO.ProductCreate.toLocal(): Local.ProductCreate = Local.ProductCreate(
    barcode, name, valueInPiece, valueUnit
)

fun DAO.CartCreate.toLocal(): Local.CartCreate = Local.CartCreate(
    shopId
)