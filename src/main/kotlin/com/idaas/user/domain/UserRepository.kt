package com.idaas.user.domain

interface UserRepository {
    fun save(user: User)
    fun findByEmail(email: String): User?
    fun findById(id: String): User?
    fun deleteByEmail(email: String)
    fun deleteById(id: String) // Add deleteById
    fun findAll(): List<User>
}

class InMemoryUserRepository : UserRepository {
    private val usersById = mutableMapOf<String, User>()
    // private val emailToId = mutableMapOf<String, String>() // Removing this for simplification

    override fun save(user: User) {
        // Ensure email uniqueness if it's a new user or email is being changed
        // This check is primarily done at the service layer (RegisterUserService).
        // If UpdateUserProfileService allowed email changes, it would also need to check.
        val existingUserWithSameEmail = usersById.values.find { it.email == user.email }
        if (existingUserWithSameEmail != null && existingUserWithSameEmail.id != user.id) {
            // This indicates an attempt to set an email that's already in use by another user.
            // This should ideally be caught by service layer validation before calling repository.save.
            // For InMemory mock, we can simulate this constraint if necessary, but often it's assumed
            // that valid data is passed to repository methods.
            // throw DataIntegrityViolationException("Email ${user.email} is already in use by another user.")
        }
        usersById[user.id] = user.copy() // Store a copy to prevent external modification issues
    }

    override fun findByEmail(email: String): User? {
        return usersById.values.find { it.email == email }?.copy()
    }

    override fun findById(id: String): User? {
        return usersById[id]?.copy()
    }

    override fun deleteByEmail(email: String) {
        val userToDelete = usersById.values.find { it.email == email }
        if (userToDelete != null) {
            usersById.remove(userToDelete.id)
        }
    }

    override fun deleteById(id: String) {
        usersById.remove(id)
    }

    override fun findAll(): List<User> {
        return usersById.values.map { it.copy() }
    }
}