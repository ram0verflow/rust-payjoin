package org.payjoindevkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Full BIP 77 v2↔v2 round trip against the in-process directory, OHTTP relay,
 * and bitcoind. Mirrors the Kotlin integration test: same RPC sequence, same
 * receiver checklist, same final assertions, plus an explicit check that the
 * broadcast transaction spends coins from both wallets.
 *
 * Every {@link TestServices} call takes a global runtime mutex and blocks, so
 * sender and receiver are driven strictly in sequence.
 */
class IntegrationTests {
    private static final Gson GSON = new Gson();
    private static final long POLL_SLEEP_MS = 250L;
    private static final long POLL_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    @Test
    void v2ToV2Payjoin() throws Exception {
        InMemoryReceiverPersister recvPersister = new InMemoryReceiverPersister();
        InMemorySenderPersister sendPersister = new InMemorySenderPersister();
        Payjoin.initTracing();
        try (TestServices services = TestServices.initialize()) {
            services.waitForServicesReady();
            String directory = services.directoryUrl();
            String relay = services.ohttpRelayUrl();
            try (OhttpKeys ohttpKeys = services.fetchOhttpKeys();
                    TestHttp http = new TestHttp(services);
                    BitcoindEnv env = Payjoin.initBitcoindSenderReceiver();
                    RpcClient senderRpc = env.getSender();
                    RpcClient receiverRpc = env.getReceiver()) {
                runV2ToV2(
                        http,
                        directory,
                        relay,
                        ohttpKeys,
                        senderRpc,
                        receiverRpc,
                        recvPersister,
                        sendPersister);
            }
        } finally {
            recvPersister.closeSession();
            sendPersister.closeSession();
        }
    }

    private static void runV2ToV2(
            TestHttp http,
            String directory,
            String relay,
            OhttpKeys ohttpKeys,
            RpcClient senderRpc,
            RpcClient receiverRpc,
            InMemoryReceiverPersister recvPersister,
            InMemorySenderPersister sendPersister)
            throws Exception {
        String receiverAddress = rpc(receiverRpc, "getnewaddress").getAsString();
        Set<OutpointRef> senderOutpoints = listOutpoints(senderRpc);
        Set<OutpointRef> receiverOutpoints = listOutpoints(receiverRpc);

        try (ReceiverBuilder builder =
                        new ReceiverBuilder(receiverAddress, directory, ohttpKeys);
                InitialReceiveTransition built = builder.build();
                Initialized session = built.save(recvPersister)) {
            UncheckedOriginalPayload firstPoll =
                    pollReceiver(session, recvPersister, http, relay);
            assertEquals(
                    null,
                    firstPoll,
                    "receiver mailbox should be empty before the sender posts");

            try (PjUri pjUri = session.pjUri()) {
                String originalPsbt = buildSweepPsbt(senderRpc, pjUri);
                try (SenderBuilder senderBuilder = new SenderBuilder(originalPsbt, pjUri);
                        InitialSendTransition newSender = senderBuilder.buildRecommended(1000L);
                        WithReplyKey withReplyKey = newSender.save(sendPersister);
                        RequestOhttpContext posted = withReplyKey.createV2PostRequest(relay)) {
                    byte[] body = http.post(posted.request());
                    try (WithReplyKeyTransition postedTransition =
                                    withReplyKey.processResponse(body, posted.ohttpCtx());
                            PollingForProposal pollingForProposal =
                                    postedTransition.save(sendPersister);
                            PayjoinProposal payjoinProposal =
                                    waitForReceiverProposal(
                                            session,
                                            recvPersister,
                                            http,
                                            relay,
                                            receiverRpc);
                            RequestResponse reply = payjoinProposal.createPostRequest(relay)) {
                        byte[] replyBody = http.post(reply.request());
                        try (PayjoinProposalTransition replyTransition =
                                        payjoinProposal.processResponse(
                                                replyBody, reply.clientResponse());
                                Monitor ignored = replyTransition.save(recvPersister)) {
                            String psbtBase64 =
                                    waitForSenderProposal(
                                            pollingForProposal,
                                            sendPersister,
                                            http,
                                            relay);
                            finishPayjoin(
                                    senderRpc,
                                    receiverRpc,
                                    psbtBase64,
                                    senderOutpoints,
                                    receiverOutpoints);
                        }
                    }
                }
            }
        }
    }

