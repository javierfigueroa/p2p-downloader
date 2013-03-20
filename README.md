Computer Networks Class Project

Description:
This project creates a peer-to-peer network for file downloading. It slightly resembles Bit-torrent. There are two distinct pieces of software – Peer and Tracker.
<br />
Each peer is both a server and a client. As a server, it offers a file that it wants to share. It registers the file with the tracker. The tracker knows all files to be shared and which peer each file is located at.
As a client, a peer may register with the tracker for downloading a file. The tracker knows, for each shared file, which peers want to download the file. These peers form a downloading group. The tracker informs each peer of a downloading group about other group members as well as the file owner. Each peer must establish a TCP connection with the file owner and additional TCP connections or UDP with up to three peers in the same downloading group.
During file download, the owner breaks the file into chunks of 100 KB. It will send each chunk to one of the peers in the downloading group. The peers then communicate amongst themselves. Each peer finds out which neighbors have chunks that it does not have, and downloads those chunks from these neighbors. This process repeats until all peers have all chunks, from which the entire file is reconstructed and stored locally.
Tracker:
It has the following functionalities:
1. Keep track of the list of online peers, the files they share, and the
downloading group for each file.
2. Inform each file owner of its downloading group and inform each peer of
other members in the same downloading group.
Commands supported:
1. register-peer <filename>: allow a peer to register a shared file with the
tracker.
2. list : allow a peer to query for the list of shared files.
3. register-group <filename> : allow a peer to register with the tracker for
downloading a file.
Peer:
Each peer supports two kinds of functionalities: uploading as a server and downloading as a client . When a peer is started, the information about the tracker is
supplied in the command line. For example, “peer sand.cise.ufl.edu 5150”, where the tracker is running at sand.cise.ufl.edu, port 5150.
When a peer is up, the user may type a command in the console window, “register- peer <filename>”, which will register a shared file with the tracker. The user may then type a command, “list”, to see which files are available. If the user wants a file, he or she types “register-group <filename>” to join the downloading group.
During the demo, you should run one tracker process and six peer processes. Each peer registers one file, and the TA will arbitrarily select a peer as a server and other five peers as clients to download the file from the server. After all clients register for downloading the file, you type “upload” command in the console window of the file owner, which retrieves the downloading group information from the tracker and uploads chunks of the file to the peers. Those peers will then download chunks amongst themselves. Each peer contacts its neighbors to find the chunks available, and it downloads those that it does not have.
As a peer downloads, it should print in its control window which chunks it is receiving and from which peers. After it completes download, it should print a summary about the list of chunks it has received and the peers from which the chunks are downloaded.
Use a video file of at least 2MB.
