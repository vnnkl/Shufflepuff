package main

import (
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
func DecodeShuffleFromJSON(JSON string) (Shuffle, error) {
	// First try V0.
	shuffleV0, errV0 := DecodeShuffleV0FromJSON(JSON)
	if errV0 == nil {
		return shuffleV0, nil
	}

	// Then try V1.
	shuffleV1, errV1 := DecodeShuffleV1FromJSON(JSON)
	if errV1 == nil {
		return shuffleV1, nil
	}

	return nil, errV0
}
