package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"net"
	"os/exec"
	"strconv"
	"time"

	"github.com/btcsuite/btcd/chaincfg"
	"github.com/btcsuite/btcutil"
	"github.com/btcsuite/btcutil/base58"
)

// Address contains the private key and the Bitcoin address derived from it.
type Address struct {
	private *btcutil.WIF
	public  btcutil.Address
}

// Player represents a simulated player. He has some number of in keys
// which will be merged to create the join transaction, one anon key
// to be his anonymized output, and a change address.
type Player struct {
	key    *btcutil.WIF
	in     []*Address
	anon   btcutil.Address
	change btcutil.Address
}

// Shuffle represents a simulated shuffle, with input and output data
// for all players.
type Shuffle []Player

// ToJSON encodes a Shuffle as a JSON string.
func (sh Shuffle) ToJSON() string {
	players := make([]struct {
		key string
		in  []struct {
			private string
			public  string
		}
		anon   string
		change string
	}, 0, len(sh))

	for _, player := range sh {
		in := make([]struct {
			private string
			public  string
		}, 0, len(player.in))
		for _, address := range player.in {
			in = append(in, struct {
				private string
				public  string
			}{
				private: address.private.String(),
				public:  address.public.EncodeAddress(),
			})
		}

		players = append(players, struct {
			key string
			in  []struct {
				private string
				public  string
			}
			anon   string
			change string
		}{
			key:    player.key.String(),
			in:     in,
			anon:   player.anon.String(),
			change: player.change.EncodeAddress(),
		})
	}

	j, _ := json.Marshal(players)
	return string(j)
}

// DecodeShuffleFromJSON tries to create a Shuffle type given a json string.
func DecodeShuffleFromJSON(JSON string) (Shuffle, error) {
	var players []struct {
		key string
		in  []struct {
			private string
			public  string
		}
		anon   string
		change string
	}

	err := json.Unmarshal([]byte(JSON), &players)
	if err != nil {
		return nil, err
	}

	shuffle := make([]Player, 0, len(players))

	for _, player := range players {
		in := make([]*Address, 0, len(player.in))
		for _, i := range player.in {
			wif, err := btcutil.DecodeWIF(i.private)
			if err != nil {
				return nil, err
			}
			address, err := btcutil.DecodeAddress(i.public, &chaincfg.TestNet3Params)
			if err != nil {
				return nil, err
			}

			in = append(in, &Address{
				private: wif,
				public:  address,
			})
		}

		anon, err := btcutil.DecodeAddress(player.anon, &chaincfg.TestNet3Params)
		if err != nil {
			return nil, err
		}

		change, err := btcutil.DecodeAddress(player.change, &chaincfg.TestNet3Params)
		if err != nil {
			return nil, err
		}

		key, err := btcutil.DecodeWIF(player.key)
		if err != nil {
			return nil, err
		}

		shuffle = append(shuffle, Player{
			key:    key,
			in:     in,
			anon:   anon,
			change: change,
		})
	}

	return Shuffle(shuffle), nil
}

// GenerateShuffle create a shuffle object by interacting over rpc with a wallet.
func GenerateShuffle(rpc *RPC, defaultAccount, testAccount string, inputNums []uint32) (Shuffle, error) {
	// Create test account.
	err := rpc.CreateNewAccount(testAccount)
	if err != nil {
		return nil, err
	}

	players := make([]Player, 0, len(inputNums))

	for _, inputNum := range inputNums {
		player := Player{
			in: make([]*Address, inputNum),
		}

		for i := uint32(0); i < inputNum; i++ {
			wif, err := rpc.CreateNewPrivKey(testAccount)
			if err != nil {
				return nil, err
			}

			player.in[i] = wif
		}

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

	return Shuffle(players), nil
}

// Fund funds the addresses in the Shuffle object from the default account.
func (sh Shuffle) Fund(rpc *RPC, defaultAccount string, funds []uint64) error {
	// TODO test whether sufficient funds exist.

	if len(funds) != len(sh) {
		return errors.New("Must have as many funds as players.")
	}

	to := make(map[btcutil.Address]uint64)
	// create a map from address to amounts.
	for i := 0; i < len(sh); i++ {
		each := funds[i] / uint64(len(sh[i].in))

		for _, address := range sh[i].in {
			to[address.public] = each
		}
	}

	_, err := rpc.SendMany(defaultAccount, to)
	if err != nil {
		return err
	}

	return nil
}

// Run starts several instances of Shufflepuff and runs the protocol.
func (sh Shuffle) Run(session string, amount, fee, initPort uint64) {
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
		w.WriteString(base58.Encode(player.key.SerializePubKey()))
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
			w.WriteString(base58.Encode(sh[i].key.SerializePubKey()))
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
func (sh Shuffle) Clear(rpc *RPC, testAccount string, to btcutil.Address, fee uint64) {
	// TODO
}
