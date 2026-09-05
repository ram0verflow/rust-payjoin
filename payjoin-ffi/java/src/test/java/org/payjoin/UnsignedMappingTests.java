package org.payjoin;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class UnsignedMappingTests {
    private static final byte[] OHTTP_KEYS = HexFormat.of().parseHex(
            "01001604ba48c49c3d4a92a3ad00ecc63a024da10ced02180c73ec12d8a7ad2cc91bb483824fe2bee8d28bfe2eb2fc6453bc4d31cd851e8a6540e86c5382af588d370957000400010003");

    @Test
    void amountMustBeNonNegative() {
        OhttpKeys keys = OhttpKeys.decode(OHTTP_KEYS);
        ReceiverBuilder builder =
                new ReceiverBuilder("tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4", "https://example.com", keys);
        assertThrows(IllegalArgumentException.class, () -> builder.withAmount(-1));
    }

    @Test
    void changeIndexMustFitU8() {
        InMemoryReceiverPersister receiverPersister = new InMemoryReceiverPersister();
        OhttpKeys keys = OhttpKeys.decode(OHTTP_KEYS);
        Initialized receiver = new ReceiverBuilder(
                        "2MuyMrZHkbHbfjudmKUy45dU4P17pjG2szK", "https://example.com", keys)
                .build()
                .save(receiverPersister);
        SenderBuilder sender = new SenderBuilder(TestUtils.originalPsbt(), receiver.pjUri());
        assertThrows(
                IllegalArgumentException.class,
                () -> sender.buildWithAdditionalFee(1000, 256, 1000, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> sender.buildWithAdditionalFee(1000, -1, 1000, false));
    }
}
