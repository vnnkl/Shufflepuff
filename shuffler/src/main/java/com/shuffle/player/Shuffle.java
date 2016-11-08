package com.shuffle.player;

import com.google.common.primitives.Ints;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.blockchain.BitcoinCore;
import com.shuffle.bitcoin.blockchain.BlockCypherDotCom;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.BlockchainDotInfo;
import com.shuffle.bitcoin.blockchain.Btcd;
import com.shuffle.bitcoin.impl.AddressImpl;
import com.shuffle.bitcoin.impl.CryptoProtobuf;
import com.shuffle.bitcoin.impl.VerificationKeyImpl;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockAddress;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockNetwork;
import com.shuffle.mock.MockProtobuf;
import com.shuffle.monad.Either;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMaps;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.MappedChannel;
import com.shuffle.p2p.MarshallChannel;
import com.shuffle.p2p.Multiplexer;
import com.shuffle.p2p.TcpChannel;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

/**
 *
 *
 * Created by Daniel Krawisz on 6/10/16.
 */
public class Shuffle {
    // Turn this on to enable test mode options.
    private static boolean TEST_MODE = false;

    // 1 / 100 of a bitcoin.
    private static long MIN_AMOUNT = 1000000;
    // 5000 satoshis
    private static long MIN_FEE = 5000L;

    // Entropy checker must think there is at least this much entropy.
    private static int MIN_APPARENT_ENTROPY = 128;

    public static OptionParser getShuffleOptionsParser() {
        OptionParser parser = new OptionParser();
        parser.accepts("help", "print help message.");

        ArgumentAcceptingOptionSpec<String> query = parser.acceptsAll(Arrays.asList("q", "query"),
                "Means of performing blockchain queries. btcd, blockcypher.com and blockchain.info are supported")
                .withRequiredArg().ofType(String.class);

        ArgumentAcceptingOptionSpec<String> blockchain = parser.acceptsAll(Arrays.asList("b", "blockchain"),
                "Which blockchain to query (test or main)")
                .withRequiredArg().ofType(String.class);

        ArgumentAcceptingOptionSpec<Long> time = parser.acceptsAll(Arrays.asList("t", "time"),
                "time at which protocol is scheduled to take place.")
                .withRequiredArg()
                .ofType(Long.class);

        if (TEST_MODE) {
            parser.accepts("crypto")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("mock");

            query.defaultsTo("mock");
            blockchain.defaultsTo("test");

            // TODO
            // What does this do?
            parser.accepts("coin")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("{\"outputs\":[],\"transactions\":[]}");

            parser.accepts("me").withRequiredArg().ofType(String.class);

            parser.accepts("local").withRequiredArg().ofType(String.class);

            parser.accepts("format").withRequiredArg().ofType(String.class)
                    .defaultsTo("protobuf");

            // Five seconds from now.
            // time.defaultsTo(System.currentTimeMillis() + 5000L);

        } else {
            query.defaultsTo("blockcypher.com");
            blockchain.defaultsTo("main");
        }

        parser.acceptsAll(Arrays.asList("S", "session"),
                "The session identifier for this round")
                .withRequiredArg()
                .ofType(String.class);

        parser.acceptsAll(Arrays.asList("B", "amount"),
                "amount to be transferred (satoshis)")
                .withRequiredArg()
                .ofType(Long.class);
        parser.acceptsAll(Arrays.asList("f", "fee"),
                "miner fee to be paid (satoshis)")
                .withRequiredArg()
                .ofType(Long.class);
        parser.acceptsAll(Arrays.asList("s", "seed"),
                "random number seed")
                .withRequiredArg()
                .ofType(String.class);

        OptionSpecBuilder port = parser.accepts("port", "Port on which to listen for connections.");
        OptionSpecBuilder change = parser.accepts("change", "Your change address. Optional.");
        OptionSpecBuilder anon = parser.accepts("anon", "A Bitcoin address to which the anonymized coins are sent.");
        OptionSpecBuilder utxos = parser.acceptsAll(Arrays.asList("u", "utxoList"), "The list of utxos we will spend from, formatted as a JSON array.");

        if (TEST_MODE) {
            port.requiredUnless("local");
            change.requiredUnless("local");
            anon.requiredUnless("local");
            utxos.requiredUnless("local");
        }

        port.withRequiredArg().ofType(Long.class);
        change.withRequiredArg().ofType(String.class);
        anon.withRequiredArg().ofType(String.class);
        utxos.withRequiredArg().ofType(String.class);

        parser.accepts("timeout", "The time in milliseconds that Shufflepuff waits before disconnecting due to a timeout.")
                .withRequiredArg()
                .ofType(Long.class)
                .defaultsTo(1000L);

        parser.accepts("minbitcoinnetworkpeers", "Minimum peers to be connected to before broadcasting transaction. Only used with blockchain.info and blockcypher.com.")
                .withRequiredArg().ofType(Long.class).defaultsTo(5L);
        parser.accepts("rpcuser", "Username to log in to btcd.")
                .withRequiredArg().ofType(String.class);
        parser.accepts("rpcpass", "Password to log in to btcd.")
                .withRequiredArg().ofType(String.class);

        parser.accepts("peers",
                "The peers we will be connecting to, formatted as a JSON array.")
                .withRequiredArg().ofType(String.class);

        parser.accepts("report", "Path to store report file.")
                .withRequiredArg().ofType(String.class);

        return parser;
    }

