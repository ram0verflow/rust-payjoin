package org.payjoindevkit

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ValidationTests {
    @Test
    fun receiverBuilderRejectsBadAddress() {
        val ohttpKeys = OhttpKeys.decode(ohttpKeysData)
        assertFailsWith<ReceiverBuilderException> {
            ReceiverBuilder("not-an-address", "https://example.com", ohttpKeys)
        }
    }

    @Test
    fun inputPairRejectsInvalidOutpoint() {
        assertFailsWith<InputPairException> {
            val txin = TxIn(
                previousOutput = OutPoint(txid = "deadbeef", vout = 0u),
                scriptSig = ByteArray(0),
                sequence = 0u,
                witness = emptyList(),
            )
            val psbtin = PsbtInput(witnessUtxo = null, redeemScript = null, witnessScript = null)
            InputPair(txin, psbtin, null)
        }
    }

    @Test
    fun senderBuilderRejectsBadPsbt() {
        val uri = Uri.parse(
            "bitcoin:tb1q6d3a2w975yny0asuvd9a67ner4nks58ff0q8g4?pj=https://example.com/pj",
        ).checkPjSupported()
        assertFailsWith<SenderInputException> {
            SenderBuilder("not-a-psbt", uri)
        }
    }
}
