## Mycelium Shufflepuff

**Mycelium Shufflepuff** is an implementation of the CoinShuffle protocol in java.
This code is experimental, so don't trust it with your life savings. A bug is 
much more likely to cause the protocol to fail than to result in lost funds, 
however. Shufflepuff does not yet use end-to-end encryption. I don't think that
this makes the protocol insecure, but I would like this feature to be working
before it is used for serious purposes. 

If you wish to support this project, please contact 
(daniel.krawisz@thingobjectentity.net) for tasks that you might try completing.

CoinShuffle description: 
http://crypsys.mmci.uni-saarland.de/projects/CoinShuffle/coinshuffle.pdf

## Functionality 

Shufflepuff can run on its own or it can be used as a library and encorporated
into a java program. 

If it is run on its own, it takes a private key, an amount to be shuffled, and a
set of IP addresses or websocket addresses denoting the other players. 

Shufflepuff can also be used as a library in another application. The other 
application would instantiate player.Player and it works from there. 
Stufflpuff comes with a bitcoinj implementation, but the user could provide an
implementation in another library too. 

Shufflepuff needs to look up address balances in order to run the protocol. 
For lite applications there is an option for querying blockchain.info. For
players with full nodes, an option is provided to look up balances using btcd. 
The user could also provide other options by extending the Bitcoin class. 

## Organization of the Project

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

status: nearly ready.

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