    public final Coin coin;
    public final String seed;
    public final long time;
    public final long amount;
    public final long fee;
    public final long timeout;
    public final Bytestring session;
    public final Crypto crypto;
    Set<Player> local = new HashSet<>();
    // TODO
    // One peer can have multiple TransactionOutPoints, so the <key,value> ordering is altered here.
    Map<Either<InetSocketAddress, Integer>, Set<TransactionOutPoint>> peers = new HashMap<>();
    // TODO
    // TransactionOutPointKey contains {TransactionOutPoint, VerificationKey}
    Set<TransactionOutPointKey> utxoList = new HashSet<>();
    public final String report; // Where to save the report.

    public final ExecutorService executor;

    private final MockNetwork<Integer, Signed<Packet<VerificationKey, Payload>>> mock = new MockNetwork<>();

    public Shuffle(OptionSet options, PrintStream stream)
            throws IllegalArgumentException, ParseException, UnknownHostException, FormatException, NoSuchAlgorithmException, AddressFormatException, MalformedURLException, BitcoinCrypto.Exception, BitcoindException, CommunicationException {

        if (options.valueOf("amount") == null) {
            throw new IllegalArgumentException("No option 'amount' supplied. We need to know what sum " +
                    "is to be shuffled for each player in the join transaction.");
        }

        if (options.valueOf("fee") == null) {
            throw new IllegalArgumentException("No option 'fee' supplied. We need to know what miner fee " +
                    "is to be paid by each player in the join transaction.");
        }

        if (options.valueOf("time") == null) {
            throw new IllegalArgumentException("No option 'time' supplied. When does the join take place?");
        }

        if (options.valueOf("seed") == null) {
            //throw new IllegalArgumentException("No option 'seed' supplied. Random seed needed!");
            seed = null;
        } else {

            // Check the random seed for apparent randomness.
            seed = (String)options.valueOf("seed");
            // Check entropy.
            if (new EntropyEstimator().put(seed) <= MIN_APPARENT_ENTROPY) {
                throw new IllegalArgumentException("Seed may not be random enough. Please provide longer seed.");
            }
        }

        if (options.valueOf("session") == null) {
            throw new IllegalArgumentException("No option 'session' supplied.");
        }

        if (options.valueOf("peers") == null) {
            throw new IllegalArgumentException("No option 'peers' supplied.");
        }

        // Check on the time.
        time = (Long)options.valueOf("time");
        long now = System.currentTimeMillis();

        if (time < now) {
            throw new IllegalArgumentException("Cannot join protocol in the past.");
        }

        // Get the session identifier.
        if (TEST_MODE) {
            session = new Bytestring(("CoinShuffle Shufflepuff test " + options.valueOf("session")).getBytes());
        } else {
            session = new Bytestring(("CoinShuffle Shufflepuff 1.0 beta " + options.valueOf("session")).getBytes());
        }

        timeout = (Long)options.valueOf("timeout");

        if (options.has("report")) {
            report = (String)options.valueOf("report");
        } else {
            report = null;
        }

        // Detect the nature of the cryptocoin network we will use.
        final String query = (String)options.valueOf("query");
        final NetworkParameters netParams;
        final Messages.ShuffleMarshaller m;

        switch ((String)options.valueOf("blockchain")) {

            case "main" : {
                netParams = MainNetParams.get();
                break;
            }

            case "test" : {
                netParams = TestNet3Params.get();
                break;
            }

            // TODO
            // case "regtest"

            default : {
                throw new IllegalArgumentException("Invalid value for blockchain.");
            }
        }

        switch (query) {
            case "bitcoin-core" : {
                if (!options.has("blockchain")) {
                    throw new IllegalArgumentException("Need to set blockchain parameter (test or main)");
                } else if (options.has("minbitcoinnetworkpeers")) {
                    throw new IllegalArgumentException("minbitcoinnetworkpeers not required for Bitcoin Core.");
                } else if (!options.has("rpcuser")) {
                    throw new IllegalArgumentException("Need to set rpcuser parameter (rpc server login)");
                } else if (!options.has("rpcpass")) {
                    throw new IllegalArgumentException("Need to set rpcpass parameter (rpc server login)");
                }

                String rpcuser = (String)options.valueOf("rpcuser");
                String rpcpass = (String)options.valueOf("rpcpass");

                coin = new BitcoinCore(netParams, rpcuser, rpcpass);
                break;
            }
            case "btcd" : {

                if (!options.has("blockchain")) {
                    throw new IllegalArgumentException("Need to set blockchain parameter (test or main)");
                } else if (options.has("minbitcoinnetworkpeers")) {
                    throw new IllegalArgumentException("minbitcoinnetworkpeers not required for btcd.");
                } else if (!options.has("rpcuser")) {
                    throw new IllegalArgumentException("Need to set rpcuser parameter (rpc server login)");
                } else if (!options.has("rpcpass")) {
                    throw new IllegalArgumentException("Need to set rpcpass parameter (rpc server login)");
                }

                String rpcuser = (String)options.valueOf("rpcuser");
                String rpcpass = (String)options.valueOf("rpcpass");

                coin = new Btcd(netParams, rpcuser, rpcpass);
                break;
            } case "blockchain.info" : {

                if (netParams.equals(TestNet3Params.get())) {
                    throw new IllegalArgumentException("Blockchain.info does not support testnet.");
                }

                if (!options.has("minbitcoinnetworkpeers")) {
                    throw new IllegalArgumentException("Need to set minbitcoinnetworkpeers parameter (min peers to connect to in Bitcoin Network)");
                } else if (options.has("rpcuser")) {
                    throw new IllegalArgumentException("Blockchain.info does not use a rpcuser parameter");
                } else if (options.has("rpcpass")) {
                    throw new IllegalArgumentException("Blockchain.info does not use a rpcpass parameter");
                }

                Long minbitcoinnetworkpeers = (Long)options.valueOf("minbitcoinnetworkpeers");

                int minbitcoinnetworkpeersInt = Ints.checkedCast(minbitcoinnetworkpeers);

                coin = new BlockchainDotInfo(netParams, minbitcoinnetworkpeersInt);
                break;
            } case "blockcypher.com" : {

                if (!options.has("blockchain")) {
                    throw new IllegalArgumentException("Need to set blockchain parameter (test or main)");
                } else if (!options.has("minbitcoinnetworkpeers")) {
                    throw new IllegalArgumentException("Need to set minbitcoinnetworkpeers parameter (min peers to connect to in Bitcoin Network)");
                } else if (options.has("rpcuser")) {
                    throw new IllegalArgumentException("Blockcypher.com does not use a rpcuser parameter");
                } else if (options.has("rpcpass")) {
                    throw new IllegalArgumentException("Blockcypher.com does not use a rpcpass parameter");
                }

                Long minbitcoinnetworkpeers = (Long)options.valueOf("minbitcoinnetworkpeers");

                int minbitcoinnetworkpeersInt = Ints.checkedCast(minbitcoinnetworkpeers);

                coin = new BlockCypherDotCom(netParams, minbitcoinnetworkpeersInt);
                break;
            } case "mock" : {
                if (TEST_MODE) {
                    try {
                        coin = MockCoin.fromJSON(new StringReader((String)options.valueOf("coin")));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Unable to parse mockchain data: "
                                + e.getMessage());
                    }
                    break;
                }
                // fallthrough.
            } default : {
                throw new IllegalArgumentException(
                        "Invalid option for 'blockchain' supplied. Available options are 'bitcoin-core', 'btcd', " +
                                "'blockcypher.com', and 'blockchain.info'. 'bitcoin-core' & 'btcd' allow for " +
                                "looking up options on a local instance of the blockchain. " +
                                "'blockcypher.com' and 'blockchain.info' allow for querying the " +
                                "blockchain over the web."
                );
            }
        }

