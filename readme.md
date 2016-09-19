
## Mycelium Shufflepuff

**Mycelium Shufflepuff** is an implementation of the CoinShuffle protocol in java.
This code is experimental, so don't trust it with your life savings.

If you wish to support this project, please contact
(daniel.krawisz@thingobjectentity.net) for tasks that you might try completing.

CoinShuffle description:
http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf

## The CoinShuffle protocol

The idea of a join transaction is that n people create a single transaction in
which each of them redeems a single output and each of them creates a new
output. Each output stores the same number of coins. Therefore, A blockchain observer
won't know who owns which output.

CoinShuffle allows n people to create a join transaction which anonymizes their
outputs even to one another. By the end, each player knows which anonymized
output he owns, but he does not know which player owns any other anonymized
output. CoinShuffle allows for the identification of a malicious player in
the case of misbehavoir.

## Functionality

Shufflepuff can run on its own or it can be used as a library and encorporated
into a java program.

### Using Shufflepuff as a standalone application

Shufflepuff currently supports only basic functionality. The user must provide
Shufflepuff with the details of the join transaciton to be constructed and
contact information for the other players.

Shufflepuff needs to look up address balances in order to run the protocol.
For lite applications there is an option for querying blockcypher.com. For
players with full nodes, an option is provided to look up balances using btcd.
(We couldn't use bitcoin-core because it does not provide the option to index
all addresses in the blockchain.

Usage:

    java -jar shuffler.jar shuffle <options...>

Options that all players must agree on for the protocol to run:

    * --session  A unique session identifier (any unique string is fine).
    * --amount   An amount to be joined per player (in satoshis).
    * --time     The time for the protocol to begin.
    * --fee      The miner's fee to be payed by each player.

Options that the player must provide to play his own role in the protocol:

    * --port     A port on which to listen for connections from other players.
    * --key      A private Bitcoin key in WIF format which holds enough funds to create the transaction (amount + fee). The funds must be in a single output.
    * --anon     An address to store the anonymized funds. The output to this address must NEVER be merged with any other output owned by the user or his anonymity is destroyed, and that of the other players is weakened.
    * --change   An optional change address. The change address is not anonymized. If a change address is not provided, any remaining funds go to the miners.

The user must provide contact information for the other players.

    * --peers   Contact info for the other players, entered as a JSON array. Each entry in the array contains a JSON object with the following parameters:
        * address  Contact info for the peer. Currently the only supported means is tcp in the form ip:peer.
        * key      A public key corresponding to a Bitcoin address containing sufficient funds.

Blockchain options:

    * --blockchain  Available options are 'test' and 'main'. Default is 'main'.
    * --query       Means of querying the blockchain. Options are 'btcd', 'blockcypher.com', 'blockchain.info'.

### Using Shufflepuff as a Library

Shufflepuff can be used as a library in some other project. In order to do
this, the other application would instantiate player.Player and it works
from there. Stufflpuff comes with a bitcoinj implementation, but the user
could provide an implementation in another library too.

Package protocol implements Coin Shuffle according to the same concepts as in
the original paper. It abstracts away a lot. In order to implement this version
of the protocol, a user would have to implement the following interfaces:

    * Coin     - provides cryptocurrency functions and objects.
    * Crypto   - provides cryptographic functions and objects.
    * Messages - provides for a implementation of the messages in Coin Shuffle
    * Network  - provides a connection to the other players in the protocol.

The purpose of this design is that a set of test cases can be developed for
the protocol as a whole before any work has been put into its details. This
means that there can already be a huge set of test cases very early
in the development of a working version of the protocol, which will greatly
reduce the risk of introducing errors as the rest of the development work
proceeds. Furthermore, this protocol can be adapted to any java implementation
of Bitcoin, or any other cryptocurrency.

### Installing Java 8 Policy Files

Because ShufflePuff requires larger key sizes than standard Java allows, the user
must can either download the Java 8 Cryptography Extension (JCE) Unlimited Strength Jurisdiction
Policy Files or use OpenJDK.  The JCE files bypass the encryption export restrictions that are in place.
The link to the Java 8 JCE Policy Files is below:
http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html

If your $JAVA_HOME environment variable is not set, set that now to the version of
Java that you will run ShufflePuff with.

**Automatic Installation**
Once you have downloaded the jce_policy-8.zip file, move it (still zipped) to the ShufflePuff
directory. Then, simply run the jcepolicy.sh script in the ShufflePuff directory.

**Manual Installation**
Once you have downloaded the jce_policy-8.zip file, unzip the file.  
Then, move the three policy files (README.txt, local_policy.jar, and
US_export_policy.jar) to the $JAVA_HOME/jre/lib/security directory.  Now you should be
able to run ShufflePuff's cryptographic functions smoothly.

### Status Log

status: Works!

Package mock includes mock implementations of all these interfaces for testing
purposes.

status: functional

Package sim allows for the protocol to be run with any number of simulated
players. There is a Simulator class which can be used to simulate runs of the
protocol with any implementation. It allows for malicious players. All test
cases work.

status: very slick

Package bitcoin will provide an implementation of Coin and Crypto which is
specific to the Bitcoin network and which provides the cryptography as described
in the paper. It relies on bitcoinj and Spongey Castle.

status: working.

Package p2p includes classes for constructing various internet channels, by
which instances of this program will communicate.

status: Working.

Package player provides for some peripheral issues about running the protocol,
such as collecting the initial data and re-running the protocol if some players
have to be eliminated. Player is also quite abstract and could be implemented in
various ways.

status: Working

Package moderator will have a server which will eventually help people to find
one another to create joins. It will also provide for a client that can schedule
and commit to joins.

status: coming along
