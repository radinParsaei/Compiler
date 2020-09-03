classes=$(subst .java,, $(foreach java-source, $(wildcard *.java), $(java-source).class))
all: submodule-VM output.jar

output.jar: $(classes)
	echo Manifest-Version: 1.0 > manifest.txt
	echo Main-Class: Main >> manifest.txt
	jar cvfm output.jar manifest.txt *.class
	$(RM) manifest.txt
%.class: %.java
	javac $<

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
	$(MAKE) -C CompilerBackend/VM clean

native-image: INF
	native-image 2>&1 >/dev/null && echo native-image installed! || (echo native-image not found && exit 1)
	native-image -jar output.jar --no-fallback -H:ReflectionConfigurationFiles=META-INF/native-image/reflect-config.json

INF: output.jar
	mkdir -p META-INF/native-image
	echo 'print "OK" &' > samplecode
	-java -agentlib:native-image-agent=config-output-dir=META-INF/native-image Main samplecode
	$(RM) samplecode
