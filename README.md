# TwitchPlays
A simple "Twitch Plays" mod for Minecraft, with a tasking system to listen to commands in chat.

## Licensing/Libraries:
Original Repo (iChun) - GNU GPLv3: https://github.com/iChun/TwitchPlays

Ported to 1.12, moved from builtin MC Twitch API (nonfunctional) to PircBotX. Improved and added various commands, improved configuration, removed iChunUtil dependency.

### Dependencies
PircBotX - GNU GPLv3: https://github.com/pircbotx/pircbotx

No changes.

### PircBotX Dependencies:

#### SLF4J (api/nop) 1.7.13 - MIT License: http://www.slf4j.org/

No changes.

#### Google Guava/Commons 19.0 - Apache License 2.0: https://github.com/google/guava/ http://www.apache.org/licenses/LICENSE-2.0

No changes.

#### Apache Commons Lang 3 3.4 - Apache License 2.0: https://commons.apache.org/proper/commons-lang/ http://www.apache.org/licenses/LICENSE-2.0

No changes.
```
NOTICE:
Apache Commons Lang
Copyright 2001-2020 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).
```

#### Apache Commons Codec 1.10 - Apache License 2.0: https://commons.apache.org/proper/commons-codec/

No changes.
```
Apache Commons Codec
Copyright 2002-2020 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).

src/test/org/apache/commons/codec/language/DoubleMetaphoneTest.java
contains test data from http://aspell.net/test/orig/batch0.tab.
Copyright (C) 2002 Kevin Atkinson (kevina@gnu.org)

===============================================================================

The content of package org.apache.commons.codec.language.bm has been translated
from the original php source code available at http://stevemorse.org/phoneticinfo.htm
with permission from the original authors.
Original source copyright:
Copyright (c) 2008 Alexander Beider & Stephen P. Morse.
```

## Building and modifying the Software:
1. Clone the repo: `git clone https://github.com/itsmeow/TwitchPlays`
2. Move to new folder: `cd TwitchPlays`
3. Set up workspace: `gradlew setupDecompWorkspace`
4. Set up for IDE (Eclipse): `gradlew eclipse`
5. Make changes in IDE
6. Build new version: `gradlew build`
7. Open `build/libs`
