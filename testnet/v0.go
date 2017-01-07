package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"net"
	"os/exec"
	"strconv"
	"time"

	"github.com/btcsuite/btcutil"
	"github.com/btcsuite/btcutil/base58"
)

// PlayerV0 represents a simulated player. He has some number of in keys
// which will be merged to create the join transaction, one anon key
// to be his anonymized output, and a change address.
type PlayerV0 struct {
	key    *Address
	anon   btcutil.Address
	change btcutil.Address
}

// ShuffleV0 represents a simulated shuffle, with input and output data
// for all players.
type ShuffleV0 []PlayerV0

// ToJSON encodes a Shuffle as a JSON string.
func (sh ShuffleV0) ToJSON() string {
	players := make([]struct {
		key struct {
			private string
			public  string
		}
		anon   string
		change string
	}, 0, len(sh))

	for _, player := range sh {
		players = append(players, struct {
			key struct {
				private string
				public  string
			}
			anon   string
			change string
		}{
			key: struct {
				private string
				public  string
			}{
				private: player.key.private.String(),
				public:  player.key.public.EncodeAddress(),
			},
			anon:   player.anon.String(),
			change: player.change.EncodeAddress(),
		})
	}

	j, _ := json.Marshal(players)
	return string(j)
}

// GenerateShuffleV0 create a shuffle object by interacting over rpc with a wallet.
func GenerateShuffleV0(rpc *RPC, defaultAccount, testAccount string, numPlayers uint32) (Shuffle, error) {
	// Create test account.
	err := rpc.CreateNewAccount(testAccount)
	if err != nil {
		return nil, err
	}

	players := make([]PlayerV0, 0, numPlayers)

	for i := uint32(0); i < numPlayers; i++ {
		player := PlayerV0{}

		key, err := rpc.CreateNewPrivKey(testAccount)
		if err != nil {
			return nil, err
		}

		player.key = key

		anon, err := rpc.CreateNewAddress(testAccount)
		if err != nil {
			return nil, err
		}

		player.anon = anon

		address, err := rpc.CreateNewAddress(testAccount)
		if err != nil {
			return nil, err
		}

		player.change = address

		players = append(players, player)
	}

	return ShuffleV0(players), nil
}

// Fund funds the addresses in the Shuffle object from the default account.
func (sh ShuffleV0) Fund(rpc *RPC, defaultAccount string, funds []uint64) error {
	// TODO test whether sufficient funds exist.

	if len(funds) != len(sh) {
		return errors.New("Must have as many funds as players.")
	}

	to := make(map[btcutil.Address]uint64)
	// create a map from address to amounts.
	for i := 0; i < len(sh); i++ {
		to[sh[i].key.public] = funds[i]
	}

	_, err := rpc.SendMany(defaultAccount, to)
	if err != nil {
		return err
	}

	return nil
}

// Run starts several instances of Shufflepuff and runs the protocol.
func (sh ShuffleV0) Run(session string, amount, fee, initPort uint64) {
	port := initPort
	when := time.Now().Add(time.Minute)
	for _, player := range sh {
		var w bytes.Buffer
		w.WriteString("java -jar shuffler.jar shuffle --blockchain test --query btcd")
		w.WriteString("--session '")
		w.WriteString(session)
		w.WriteString("' --amount ")
		w.WriteString(strconv.FormatUint(amount, 10))
		w.WriteString(" --time ")
		w.WriteString(strconv.FormatUint(uint64(when.Unix()), 10))
		w.WriteString(" --fee ")
		w.WriteString(strconv.FormatUint(fee, 10))
		w.WriteString(" --port ")
		w.WriteString(strconv.FormatUint(port, 10))
		w.WriteString(" --key ")
		w.WriteString(base58.Encode(player.key.private.SerializePubKey()))
		w.WriteString(" --anon ")
		w.WriteString(player.anon.EncodeAddress())
		w.WriteString(" --change ")
		w.WriteString(player.change.EncodeAddress())
		w.WriteString(" --peers '[")
		last := false
		for i := uint64(0); i < uint64(len(sh)); i++ {
			p := i + initPort

			if p == port {
				continue
			}

			if last {
				w.WriteString(", ")
			}

			w.WriteString("{\"address\":\"")
			w.WriteString(net.JoinHostPort("127.0 0.1:", strconv.FormatUint(p, 10)))
			w.WriteString("\", \"key\":\"")
			w.WriteString(base58.Encode(sh[i].key.private.SerializePubKey()))
			w.WriteString("\"}")

			last = true
		}
		w.WriteString("]'")

		port++

		go func() {
			exec.Command(string(w.Bytes())).Run()
		}()
	}
}

// Clear moves all funds in testAccount to the to address, other than the
// miner's fee.
func (sh ShuffleV0) Clear(rpc *RPC, testAccount string, to btcutil.Address, fee uint64) {
	// TODO
}
