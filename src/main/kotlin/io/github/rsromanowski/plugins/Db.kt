package io.github.rsromanowski.plugins

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.*
import io.github.rsromanowski.CardQueries
import rsromanowski.Database
import javax.sql.DataSource

object DatabaseSingleton {
    fun createHikariDataSource(
        url : String,
    ) = HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        maximumPoolSize = 3
        // isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })
}

fun doDatabaseThings(dataSource: DataSource/*driver: SqlDriver*/) {
    val driver: SqlDriver = dataSource.asJdbcDriver()
    val database = Database(driver)
    val cardQueries: CardQueries = database.cardQueries

    println("Cards: ${cardQueries.selectAll().executeAsList()}")
}
