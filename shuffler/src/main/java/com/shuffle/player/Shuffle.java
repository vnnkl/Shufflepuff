package com.shuffle.player;

import com.shuffle.bitcoin.Coin;
import com.shuffle.mock.MockCoin;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

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

    private Shuffle() {}

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
        } else {
            query.defaultsTo("blockchain.info");
            blockchain.defaultsTo("main");
        }

        parser.acceptsAll(Arrays.asList("B", "amount"),
                "amount to be transferred (satoshis)").withRequiredArg().ofType(Integer.class);
        parser.acceptsAll(Arrays.asList("t", "time"), "time at which protocol is scheduled to take place.").withRequiredArg().ofType(String.class);
        parser.acceptsAll(Arrays.asList("s", "seed"), "random number seed").withRequiredArg().ofType(String.class);

        if (TEST_MODE) {
            parser.accepts("me").withRequiredArg().ofType(String.class);
        }

        parser.accepts("key").withRequiredArg().ofType(String.class);
        parser.accepts("change").withRequiredArg().ofType(String.class);
        parser.accepts("output").withRequiredArg().ofType(String.class);

        parser.accepts("peers").withRequiredArg().ofType(String.class);

        return parser;
    }

    private static class Environment {
        Coin coin;
        String seed;
        long time;

        public Environment(OptionSet options, PrintStream stream)
                throws IllegalArgumentException, ParseException {

            switch ((String)options.valueOf("query")) {
                case "btcd" : {
                    // TODO
                    break;
                }
                case "blockchain.info" : {
                    stream.print("Warning: you have chosen to query address balances over through a " +
                            " third party service. \n");
                    break;
                }
                case "mock" : {
                    if (TEST_MODE) {
                        try {
                            System.out.println("About to parse " + (String)options.valueOf("coin"));
                            coin = MockCoin.fromJSON(new StringReader((String)options.valueOf("coin")));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Unable to parse mockchain data: "
                                    + e.getMessage() + "\n");
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
                                    " querying the blockchain over the web through blockchain.info.\n"
                    );
                }
            }

            if (!options.has("amount")) {
                throw new IllegalArgumentException("No option 'amount' supplied. We need to know what sum " +
                        "is to be shuffled for each player in the join transaction.\n");
            }

            if (!options.has("time")) {
                throw new IllegalArgumentException("No option 'time' supplied. When does the join take place?\n");
            }

            if (!options.has("seed")) {
                throw new IllegalArgumentException("No option 'seed' supplied. Random seed needed!\n");
            }

            seed = (String)options.valueOf("seed");
            // Check entropy.
            if (new EntropyEstimator().put(seed) < 128) {
                throw new IllegalArgumentException("Seed may not be random enough. Please provide longer seed.");
            }

            time = new SimpleDateFormat().parse((String)options.valueOf("time")).getTime();
        }

    }

    public static void main(String[] opts) throws IOException {

        OptionParser parser = getShuffleOptionsParser();
        OptionSet options = null;
        try {
            options = parser.parse(opts);
        } catch (Exception e) {
            // Show the user some json parser error.
            System.out.println(e.getMessage());
            return;
        }

        // Check for help flag.
        if (options.has("help")) {
            parser.printHelpOn(System.out);
            return;
        }

        Environment environment;
        try {
            environment = new Environment(options, System.out);
        } catch (IllegalArgumentException | ParseException e) {
            System.out.print(e.getMessage());
            return;
        }

        System.out.println("Options check out ok!");
    }
}
