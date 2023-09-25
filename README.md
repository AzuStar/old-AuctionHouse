# old-AuctionHouse

A server and a client who establish their connection through message signing. They sign with private keys and use public key to establish a secure symetrical key, they then talk over RPC to make requests to each other.

Servers can be expandable on-demand and they all include built-in Orchestrator with a fail-over functionality. 

Health checks perform requests that prevent other nodes from terminating it, once healthcheck is put on halt, the faulty topology is terminated and servers reorder themselves into a new topology.

## Notes to self
The design is according to specification of distributed systems, but my personal notes are: 
- All nodes on the network don't need to communicate with everyone else. 
- There should be primary replicas who need to talk to each other only and then replicate DB to children for CDN purposes.
- Transaction queue will also ensure better availablity because server count of over 20 will start to flood network layer with healthchecks.
