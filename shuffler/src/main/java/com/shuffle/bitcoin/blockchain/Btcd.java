/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import com.shuffle.bitcoin.CoinNetworkException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.store.BlockStoreException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Eugene Siegel on 4/22/16.
 */

// TODO
// No addrindex
/**
 *
 * This class allows lookup of transactions associated to any Bitcoin address.
 * This in turn, allows address balance lookups for any Bitcoin address.
 * The lookups are through Btcd.
 *
 *
 * Instructions:
 *
 * In your Btcd.conf file, set an rpcuser and rpcpass (rpclimituser & rpclimitpass also works).
 * Make sure addrindex = 1 (this indexes Bitcoin addresses) and notls = 1 (currently TLS is not supported)
 *
 * Alternatively, if you do not wish to edit the config file for addrindex and notls,
 * you can use the two flags in the command line:
 * "./btcd --addrindex --notls"
 *
 */

// TODO
// TLS

public class Btcd extends Bitcoin {

    private final String rpcuser;
    private final String rpcpass;
    private final URL url;

    public Btcd(NetworkParameters netParams, String rpcuser, String rpcpass)
            throws MalformedURLException {

        super(netParams, 0);
        this.rpcuser = rpcuser;
        this.rpcpass = rpcpass;

        if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
            url = new URL("http://127.0.0.1:8334");
        } else if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            url = new URL("http://127.0.0.1:18334");
        } else {
            throw new IllegalArgumentException("Invalid network parameters passed to btcd. ");
        }
    }

    /**
     * This method takes in a transaction hash and returns a bitcoinj transaction object.
     */
    synchronized org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {

        org.bitcoinj.core.Transaction tx;
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"getrawtransaction\", \"params\":[\"" + transactionHash + "\"]}";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        connection.setDoInput(true);
        OutputStream out = connection.getOutputStream();
        out.write(requestBody.getBytes());

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

            JSONObject json = new JSONObject(response.toString());
            String hexTx = (String) json.get("result");
            HexBinaryAdapter adapter = new HexBinaryAdapter();
            byte[] bytearray = adapter.unmarshal(hexTx);
            Context context = Context.getOrCreate(netParams);
            tx = new org.bitcoinj.core.Transaction(netParams, bytearray);

        } else {
            throw new IOException();
        }

        out.flush();
        out.close();

        return tx;

    }

    /**
     * This method will take in a set of UTXOs and return a List of all associated transactions.
     *
     * We don't use Btcd's "searchrawtransactions" RPC call because we are not looking for the
     * transactions pertaining to a particular address, but rather the transactions related to
     * a set of UTXOs.
     *
     * // TODO
     * // description
     */
    public synchronized List<Transaction> getTransactionsFromUtxosInner(HashSet<TransactionOutPoint> t) throws IOException {

        List<Transaction> txList = new ArrayList<>();
        HashSet<Transaction> checkDuplicateTx = new HashSet<>();
        for (TransactionOutPoint tO : t) {
            try {
                String txid = tO.getHash().toString();
                org.bitcoinj.core.Transaction tx = getTransaction(txid);
                boolean confirmed = !inMempool(txid);
                Transaction bTx = new Transaction(txid, tx, false, confirmed);
                if (!checkDuplicateTx.contains(bTx)) {
                    txList.add(bTx);
                }
                checkDuplicateTx.add(bTx);
            } catch (IOException e) {
                throw new IOException();
            }
        }

        return txList;

    }

    @Override
    protected boolean send(Bitcoin.Transaction t) throws ExecutionException, InterruptedException, CoinNetworkException {
        if (!t.canSend || t.sent) {
            return false;
        }

        try {
            String hexTx = DatatypeConverter.printHexBinary(t.bitcoinj().bitcoinSerialize());
            String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"sendrawtransaction\", \"params\":[\"" + hexTx + "\"]}";
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            Base64 b = new Base64();
            String authString = rpcuser + ":" + rpcpass;
            String encoding = b.encodeAsString(authString.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoding);
            connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
            OutputStream out = connection.getOutputStream();
            out.write(requestBody.getBytes());
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while (true) {
                try {
                    if ((line = rd.readLine()) == null) break;
                } catch (IOException e) {
                    return false;
                }
                response.append(line);
                response.append('\r');
            }
            rd.close();
            JSONObject json = new JSONObject(response.toString());
            if (json.isNull("result")) {
                JSONObject errorObj = json.getJSONObject("error");
                String errorMsg = errorObj.getString("message");
                String parsed = errorMsg.substring(0,37);
                // transaction is already in mempool, return true
                if (parsed.equals("TX rejected: already have transaction")) {
                    return true;
                } else {
                    throw new CoinNetworkException(errorMsg);
                }
            }

        } catch (IOException | BlockStoreException e) {
            return false;
        }

        return true;
    }

    synchronized boolean isUtxo(String hexTx, int vout) throws IOException {
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"gettxout\", \"params\":[\"" + hexTx + "\"";
        requestBody = requestBody.concat("," + Integer.toString(vout) + ",false]}");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        OutputStream out = connection.getOutputStream();
        out.write(requestBody.getBytes());
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException();
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while (true) {
            try {
                if ((line = rd.readLine()) == null) break;
            } catch (IOException e) {
                throw new IOException();
            }
            response.append(line);
            response.append('\r');
        }
        rd.close();

        JSONObject json = new JSONObject(response.toString());

        return !(json.isNull("result"));
    }

    private synchronized boolean inMempool(String transactionHash) throws IOException {
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"getrawmempool\", \"params\":[false]}";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        OutputStream out = connection.getOutputStream();
        out.write(requestBody.getBytes());
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException();
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while (true) {
            try {
                if ((line = rd.readLine()) == null) break;
            } catch (IOException e) {
                throw new IOException();
            }
            response.append(line);
            response.append('\r');
        }
        rd.close();
        JSONObject json = new JSONObject(response.toString());
        // TODO
        // What about an empty Btcd mempool ?
        if (json.isNull("result")) {
            JSONObject errorObj = json.getJSONObject("error");
            String errorMsg = errorObj.getString("message");
            if (errorMsg.isEmpty()) {
                throw new IOException();
            }
        }

        JSONArray jsonArray = json.getJSONArray("result");

        return jsonArray.toString().contains("\"" + transactionHash + "\"");
    }

    @Override
    protected synchronized List<Transaction> getTransactionsFromUtxos(HashSet<TransactionOutPoint> t) throws IOException, CoinNetworkException, AddressFormatException {
        return getTransactionsFromUtxosInner(t);
    }

}
