package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	flags "github.com/jessevdk/go-flags"
)

const defaultFilename = "shuffle.dat"

// Config contains parameters read from the command-line for setting up a join.
type Config struct {
	user           string   `long:"user" description:"The rpc username."`
	password       string   `long:"password" description:"The rpc password."`
	passphrase     string   `long:"passphrase" description:"The wallet password."`
	session        string   `long:"session" description:"the session identifier."`
	players        uint32   `long:"players" description:"The number of players."`
	amount         uint64   `long:"amount" description:"The amount to be joined."`
	initialFunds   []uint64 `long:"initialfunds" description:"Initial funds per player."`
	defaultAccount string   `long:"defaultaccount" description:"Name of account containing the initial funds."`
	testAccount    string   `long:"testaccount" description:"Account to be used for the test itself."`
	filename       string   `long:"filename" description:"File to save data for join."`
}

// RPC generates an RPC struct from a Setup struct.
func (cfg *Config) RPC() *RPC {
	return &RPC{
		user:       cfg.user,
		password:   cfg.password,
		passphrase: cfg.passphrase,
		server:     defaultServer,
	}
}

// newParser returns a new command line flags parser.
func newParser(cfg *Config) *flags.Parser {
	appName := filepath.Base(os.Args[0])
	appName = strings.TrimSuffix(appName, filepath.Ext(appName))

	p := flags.NewNamedParser(appName, flags.Default)

	p.AddGroup("Setup Options", "", cfg)

	return p
}

// LoadSetup reads a set of arguments and creates a Setup struct.
func LoadSetup(args []string) (*Config, error) {
	cfg := Config{
		user:       defaultUsername,
		password:   defaultPassword,
		passphrase: defaultPassphrase,
		filename:   defaultFilename,
	}

	_, err := newParser(&cfg).ParseArgs(args)
	if err != nil {
		return nil, err
	}

	if cfg.players == 0 {
		return nil, errors.New("Must specify a number of players >= 2")
	}

	if cfg.defaultAccount == "" {
		return nil, errors.New("Must specify a default account.")
	}

	if cfg.testAccount == "" {
		return nil, errors.New("Must specify a test account.")
	}

	if cfg.initialFunds == nil {
		return nil, errors.New("Must specify initial funds per player.")
	}

	// Check if the file exists. (It should not exist).
	_, err = os.Stat(cfg.filename)
	if err == nil {
		return nil, fmt.Errorf("File %s exists already. Is a join already in progress?", cfg.filename)
	}

	return &cfg, nil
}

// LoadRun reads a set of arguments for the run command.
func LoadRun(args []string) (*Config, error) {
	cfg := Config{
		user:       defaultUsername,
		password:   defaultPassword,
		passphrase: defaultPassphrase,
		filename:   defaultFilename,
	}

	_, err := newParser(&cfg).ParseArgs(args)
	if err != nil {
		return nil, err
	}

	if cfg.defaultAccount == "" {
		return nil, errors.New("Must specify a default account.")
	}

	if cfg.testAccount == "" {
		return nil, errors.New("Must specify a test account.")
	}

	if cfg.amount == 0 {
		return nil, errors.New("No amount specified")
	}

	if cfg.session == "" {
		return nil, errors.New("Must specify a session id.")
	}

	// Check if the file exists. It should exist.
	_, err = os.Stat(cfg.filename)
	if err != nil {
		return nil, fmt.Errorf("File %s does not exist.", cfg.filename)
	}

	return &cfg, nil
}
