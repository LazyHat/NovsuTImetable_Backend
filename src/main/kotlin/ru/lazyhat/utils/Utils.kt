package ru.lazyhat.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap

fun LocalDateTime.Companion.now(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

infix fun <T> ExpressionWithColumnType<T>.eqOrTrue(t: T?): Op<Boolean> =
    if (t == null) Op.TRUE else EqOp(this, wrap(t))

infix fun <T> ExpressionWithColumnType<T>.eqOrFalse(t: T?): Op<Boolean> =
    if (t == null) Op.FALSE else EqOp(this, wrap(t))