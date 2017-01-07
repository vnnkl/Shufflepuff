package main

import (
	"errors"
	"os"
	"path/filepath"
	"strings"

	flags "github.com/jessevdk/go-flags"
)

// Setup contains parameters read from the command-line for setting up a join.
type Setup struct {
	user           string   `long:"user" description:"The rpc username."`
	password       string   `long:"password" description:"The rpc password."`
	passphrase     string   `long:"passphrase" description:"The wallet password."`
	players        uint32   `long:"players" description:"The number of players."`
	amount         uint64   `long:"amount" description:"The amount to be joined."`
	initialFunds   []uint64 `long:"initialfunds" description:"Initial funds per player."`
	defaultAccount string   `long:"defaultaccount" description:"Name of account containing the initial funds."`
	testAccount    string   `long:"testaccount" description:"Account to be used for the test itself."`
}

// RPC generates an RPC struct from a Setup struct.
func (s *Setup) RPC() *RPC {
	return &RPC{
		user:       s.user,
		password:   s.password,
		passphrase: s.passphrase,
		server:     defaultServer,
	}
}

// newSetupParser returns a new command line flags parser.
func newSetupParser(setup *Setup) *flags.Parser {
	appName := filepath.Base(os.Args[0])
	appName = strings.TrimSuffix(appName, filepath.Ext(appName))

	p := flags.NewNamedParser(appName, flags.Default)

	p.AddGroup("Setup Options", "", setup)

	return p
}

// LoadSetup reads a set of arguments and creates a Setup struct.
func LoadSetup(args []string) (*Setup, error) {
	setup := Setup{
		user:       defaultUsername,
		password:   defaultPassword,
		passphrase: defaultPassphrase,
	}

	_, err := newSetupParser(&setup).ParseArgs(args)
	if err != nil {
		return nil, err
	}

	if setup.players == 0 {
		return nil, errors.New("Must specify a number of players >= 2")
	}

	if setup.amount == 0 {
		return nil, errors.New("No amount specified")
	}

	if setup.defaultAccount == "" {
		return nil, errors.New("Must specify a default account.")
	}

	if setup.testAccount == "" {
		return nil, errors.New("Must specify a test account.")
	}

	if setup.initialFunds == nil {
		return nil, errors.New("Must specify initial funds per player.")
	}

	return &setup, nil
}
