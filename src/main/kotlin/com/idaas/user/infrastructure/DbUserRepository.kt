package com.idaas.user.infrastructure

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val phone = varchar("phone", 20)
    val hashedPassword = varchar("hashed_password", 255).nullable()
    val roles = varchar("roles", 255).default("USER") // Store roles as comma-separated string
    override val primaryKey = PrimaryKey(id)
}

class DbUserRepository(private val db: Database) : UserRepository {
    init {
        transaction(db) {
            create(UsersTable)
        }
    }

    override fun save(user: User) {
        println("[DbUserRepository] Saving user: $user")
        transaction(db) {
            val exists = UsersTable.select { UsersTable.id eq user.id }.count() > 0
            if (exists) {
                UsersTable.update({ UsersTable.id eq user.id }) {
                    it[email] = user.email
                    it[name] = user.name
                    it[phone] = user.phone
                    it[hashedPassword] = user.hashedPassword
                    it[roles] = user.roles.joinToString(",")
                }
            } else {
                UsersTable.insert {
                    it[id] = user.id
                    it[email] = user.email
                    it[name] = user.name
                    it[phone] = user.phone
                    it[hashedPassword] = user.hashedPassword
                    it[roles] = user.roles.joinToString(",")
                }
            }
        }
    }

    override fun findByEmail(email: String): User? = transaction(db) {
        UsersTable.select { UsersTable.email eq email }
            .mapNotNull { row ->
                User(
                    id = row[UsersTable.id],
                    email = row[UsersTable.email],
                    name = row[UsersTable.name],
                    phone = row[UsersTable.phone],
                    hashedPassword = row[UsersTable.hashedPassword],
                    roles = row[UsersTable.roles].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                )
            }
            .singleOrNull()
    }

    override fun findById(id: String): User? = transaction(db) {
        println("[DbUserRepository] Looking up user by id: $id")
        UsersTable.select { UsersTable.id eq id }
            .mapNotNull { row ->
                User(
                    id = row[UsersTable.id],
                    email = row[UsersTable.email],
                    name = row[UsersTable.name],
                    phone = row[UsersTable.phone],
                    hashedPassword = row[UsersTable.hashedPassword],
                    roles = row[UsersTable.roles].split(',').map { it.trim() }.filter { it.isNotEmpty() }
                )
            }
            .singleOrNull().also { println("[DbUserRepository] Found user: $it") }
    }

    override fun deleteByEmail(email: String) {
        transaction(db) {
            UsersTable.deleteWhere { UsersTable.email eq email }
        }
    }

    override fun deleteById(id: String) { // Correct placement
        transaction(db) {
            UsersTable.deleteWhere { UsersTable.id eq id }
        }
    }

    override fun findAll(): List<User> = transaction(db) {
        UsersTable.selectAll().map { row ->
            User(
                id = row[UsersTable.id],
                email = row[UsersTable.email],
                name = row[UsersTable.name],
                phone = row[UsersTable.phone],
                hashedPassword = row[UsersTable.hashedPassword],
                roles = row[UsersTable.roles].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            )
        }
    }

    fun dropSchema() {
        transaction(db) {
            drop(UsersTable)
        }
    }
}