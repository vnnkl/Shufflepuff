package main

import (
	"bytes"
	"io/ioutil"
	"os"
)

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

func cfg(args []string) []string {
	if args == nil {
		var b bytes.Buffer
		newParser(&Config{}).WriteHelp(&b)
		return []string{"", b.String()}
	}

	cfg, err := LoadSetup(args)
	if err != nil {
		return []string{err.Error()}
	}

	rpc := cfg.RPC()

	shuffle, err := GenerateShuffleV0(rpc, cfg.defaultAccount, cfg.testAccount, cfg.players)
	if err != nil {
		return []string{err.Error()}
	}

	err = shuffle.Fund(rpc, cfg.defaultAccount, cfg.initialFunds)
	if err != nil {
		return []string{err.Error()}
	}

	// Save the data to a file.
	err = ioutil.WriteFile(cfg.filename, []byte(shuffle.ToJSON()), 0644)
	if err != nil {
		return []string{err.Error()}
	}

	return nil
}

const defaultInitPort = 5001

func run(args []string) []string {
	if args == nil {
		return []string{"not implemented"}
	}

	cfg, err := LoadRun(args)
	if err != nil {
		return []string{err.Error()}
	}

	// Read JSON string from file.
	json, err := ioutil.ReadFile(cfg.filename)
	if err != nil {
		return []string{err.Error()}
	}

	shuffle, err := DecodeShuffleFromJSON(string(json))
	if err != nil {
		return []string{err.Error()}
	}

	// Run the protocol.
	shuffle.Run(cfg.session, cfg.amount, 0, defaultInitPort)

	return nil
}

func cleanup(args []string) []string {
	if args == nil {
		return []string{"not implemented"}
	}

	return nil // TODO
}

// methods contains the allowed operations of the simulation program.
// It is a map from the method name to a function which takes the rest
// of the args provided by the user and returns a list of lines that
// say what happened.
var methods = map[string]func([]string) []string{
	"cfg": cfg,
	"run": run,
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
