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
	"github.com/btcsuite/btcutil"
)

// RPC represents a bitcoin wallet rpc server. 
type RPC struct {
	user     string
	password string
	server   string
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
func (rpc *RPC) CreateNewAccount(name string) error {
	response, err := rpc.rpcCommand(btcjson.CreateNewAccountCmd{
		Account: name,
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

// CreateNewAddress sends a createnewaddress command.
func (rpc *RPC) CreateNewAddress() (btcutil.Address, error) {
	response, err := rpc.rpcCommand(btcjson.GetNewAddressCmd{})

	if err != nil {
		return nil, err
	}

	// Expected response is an address.
	addr, err := btcutil.DecodeAddress(response, &chaincfg.TestNet3Params)
	if err != nil {
		return nil, fmt.Errorf("Could not decode address %s; got error %s", response, err.Error())
	}

	return addr, nil
}

func writeName(w *bytes.Buffer, name string) {
	w.WriteString("'")
	w.WriteString(name)
	w.WriteString("'")
}

func availableMethods() string {
	var w bytes.Buffer
	w.WriteString("Available methods are ")
	i := 1
	for name := range methods {
		if i == len(methods) {
			if i == 1 {
				writeName(&w, name)
				w.WriteString(".")

				break
			}

			w.WriteString("and ")
			writeName(&w, name)
			w.WriteString(".")

			break
		}

		writeName(&w, name)
		w.WriteString(", ")
		i++
	}
	return w.String()
}

func maxMethodLength() int {
	max := 0
	for name := range methods {
		if len(name) > max {
			max = len(name)
		}
	}

	return max
}

func whitespace(w *bytes.Buffer, n int) {
	for i := 0; i < n; i++ {
		w.WriteString(" ")
	}
}

func helpAll() []string {
	lines := make([]string, 1, len(methods)+1)
	lines[0] = "Methods:"

	max := maxMethodLength()

	for name, method := range methods {
		for _, helpMsg := range method(nil) {
			var w bytes.Buffer
			w.WriteString("\t")
			whitespace(&w, max-len(name))
			w.WriteString(name)
			w.WriteString(" ")
			w.WriteString(helpMsg)
			lines = append(lines, w.String())
		}
	}

	return lines
}

func help(args []string) []string {
	if args == nil || len(args) > 1 {
		return []string{"         -- show help for all methods.", "<method> -- show help for a particular method."}
	}

	if len(args) == 0 {
		return helpAll()
	}

	if method, ok := methods[args[0]]; ok {
		return method(nil)
	}

	return append([]string{"Invalid method specified."}, help(nil)...)
}

func setup(args []string) []string {
	if args == nil {
		return []string{"not implemented"}
	}

	return nil
}

func run(args []string) []string {
	if args == nil {
		return []string{"not implemented"}
	}

	return nil
}

func cleanup(args []string) []string {
	if args == nil {
		return []string{"not implemented"}
	}

	return nil
}

// methods contains the allowed operations of the simulation program.
// It is a map from the method name to a function which takes the rest
// of the args provided by the user and returns a list of lines that
// say what happened.
var methods = map[string]func([]string) []string{
	"setup":   setup,
	"run":     run,
	"cleanup": cleanup,
}

// Read a message from the command-line and turn it into the appropriate
// Shufflepuff
func main() {
	println("Welcome to the Shufflepuff Testnet simulator!")

	// Insert methods into the map.
	methods["help"] = help

	args := os.Args[1:]

	if len(args) == 0 {
		println("No method specified. Use 'help' for more information.")
		println(availableMethods())

		return
	}

	methodName := args[0]
	if method, ok := methods[methodName]; ok {
		for _, line := range method(args[1:]) {
			println(line)
		}

		return
	}

	println("Invalid method ", methodName, ".")
	println(availableMethods())

	return
}
