package com.shuffle;

import com.shuffle.player.Shuffle;

import java.io.PrintStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Created by Daniel Krawisz on 6/10/16.
 */
public class Main {

    private Main() {}

    public static void printHelpMessage(PrintStream stream) {// TODO
        stream.println("To use Shufflepuff, run 'java -jar shuffler.jar <command> <options...>");
        stream.println("Use 'java -jar shuffler.jar <command> --help for more info.");
        stream.println("  Supported commands:");
        stream.println("    * shuffle   - connect with several players and create a join transaction");
        stream.println("                  using the CoinShuffle protocol.");
    }

    public static void printVersionMessage(PrintStream stream) {
        stream.println("Shufflepuff version 0.1 alpha");
    }

    public static OptionParser getBaseOptionsParser() {
        OptionParser parser = new OptionParser();
        parser.accepts("help", "print help message.");
        parser.accepts("version", "print version.");
        return parser;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            printHelpMessage(System.out);
            return;
        }

        String[] opts = new String[args.length - 1];
        System.arraycopy(args, 1, opts, 0, opts.length);

        try {
            switch (args[0]) {
                case "shuffle" : {
                    Shuffle.main(opts);
                    break;
                }
                case "mediate" : {
                    // TODO
                    break;
                }
                case "lobby" : {
                    // TODO
                    break;
                }
                default: {

                    OptionParser parser = getBaseOptionsParser();
                    OptionSet options;

                    try {
                        options = parser.parse(args);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        return;
                    }

                    if (options == null) {
                        parser.printHelpOn(System.out);
                        return;
                    }

                    if (options.has("version")) {
                        printVersionMessage(System.out);
                        return;
                    }

                    printHelpMessage(System.out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
