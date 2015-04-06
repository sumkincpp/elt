### What other projects does ELT depend on? ###

ELT does not depend on any project. We forked the necessary parts of [CDT](http://www.eclipse.org/cdt/) and [Target Management](http://www.eclipse.org/tm/) that are necessary for ELT to run. These parts are included in the plug-in. No extra plug-ins are necessary.

### Why fork instead of enhancing existing projects? ###

For the record, the Target Management Terminal is great. It is a great piece of technology. The problem is that it provides more functionality than we needed, and it is missing some features.

The main reason for the fork is that we need to issue command-line commands in a way that doesn’t suck for the user. We need a quick way to say “hey terminal, execute this command, pronto”. TM’s terminal is very generic. It has this notion of connectors, which is a good idea but not useful to us. Setting it up to do what we need to do would require, to my understanding, some user interaction. We wanted to avoid this.

The things that TM’s terminal is missing are the user-friendly features, the ones in this project.

Another issue was connecting TM’s terminal with local bash. TM has a “Local Terminal” connector in incubation, but it has a dependency on CDT. Depending on CDT just for its PTY support is not my favorite idea. There we go, we forked CDT too.

### Why not Windows? ###

ELT is built using a fork of CDT's Pseudo-terminal support, which does not support Windows. Patches welcome! :)