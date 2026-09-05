package org.payjoindevkit

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class UriTests {
    @Test
    fun urlEncodedPayjoinParameter() {
        val uri = "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX?amount=1&pj=https://example.com?ciao"
        Uri.parse(uri).use { parsed ->
            parsed.checkPjSupported().use { pjUri ->
                assertContains(pjUri.pjEndpoint(), "example.com")
            }
        }
    }

    @Test
    fun missingAmountShouldBeOk() {
        val uri = "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX?pj=https://testnet.demo.btcpayserver.org/BTC/pj"
        assertNotNull(Url.parse(uri))
    }

    @Test
    fun validUrisWithDifferentAddressesAndEndpoints() {
        val https = exampleUrl()
        val onion = "http://vjdpwgybvubne5hda6v4c5iaeeevhge6jvo3w2cl6eocbwwvwxp7b7qd.onion"
        val addresses = listOf(
            "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX",
            "BITCOIN:TB1Q6D3A2W975YNY0ASUVD9A67NER4NKS58FF0Q8G4",
            "bitcoin:tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4",
        )
        for (address in addresses) {
            for (pj in listOf(https, onion)) {
                assertNotNull(Url.parse("$address?amount=1&pj=$pj"))
            }
        }
    }

    @Test
    fun uriParseSmoke() {
        Uri.parse("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4").use { uri ->
            assertNotNull(uri.address())
        }
        assertFailsWith<UriParseException> { Uri.parse("not-a-uri") }
    }
}
