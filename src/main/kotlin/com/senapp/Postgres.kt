package com.senapp.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import java.net.URI
import javax.sql.DataSource

object Postgres {
    var ds: DataSource? = null

    fun init(env: ApplicationEnvironment) {
        if (ds != null) return

        val urlFromEnv = System.getenv("DB_URL") ?: env.config.propertyOrNull("db.url")?.getString()
        val cfg = HikariConfig()

        if (!urlFromEnv.isNullOrBlank()) {
            // Admite postgres:// y postgresql://
            val normalized = urlFromEnv
                .replace("postgresql://", "http://")
                .replace("postgres://", "http://")
            val uri = URI(normalized)
            val (user, pass) = uri.userInfo.split(":", limit = 2)
            val jdbc = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
            cfg.jdbcUrl = jdbc
            cfg.username = user
            cfg.password = pass
        } else {
            val host = System.getenv("DB_HOST") ?: env.config.property("db.host").getString()
            val port = (System.getenv("DB_PORT") ?: env.config.property("db.port").getString()).toInt()
            val db   = System.getenv("DB_NAME") ?: env.config.property("db.name").getString()
            val user = System.getenv("DB_USER") ?: env.config.property("db.user").getString()
            val pass = System.getenv("DB_PASS") ?: env.config.property("db.pass").getString()
            cfg.jdbcUrl = "jdbc:postgresql://$host:$port/$db?sslmode=require"
            cfg.username = user
            cfg.password = pass
        }

        cfg.maximumPoolSize = 5
        cfg.isAutoCommit = true
        cfg.validate()
        ds = HikariDataSource(cfg)
    }
}
