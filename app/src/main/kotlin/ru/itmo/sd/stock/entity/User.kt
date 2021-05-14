package ru.itmo.sd.stock.entity

import java.math.BigDecimal
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class User(
    @Id
    var id: String = UUID.randomUUID().toString(),
    var login: String? = null,
    var balance: BigDecimal = BigDecimal(0)
)