package ru.itmo.sd.stock

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import ru.itmo.sd.stock.entity.Company
import ru.itmo.sd.stock.entity.Shares
import ru.itmo.sd.stock.exception.CompanyNotFoundException
import ru.itmo.sd.stock.repository.CompanyRepository
import ru.itmo.sd.stock.repository.SharesRepository
import java.math.BigDecimal

@Service
class StockService(private val companyRepository: CompanyRepository, private val sharesRepository: SharesRepository) {
    fun findCompanyById(companyId: String): Company? = companyRepository.findByIdOrNull(companyId)

    fun getCompanyById(companyId: String): Company = companyRepository.findByIdOrNull(companyId)
        ?: throw CompanyNotFoundException(companyId)

    fun saveCompany(company: Company): Company? = companyRepository.save(company)

    fun getCompanyShares(companyId: String): BigDecimal? {
        return companyRepository.findByIdOrNull(companyId)?.sharesPrice
    }

    fun saveShares(shares: List<Shares>): List<Shares> = sharesRepository.saveAll(shares)

    fun findShares(companyId: String) =
        sharesRepository.findByCompanyId(companyId)

    fun findCompanyFreeShares(companyId: String) =
        sharesRepository.findByCompanyIdAndOwnerId(companyId, null)

    fun findByCompanyIdAndOwnerId(companyId: String, ownerId: String) =
        sharesRepository.findByCompanyIdAndOwnerId(companyId, null)

    fun findByOwnerId(ownerId: String) = sharesRepository.findByOwnerId(ownerId)

    fun buyCompanyShares(companyId: String, ownerId: String, amount: Int) {
        val freeShares = sharesRepository.findByCompanyIdAndOwnerId(companyId, null)
        for (i in 0 until amount) {
            freeShares[i].ownerId = ownerId
        }
        sharesRepository.saveAll(freeShares)
    }
}