    private static             UncheckedOriginalPayload pollReceiver(
            Initialized session,
            InMemoryReceiverPersister recvPersister,
            TestHttp http,
            String relay)
            throws Exception {
        try (RequestResponse requestResponse = session.createPollRequest(relay)) {
            byte[] body = http.post(requestResponse.request());
            try (InitializedTransition transition =
                    session.processResponse(body, requestResponse.clientResponse())) {
                InitializedTransitionOutcome outcome = transition.save(recvPersister);
                if (outcome instanceof InitializedTransitionOutcome.Progress progress) {
                    // Keep inner; closing Progress would free the payload handle.
                    return progress.inner();
                }
                if (outcome instanceof InitializedTransitionOutcome.Stasis stasis) {
                    stasis.close();
                    return null;
                }
                fail("unexpected receiver poll outcome: " + outcome);
                return null;
            }
        }
    }

    private static PayjoinProposal waitForReceiverProposal(
            Initialized session,
            InMemoryReceiverPersister recvPersister,
            TestHttp http,
            String relay,
            RpcClient receiverRpc)
            throws Exception {
        long deadline = System.nanoTime() + POLL_TIMEOUT_NS;
        int attempts = 0;
        while (System.nanoTime() < deadline) {
            attempts += 1;
            UncheckedOriginalPayload original =
                    pollReceiver(session, recvPersister, http, relay);
            if (original != null) {
                try (original) {
                    return processUncheckedProposal(original, recvPersister, receiverRpc);
                }
            }
            Thread.sleep(POLL_SLEEP_MS);
        }
        fail("Timed out waiting for sender original after " + attempts + " poll(s)");
        return null;
    }

