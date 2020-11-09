classes=$(subst .java,, $(foreach java-source, $(wildcard *.java), $(java-source).class))

ifeq ($(OS),Windows_NT)
EXT:=dll
NAME:=VM
else
NAME:=libVM
UNAME := $(shell uname -s)
ifeq ($(UNAME),Linux)
EXT:=so
endif
ifeq ($(UNAME),Darwin)
EXT=dylib
endif
endif

all: output.jar $(NAME).$(EXT)

$(NAME).$(EXT):
	$(MAKE) -C CompilerBackend $(NAME).$(EXT)
	cp CompilerBackend/$(NAME).$(EXT) .

output.jar: $(classes)
	echo Manifest-Version: 1.0 > manifest.txt
	echo Main-Class: Main >> manifest.txt
	jar cvfm output.jar manifest.txt *.class
	$(RM) manifest.txt

%.class: %.java
	javac -source 1.8 -XDignore.symbol.file=true $<

ifeq ($(USE_GMP_LIB),1)
submodule-VM:
	$(MAKE) -C CompilerBackend/VM EXT_LDFLAGS='-lgmp -lgmpxx' EXT_CFLAGS='-DUSE_GMP_LIB'
else
submodule-VM:
	$(MAKE) -C CompilerBackend/VM
endif

clean-classes:
	$(RM) -r *.class output.jar META-INF output

clean: clean-classes
	$(MAKE) -C CompilerBackend/VM -f Makefile.old clean

native-image: INF
	native-image 2>&1 >/dev/null && echo native-image installed! || (echo native-image not found && exit 1)
	native-image -jar output.jar --no-fallback -H:ReflectionConfigurationFiles=META-INF/native-image/reflect-config.json

repl-native-image:
	native-image 2>&1 >/dev/null && echo native-image installed! || (echo native-image not found && exit 1)
	native-image -jar repl/out/artifacts/repl_jar/repl.jar --no-fallback --allow-incomplete-classpath -H:ReflectionConfigurationFiles=META-INF-repl/native-image/reflect-config.json -H:ResourceConfigurationFiles=META-INF-repl/native-image/resource-config.json -H:Name=output

INF: output.jar
	mkdir -p META-INF/native-image
	echo 'print "OK" &' > samplecode
	-java -agentlib:native-image-agent=config-output-dir=META-INF/native-image Main samplecode
	$(RM) samplecode
