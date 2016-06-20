package com.shuffle.player;

import com.google.common.primitives.Ints;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.BlockchainDotInfo;
import com.shuffle.bitcoin.blockchain.Btcd;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockAddress;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.monad.Either;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 *
 *
 * Created by Daniel Krawisz on 6/10/16.
 */
public class Shuffle {
    // Turn this on to enable test mode options.
    private static boolean TEST_MODE = true;

    // 1 / 100 of a bitcoin.
    private static long MIN_AMMOUNT = 1000000;

    // Entropy checker must think there is at least this much entropy.
    private static int MIN_APPARENT_ENTROPY = 128;

    public static OptionParser getShuffleOptionsParser() {
        OptionParser parser = new OptionParser();
        parser.accepts("help", "print help message.");

        ArgumentAcceptingOptionSpec<String> query = parser.acceptsAll(Arrays.asList("q", "query"),
                "Means of performing blockchain queries. btcd and blockchain.info are supported")
                .withRequiredArg().ofType(String.class);

        ArgumentAcceptingOptionSpec<String> blockchain = parser.acceptsAll(Arrays.asList("b", "blockchain"),
                "Which blockchain to query (test or main)")
                .withRequiredArg().ofType(String.class);

        if (TEST_MODE) {
            parser.accepts("prng")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("mock");

            parser.accepts("signatures")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("mock");

            parser.accepts("encryption")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("mock");

            query.defaultsTo("mock");
            blockchain.defaultsTo("test");

            parser.accepts("coin")
                    .withRequiredArg()
                    .ofType(String.class)
                    .defaultsTo("{\"outputs\":[],\"transactions\":[]}");

            parser.accepts("me").withRequiredArg().ofType(String.class);
        } else {
            query.defaultsTo("blockchain.info");
            blockchain.defaultsTo("main");
        }

        parser.acceptsAll(Arrays.asList("S", "session"),
                "The session identifier for this round")
                .withRequiredArg()
                .ofType(String.class);

        parser.acceptsAll(Arrays.asList("B", "amount"),
                "amount to be transferred (satoshis)")
                .withRequiredArg()
                .ofType(Integer.class);
        parser.acceptsAll(Arrays.asList("t", "time"),
                "time at which protocol is scheduled to take place.")
                .withRequiredArg()
                .ofType(String.class);
        parser.acceptsAll(Arrays.asList("s", "seed"),
                "random number seed")
                .withRequiredArg()
                .ofType(String.class);

        parser.acceptsAll(Arrays.asList("k", "key"), "Your private key.")
                .requiredUnless("local")
                .withRequiredArg().ofType(String.class);
        parser.accepts("change", "Your change address. Optional")
                .requiredUnless("local")
                .withRequiredArg().ofType(String.class);
        parser.accepts("anon")
                .requiredUnless("local")
                .withRequiredArg()
                .ofType(String.class);
        parser.accepts("timeout")
                .withRequiredArg()
                .ofType(Long.class)
                .defaultsTo(1000L);


        parser.accepts("minBitcoinNetworkPeers")
                .withRequiredArg().ofType(Long.class).defaultsTo(5L);
        parser.accepts("rpcuser")
                .withRequiredArg().ofType(String.class);
        parser.accepts("rpcpass")
                .withRequiredArg().ofType(String.class);

        parser.accepts("peers",
                "The peers we will be connecting to, formatted as a JSON array.")
                .withRequiredArg().ofType(String.class);

        parser.accepts("report").withRequiredArg().ofType(String.class);

        return parser;
    }

    public final Coin coin;
    public final String seed;
    public final long time;
    public final long amount;
    public final long timeout;
    public final SessionIdentifier session;
    public final Crypto crypto;
    Set<Player<Either<InetSocketAddress, Integer>>> local = new HashSet<>();
    Map<VerificationKey, Either<InetSocketAddress, Integer>> peers = new HashMap<>();
    public final String report; // Where to save the report.

