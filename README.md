# Compiler
### a simple compiler powered by https://github.com/radinParsaei/VM https://github.com/radinParsaei/CompilerBackend and https://github.com/radinParsaei/compiler-test

## compile
to compile this project, use `make` in the project root directory
suggestion: install gmplib using `brew install gmp` on macOS or `sudo apt-get install libgmp3-dev` on Debian based distros, `sudo yum install gmp-devel` or `sudo dnf install gmp-devel` on RHEL based distros (like centOS) and instead of `make` run `make USE_GMP_LIB=1`

### if a problem occurred
1. try the suggestion and use gmplib
2. run `git submodule update --init --recursive --remote` and try again

### make a native image
if you have Graal VM installation use `make; make native-image` or `make USE_GMP_LIB=1; make native-image` now you can checkout ./output that's fast and native (you need to install native-image with `gu install native-image`)

### run
`java -jar output.jar <filename>` or `./output <filename>`
`java -jar output.jar --shell` or `./output --shell` to get a shell
`java -jar output.jar --output=a <filename>` or `./output --output=a <filename> to compile a code to VM assembly
`java -jar output.jar --serialize=a <filename>` to serialize a program
and to execute it use java -jar <serializedfile> or nothing (graalVM doesn't support serialization)



error icon in the web comes from https://www.flaticon.com/premium-icon/cancel_1008927?term=cancel&page=1&position=12&page=1&position=12&related_id=1008927&origin=tag

run `clonePKG.sh` first
