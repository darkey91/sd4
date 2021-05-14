package ru.itmo.sd.stock

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplicationTest {
    @Container
    val server: GenericContainer<*> =
        FixedHostPortGenericContainer<Nothing>("stock-app:0.0.1-SNAPSHOT")
            .apply { withExposedPorts(8080) }
            .withFixedExposedPort(8080, 8080)

    private val mapper = jacksonObjectMapper()

    @BeforeAll
    fun setUp() {
        server.start()
    }

    @AfterAll
    fun endUp() {
        server.stop()
    }

    @Test
    fun createCompany() {
        val companyName = "Company"
        val initialSharesPrice = BigDecimal("10.00")
        val request = getGETRequest(
            "/createCompany",
            mapOf("companyName" to companyName, "price" to initialSharesPrice.toPlainString())
        )
        val response: HttpResponse<String> =
            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
    }

    @Test
    fun createUser() {
        val createUserRq = getGETRequest("/createUser", mapOf("login" to "login", "balance" to "1"))
        val createUserRs = HttpClient.newHttpClient().send(createUserRq, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, createUserRs.statusCode())
        val userStr = mapper.readValue<Map<String, String>>(createUserRs.body())["user"]
        assertNotNull(userStr)
        val user =  mapper.readValue<Map<String, String>>(userStr)
        assertNotNull(user)
        assertNotNull(user["id"])
        assertEquals("login", user["login"])
    }

    @Test
    fun increaseCompanyAndIssueShares() {
        val createCompanyRequest =
            getGETRequest("/createCompany", mapOf("companyName" to "Company 0", "price" to "10.00"))
        val createCompanyResponse =
            HttpClient.newHttpClient().send(createCompanyRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, createCompanyResponse.statusCode())
        val companyId = getCompanyId(createCompanyResponse.body())

        val issueShareRequest = getGETRequest("/issueShares", mapOf("companyId" to companyId, "amount" to "3"))
        val issueShareResponse =
            HttpClient.newHttpClient().send(issueShareRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, issueShareResponse.statusCode())

        val price = getCompanySharesPrice(companyId)
        assertEquals("10.00", price.toString())

        val changePriceRq = getGETRequest("/changePrice", mapOf("companyId" to companyId, "newPrice" to "15"))
        val changePriceRs = HttpClient.newHttpClient().send(changePriceRq, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, changePriceRs.statusCode())

        val actualNewPrice = getCompanySharesPrice(companyId)
        assertEquals("15.00", actualNewPrice.toString())
    }

    private fun getCompanyId(createCompanyResponse: String): String {
        val createCompanyResponseBody = mapper.readValue<Map<String, String>>(createCompanyResponse)
        val company = mapper.readValue<Map<String, String>>(createCompanyResponseBody["company"]!!)
        return company["id"]!!
    }
    private fun getCompanySharesPrice(companyId: String): BigDecimal {
        val getCompanySharesPriceRq = getGETRequest("/getCompanySharesPrice", mapOf("companyId" to companyId))
        val getCompanySharesPriceRs = HttpClient.newHttpClient().send(getCompanySharesPriceRq, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, getCompanySharesPriceRs.statusCode())

        val getCompanySharesPriceBody = mapper.readValue<Map<String, BigDecimal>>(getCompanySharesPriceRs.body())
        return getCompanySharesPriceBody["price"]!!
    }

    private fun getGETRequest(uri: String, params: Map<String, String>): HttpRequest {
        val queryParams = params.takeIf { it.isNotEmpty() }
            ?.entries?.joinToString("&") { (k, v) -> "$k=${encodeValue(v)}" }
            ?.let { "?$it" }

        return HttpRequest.newBuilder()
            .uri(URI("http://localhost:8080$uri$queryParams"))
            .GET()
            .header("Accept", "application/json")
            .build()
    }

    private fun encodeValue(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }
}