    private static PayjoinProposal processUncheckedProposal(
            UncheckedOriginalPayload proposal,
            InMemoryReceiverPersister recvPersister,
            RpcClient receiverRpc)
            throws Exception {
        try (UncheckedOriginalPayloadTransition transition =
                proposal.checkBroadcastSuitability(
                        null, new MempoolAcceptanceCallback(receiverRpc))) {
            MaybeInputsOwned maybeInputsOwned = transition.save(recvPersister);
            try (maybeInputsOwned) {
                return processMaybeInputsOwned(maybeInputsOwned, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processMaybeInputsOwned(
            MaybeInputsOwned proposal,
            InMemoryReceiverPersister recvPersister,
            RpcClient receiverRpc)
            throws Exception {
        try (MaybeInputsOwnedTransition transition =
                proposal.checkInputsNotOwned(new IsInputOwnedCallback(receiverRpc))) {
            MaybeInputsSeen maybeInputsSeen = transition.save(recvPersister);
            try (maybeInputsSeen) {
                return processMaybeInputsSeen(maybeInputsSeen, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processMaybeInputsSeen(
            MaybeInputsSeen proposal,
            InMemoryReceiverPersister recvPersister,
            RpcClient receiverRpc)
            throws Exception {
        try (MaybeInputsSeenTransition transition =
                proposal.checkNoInputsSeenBefore(new CheckInputsNotSeenCallback())) {
            OutputsUnknown outputsUnknown = transition.save(recvPersister);
            try (outputsUnknown) {
                return processOutputsUnknown(outputsUnknown, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processOutputsUnknown(
            OutputsUnknown proposal,
            InMemoryReceiverPersister recvPersister,
            RpcClient receiverRpc)
            throws Exception {
        try (OutputsUnknownTransition transition =
                proposal.identifyReceiverOutputs(new IsScriptOwnedCallback(receiverRpc))) {
            WantsOutputs wantsOutputs = transition.save(recvPersister);
            try (wantsOutputs) {
                return processWantsOutputs(wantsOutputs, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processWantsOutputs(
            WantsOutputs proposal, InMemoryReceiverPersister recvPersister, RpcClient receiverRpc)
            throws Exception {
        try (WantsOutputsTransition transition = proposal.commitOutputs()) {
            WantsInputs wantsInputs = transition.save(recvPersister);
            try (wantsInputs) {
                return processWantsInputs(wantsInputs, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processWantsInputs(
            WantsInputs proposal, InMemoryReceiverPersister recvPersister, RpcClient receiverRpc)
            throws Exception {
        List<InputPair> inputs = getInputs(receiverRpc);
        WantsFeeRange wantsFeeRange;
        try (WantsInputs contributed = proposal.contributeInputs(inputs);
                WantsInputsTransition transition = contributed.commitInputs()) {
            wantsFeeRange = transition.save(recvPersister);
        } finally {
            for (InputPair pair : inputs) {
                pair.close();
            }
        }
        try (wantsFeeRange) {
            return processWantsFeeRange(wantsFeeRange, recvPersister, receiverRpc);
        }
    }

    private static PayjoinProposal processWantsFeeRange(
            WantsFeeRange proposal, InMemoryReceiverPersister recvPersister, RpcClient receiverRpc)
            throws Exception {
        try (WantsFeeRangeTransition transition = proposal.applyFeeRange(1L, 10L)) {
            ProvisionalProposal provisional = transition.save(recvPersister);
            try (provisional) {
                return processProvisionalProposal(provisional, recvPersister, receiverRpc);
            }
        }
    }

    private static PayjoinProposal processProvisionalProposal(
            ProvisionalProposal proposal,
            InMemoryReceiverPersister recvPersister,
            RpcClient receiverRpc)
            throws Exception {
        try (ProvisionalProposalTransition transition =
                proposal.finalizeProposal(new ProcessPsbtCallback(receiverRpc))) {
            return transition.save(recvPersister);
        }
    }

    private static String waitForSenderProposal(
            PollingForProposal pollingForProposal,
            InMemorySenderPersister sendPersister,
            TestHttp http,
            String relay)
            throws Exception {
        long deadline = System.nanoTime() + POLL_TIMEOUT_NS;
        int attempts = 0;
        PollingForProposal sender = pollingForProposal;
        try {
            while (System.nanoTime() < deadline) {
                attempts += 1;
                try (RequestOhttpContext pollReq = sender.createPollRequest(relay)) {
                    byte[] body = http.post(pollReq.request());
                    try (PollingForProposalTransition transition =
                            sender.processResponse(body, pollReq.ohttpCtx())) {
                        PollingForProposalTransitionOutcome outcome =
                                transition.save(sendPersister);
                        if (outcome
                                instanceof
                                PollingForProposalTransitionOutcome.Progress progress) {
                            return progress.psbtBase64();
                        }
                        if (outcome
                                instanceof PollingForProposalTransitionOutcome.Stasis stasis) {
                            PollingForProposal next = stasis.inner();
                            if (sender != pollingForProposal) {
                                sender.close();
                            }
                            sender = next;
                            Thread.sleep(POLL_SLEEP_MS);
                            continue;
                        }
                        fail("unexpected sender poll outcome: " + outcome);
                    }
                }
            }
            fail("Timed out waiting for receiver proposal after " + attempts + " poll(s)");
            return null;
        } finally {
            if (sender != pollingForProposal) {
                sender.close();
            }
        }
    }

    private static void finishPayjoin(
            RpcClient senderRpc,
            RpcClient receiverRpc,
            String psbtBase64,
            Set<OutpointRef> senderOutpoints,
            Set<OutpointRef> receiverOutpoints)
            throws Exception {
        String payjoinPsbt =
                rpc(senderRpc, "walletprocesspsbt", jsonString(psbtBase64))
                        .getAsJsonObject()
                        .get("psbt")
                        .getAsString();
        String finalPsbt =
                rpc(senderRpc, "finalizepsbt", jsonString(payjoinPsbt), "false")
                        .getAsJsonObject()
                        .get("psbt")
                        .getAsString();
        String finalTxHex =
                rpc(senderRpc, "finalizepsbt", jsonString(finalPsbt), "true")
                        .getAsJsonObject()
                        .get("hex")
                        .getAsString();
        String txid =
                rpc(senderRpc, "sendrawtransaction", jsonString(finalTxHex)).getAsString();
        assertTrue(!txid.isEmpty(), "sendrawtransaction should accept the payjoin");

        double networkFees =
                rpc(senderRpc, "decodepsbt", jsonString(finalPsbt))
                        .getAsJsonObject()
                        .get("fee")
                        .getAsDouble();
        JsonObject decodedTx =
                rpc(senderRpc, "decoderawtransaction", jsonString(finalTxHex)).getAsJsonObject();
        JsonArray vins = decodedTx.getAsJsonArray("vin");
        JsonArray vouts = decodedTx.getAsJsonArray("vout");
        assertEquals(2, vins.size());
        assertEquals(1, vouts.size());

        Set<OutpointRef> spent = new HashSet<>();
        for (JsonElement vin : vins) {
            JsonObject obj = vin.getAsJsonObject();
            spent.add(
                    new OutpointRef(
                            obj.get("txid").getAsString(), obj.get("vout").getAsInt()));
        }
        assertTrue(
                spent.stream().anyMatch(senderOutpoints::contains),
                "final tx should spend a sender input");
        assertTrue(
                spent.stream().anyMatch(receiverOutpoints::contains),
                "final tx should spend a receiver input");

        double receiverPending =
                rpc(receiverRpc, "getbalances")
                        .getAsJsonObject()
                        .getAsJsonObject("mine")
                        .get("untrusted_pending")
                        .getAsDouble();
        assertEquals(100.0 - networkFees, receiverPending, 1e-6);
        double senderBalance = rpc(senderRpc, "getbalance").getAsDouble();
        assertEquals(0.0, senderBalance, 1e-6);
    }

    private static String buildSweepPsbt(RpcClient sender, PjUri pjUri) throws Exception {
        String outputs = "{" + jsonString(pjUri.address()) + ":50}";
        String options = "{\"lockUnspents\":true,\"fee_rate\":10,\"subtractFeeFromOutputs\":[0]}";
        String psbt =
                rpc(sender, "walletcreatefundedpsbt", "[]", outputs, "0", options)
                        .getAsJsonObject()
                        .get("psbt")
                        .getAsString();
        return rpc(sender, "walletprocesspsbt", jsonString(psbt), "true", jsonString("ALL"), "false")
                .getAsJsonObject()
                .get("psbt")
                .getAsString();
    }

    private static List<InputPair> getInputs(RpcClient rpcConnection) throws Exception {
        JsonArray utxos = rpc(rpcConnection, "listunspent").getAsJsonArray();
        List<InputPair> pairs = new ArrayList<>();
        try {
            for (JsonElement utxo : utxos) {
                JsonObject obj = utxo.getAsJsonObject();
                String txid = obj.get("txid").getAsString();
                int vout = obj.get("vout").getAsInt();
                byte[] scriptPubkey =
                        java.util.HexFormat.of().parseHex(obj.get("scriptPubKey").getAsString());
                long amountSat = Math.round(obj.get("amount").getAsDouble() * 100_000_000.0);
                TxIn txin = new TxIn(new OutPoint(txid, vout), new byte[0], 0, List.of());
                PsbtInput psbtIn = new PsbtInput(new TxOut(amountSat, scriptPubkey), null, null);
                pairs.add(new InputPair(txin, psbtIn, null));
            }
        } catch (Exception e) {
            for (InputPair pair : pairs) {
                pair.close();
            }
            throw e;
        }
        return pairs;
    }

    private static Set<OutpointRef> listOutpoints(RpcClient client) throws Exception {
        Set<OutpointRef> outpoints = new HashSet<>();
        for (JsonElement utxo : rpc(client, "listunspent").getAsJsonArray()) {
            JsonObject obj = utxo.getAsJsonObject();
            outpoints.add(new OutpointRef(obj.get("txid").getAsString(), obj.get("vout").getAsInt()));
        }
        return outpoints;
    }

    private static JsonElement rpc(RpcClient client, String method, String... params)
            throws Exception {
        return JsonParser.parseString(client.call(method, List.of(params)));
    }

    /** Quote a string so {@code RpcClient.call} treats it as JSON, not a fallback string. */
    private static String jsonString(String value) {
        return GSON.toJson(value);
    }

    private record OutpointRef(String txid, int vout) {}

    private static final class MempoolAcceptanceCallback implements CanBroadcast {
        private final RpcClient connection;

        MempoolAcceptanceCallback(RpcClient connection) {
            this.connection = connection;
        }

        @Override
        public boolean callback(byte[] tx) {
            try {
                String hexTx = java.util.HexFormat.of().formatHex(tx);
                return rpc(connection, "testmempoolaccept", "[" + jsonString(hexTx) + "]")
                        .getAsJsonArray()
                        .get(0)
                        .getAsJsonObject()
                        .get("allowed")
                        .getAsBoolean();
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static final class IsScriptOwnedCallback implements IsScriptOwned {
        private final RpcClient connection;

        IsScriptOwnedCallback(RpcClient connection) {
            this.connection = connection;
        }

        @Override
        public boolean callback(byte[] script) {
            try {
                JsonObject decoded =
                        rpc(
                                        connection,
                                        "decodescript",
                                        jsonString(java.util.HexFormat.of().formatHex(script)))
                                .getAsJsonObject();
                List<String> candidates = new ArrayList<>();
                if (decoded.has("address") && decoded.get("address").isJsonPrimitive()) {
                    candidates.add(decoded.get("address").getAsString());
                }
                if (decoded.has("addresses") && decoded.get("addresses").isJsonArray()) {
                    for (JsonElement item : decoded.getAsJsonArray("addresses")) {
                        if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                            candidates.add(item.getAsString());
                        }
                    }
                }
                if (decoded.has("p2sh") && decoded.get("p2sh").isJsonPrimitive()) {
                    candidates.add(decoded.get("p2sh").getAsString());
                }
                if (decoded.has("segwit") && decoded.get("segwit").isJsonObject()) {
                    JsonObject segwit = decoded.getAsJsonObject("segwit");
                    if (segwit.has("address") && segwit.get("address").isJsonPrimitive()) {
                        candidates.add(segwit.get("address").getAsString());
                    }
                    if (segwit.has("addresses") && segwit.get("addresses").isJsonArray()) {
                        for (JsonElement item : segwit.getAsJsonArray("addresses")) {
                            if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                                candidates.add(item.getAsString());
                            }
                        }
                    }
                }
                for (String addr : candidates) {
                    JsonElement mine =
                            rpc(connection, "getaddressinfo", jsonString(addr))
                                    .getAsJsonObject()
                                    .get("ismine");
                    if (mine != null && mine.isJsonPrimitive() && mine.getAsBoolean()) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static final class IsInputOwnedCallback implements IsInputOwned {
        private final RpcClient connection;

        IsInputOwnedCallback(RpcClient connection) {
            this.connection = connection;
        }

        @Override
        public boolean callback(OutPoint outpoint) {
            try {
                JsonElement txOut =
                        rpc(
                                connection,
                                "gettxout",
                                jsonString(outpoint.txid()),
                                Integer.toString(outpoint.vout()),
                                "true");
                if (txOut instanceof JsonNull || txOut == null || !txOut.isJsonObject()) {
                    return false;
                }
                String scriptHex =
                        txOut.getAsJsonObject()
                                .getAsJsonObject("scriptPubKey")
                                .get("hex")
                                .getAsString();
                return new IsScriptOwnedCallback(connection)
                        .callback(java.util.HexFormat.of().parseHex(scriptHex));
            } catch (Exception e) {
                return false;
            }
        }
    }

    private static final class CheckInputsNotSeenCallback implements IsOutputKnown {
        @Override
        public boolean callback(OutPoint outpoint) {
            return false;
        }
    }

    private static final class ProcessPsbtCallback implements ProcessPsbt {
        private final RpcClient connection;

        ProcessPsbtCallback(RpcClient connection) {
            this.connection = connection;
        }

        @Override
        public String callback(String psbt) throws ForeignException {
            try {
                return rpc(connection, "walletprocesspsbt", jsonString(psbt))
                        .getAsJsonObject()
                        .get("psbt")
                        .getAsString();
            } catch (Exception e) {
                throw new ForeignException.InternalException(
                        e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }
    }
}
