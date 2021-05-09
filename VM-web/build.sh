if [[ -z "$LIBS_FLAGS" ]]
then
	LIBS_FLAGS="-I../CompilerBackend/VM/libs/random ../CompilerBackend/VM/libs/random/random.cpp -I../CompilerBackend/VM/libs/input ../CompilerBackend/VM/libs/input/input.cpp -DTARGET=Emscripten"
fi
emcc -c ../CompilerBackend/VM/BigNumber/src/BigNumber/number.c
em++ -Os -std=c++14 -DSTD_INCLUDED -DUSE_READLINE -I../CompilerBackend/VM/Value -I../CompilerBackend/VM -I../CompilerBackend/VM/libs -s WASM=1 --pre-js pre.js -DSTATIC_BUILD_MODE $LIBS_FLAGS -s MAIN_MODULE=1 -s "EXTRA_EXPORTED_RUNTIME_METHODS=['ccall', 'stringToUTF8']" -I../CompilerBackend/VM/BigNumber/src/BigNumber ../CompilerBackend/VM/VM.cpp web.cpp number.o ../CompilerBackend/VM/BigNumber/src/BigNumber/BigNumber.cpp -o VM.html
cat pre.js
