/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.monad.Summable;
import com.shuffle.monad.SummableMap;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Collector;
import com.shuffle.p2p.Connect;
import com.shuffle.protocol.CoinShuffle;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.InvalidParticipantSetException;
import com.shuffle.protocol.TimeoutException;
import com.shuffle.protocol.blame.Evidence;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.protocol.message.Phase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 * Created by Daniel Krawisz on 2/1/16.
 */
class Player {
    private static final Logger log = LogManager.getLogger(Player.class);

    private final Bytestring session;

    private final SigningKey sk;

    private final Coin coin;

    private final Crypto crypto;

    private final Channel<VerificationKey, Signed<Packet<VerificationKey, P>>> channel;

    private final SortedSet<VerificationKey> addrs;

    private final long time; // The time at which the join is scheduled to happen.

    private final long amount;
    private final Address anon;
    private final Address change;
    private final Marshaller<Message.Atom> am;
    private final Marshaller<Packet<VerificationKey, P>> pm;
    private final PrintStream stream;

    public Report report = null;

    private final Executor exec;

    private Running running = null;

    Player(
         SigningKey sk,
         Bytestring session,
         Address anon,
         Address change, // Can be null to indicate no change address.
         SortedSet<VerificationKey> addrs,
         long time,
         long amount,
         Coin coin, // Connects us to the Bitcoin or other cryptocurrency netork.
         Crypto crypto,
         Channel<VerificationKey, Signed<Packet<VerificationKey, P>>> channel,
         Marshaller<Message.Atom> am,
         Marshaller<Packet<VerificationKey, P>> pm,
         Executor exec,
         PrintStream stream
    ) {
        if (sk == null || coin == null || session == null || addrs == null
                || crypto == null || anon == null || channel == null || exec == null) {
            throw new NullPointerException();
        }
        this.session = session;
        this.sk = sk;
        this.coin = coin;
        this.crypto = crypto;
        this.time = time;
        this.amount = amount;
        this.anon = anon;
        this.change = change;
        this.channel = channel;
        this.addrs = addrs;
        this.am = am;
        this.pm = pm;
        this.exec = exec;
        this.stream = stream;
    }

    public Running start() throws IOException, InterruptedException {
        if (running != null) return running;

        return new Running(new Connect<>(channel, crypto));
    }

    public class Running {

        // Wait until the appointed time.
        final Connect<VerificationKey, Signed<Packet<VerificationKey, P>>> connect;

        Running(Connect<VerificationKey, Signed<Packet<VerificationKey, P>>> connect) {
            this.connect = connect;
        }

        private Report playInner(Chan<Phase> ch) throws InterruptedException, IOException {

            // Remove me.
            SortedSet<VerificationKey> connectTo = new TreeSet<>();
            connectTo.addAll(addrs);
            connectTo.remove(sk.VerificationKey());

            long wait = time - System.currentTimeMillis();
            Thread.sleep(wait);

            // Begin connecting to all peers.
            final Collector<VerificationKey, Signed<Packet<VerificationKey, P>>> collector
                    = connect.connect(connectTo, 3);

            if (collector == null) throw new NullPointerException();

            // Run the protocol.
            try {
                // If the protocol returns correctly without throwing a Matrix, then
                // it has been successful.
                Messages messages = new Messages(session, sk, collector.connected, collector.inbox, am, pm);
                CoinShuffle cs = new CoinShuffle(messages, crypto, coin);
                return Report.success(cs.runProtocol(amount, sk, addrs, anon, change, ch));
            } catch (Matrix m) {
                return Report.failure(m, addrs);
            } catch (TimeoutException e) {
                return Report.timeout(e);
            } catch (CoinNetworkException
                    | InvalidParticipantSetException
                    | FormatException
                    | NoSuchAlgorithmException e) {
                stream.println(e.getMessage());
                throw new RuntimeException(e);
            } finally {
                connect.close();
            }

        }

