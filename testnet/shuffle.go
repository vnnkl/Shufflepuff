package main

import (
	"encoding/json"

	"github.com/btcsuite/btcd/chaincfg"
	"github.com/btcsuite/btcutil"
)

// Player represents a simulated player. He has some number of in keys
// which will be merged to create the join transaction, one anon key
// to be his anonymized output, and a change address.
type Player struct {
	in     []*btcutil.WIF
	anon   *btcutil.WIF
	change btcutil.Address
}

// Shuffle represents a simulated shuffle, with input and output data
// for all players.
type Shuffle []Player

// ToJSON encodes a Shuffle as a JSON string. 
func (sh Shuffle) ToJSON() string {
	players := make([]struct {
		in     []string
		anon   string
		change string
	}, 0, len(sh))

	for _, player := range sh {
		in := make([]string, 0, len(player.in))
		for _, wif := range player.in {
			in = append(in, wif.String())
		}

		players = append(players, struct {
			in     []string
			anon   string
			change string
		}{
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
		in     []string
		anon   string
		change string
	}

	err := json.Unmarshal([]byte(JSON), &players)
	if err != nil {
		return nil, err
	}

	shuffle := make([]Player, 0, len(players))

	for _, player := range players {
		in := make([]*btcutil.WIF, 0, len(player.in))
		for _, i := range player.in {
			wif, err := btcutil.DecodeWIF(i)
			if err != nil {
				return nil, err
			}
			in = append(in, wif)
		}

		anon, err := btcutil.DecodeWIF(player.anon)
		if err != nil {
			return nil, err
		}

		change, err := btcutil.DecodeAddress(player.change, &chaincfg.TestNet3Params)
		if err != nil {
			return nil, err
		}

		shuffle = append(shuffle, Player{
			in:     in,
			anon:   anon,
			change: change,
		})
	}

	return Shuffle(shuffle), nil
}