        // Check cryptography options.
        boolean mockCrypto = false;
        if (TEST_MODE) {
            String cryptography = (String) options.valueOf("crypto");
            String format = (String) options.valueOf("format");

            switch (cryptography) {
                case "mock":
                    mockCrypto = true;
                    if (seed == null) {
                        crypto = new MockCrypto(new InsecureRandom(0));
                    } else {
                        crypto = new MockCrypto(new InsecureRandom(seed.hashCode()));
                    }

                    if (!query.equals("mock")) {
                        throw new IllegalArgumentException("Can only use mock Bitcoin network with mock cryptography.");
                    }

                    break;
                case "real":

                    crypto = new BitcoinCrypto(netParams);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized crypto option value " + cryptography);
            }

            switch (format) {
                case "java":
                    m = new JavaShuffleMarshaller();
                    break;
                case "protobuf":
                    if (mockCrypto) {
                        m = new MockProtobuf();
                    } else {
                        m = new CryptoProtobuf();
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }

        } else {
            crypto = new BitcoinCrypto(netParams);
            m = new CryptoProtobuf();
        }

        amount = (Long)options.valueOf("amount");
        if (amount <= MIN_AMOUNT) {
            throw new IllegalArgumentException("Amount is too small. ");
        }

        fee = (Long)options.valueOf("fee");
        if (fee <= MIN_FEE) {
            throw new IllegalArgumentException("Fee is too small. ");
        }


        // TODO
        // Can you specify --peers[] and --local[] ?

        // Finally, get the peers.
        JSONArray jsonPeers = readJSONArray((String)options.valueOf("peers"));
        if (jsonPeers == null) {
            throw new IllegalArgumentException("Could not read " + options.valueOf("peers") + " as json array.");
        }

        // TODO
        // Store each peer's utxoList in "peers"
        SortedSet<String> checkDuplicateAddress = new TreeSet<>();
        for (int i = 1; i <= jsonPeers.size(); i ++) {
            JSONObject o;
            try {
                o = (JSONObject) jsonPeers.get(i - 1);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read "
                        + jsonPeers.get(i - 1) + " as json object.");
            }

            String addr, utxos;

            try {
                addr = (String) o.get("address");
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read " + o.get("address") + " as string.");
            }
            try {
                utxos = (String) o.get("utxoList");
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read " + o.get("utxoList") + " as string.");
            }

            if (addr == null) {
                throw new IllegalArgumentException("Peer missing field \"address\".");
            }
            if (utxos == null) {
                throw new IllegalArgumentException("Peer missing field \"utxoList\".");
            }

            if (checkDuplicateAddress.contains(addr)) {
                throw new IllegalArgumentException("Duplicate address.");
            } else {
                checkDuplicateAddress.add(addr);
            }

            // Try to read address as host:port.
            String[] parts = addr.split(":");
            if (parts.length != 2)
                throw new NullPointerException("Could not read " + addr + " as tcp address");

            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException eb) {
                throw new NullPointerException("Could not read " + addr + " as tcp address");
            }

