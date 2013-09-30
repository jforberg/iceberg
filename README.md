# Iceberg
*A client for Amazon Glacier, in Clojure*

Iceberg is a Clojure library and command-line client for the Amazon Glacier backup service. 

## What it should be able to do

* Perform unidirectional backups to Glacier.
* Encryption. 
* Restore a backup only from what is stored at Glacier.
* Have a stable protocol/API (so that backups can be reliably restored).

## What it can currently do

* Not very much.
* Authentication using Amazon's strange scheme.

Copyright © 2013 Johan Förberg <johan@forberg.se>
