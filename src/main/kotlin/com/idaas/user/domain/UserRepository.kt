package com.idaas.user.domain

interface UserRepository {
    fun save(user: User)
    fun findByEmail(email: String): User?
    fun findById(id: String): User?
    fun deleteByEmail(email: String)
    fun findAll(): List<User>
}

class InMemoryUserRepository : UserRepository {
    private val usersById = mutableMapOf<String, User>()
    private val emailToId = mutableMapOf<String, String>()

    override fun save(user: User) {
        usersById[user.id] = user
        emailToId[user.email] = user.id
    }
    override fun findByEmail(email: String): User? = emailToId[email]?.let { usersById[it] }
    override fun findById(id: String): User? = usersById[id]
    override fun deleteByEmail(email: String) {
        val id = emailToId.remove(email)
        if (id != null) {
            usersById.remove(id)
        }
    }
    override fun findAll(): List<User> = usersById.values.toList()
} 