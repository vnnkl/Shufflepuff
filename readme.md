# Mycelium Shufflepuff

**Mycelium Shufflepuff** is an implementation of the CoinShuffle protocol in Java.
This code is experimental, so don't trust it with your life savings.

If you wish to support this project, please contact
(daniel.krawisz@thingobjectentity.net) for tasks that you might try completing.

CoinShuffle white paper:
https://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf

## The CoinShuffle protocol

The idea of a join transaction is that `n` people create a single transaction in
which each of them redeems a single output and each of them creates a new
output. Each output stores the same number of coins. Therefore, a blockchain observer
won't know who owns which output.

CoinShuffle improves user privacy by frustrating attempts to link transactions to a
particular user. A participant (aka "player") can increase their anonymity set by
making it impossible to determine exactly who paid who because inputs and outputs are
indistinguishable (even to other players). Each player knows which output they own,
but cannot link the inputs and outputs of other players. CoinShuffle also allows the
identification and elimination of malicious players caught misbehaving.

## Functionality

Shufflepuff can run on its own or it can be used as a library and incorporated
into a Java program.

### Using Shufflepuff as a standalone application

Shufflepuff currently supports only basic functionality. The user must provide
Shufflepuff with the details of the join transaction to be constructed and
contact information for the other players.

Shufflepuff needs to look up address balances in order to run the protocol.
For players with full nodes, an option is provided to look up balances using
[btcd](https://github.com/btcsuite/btcd). Bitcoin Core support is coming soon!

To generate the `shuffler.jar` file, first `cd` to the Shufflepuff directory.
Then, run `gradle clean` followed by `gradle jar`.  The `shuffler.jar` file will now be
installed in the `shuffler/build/libs/` directory.

Usage:

    java -jar shuffler.jar shuffle <options...>

Options that all players must agree on for the protocol to run:

    * --session  A unique session identifier (any unique string is fine).
    * --amount   An amount to be joined per player (in satoshis).
    * --time     The time for the protocol to begin.
    * --fee      The mining fee to be payed by each player.

Options that the player must provide to participate in the protocol:

<b>Note: You MUST forward this port if you aren't using localhost.</b>

    * --port     A port on which to listen for connections from other players.
    * --key      A private Bitcoin key in [WIF format](https://en.bitcoin.it/wiki/Wallet_import_format) which holds enough funds to create the transaction (amount + fee). The funds must be in a single output.
    * --anon     An address to store the anonymized funds. The output to this address must NEVER be merged with any other output owned by the user or his anonymity is destroyed, and that of the other players is weakened.
    * --change   An optional change address. The change address is not anonymized. If a change address is not provided, any remaining funds go to the miners as a fee.

The user must provide contact information for the other players:

    * --peers   Contact info for the other players, entered as a JSON array. Each entry in the array contains a JSON object with the following parameters:
        * address  Contact info for the peer. Currently the only supported means is tcp in the form ip:peer.
        * key      A public key corresponding to a Bitcoin address containing sufficient funds.

Blockchain options:

    * --blockchain  Available options are 'test' and 'main'. Default is 'main'.
    * --query       Means of querying the blockchain. Only option is 'btcd'.

### Using Shufflepuff as a Library

Shufflepuff can be used as a library in other projects. In order to do
this, the other application would instantiate `player.Player` and then use as needed.
Shufflepuff comes with a [bitcoinj](https://github.com/bitcoinj/bitcoinj) implementation,
but the user could provide an implementation in another library too.

Package protocol implements CoinShuffle according to the same concepts as in
the original paper. It abstracts away a lot. In order to implement this version
of the protocol, a user would have to implement the following interfaces:

    * Coin     - provides cryptocurrency functions and objects.
    * Crypto   - provides cryptographic functions and objects.
    * Messages - provides for a implementation of the messages in CoinShuffle
    * Network  - provides a connection to the other players in the protocol.

This design allows us to develop a set of protocol test cases before any work
has been invested into the final code. Starting with tests will greatly reduce
the risk of introducing errors as development proceeds. Furthermore, this
protocol can be adapted to any Java implementation of Bitcoin or another cryptocurrency.

### Enable strong cryptography for Java 8

Because Shufflepuff requires larger key sizes than Oracle's JDK allows by default, you have 2 options:

1. Use [OpenJDK 8](http://openjdk.java.net/install/) (which does not have the restrictions, but is not easy to install on Mac OSX).  You can then setup your `$JAVA_HOME` and then build the project.

2. If you want to continue using Oracle's JDK, you must [download the Java 8 Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) to disable the U.S. encryption export restrictions.

If your `$JAVA_HOME` environment variable is not set, set that now to the version of
java that you will use to run Shufflepuff.

**Automatic Installation**
Once you have downloaded the `jce_policy-8.zip` file, move it (still zipped) to the Shufflepuff
directory. Then, simply run the `jcepolicy.sh` script in the Shufflepuff directory.

**Manual Installation**
Once you have downloaded the `jce_policy-8.zip` file, unzip the file.
Then, move the three policy files (`README.txt`, `local_policy.jar`, and
`US_export_policy.jar`) to the `$JAVA_HOME/jre/lib/security` directory.  Now you should be
able to run Shufflepuff's cryptographic functions smoothly.

If you have problems, you can [use this tool](https://github.com/jonathancross/jc-docs/blob/master/java-strong-crypto-test) to test support for secure cryptographic keys.

### Bouncy Castle for Mac

For Mac users, Bouncy Castle must be added to the list of Java security providers.

If your `$JAVA_HOME` environment variable is not set, set that now to the version of
java that you will use to run Shufflepuff.

**Automatic Installation**
Simply run `sudo -E build_bc.sh` in the Shufflepuff directory.
The -E flag is required since the script accesses `$JAVA_HOME`.

**Manual Installation**
Download the [Bouncy Castle JAR](http://www.bouncycastle.org/download/bcprov-jdk15on-155.jar) and move it to the `$JAVA_HOME/jre/lib/ext` directory.
Then, open the `$JAVA_HOME/jre/lib/security/java.security` file and add the line <br /> `security.provider.<n>=org.bouncycastle.jce.provider.BouncyCastleProvider`
at the end of the list of security providers.  Note: `<n>` represents the number of the next security provider.

### A note about Java Heap space

Depending on your machine, Shufflepuff can exhaust your Java installation's Heap space.
If this is the case, try adjusting both the minimum memory and maximum memory allocation settings.

### Status Log

status: Works!

Package mock includes mock implementations of all these interfaces for testing
purposes.

status: functional

Package **sim** allows for the protocol to be run with any number of simulated
players. There is a Simulator class which can be used to simulate runs of the
protocol with any implementation. It allows for malicious players. All test
cases work.

status: very slick

Package **bitcoin** will provide an implementation of Coin and Crypto which is
specific to the Bitcoin network and which provides the cryptography as described
in the paper. It relies on bitcoinj and [Spongy Castle](https://rtyley.github.io/spongycastle/).

status: working.

Package **p2p** includes classes for constructing various Internet channels, by
which instances of this program will communicate.

status: working.

Package **player** provides for some peripheral issues about running the protocol,
such as collecting the initial data and re-running the protocol if some players
have to be eliminated. Player is also quite abstract and could be implemented in
various ways.

status: working

Package **moderator** will have a server which will eventually help people to find
one another to create joins. It will also provide for a client that can schedule
and commit to joins.

status: coming along