        public synchronized Report play() throws IOException, InterruptedException {
            if (report != null) return report;

            final Chan<Phase> ch = new BasicChan<>(2);
            final Chan<Report> r = new BasicChan<>(2);

            stream.println("  Player " + sk.VerificationKey() + " begins " + session);

            exec.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        r.send(playInner(ch));
                    } catch (InterruptedException | IOException | NullPointerException e) {
                        throw new RuntimeException(e);
                    } finally {
                        ch.close();
                        r.close();
                    }
                }
            });

            while(true) {
                Phase phase = ch.receive();
                if (phase == null) break;
                stream.println("  Player " + sk.VerificationKey() + " reaches phase " + phase);
            }

            report = r.receive();
            if (report == null) throw new NullPointerException();
            return report;
        }

        public synchronized Future<Summable.SummableElement<Map<VerificationKey, Report>>> playConcurrent()
                throws InterruptedException {

            final Chan<Report> cr = new BasicChan<>(2);

            exec.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        cr.send(play());
                    } catch (InterruptedException | IOException | NullPointerException e) {
                        throw new RuntimeException(e);
                    } finally {
                        cr.close();
                    }
                }
            });

            return new Future<Summable.SummableElement<Map<VerificationKey, Report>>>() {
                private boolean done = false;

                @Override
                public boolean cancel(boolean b) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return done;
                }

                @Override
                public Summable.SummableElement<Map<VerificationKey, Report>> get()
                        throws InterruptedException, ExecutionException {

                    if (done) return null;
                    Report r = cr.receive();
                    done = true;
                    if (r == null) return null;
                    return new SummableMap<>(sk.VerificationKey(), r);
                }

                @Override
                public Summable.SummableElement<Map<VerificationKey, Report>> get(long l, TimeUnit timeUnit)
                        throws InterruptedException, ExecutionException,
                        java.util.concurrent.TimeoutException {

                    if (done) return null;
                    Report r = cr.receive(l, timeUnit);
                    done = true;
                    if (r == null) return null;
                    Map<VerificationKey, Report> map = new HashMap<>();
                    map.put(sk.VerificationKey(), r);
                    return new SummableMap<>(map);
                }
            };
        }
    }

    public static class Report {
        public final Transaction t;
        public final TimeoutException timeout;
        public final Matrix blame;

        private Report(Transaction t) {
            this.t = t;
            this.timeout = null;
            this.blame = null;
        }

        private Report(TimeoutException timeout) {
            this.t = null;
            this.timeout = timeout;
            this.blame = null;
        }

        private Report(Matrix blame) {
            this.blame = blame;
            timeout = null;
            t = null;
        }

        @Override
        public String toString() {
            if (t != null) {
                return "Successful round; transaction is " + t;
            }
            if (blame != null) {
                return "Unsuccessful round; blame is " + blame;
            }
            if (timeout != null) {
                return "Unsuccessful round; timeout error " + timeout;
            }
            throw new NullPointerException();
        }

        public static Report success(Transaction t) {
            return new Report(t);
        }

        public static Report failure(Matrix blame, SortedSet<VerificationKey> identities) {


            // The eliminated players.
            SortedSet<VerificationKey> eliminated = new TreeSet<>();

            // *** construct error report ***

            // We must construct a message that informs other players of who will be eliminated.
            // This message must say who will be eliminated and who will remain, and it must
            // provide evidence as to why the eliminated players should be eliminated, if it would
            // not be obvious to everyone.

            // The set of players who are not eliminated.
            SortedSet<VerificationKey> remaining = new TreeSet<>();

            // The set of players who have been blamed, the players blaming them,
            // and the evidence provided.
            Map<VerificationKey, Map<VerificationKey, Evidence>> blamed = new HashMap<>();

            // Next we go through players and find those which are not eliminated.
            for (VerificationKey player : identities) {
                // Who blames this player?
                Map<VerificationKey, Evidence> accusers = blame.getAccusations(player);

                // If nobody has blamed this player, then he's ok and can be stored in remaining.
                if (accusers == null || accusers.isEmpty()) {
                    remaining.add(player);
                } else {
                    blamed.put(player, accusers);
                }
            }

            // Stores evidences required to eliminate players.
            Map<VerificationKey, Evidence> evidences = new HashMap<>();

            // Next go through the set of blamed players and decide what to do with them.
            for (Map.Entry<VerificationKey, Map<VerificationKey, Evidence>> entry : blamed.entrySet()) {
                VerificationKey player = entry.getKey();

                Map<VerificationKey, Evidence> accusers = entry.getValue();

                // Does everyone blame this player except himself?
                Set<VerificationKey> everyone = new HashSet<>();
                everyone.addAll(identities);
                everyone.removeAll(accusers.keySet());

                if (everyone.size() == 0 || everyone.size() == 1 && everyone.contains(player)) {
                    eliminated.add(player); // Can eliminate this player without extra evidence.
                    continue;
                }

                // Why is this player blamed? Is it objective?
                // If not, include evidence.
                // sufficient contains sufficient evidence to eliminate the player.
                // (theoretically not all other players could have provided logically equivalent
                // evidence against him, so we just need sufficient evidence.)
                Evidence sufficient = null;
                f : for (Evidence evidence : accusers.values()) {
                    switch (evidence.reason) {
                            // TODO all cases other than default are not complete.
                        case DoubleSpend:
                            // fallthrough
                        case InsufficientFunds:
                            // fallthrough
                        case InvalidSignature: {
                            sufficient = evidence;
                            break;
                        }
                        case NoFundsAtAll: {
                            if (sufficient == null) {
                                sufficient = evidence;
                            }
                            break;
                        }
                        default: {
                            // Other cases than those specified above are are objective.
                            // We can be sure that other players agree
                            // that this player should be eliminated.
                            sufficient = null;
                            eliminated.add(player);
                            break f;
                        }
                    }
                }

                // Include evidence if required.
                if (sufficient != null) {
                    evidences.put(player, sufficient);
                }
            }

            // Remove eliminated players from blamed.
            for (VerificationKey player : eliminated) {
                blamed.remove(player);
            }

            if (blamed.size() > 0) {
                // TODO How could this happen and what to do about it?
            }

            return new Report(blame);
        }

        public static Report timeout(TimeoutException e) {
         return new Report(e);
      }
    }
}
