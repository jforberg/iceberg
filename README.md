# Iceberg

Iceberg is a backup system targeting the Amazon Glacier cloud storage. It's written in Clojure and runs on the JVM.

## Planned features

* Unidirectional backup to the Glacier service.
* Encryption of all uploaded material.
* Comression where appropriate (heuristics for deciding when it is).
* Restoration of backup, to file system or for consistency checking.
* A stable protocol/API/file format so that backups from previous versions can be reliably restored.
* Implementation of 

## Implemented features

* Authentication using Amazon's scheme is fairly complete.
* Beginning support for Glacier's REST API.
* Rudimentary filesystem abstraction.

# What is Glacier?

Glacier is Amazon's long-term backup service. Its performance characteristics are similar to those of old-school tape backups &mdash; put requests and storage are very cheap, but file listing and retrieval is expensive (time-wise). This is well suited for a backup service but also presents some interesting challenges. Specifically, the inability to do file listings/stats in a timely manner precludes the use of most common backup tools.

http://aws.amazon.com/glacier/

As far as I have understood, Iceberg is the first attempt to build an open-source backup system targeting Glacier.

# License

Copyright © 2013-2014 Johan Förberg <johan@forberg.se>

Iceberg is open-source under a [BSD 2-clause license](http://opensource.org/licenses/BSD-2-Clause).