    public Shuffle(OptionSet options, PrintStream stream)
            throws IllegalArgumentException, ParseException {

        if (!options.has("amount")) {
            throw new IllegalArgumentException("No option 'amount' supplied. We need to know what sum " +
                    "is to be shuffled for each player in the join transaction.");
        }

        if (!options.has("time")) {
            throw new IllegalArgumentException("No option 'time' supplied. When does the join take place?");
        }

        if (!options.has("seed")) {
            throw new IllegalArgumentException("No option 'seed' supplied. Random seed needed!");
        }

        if (!options.has("session")) {
            throw new IllegalArgumentException("No option 'session' supplied.");
        }

        if (!options.has("players")) {
            throw new IllegalArgumentException("No option 'session' supplied.");
        }

        // Check on the time.
        time = new SimpleDateFormat()
                .parse((String)options.valueOf("time")).getTime();
        long now = System.currentTimeMillis();

        if (time < now) {
            throw new IllegalArgumentException("Cannot join protocol in the past.");
        }

        // Check the random seed for apparent randomness.
        seed = (String)options.valueOf("seed");
        // Check entropy.
        if (new EntropyEstimator().put(seed) <= MIN_APPARENT_ENTROPY) {
            throw new IllegalArgumentException("Seed may not be random enough. Please provide longer seed.");
        }

        // Get the session identifier.
        if (TEST_MODE) {
            session = SessionIdentifier.TestSession((String) options.valueOf("session"));
        } else {
            session = SessionIdentifier.Session((String) options.valueOf("session"));
        }

        timeout = (Long)options.valueOf("timeout");

        if (options.has("report")) {
            report = (String)options.valueOf("report");
        } else {
            report = null;
        }

        // Detect the nature of the cryptocoin network we will use.
        String query = (String)options.valueOf("query");
        switch (query) {
            case "btcd" : {

                if (!options.has("blockchain")) {
                    throw new IllegalArgumentException("Need to set blockchain parameter (test or main)");
                } else if (!options.has("minBitcoinNetworkPeers")) {
                    throw new IllegalArgumentException("Need to set minBitcoinNetworkPeers parameter (min peers to connect to in Bitcoin Network)");
                } else if (!options.has("rpcuser")) {
                    throw new IllegalArgumentException("Need to set rpcuser parameter (rpc server login)");
                } else if (!options.has("rpcpass")) {
                    throw new IllegalArgumentException("Need to set rpcpass parameter (rpc server login)");
                }

                NetworkParameters netParams = null;

                switch ((String)options.valueOf("blockchain")) {

                    case "main" : {
                        netParams = MainNetParams.get();
                        break;
                    }

                    case "test" : {
                        netParams = TestNet3Params.get();
                        break;
                    }
                }

                Long minBitcoinNetworkPeers = (Long) options.valueOf("minBitcoinNetworkPeers");
                String rpcuser = (String)options.valueOf("rpcuser");
                String rpcpass = (String)options.valueOf("rpcpass");

                int minBitcoinNetworkPeersInt = Ints.checkedCast(minBitcoinNetworkPeers);

                coin = new Btcd(netParams, minBitcoinNetworkPeersInt, rpcuser, rpcpass);
                break;
            }
            case "blockchain.info" : {
                stream.print("Warning: you have chosen to query address balances over through a " +
                        " third party service.");

                if (!options.has("blockchain")) {
                    throw new IllegalArgumentException("Need to set blockchain parameter (test or main)");
                } else if (!options.has("minBitcoinNetworkPeers")) {
                    throw new IllegalArgumentException("Need to set minBitcoinNetworkPeers parameter (min peers to connect to in Bitcoin Network)");
                } else if (options.has("rpcuser")) {
                    throw new IllegalArgumentException("Blockchain.info does not use a rpcuser parameter");
                } else if (options.has("rpcpass")) {
                    throw new IllegalArgumentException("Blockchain.info does not use a rpcpass parameter");
                }

                NetworkParameters netParams = null;

                switch ((String)options.valueOf("blockchain")) {

                    case "main" : {
                        netParams = MainNetParams.get();
                        break;
                    }

                    case "test" : {
                        netParams = TestNet3Params.get();
                        break;
                    }
                }

                Long minBitcoinNetworkPeers = (Long)options.valueOf("minBitcoinNetworkPeers");

                int minBitcoinNetworkPeersInt = Ints.checkedCast(minBitcoinNetworkPeers);

                coin = new BlockchainDotInfo(netParams, minBitcoinNetworkPeersInt);
                break;
            }
            case "mock" : {
                if (TEST_MODE) {
                    try {
                        System.out.println("About to parse " + (String)options.valueOf("coin"));
                        coin = MockCoin.fromJSON(new StringReader((String)options.valueOf("coin")));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Unable to parse mockchain data: "
                                + e.getMessage());
                    }
                    break;
                }
                // fallthrough.
            }
            default : {
                throw new IllegalArgumentException(
                        "Invalid option for 'blockchain' supplied. Available options are 'btcd' " +
                                "and 'blockchain.info'. 'btcd' allows for looking up options on " +
                                "a local instance of the blockchain. 'blockchain.info' allows for" +
                                " querying the blockchain over the web through blockchain.info."
                );
            }
        }

        // Check cryptography options.
        if (TEST_MODE) {
            String prng = (String) options.valueOf("prng");
            String signatures = (String) options.valueOf("signatures");
            String encryption = (String) options.valueOf("encryption");

            if (!prng.equals("mock")) {
                throw new IllegalArgumentException("mock crypto only supported currently.");
            }
            if (!signatures.equals("mock")) {
                throw new IllegalArgumentException("mock crypto only supported currently.");
            }
            if (!encryption.equals("mock")) {
                throw new IllegalArgumentException("mock crypto only supported currently.");
            }

            if (!query.equals("mock")) {
                throw new IllegalArgumentException("Can only use mock Bitcoin network with mock cryptography.");
            }
        } else {
            throw new IllegalArgumentException("Shufflepuff pre-alpha must be compiled in test mode.");
        }

