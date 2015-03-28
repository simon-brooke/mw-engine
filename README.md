# mw-engine

Core cellular automaton engine for MicroWorld.

## Part of the overall Microworld system 

While this code works and is interesting on its own, you also need at least
*mw-parser* and *mw-ui*. There will be other modules in due course.

## Usage

Primary entry points are make-world and run-world, both in mw-engine.core. See
source or generated documentation for details. Documentation can be generated
using

    lein marg

To build the whole system, place all MicroWorld projects in a common directory,
and from that directory run *buildall.sh*.

## License

Copyright © 2014 Simon Brooke

Distributed under the terms of the 
[GNU General Public License v2](http://www.gnu.org/licenses/gpl-2.0.html)