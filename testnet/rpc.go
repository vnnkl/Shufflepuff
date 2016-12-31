package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"

	"github.com/btcsuite/btcd/btcjson"
	"github.com/btcsuite/btcd/chaincfg"
	"github.com/btcsuite/btcd/chaincfg/chainhash"
	"github.com/btcsuite/btcutil"
)

var minConf = 6

// RPC represents a bitcoin wallet rpc server.
type RPC struct {
	user       string
	password   string
	server     string
	passphrase string
}

// sendPostRequest sends the marshalled JSON-RPC command using HTTP-POST mode
// to the server described in the passed config struct.  It also attempts to
// unmarshal the response as a JSON-RPC response and returns either the result
// field or the error field depending on whether or not there is an error.
func (rpc *RPC) sendPostRequest(marshalledJSON []byte) ([]byte, error) {
	// Generate a request to the configured RPC server.
	protocol := "http"
	url := protocol + "://" + rpc.server
	bodyReader := bytes.NewReader(marshalledJSON)
	httpRequest, err := http.NewRequest("POST", url, bodyReader)
	if err != nil {
		return nil, err
	}
	httpRequest.Close = true
	httpRequest.Header.Set("Content-Type", "application/json")

	// Configure basic access authorization.
	httpRequest.SetBasicAuth(rpc.user, rpc.password)

	// Create the new HTTP client and submit the request.
	httpClient := http.Client{
		Transport: &http.Transport{},
	}
	httpResponse, err := httpClient.Do(httpRequest)
	if err != nil {
		return nil, err
	}

	// Read the raw bytes and close the response.
	respBytes, err := ioutil.ReadAll(httpResponse.Body)
	httpResponse.Body.Close()
	if err != nil {
		err = fmt.Errorf("error reading json reply: %v", err)
		return nil, err
	}

	// Handle unsuccessful HTTP responses
	if httpResponse.StatusCode < 200 || httpResponse.StatusCode >= 300 {
		// Generate a standard error to return if the server body is
		// empty.  This should not happen very often, but it's better
		// than showing nothing in case the target server has a poor
		// implementation.
		if len(respBytes) == 0 {
			return nil, fmt.Errorf("%d %s", httpResponse.StatusCode,
				http.StatusText(httpResponse.StatusCode))
		}
		return nil, fmt.Errorf("%s", respBytes)
	}

	// Unmarshal the response.
	var resp btcjson.Response
	if err := json.Unmarshal(respBytes, &resp); err != nil {
		return nil, err
	}

	if resp.Error != nil {
		return nil, resp.Error
	}
	return resp.Result, nil
}

func (rpc *RPC) rpcCommand(command interface{}) (string, error) {
	// Marshal the command into a JSON-RPC byte slice in preparation for
	// sending it to the RPC server.
	marshalledJSON, err := btcjson.MarshalCmd(1, command)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}

	response, err := rpc.sendPostRequest(marshalledJSON)

	// TODO check for wallet passphrase error and unlock wallet.

	return string(response), err
}

// CreateNewAccount sends a createnewaccount command.
func (rpc *RPC) CreateNewAccount(account string) error {
	response, err := rpc.rpcCommand(btcjson.CreateNewAccountCmd{
		Account: account,
	})
	if err != nil {
		return err
	}

	// Expected response is null.
	if response != "null" {
		return errors.New(response)
	}

	return nil
}

// CreateNewAddress sends an rpc getnewaddress command.
func (rpc *RPC) CreateNewAddress(account string) (btcutil.Address, error) {
	response, err := rpc.rpcCommand(btcjson.GetNewAddressCmd{
		Account: &account,
	})
	if err != nil {
		return nil, err
	}

	// Expected response is an address.
	addr, err := btcutil.DecodeAddress(response, &chaincfg.TestNet3Params)
	if err != nil {
		return nil, fmt.Errorf("Could not decode address: %s; got error %s", response, err.Error())
	}

	return addr, nil
}

// SendFrom sends an rpc sendfrom command.
func (rpc *RPC) SendFrom(account string, to btcutil.Address, amount uint64) (*chainhash.Hash, error) {
	response, err := rpc.rpcCommand(btcjson.SendFromCmd{
		FromAccount: account,
		ToAddress:   to.EncodeAddress(),
		Amount:      float64(amount) / 100000000,
		MinConf:     &minConf,
	})
	if err != nil {
		return nil, err
	}
	hash, err := chainhash.NewHashFromStr(string(response))
	if err != nil {
		return nil, fmt.Errorf("Could not decode tx hash: %s; got error %s", response, err.Error())
	}

	return hash, nil
}

// SendMany sends an rpc sendmany command.
func (rpc *RPC) SendMany(account string, amounts map[btcutil.Address]uint64) (*chainhash.Hash, error) {
	floatAmounts := make(map[string]float64)
	for addr, amount := range amounts {
		floatAmounts[addr.EncodeAddress()] = float64(amount) / 100000000
	}
	response, err := rpc.rpcCommand(btcjson.SendManyCmd{
		FromAccount: account,
		Amounts:     floatAmounts,
		MinConf:     &minConf,
	})
	if err != nil {
		return nil, err
	}
	hash, err := chainhash.NewHashFromStr(string(response))
	if err != nil {
		return nil, fmt.Errorf("Could not decode tx hash: %s; got error %s", response, err.Error())
	}

	return hash, nil
}

// dumpPrivateKey sends a dumpprivkey command.
func (rpc *RPC) dumpPrivateKey(address btcutil.Address) (*btcutil.WIF, error) {
	response, err := rpc.rpcCommand(btcjson.DumpPrivKeyCmd{
		Address: address.EncodeAddress(),
	})
	if err != nil {
		return nil, err
	}

	// Expected response is an address.
	wif, err := btcutil.DecodeWIF(response)
	if err != nil {
		return nil, fmt.Errorf("Could not decode WIF: %s; got error %s", response, err.Error())
	}

	return wif, nil
}

// WalletPassphrase sends a walletpassphrase command.
func (rpc *RPC) WalletPassphrase(timeout uint32) error {
	response, err := rpc.rpcCommand(btcjson.WalletPassphraseCmd{
		Passphrase: rpc.passphrase,
		Timeout:    int64(timeout),
	})
	if err != nil {
		return err
	}

	// Expected response is null.
	if response != "null" {
		return errors.New(response)
	}

	return nil
}

// CreateNewPrivKey creates a new private key.
func (rpc *RPC) CreateNewPrivKey(account string) (*btcutil.WIF, error) {
	// Create a new address.
	address, err := rpc.CreateNewAddress(account)
	if err != nil {
		return nil, err
	}

	// Then get the private key.
	wif, err := rpc.dumpPrivateKey(address)
	if err != nil {
		return nil, err
	}

	return wif, nil
}
