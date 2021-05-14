package ru.itmo.sd.stock.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.sd.stock.entity.User

@Repository
interface UserRepository: JpaRepository<User, String> {
    fun findUserByLogin(login: String): User?
}