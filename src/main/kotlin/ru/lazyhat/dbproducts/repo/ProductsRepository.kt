package ru.lazyhat.dbproducts.repo

import ru.lazyhat.dbproducts.models.DAO
import ru.lazyhat.dbproducts.models.Local
import ru.lazyhat.dbproducts.models.toDAO
import ru.lazyhat.dbproducts.models.toLocal
import ru.lazyhat.dbproducts.schemas.*

interface ProductsRepository {
    suspend fun getAllProducts(): List<DAO.Product>
    suspend fun getProductsFromBarcode(barcode: ULong): List<DAO.Product.Barcode>
    suspend fun searchProductsByName(name: String): List<DAO.Product>
    suspend fun createProduct(productCreate: DAO.ProductCreate): DAO.Product
    suspend fun createShop(shopCreate: DAO.ShopCreate): DAO.Shop
    suspend fun getAllShops(): List<DAO.Shop>
    suspend fun createCart(cartCreate: DAO.CartCreate): DAO.Cart
    suspend fun getAllCarts(): List<DAO.Cart>
    suspend fun getShoppedPrices(productId: ULong): List<Pair<DAO.Shop, DAO.Price>>
}

class ProductsRepositoryImpl(
    private val shopsService: ShopsService,
    private val productsService: ProductsService,
    private val cartsService: CartsService,
    private val priceService: PricesService,
    private val productRecordsService: ProductRecordsService
) : ProductsRepository {
    override suspend fun getAllProducts(): List<DAO.Product> = productsService.selectAll().map { it.toDAO() }

    override suspend fun getProductsFromBarcode(barcode: ULong): List<DAO.Product.Barcode> =
        productsService.find(Local.ProductFind(barcode = barcode))
            .mapNotNull { it.toDAO() as? DAO.Product.Barcode }

    override suspend fun searchProductsByName(name: String): List<DAO.Product> =
        productsService.find(Local.ProductFind(name = name)).map { it.toDAO() }

    override suspend fun createProduct(productCreate: DAO.ProductCreate): DAO.Product =
        productsService.insert(productCreate.toLocal()).toDAO()

    override suspend fun createShop(shopCreate: DAO.ShopCreate): DAO.Shop =
        shopsService.insert(shopCreate.toLocal()).toDAO()

    override suspend fun getAllShops(): List<DAO.Shop> = shopsService.selectAll().map(Local.Shop::toDAO)

    override suspend fun createCart(cartCreate: DAO.CartCreate): DAO.Cart {
        val cart = cartsService.insert(cartCreate.toLocal())
        cartCreate.products.forEach {
            val price =
                priceService.findLatest(it.productId, cartCreate.shopId)?.takeIf { price -> price.value == it.price }
                    ?: priceService.insert(Local.PriceCreate(it.productId, cartCreate.shopId, it.price))
            productRecordsService.insert(
                Local.ProductRecordCreate(
                    it.productId,
                    cart.id,
                    price.id,
                    it.amount
                )
            )
        }
        return cart.toDAO(
            shopsService.findById(cart.shopId)!!.toDAO(),
            productRecordsService.find(Local.ProductRecordFind(cartId = cart.id)).map {
                val product = productsService.findById(it.productId)!!.toDAO()
                val price = priceService.findById(it.priceId)!!.toDAO()
                it.toDAO(product, price)
            }
        )
    }

    override suspend fun getAllCarts(): List<DAO.Cart> = cartsService.selectAll().map {
        val shop = shopsService.findById(it.shopId)!!.toDAO()
        val productRecords =
            productRecordsService.find(Local.ProductRecordFind(cartId = it.id)).map {
                val product = productsService.findById(it.productId)!!.toDAO()
                val price = priceService.findById(it.priceId)!!.toDAO()
                it.toDAO(product, price)
            }
        it.toDAO(shop, productRecords)
    }

    override suspend fun getShoppedPrices(productId: ULong): List<Pair<DAO.Shop, DAO.Price>> =
        shopsService.selectAll().map { it.toDAO() }.mapNotNull { shop ->
            priceService.findLatest(productId, shop.id)?.toDAO()?.let {
                shop to it
            }
        }
}