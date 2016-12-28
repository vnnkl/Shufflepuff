package main

import (
	"bytes"
	"os"
)

// methods contains the allowed operations of the simulation program.
// It is a map from the method name to a function which takes the rest
// of the args provided by the user and returns a list of lines that
// say what happened.
var methods = make(map[string]func([]string) []string)

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

// main
func main() {
	println("Welcome to the Shufflepuff Testnet simulator!")

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
