package ru.itmo.sd.stock.entity

import java.math.BigDecimal
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class Company(
    @Id
    var id: String = UUID.randomUUID().toString(),

    var name: String? = null,

    var sharesPrice: BigDecimal = BigDecimal(0)
)