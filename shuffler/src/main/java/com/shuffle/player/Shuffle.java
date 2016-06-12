package com.shuffle.player;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

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
        }

        parser.acceptsAll(Arrays.asList("b", "blockchain"),
                "Means of performing blockchain queries. btcd and blockchain.info are supported").withRequiredArg().ofType(String.class);

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

    private static class InvalidOption extends Throwable {
        private final String x;

        private InvalidOption(String x) {
            this.x = x;
        }

        @Override
        public String getMessage() {
            return x;
        }
    }

    public static void checkOptions(OptionSet options, PrintStream stream) throws InvalidOption {

        switch ((String)options.valueOf("blockchain")) {
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
                    // TODO
                    break;
                }
                // Deliberate fallthrough.
            }
            default : {
                throw new InvalidOption(
                        "No option 'blockchain' supplied. some means of looking Available options are 'btcd' and " +
                                "'blockchain.info'. 'btcd' allows for looking up options on a " +
                                "local instance of the blockchain. 'blockchain.info' allows for " +
                                "querying the blockchain over the web through blockchain.info.\n"
                );
            }
        }

        if (!options.has("amount")) {
            throw new InvalidOption("No option 'amount' supplied. We need to know what sum is to be " +
            "shuffled for each player in the join transaction.\n");
        }

        if (!options.has("time")) {
            throw new InvalidOption("No option 'time' supplied. When does the join take place?\n");
        }

        if (!options.has("seed")) {
            throw new InvalidOption("No option 'seed' supplied. Random seed needed!\n");
        }
    }

    public static void main(String[] opts) throws IOException {

        OptionParser parser = getShuffleOptionsParser();
        OptionSet options = parser.parse(opts);

        if (options == null) {
            parser.printHelpOn(System.out);
            return;
        }

        // Check for help flag.
        if (options.has("help")) {
            parser.printHelpOn(System.out);
            return;
        }

        try {
            checkOptions(options, System.out);
        } catch (InvalidOption invalidOption) {
            System.out.print(invalidOption.getMessage());
        }

        System.out.println("Options check out ok!");
    }
}
