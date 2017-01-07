package main

import (
	"encoding/json"

	"github.com/btcsuite/btcd/chaincfg"
	"github.com/btcsuite/btcutil"
)

// Shuffle represents a shuffle simulations. Different implementations
// of this interface represent different versions of the protocol.
type Shuffle interface {
	ToJSON() string
	Fund(rpc *RPC, defaultAccount string, funds []uint64) error
	Run(session string, amount, fee, initPort uint64)
	Clear(rpc *RPC, testAccount string, to btcutil.Address, fee uint64)
}

// Address contains the private key and the Bitcoin address derived from it.
type Address struct {
	private *btcutil.WIF
	public  btcutil.Address
}

// DecodeShuffleFromJSON tries to create a Shuffle type given a json string.
// TODO: right now it only decodes V1. It should be able to do either V0 or V1.
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

	shuffle := make([]PlayerV1, 0, len(players))

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

		shuffle = append(shuffle, PlayerV1{
			key:    key,
			in:     in,
			anon:   anon,
			change: change,
		})
	}

	return ShuffleV1(shuffle), nil
}
