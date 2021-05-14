package ru.itmo.sd.stock.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import ru.itmo.sd.stock.entity.Shares

@Repository
interface SharesRepository: JpaRepository<Shares, String> {

    fun findByCompanyId(companyId: String): List<Shares>

    fun findByOwnerId(userId: String): List<Shares>

    fun findByCompanyIdAndOwnerId(companyId: String, ownerId: String?): List<Shares>
}