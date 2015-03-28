# mw-engine

Core cellular automaton engine for MicroWorld.

## Part of the overall MicroWorld system 

While this code works and is interesting on its own, you also need at least
[mw-parser](https://github.com/simon-brooke/mw-parser) and 
[mw-ui](https://github.com/simon-brooke/mw-ui). There will be other 
modules in due course.

You can see MicroWorld in action [here](http://www.journeyman.cc/microworld/) -
but please don't be mean to my poor little server. If you want to run big maps
or complex rule-sets, please run it on your own machines.

## Usage

Primary entry points are make-world and run-world, both in mw-engine.core. See
source or generated documentation for details. Documentation can be generated
using

    lein marg

To build the whole system, place all MicroWorld projects in a common directory,
and from that directory run *buildall.sh*. Alternatively, in each MicroWorld 
project directory, run

	lein clean
	lein compile
	lein marg
	lein install

and then from the mw-ui directory, run

	lein ring server

## License

Copyright © 2014 Simon Brooke

Distributed under the terms of the 
[GNU General Public License v2](http://www.gnu.org/licenses/gpl-2.0.html)