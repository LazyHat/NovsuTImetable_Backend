package ru.lazyhat.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import ru.lazyhat.dbnovsu.schemas.GroupsService
import ru.lazyhat.dbnovsu.schemas.LessonsService
import ru.lazyhat.dbproducts.models.DAO
import ru.lazyhat.dbproducts.repo.ProductsRepository

fun Application.configureRouting() {
    val groupsService by inject<GroupsService>()
    val lessonsService by inject<LessonsService>()
    val productsRepository by inject<ProductsRepository>()

    routing {
        get {
            call.respondText("Hello World!")
        }
        route("tt") {
            route("groups") {
                get{
                    call.respond(groupsService.selectAll());
                }
                route("{id}") {
                    get {
                        val id = call.parameters["id"]!!.toUInt()
                        call.respond(
                            groupsService.selectById(id) ?: HttpStatusCode.BadRequest
                        )
                    }
                    get("lessons") {
                        val id = call.parameters["id"]!!.toUInt()
                        call.respond(
                            lessonsService.selectByGroup(id)
                        )
                    }
                }
            }
        }
        route("products") {
            route("products") {
                get {
                    call.respond(productsRepository.getAllProducts())
                }
                route("barcode/{barcode}") {
                    get {
                        val barcode = call.parameters["barcode"]?.toULongOrNull()
                        val pro = barcode?.let {
                            productsRepository.getProductsFromBarcode(barcode)
                        }
                        call.respond(pro ?: HttpStatusCode.BadRequest)
                    }
                }
                route("name/{name}") {
                    get {
                        val name = call.parameters["name"]
                        val pro = name?.let {
                            productsRepository.searchProductsByName(name)
                        }
                        call.response.headers.appendIfAbsent("Content-Type", "application/json")
                        call.respond(pro?.let { Json.encodeToString(pro) } ?: HttpStatusCode.BadRequest)
                    }
                }
                post {
                    val product: DAO.ProductCreate = call.receive()
                    call.respond(HttpStatusCode.Created, productsRepository.createProduct(product))
                }
            }
            route("shops") {
                get {
                    call.respond(productsRepository.getAllShops())
                }
                post {
                    val form: DAO.ShopCreate = call.receive()
                    call.respond(HttpStatusCode.Created, productsRepository.createShop(form))
                }
            }
            route("carts") {
                get {
                    val carts = productsRepository.getAllCarts()
                    call.respond(carts)
                }
                post {
                    val cart: DAO.CartCreate = call.receive()
                    productsRepository.createCart(cart)
                    call.respond(HttpStatusCode.Created)
                }
            }
            route("prices") {
                get("shopped/{productId}") {
                    val productId = call.parameters["productId"]!!.toULong()
                    call.respond(productsRepository.getShoppedPrices(productId))
                }
            }
        }
    }
}
