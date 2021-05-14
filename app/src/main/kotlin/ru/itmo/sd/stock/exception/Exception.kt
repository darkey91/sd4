package ru.itmo.sd.stock.exception

open class ServiceException(reason: String) : RuntimeException(reason)

class CompanyNotFoundException(id: String): ServiceException(
    "company(id=$id) was not found"
)

class InsufficientFundsException(userId: String): ServiceException(
    "user(id=$userId) has not enough money"
)
