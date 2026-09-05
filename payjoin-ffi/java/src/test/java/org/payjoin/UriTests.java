package org.payjoin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UriTests {
    @Test
    void urlEncodedPayjoinParameter() {
        String uri = "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX?amount=1&pj=https://example.com?ciao";
        assertNotNull(Url.parse(uri));
    }

    @Test
    void validUrl() {
        String uri = "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX?amount=1&pj=https://example.com?ciao";
        assertNotNull(Url.parse(uri));
    }

    @Test
    void missingAmountShouldBeOk() {
        String uri =
                "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX?pj=https://testnet.demo.btcpayserver.org/BTC/pj";
        assertNotNull(Url.parse(uri));
    }

    @Test
    void validUrisWithDifferentAddressesAndEndpoints() {
        String https = TestUtils.exampleUrl();
        String onion = "http://vjdpwgybvubne5hda6v4c5iaeeevhge6jvo3w2cl6eocbwwvwxp7b7qd.onion";
        String[] addresses = {
            "bitcoin:12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX",
            "BITCOIN:TB1Q6D3A2W975YNY0ASUVD9A67NER4NKS58FF0Q8G4",
            "bitcoin:tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4",
        };
        for (String address : addresses) {
            for (String pj : new String[] {https, onion}) {
                assertNotNull(Url.parse(address + "?amount=1&pj=" + pj));
            }
        }
    }

    @Test
    void uriParseSmoke() {
        try (Uri uri = Uri.parse("bitcoin:bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")) {
            assertNotNull(uri.address());
        }
        assertThrows(UriParseException.class, () -> Uri.parse("not-a-uri"));
    }
}
