# Settings
ANDROID := /opt/android-sdk-update-manager/platforms/android-10/android.jar
PACKAGE := org.pileus.spades
OUTPUT  := bin/Spades.apk

# Sources
RES     := $(shell find res -name '*.xml')
SRC     := $(shell find src -name '*.java')

# Objects
GEN     := gen/$(subst .,/,$(PACKAGE))/R.java
OBJ     := $(subst .java,.class,   \
                $(SRC:src/%=obj/%) \
                $(GEN:gen/%=obj/%))

# Targets
default: run

compile: $(OBJ)

debug: $(OUTPUT)

clean:
	rm -rf bin gen obj

logcat:
	adb logcat Spades:D AndroidRuntime:E '*:S'

run: bin/install.stamp
	adb shell am start -W -a android.intent.action.MAIN -n $(PACKAGE)/.Main

install bin/install.stamp: $(OUTPUT)
	adb install -r $+
	touch bin/install.stamp

uninstall:
	adb uninstall $(PACKAGE)
	rm bin/install.stamp

graphics:
	git checkout graphics  --    \
	        'opt/drawable/*.svg' \
	        'opt/drawable/*.xcf' \
	        'res/drawable/*.png' \
	        'res/drawable/*.jpg'
	git reset HEAD --            \
	        'opt/drawable/*.svg' \
	        'opt/drawable/*.xcf' \
	        'res/drawable/*.png' \
	        'res/drawable/*.jpg'

convert:
	for svg in opt/drawable/*.svg; do        \
	        png=$${svg/svg/png};             \
	        png=$${png/opt/res};             \
	        rsvg-convert -w 2048 -h 2048     \
	                $$svg -o $$png;          \
	        convert -trim -resize '256x256!' \
	                $$png $$png;             \
	done

# Rules
%.apk: %.dex %.res | bin
	@echo "APK    $@.in"
	@apkbuilder $@.in -f $*.dex -z $*.res
	@echo ALIGN $@
	@zipalign -f 4 $@.in $@

%.dex: $(OBJ) | bin
	@echo "DEX    $@ "
	@dx --dex --output $@ obj

%.res: AndroidManifest.xml $(RES) | bin
	@echo "RES    $@"
	@aapt package -f -m               \
		-I $(ANDROID)             \
		-M AndroidManifest.xml    \
		-S res                    \
		-F $*.res

$(OBJ): $(SRC) $(GEN) | obj
	@echo "JAVAC  $@"
	@javac -g -Xlint:unchecked        \
		-bootclasspath $(ANDROID) \
		-encoding      UTF-8      \
		-source        1.5        \
		-target        1.5        \
		-classpath     obj        \
		-d             obj        \
		$+

$(GEN): AndroidManifest.xml $(RES) | gen
	@echo "GEN    $@"
	@aapt package -f -m               \
		-I $(ANDROID)             \
		-M AndroidManifest.xml    \
		-S res                    \
		-J gen

# Directories
bin gen obj:
	@mkdir -p $@
