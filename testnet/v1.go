package main

import (
	"encoding/json"
	"errors"

	"github.com/btcsuite/btcutil"
)

// PlayerV1 represents a simulated player. He has some number of in keys
// which will be merged to create the join transaction, one anon key
// to be his anonymized output, and a change address.
type PlayerV1 struct {
	key    *btcutil.WIF
	in     []*Address
	anon   btcutil.Address
	change btcutil.Address
}

// ShuffleV1 represents a simulated shuffle, with input and output data
// for all players.
type ShuffleV1 []PlayerV1

// ToJSON encodes a Shuffle as a JSON string.
func (sh ShuffleV1) ToJSON() string {
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

// GenerateShuffleV1 create a shuffle object by interacting over rpc with a wallet.
func GenerateShuffleV1(rpc *RPC, defaultAccount, testAccount string, inputs []uint64) (Shuffle, error) {
	// Create test account.
	err := rpc.CreateNewAccount(testAccount)
	if err != nil {
		return nil, err
	}

	players := make([]PlayerV1, 0, len(inputs))

	for _, inputNum := range inputs {
		player := PlayerV1{
			in: make([]*Address, inputNum),
		}

		for i := uint64(0); i < inputNum; i++ {
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

	return ShuffleV1(players), nil
}

// Fund funds the addresses in the Shuffle object from the default account.
func (sh ShuffleV1) Fund(rpc *RPC, defaultAccount string, funds []uint64) error {
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
func (sh ShuffleV1) Run(session string, amount, fee, initPort uint64) {
	// TODO
}

// Clear moves all funds in testAccount to the to address, other than the
// miner's fee.
func (sh ShuffleV1) Clear(rpc *RPC, testAccount string, to btcutil.Address, fee uint64) {
	// TODO
}