        // Create the crypto interface.
        crypto = new MockCrypto(new InsecureRandom(seed.hashCode()));

        amount = (Long)options.valueOf("amount");
        if (amount <= MIN_AMMOUNT) {
            throw new IllegalArgumentException("Amount is too small. ");
        }

        // Get information for this player. (In test mode, one node
        // may run more than one player.)
        if (TEST_MODE && options.has("local")) {
            if (options.has("key")) {
                throw new IllegalArgumentException("Option 'key' not needed when 'local' is defined.");
            }
            if (options.has("change")) {
                throw new IllegalArgumentException("Option 'change' not needed when 'local' is defined.");
            }
            if (options.has("anon")) {
                throw new IllegalArgumentException("Option 'anon' not needed when 'local' is defined.");
            }

            JSONArray local = readJSONArray((String)options.valueOf("local"));

            for (int i = 1; i <= local.size(); i ++) {
                JSONObject o = null;
                try {
                    o = (JSONObject) local.get(i - 1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Could not read "
                            + local.get(i - 1) + " as json object.");
                }

                String key = (String)o.get("key");
                String anon = (String) o.get("anon");
                String change = (String) o.get("change");
                if (key == null) {
                    throw new IllegalArgumentException("Player missing field \"key\".");
                }
                if (anon == null) {
                    throw new IllegalArgumentException("Player missing field \"anon\".");
                }

                this.local.add(readPlayer(options, key, anon, change));
            }
        } else {
            if (!options.has("key")) {
                throw new IllegalArgumentException("Missing option 'key'.");
            }
            if (options.has("anon")) {
                throw new IllegalArgumentException("Missing option 'anon'.");
            }

            String key = (String)options.valueOf("key");
            String anon = (String)options.valueOf("anon");
            if (!options.has("change")) {
                this.local.add(readPlayer(options, key, anon, null));
            } else {
                this.local.add(readPlayer(options, key, anon, (String)options.valueOf("change")));
            }
        }

        // Finally, get the peers.
        Set<Either<InetSocketAddress, Integer>> addresses = new HashSet<>();
        JSONArray peers = readJSONArray((String)options.valueOf("peers"));
        for (int i = 1; i <= local.size(); i ++) {
            JSONObject o = null;
            try {
                o = (JSONObject) peers.get(i - 1);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read "
                        + peers.get(i - 1) + " as json object.");
            }

            String key = (String)o.get("key");
            String addr = (String)o.get("address");
            if (key == null) {
                throw new IllegalArgumentException("Peer missing field \"key\".");
            }
            if (addr == null) {
                throw new IllegalArgumentException("Peer missing field \"address\".");
            }

            VerificationKey vk;
            if (TEST_MODE) {

            } else {
                // TODO
                throw new IllegalArgumentException();
            }

            // Try to read address as integer.
            Either<InetSocketAddress, Integer> address;
            try {
                Integer intAddr = Integer.parseInt(addr);
                address = new Either<>(null, intAddr);
            } catch (NumberFormatException e) {
                // TODO: construct InetSocketAddress.
            }
        }
    }

    private Player<Either<InetSocketAddress, Integer>> readPlayer(OptionSet options, String key, String anon, String change) {
        SigningKey sk;
        Address anonAddress;
        Address changeAddress;
        if (TEST_MODE) {
            switch ((String)options.valueOf("signatures")) {
                case "mock" : {
                    sk = new MockSigningKey(Integer.parseInt(key));
                    anonAddress = new MockAddress(anon);
                    if (change == null) {
                        changeAddress = null;
                    } else {
                        changeAddress = new MockAddress(change);
                    }
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Only test crypto supported in this pre-alpha version.");
                }
            }
        } else {
            // TODO
            throw new IllegalArgumentException("Can only run in test mode.");
        }

        if (changeAddress == null) {
            return new Player<Either<InetSocketAddress, Integer>>(
                    sk, session, coin, crypto, anonAddress, time, amount, timeout);
        } else {
            return new Player<Either<InetSocketAddress, Integer>>(
                    sk, session, coin, crypto, anonAddress, changeAddress, time, amount, timeout);
        }
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

    public static void main(String[] opts) throws IOException {

        OptionParser parser = getShuffleOptionsParser();
        OptionSet options = null;
        try {
            options = parser.parse(opts);
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
        } catch (IllegalArgumentException | ClassCastException | ParseException e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.println("Options check out ok!");
    }
}
