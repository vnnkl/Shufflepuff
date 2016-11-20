/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.CoinNetworkException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.store.BlockStoreException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;


/**
 * Created by Eugene Siegel on 4/22/16.
 */

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

// TODO TLS

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

        org.bitcoinj.core.Transaction tx = null;
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

        }

        out.flush();
        out.close();

        return tx;

    }

    /**
     * This method will take in an address hash and return a List of all transactions associated with
     * this address.  These transactions are in bitcoinj's Transaction format.
     */
    public synchronized List<Transaction> getAddressTransactionsInner(HashSet<TransactionOutPoint> t) throws IOException {

        /*
        List<Transaction> txList = null;
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"searchrawtransactions\", \"params\":[\"" + address + "\"]}";

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
            JSONArray jsonarray = null;
            txList = new LinkedList<>();
            if (json.isNull("result")) {
                return txList;
            } else {
                jsonarray = json.getJSONArray("result");
            }
            for (int i = 0; i < jsonarray.length(); i++) {
                JSONObject currentJson = jsonarray.getJSONObject(i);
                String txid = currentJson.get("txid").toString();
                HexBinaryAdapter adapter = new HexBinaryAdapter();
                byte[] bytearray = adapter.unmarshal(currentJson.get("hex").toString());
                // is this context necessary?
                Context context = Context.getOrCreate(netParams);
                int confirmations = Integer.parseInt(currentJson.get("confirmations").toString());
                boolean confirmed;
                if (confirmations == 0) {
                    confirmed = false;
                } else {
                    confirmed = true;
                }
                org.bitcoinj.core.Transaction bitTx = new org.bitcoinj.core.Transaction(netParams, bytearray);
                Transaction tx = new Transaction(txid, bitTx, false, confirmed);
                txList.add(tx);
            }

        }

        out.flush();
        out.close();

        return txList;*/
        return null;

    }

    @Override
    protected boolean send(Bitcoin.Transaction t) throws ExecutionException, InterruptedException, CoinNetworkException {
        if (!t.canSend || t.sent) {
            return false;
        }

        String hexTx = null;
        try {
            hexTx = DatatypeConverter.printHexBinary(t.bitcoinj().bitcoinSerialize());
        } catch (BlockStoreException e) {
            return false;
        } catch (IOException er) {
            return false;
        }
        String requestBody = "{\"jsonrpc\":\"2.0\",\"id\":\"null\",\"method\":\"sendrawtransaction\", \"params\":[\"" + hexTx + "\"]}";

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            return false;
        }
        connection.setDoOutput(true);
        try {
            connection.setRequestMethod("POST");
        } catch (java.net.ProtocolException e) {
            return false;
        }
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        Base64 b = new Base64();
        String authString = rpcuser + ":" + rpcpass;
        String encoding = b.encodeAsString(authString.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + encoding);
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        connection.setDoInput(true);
        OutputStream out;
        try {
            out = connection.getOutputStream();
        } catch (IOException e) {
            return false;
        }
        try {
            out.write(requestBody.getBytes());
        } catch (IOException e) {
            return false;
        }

        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return false;
        } catch (IOException e) {
            return false;
        }

        InputStream is;
        try {
            is = connection.getInputStream();
        } catch (IOException e) {
            return false;
        }
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

        try {
            rd.close();
        } catch (IOException e) {
            return false;
        }

        JSONObject json = new JSONObject(response.toString());
        if (json.isNull("result")) {
            JSONObject errorObj = json.getJSONObject("error");
            String errorMsg = errorObj.getString("message");
            String parsed = errorMsg.substring(0,37);
            // transaction is already in mempool, return true
            if (parsed.equals("TX rejected: already have transaction")) {
                return true;
            }
            throw new CoinNetworkException(errorMsg);
        }

        return true;
    }

    synchronized boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException {
        return false;
    }



    @Override
    protected synchronized List<Transaction> getAddressTransactions(HashSet<TransactionOutPoint> t) throws IOException, CoinNetworkException, AddressFormatException {
        return getAddressTransactionsInner(t);
    }

}