            InetSocketAddress tcp = new InetSocketAddress(parts[0], port);
            Either<InetSocketAddress, Integer> address = new Either<>(tcp, null);

            if (peers.containsKey(address)) {
                throw new IllegalArgumentException("Duplicate address " + address);
            }

            JSONArray jsonUtxos = readJSONArray(utxos);
            if (jsonUtxos == null) {
                throw new IllegalArgumentException("Could not read " + o.get("utxoList") + " as json array.");
            }

            SortedSet<TransactionOutPoint> checkDuplicateUtxo = new TreeSet<>();
            for (int j = 1; j <= jsonUtxos.size(); j++) {
                JSONObject obj;
                try {
                    obj = (JSONObject) jsonUtxos.get(i - 1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read "
                            + jsonUtxos.get(i - 1) + " as json object.");
                }

                // Long because we compare to null
                Long vout;
                Sha256Hash transactionHash;
                try {
                    vout = (Long) obj.get("vout");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + obj.get("vout") + " as Long");
                }
                try {
                    transactionHash = Sha256Hash.wrap((String) obj.get("transactionHash"));
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + obj.get("transactionHash") + " as string");
                }
                if (vout == null) {
                    throw new IllegalArgumentException("Utxo missing field \"vout\".");
                }
                if (transactionHash == null) {
                    throw new IllegalArgumentException("Utxo missing field \"transactionHash\".");
                }
                // Error if vout / transactionHash not in correct format.
                TransactionOutPoint t = new TransactionOutPoint(netParams, vout, transactionHash);
                if (checkDuplicateUtxo.contains(t)) {
                    throw new IllegalArgumentException("Duplicate TransactionOutPoint.");
                } else {
                    checkDuplicateUtxo.add(t);
                }
            }

            // TODO
            // Is checkDuplicateUtxo safe to use as a Set<TransactionOutPoint>?
            // Probably...
            peers.put(address, checkDuplicateUtxo);
        }

        executor = Executors.newFixedThreadPool(10);

        // Get information for this player. (In test mode, one node
        // may run more than one player.)
        if (TEST_MODE && options.has("local")) {
            if (options.has("change")) {
                throw new IllegalArgumentException("Option 'change' not needed when 'local' is defined.");
            }
            if (options.has("anon")) {
                throw new IllegalArgumentException("Option 'anon' not needed when 'local' is defined.");
            }
            if (options.has("port")) {
                throw new IllegalArgumentException("Option 'port' not needed when 'local' is defined.");
            }
            if (options.has("utxoList")) {
                throw new IllegalArgumentException("Option 'utxoList' not needed when 'local' is defined");
            }

            // TODO
            // UtxoList
            JSONArray local = readJSONArray((String)options.valueOf("local"));
            if (local == null) {
                throw new IllegalArgumentException("Could not read " + options.valueOf("local") + " as json array.");
            }
            if (local.size() < 1) {
                throw new IllegalArgumentException("Must provide at least one local player.");
            }

            if (jsonPeers.size() + local.size() < 2) {
                throw new IllegalArgumentException("At least two players total must be specified.");
            }

            for (int i = 1; i <= local.size(); i ++) {
                JSONObject o;
                try {
                    o = (JSONObject) local.get(i - 1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read "
                            + local.get(i - 1) + " as json object.");
                }

                String anon, change, utxos;
                Long port;
                try {
                    anon = (String) o.get("anon");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("anon") + " as string.");
                }
                try {
                    change = (String) o.get("change");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("change") + " as string.");
                }
                try {
                    port = (Long) o.get("port");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("port") + " as string.");
                }
                try {
                    utxos = (String) o.get("utxoList");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("utxoList") + " as string.");
                }

                JSONArray jsonUtxos = readJSONArray(utxos);

                if (anon == null) {
                    throw new IllegalArgumentException("Player missing field \"anon\".");
                }
                if (port == null) {
                    throw new IllegalArgumentException("Player missing field \"port\".");
                }
                if (utxos == null) {
                    throw new IllegalArgumentException("Player missing field \"utxoList\".");
                }

                if (jsonUtxos == null) {
                    throw new IllegalArgumentException("Could not read " + o.get("utxoList") + " as json array.");
                }

                // TODO
                // Parse local peer utxoList

                SortedSet<TransactionOutPoint> checkDuplicateUtxo = new TreeSet<>();
                for (int j = 1; j <= jsonUtxos.size(); j++) {
                    JSONObject obj;
                    try {
                        obj = (JSONObject) jsonUtxos.get(j - 1);
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Could not read "
                                + jsonUtxos.get(j - 1) + " as json object.");
                    }

                    //Long because we compare to null
                    Long vout;
                    Sha256Hash transactionHash;
                    try {
                        vout = (Long) obj.get("vout");
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Could not read option " + obj.get("vout") + " as Long");
                    }
                    try {
                        transactionHash = Sha256Hash.wrap((String) obj.get("transactionHash"));
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Could not read option " + obj.get("transactionHash") + " as string");
                    }
                    if (vout == null) {
                        throw new IllegalArgumentException("Utxo missing field \"vout\".");
                    }
                    if (transactionHash == null) {
                        throw new IllegalArgumentException("Utxo missing field \"transactionHash\".");
                    }
                    // Error if vout / transactionHash not in correct format.
                    TransactionOutPoint t = new TransactionOutPoint(netParams, vout, transactionHash);
                    if (checkDuplicateUtxo.contains(t)) {
                        throw new IllegalArgumentException("Duplicate TransactionOutPoint.");
                    } else {
                        checkDuplicateUtxo.add(t);
                    }
                    // TODO
                    // store in some "local" list
                }

                this.local.add(readPlayer(options, i, port, anon, change, m));
            }
        } else {
            if (jsonPeers.size() == 0) {
                throw new IllegalArgumentException("At least one other player must be specified.");
            }

            if (!options.has("anon")) {
                throw new IllegalArgumentException("Missing option 'anon'.");
            }
            if (!options.has("port")) {
                throw new IllegalArgumentException("Missing option 'port'.");
            }
            if (!options.has("utxoList")) {
                throw new IllegalArgumentException("Missing option 'utxoList'.");
            }

            // Get our UTXOs
            JSONArray jsonUtxos = readJSONArray((String)options.valueOf("utxoList"));
            if (jsonUtxos == null) {
                throw new IllegalArgumentException("Could not read " + options.valueOf("utxoList") + " as json array.");
            }

            SortedSet<TransactionOutPoint> checkDuplicateUtxo = new TreeSet<>();
            for (int i = 1; i <= jsonUtxos.size(); i++) {
                JSONObject o;
                try {
                    o = (JSONObject) jsonUtxos.get(i - 1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read "
                            + jsonUtxos.get(i - 1) + " as json object.");
                }

                // Long because we compare to null
                Long vout;
                Sha256Hash transactionHash;
                String key;
                try {
                    vout = (Long) o.get("vout");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("vout") + " as Long");
                }
                try {
                    transactionHash = Sha256Hash.wrap((String) o.get("transactionHash"));
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("transactionHash") + " as string");
                }
                try {
                    key = (String) o.get("key");
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read option " + o.get("key") + " as string.");
                }

                if (vout == null) {
                    throw new IllegalArgumentException("Utxo missing field \"vout\".");
                }
                if (transactionHash == null) {
                    throw new IllegalArgumentException("Utxo missing field \"transactionHash\".");
                }
                if (key == null) {
                    throw new IllegalArgumentException("Utxo missing field \"key\".");
                }
                // Error if vout / transactionHash not in correct format.
                TransactionOutPoint t = new TransactionOutPoint(netParams, vout, transactionHash);
                // The key that can sign the TransactionOutput specified by TransactionOutPoint
                VerificationKey vk = new VerificationKeyImpl(key, netParams);
                if (checkDuplicateUtxo.contains(t)) {
                    throw new IllegalArgumentException("Duplicate TransactionOutPoint.");
                } else {
                    checkDuplicateUtxo.add(t);
                }

                TransactionOutPointKey tk = new TransactionOutPointKey(t, vk);
                if (utxoList.contains(tk)) {
                    throw new IllegalArgumentException("Duplicate TransactionOutPointKey " + tk);
                }
                utxoList.add(tk);
            }

            // TODO
            // local utxoList
            String anon = (String)options.valueOf("anon");
            Long port = (Long)options.valueOf("port");
            if (!options.has("change")) {
                this.local.add(readPlayer(options, 1, port, anon, null, m));
            } else {
                this.local.add(readPlayer(options, 1, port, anon, (String)options.valueOf("change"), m));
            }
        }

    }

    // TODO
    // use the UTXO list in here.
    private Player readPlayer(
            OptionSet options,
            int id,
            long port,
            String anon,
            String change,
            Messages.ShuffleMarshaller m) throws UnknownHostException, FormatException, AddressFormatException {

        Address anonAddress;
        Address changeAddress;
        if (TEST_MODE) {
            switch ((String)options.valueOf("crypto")) {
                case "mock" : {
                    anonAddress = new MockAddress(anon);
                    if (change == null) {
                        changeAddress = null;
                    } else {
                        changeAddress = new MockAddress(change);
                    }
                    break;
                }
                case "real" : {
                    anonAddress = new AddressImpl(anon);
                    if (change == null) {
                        changeAddress = null;
                    } else {
                        changeAddress = new AddressImpl(change);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Only test crypto supported in this pre-alpha version.");
                }
            }
        } else {
            anonAddress = new AddressImpl(anon);
            if (change == null) {
                changeAddress = null;
            } else {
                changeAddress = new AddressImpl(change);
            }
        }

        // TODO
        // Parse keys? peers.put?
        // peers.put(null, new Either<>(null, id));

        // peers
        Channel<VerificationKey, Signed<Packet<VerificationKey, Payload>>> channel =
                new MappedChannel<>(
                        new Multiplexer<>(
                                new MarshallChannel<>(
                                        new TcpChannel(
                                                new InetSocketAddress(InetAddress.getLocalHost(), (int)port)),
                                        m.signedMarshaller()),
                                mock.node(id)),
                        null);

        return new Player(
                null, session, anonAddress,
                changeAddress, null, time,
                amount, fee, coin, crypto, channel, m, System.out, null);
    }

    private static JSONArray readJSONArray(String ar) {

        try {
            JSONObject json = (JSONObject) JSONValue.parse("{\"x\":" + ar + "}");
            if (json == null) {
                throw new IllegalArgumentException("Could not parse json object " + ar + ".");
            }

            return (JSONArray) json.get("x");

        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Could not parse json object " + ar + ".");
        }
    }

    public Collection<Player.Report> cycle()
            throws IOException, InterruptedException, ExecutionException,
            CoinNetworkException, AddressFormatException, ProtocolFailure {

        List<Player.Running> running = new LinkedList<>();

        for(Player p : local) {
            running.add(p.start());
        }

        // If there's just one player, we don't need to run in a separate thread.
        if (running.size() == 1) {
            for (Player.Running p : running) {
                Collection<Player.Report> reports = new HashSet<>();
                reports.add(p.play());
                return reports; // Only one here, so we can return immediately.
            }
        }

        // Construct the future which represents all players' results.
        SummableFuture<Map<VerificationKey, Player.Report>> future
                = new SummableFutureZero<>(
                new SummableMaps<VerificationKey, Player.Report>());

        // Start the connection (this must be done after all Channel objects have been created
        // because everyone must be connected to the internet at the time they attempt to start
        // connecting to one another.
        for (Player.Running p : running) {
            future = future.plus(new NaturalSummableFuture<>(p.playConcurrent()));
        }

        Map<VerificationKey, Player.Report> reportMap = future.get();
        if (reportMap == null) {
            throw new ProtocolFailure();
        }

        return reportMap.values();
    }

    public static class ProtocolFailure extends Exception {

    }

    public void close() {
        executor.shutdownNow();
    }

    public static void main(String[] opts) throws IOException, BitcoindException, CommunicationException {

        OptionParser parser = getShuffleOptionsParser();
        OptionSet options;
        try {
            options = parser.parse(opts);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            parser.printHelpOn(System.out);
            return;
        }

        // Check for help flag.
        if (options.has("help")) {
            parser.printHelpOn(System.out);
            return;
        }

        Shuffle shuffle;
        try {
            shuffle = new Shuffle(options, System.out);
        } catch (AddressFormatException a) {
            System.out.println("Invalid private key: " + a.getMessage());
            return;
        } catch (BitcoinCrypto.Exception e) {
            System.out.print(e.getMessage());
            return;
        } catch (IllegalArgumentException
                //| ClassCastException
                | ParseException
                | FormatException
                | UnknownHostException
                | NoSuchAlgorithmException e) {

            System.out.println("Unable to setup protocol: " + e.getMessage());
            return;
        }

        // Warn for blockchain.info or other blockchain service.
        if (options.valueOf("query").equals("blockchain.info") ||
                options.valueOf("query").equals("blockcypher.com")) {

            System.out.println("Warning: you have chosen to query address " +
                    "balances over through a third party service!");
        }

        Collection<Player.Report> reports;
        try {
            reports = shuffle.cycle();
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            throw new RuntimeException(e);
        } catch (CoinNetworkException | AddressFormatException e) {
            System.out.println("Protocol failed: " + e.getMessage());
            return;
        } catch (ProtocolFailure e) {
            System.out.println("Protocol failed as the result of a bug. Please alert Daniel Krawisz" +
                    " at daniel.krawisz@thingobjectentity.net and include the logs that Shufflepuff" +
                    " outputted. shutting down.");
            return;
        } finally {
            shuffle.close();
        }

        for (Player.Report report : reports) {
            System.out.println(report.toString());
        }
    }
}