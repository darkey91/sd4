package ru.itmo.sd.stock.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.sd.stock.StockService
import ru.itmo.sd.stock.entity.Company
import ru.itmo.sd.stock.entity.Shares
import java.math.BigDecimal

@RestController
class StockController(private val mapper: ObjectMapper, private val stockService: StockService) {
    @GetMapping("/createCompany")
    fun createCompany(@RequestParam("companyName") companyName: String, @RequestParam("price") price: BigDecimal): ResponseEntity<Map<String, String>> {
        val newCompany = Company(name = companyName, sharesPrice = price)
        val savedCompany = stockService.saveCompany(newCompany)
        return ResponseEntity.ok(mapOf("company" to mapper.writeValueAsString(savedCompany)))
    }

    @GetMapping("/getCompanySharesPrice")
    fun getCompanySharesPrice(@RequestParam("companyId") companyId: String): ResponseEntity<Map<String, String?>> {
        val price = stockService.getCompanyShares(companyId) ?: return ResponseEntity.badRequest().body(mapOf())
        return ResponseEntity.ok(mapOf("price" to price.toPlainString()))
    }

    @GetMapping("/issueShares")
    fun issueShares(@RequestParam("companyId") companyId: String, @RequestParam("amount") amount: Int): ResponseEntity<Map<String, String>> {
        if (amount < 0) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid amount=$amount"))
        }
        val company = stockService.findCompanyById(companyId)!!
        val shares = Array(amount) { _ -> Shares(companyId = companyId, price = company.sharesPrice) }
        stockService.saveShares(shares.toList())
        return ResponseEntity.ok(mapOf())
    }

    @GetMapping("/buyCompanyShares")
    fun buyCompanyShares( @RequestParam("companyId") companyId: String, @RequestParam("ownerId") ownerId: String, @RequestParam("amount") amount: Int): ResponseEntity<Map<String, String?>> {
        stockService.buyCompanyShares(companyId, ownerId, amount)
        return ResponseEntity.ok(mapOf())
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @GetMapping("/changePrice")
    fun changePrice(@RequestParam("companyId") companyId: String,@RequestParam("newPrice") newPrice: BigDecimal): ResponseEntity<Map<String, String>> {
        if (newPrice < BigDecimal.ZERO) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid newPrice=$newPrice"))
        }
        val company = stockService.findCompanyById(companyId)!!.apply { sharesPrice = newPrice }
        val companyShares = stockService.findShares(companyId)
            .map {
                it.price = newPrice
                it
            }
        stockService.saveCompany(company)
        stockService.saveShares(companyShares)
        return ResponseEntity.ok(mapOf())
    }
}