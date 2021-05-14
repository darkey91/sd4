package ru.itmo.sd.stock.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.itmo.sd.stock.StockService
import ru.itmo.sd.stock.entity.User
import ru.itmo.sd.stock.exception.InsufficientFundsException
import ru.itmo.sd.stock.exception.ServiceException
import ru.itmo.sd.stock.repository.UserRepository
import java.math.BigDecimal

@RestController
class UserController(private val mapper: ObjectMapper, private val userRepository: UserRepository, private val stockService: StockService) {

    @GetMapping("/createUser")
    fun createUser(@RequestParam("login") login: String, @RequestParam("balance") balance: BigDecimal?): ResponseEntity<Map<String, String>> =
        userRepository.findUserByLogin(login)?.let {
            //user already exists
            ResponseEntity.badRequest().build()
        } ?: ResponseEntity.ok(
            mapOf(
                "user" to mapper.writeValueAsString(
                    userRepository.save(User(login = login, balance = balance ?: BigDecimal.ZERO))
                )
            )
        )

    @GetMapping("/increaseBalance")
    @Transactional(propagation = Propagation.REQUIRED)
    fun increaseBalance(@RequestParam("userId") userId: String, @RequestParam("value") value: BigDecimal) {
        val user = userRepository.findByIdOrNull(userId)!!
        user.balance += value
        userRepository.save(user)
    }

    @GetMapping("/buyShares")
    @Transactional(propagation = Propagation.REQUIRED)
    fun buyShares(@RequestParam("companyId") companyId: String,@RequestParam("userId") userId: String,@RequestParam("amount") amount: Int): ResponseEntity<Map<String, String?>> =
        try {
            val price = stockService.getCompanyById(companyId).sharesPrice
            val delta = price.multiply(BigDecimal("-1"))
            val userBalance = userRepository.findByIdOrNull(userId)?.balance ?: throw ServiceException("No user with id=$userId")
            if (userBalance < delta) { throw InsufficientFundsException(userId) }
            increaseBalance(userId, delta)
            stockService.buyCompanyShares(companyId, userId, amount)

            ResponseEntity.ok(mapOf())
        } catch (e: ServiceException) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @GetMapping("/getUserShares")
    fun getUserShares(@RequestParam("userId") userId: String): ResponseEntity<Map<String, String?>> =
        stockService.findByOwnerId(userId).let {
            ResponseEntity.ok(mapOf("amount" to it.size.toString(), "shares" to mapper.writeValueAsString(it)))
        }

    @GetMapping("/sellShares")
    @Transactional(propagation = Propagation.REQUIRED)
    fun sellShares( @RequestParam("userId") userId: String, @RequestParam("companyId") companyId: String, @RequestParam("amount") amount: Int): ResponseEntity<Map<String, String?>> =
        try {
            val price = stockService.getCompanyById(companyId).sharesPrice
            increaseBalance(userId, price)
            val sharesByBuyer = stockService.findByCompanyIdAndOwnerId(companyId, userId)
            if (amount > sharesByBuyer.size) { throw ServiceException("Only ${sharesByBuyer.size} shares is available.") }
            for (i in 0 until amount) {
                sharesByBuyer[i].ownerId = null
            }
            stockService.saveShares(sharesByBuyer)
            ResponseEntity.ok(mapOf())
        } catch (e: ServiceException) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
}
