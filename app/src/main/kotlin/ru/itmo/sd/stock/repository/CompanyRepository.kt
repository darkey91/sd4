package ru.itmo.sd.stock.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itmo.sd.stock.entity.Company

@Repository
interface CompanyRepository: JpaRepository<Company, String> {
    fun findCompanyByName(name: String): Company?
}