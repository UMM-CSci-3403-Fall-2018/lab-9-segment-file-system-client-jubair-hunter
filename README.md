Segmented-File-Server-client
============================

The starter code and (limited) tests for the client code for the Segmented File System lab.

# Background

Those wacky folks at OutOfMoney.com are at it again, and have come up with another awesome money making scheme. This time they're setting up a service where users will use a client program to contact a new OutOfMoney.com server (using another old computer they found in the basement at the high school). Every time the client contacts the server, the server will send back three randomly chosen bits of of Justin Bieber arcana. These could be sound files, videos, photos, or text files (things like lyrics). They've got the server up and running, but the kid that had prototyped the client software has moved away suddenly. (His mom works for the CIA and they move a lot, usually with little notice.) Unfortunately he took all the code with him, and isn't responding to any attempts to contact him on Facebook.

In a rare fit of sanity, they've brought you in to help them out by building the backend of the client.

*This is one of the more time-consuming labs, so prepare and plan accordingly.*

This starter code comes with some simple Aruba/Cucumber tests, but as discussed below you'll almost certainly want to add additional JUnit tests of your own to test the design and implementation of your data management tools.

# Segmenting the files

They're using socket-based connections between the client and server, but someone who is long since fired decided that it was important that the server break the files up into 1K chunks and send each chunk separately using UDP. Because of various sources of asynchrony such as threads on the server and network delays, you can't be sure you'll receive the chunks in the right order, so you'll need to collect and re-assemble them before writing out the file.

Unlike the socket connections we used for the Echo Server earlier, this system uses UDP or Datagram sockets. "Regular" sockets (also known as stream sockets or TCP sockets) provide you with a stable two-way connection that remains active until you disconnect. Datagram sockets are less structured, and you essentially just send out packets of information which can arrive at their destination in an arbitrary order, much like the description of packets in Chapter 7 of [Saltzer and Kaashoek](http://ocw.mit.edu/resources/res-6-004-principles-of-computer-system-design-an-introduction-spring-2009/). On the listening end, instead of reading a stream of data, data arrives in packets, and it's your job to interpret their contents. Typically there is some sort of protocol that describes how packets are formed and interpreted; otherwise we end up with an impossible guessing game.

Java provides direct support for UPD/datagram sockets primarily through the `DatagramSocket` and `DatagramPacket` classes. See the tutorial [Writing a Datagram Client and Server](http://download.oracle.com/javase/tutorial/networking/datagrams/clientServer.html) for more.

:bangbang: ***There are security implications of using UDP,*** which is one of many reasons why one would tend to use TCP instead unless you have a good reason to use UDP. If you establish a datagram socket, you're essentially willing to receive packets from anyone sending to the UDP port you're listening on. In this lab, for example, there's nothing to stop someone from throwing stuff at your UDP port at the same time that the server is sending you files there, and there's no simple way to distinguish legitimate packets from bogus (and possibly) malicious packets. In this case (partly because we're using Java) it's hard to do much more than corrupt the files and (if your error checking isn't great) crash the client. In systems where buffer overruns are possible (typically programmed in C), then attacks on UDP ports can in extreme cases lead to gaining root.

:bangbang: Make sure you shut down all your client processes before you leave the lab. If you leave them running you can block ports and make it impossible for other people to work on their lab on that computer.

# The OutOfMoney.com protocol

Your job is to write a (Java) program that sends a UDP/datagram packet to the server, and then waits and receives packets from the server until all three files are completely received. When a file is complete, it should be written to disk using the file name sent in the header packet. When all three files have been written to disk, the client should terminate cleanly. As mentioned above, since the file will be broken up into chunks and sent using UDP, we need a protocol to tell us how to interpret the pieces we receive so we can correctly assemble them. Those clever clogs at OutOfMoney.com didn't have much experience (ok, any experience) designing these kinds of protocols, so theirs isn't necessarily the greatest, but it gets the job done.

In this protocol there are essentially two kinds of packets

-   A header packet with a unique file ID for the file being transfered, and the actual name of the file so we'll know what to call it after we've assembled the pieces
-   A data packet, with the unique file ID (so we know what file this is part of), the packet number, and the data for that chunk.

Each packet starts with a status byte that indicates which type of packet it is:

-   If the status byte is even (i.e., the least significant bit is 0), then this is a header packet
-   If the status byte is odd (i.e., the least significant bit is 1), then this is a data packet
-   If the status byte's second bit is also 1 (i.e., it's 3 mod 4), then this is the *last* data packet for this file. They could have included the number of packets in the header packet, but they chose to to mark the last packet instead. Note that the last data packet (in terms of being the last bytes in the file) isn't guaranteed to come last, and might come anywhere in the stream including possibly being the *first* packet to arrive.

The packet numbers are consecutive and start from 0. So if a file is split into 18 chunks, there will be 18 data packets numbered 0 through 17, as well as the header packet for that file (for a total of 19 packets). The file IDs do *not* start with any particular value or run in any particular order, so you can't assume for example that they'll be 0, 1, and 2.

The structure of a header packet is then:

| status byte | file ID | file name                           |
|:------------|:--------|:------------------------------------|
| 1 byte      | 1 byte  | the rest of the bytes in the packet |

The structure of a data packet is:

| status byte | file ID | packet number | data                                |
|:------------|:--------|:--------------|:------------------------------------|
| 1 byte      | 1 byte  | 2 bytes       | the rest of the bytes in the packet |

:bangbang: Note that you'll need to look at the length field in the received packet to figure out how many bytes are in "the rest of the bytes in the package". Most of the received packets will probably be "full", but the last packet is likely to be "short".

The decision to only use 1 byte for the file ID means that there can't be more than 256 files being transferred to a given client at a time. Given that the current business plan is to always send exactly three files that shouldn't be a problem, but they'll need to be aware of the limitation if they want to expand the service later.

*Question to think about: Given that we're using 2 bytes for the packet number, and breaking files into 1K chunks, what's the largest file we can transfer using this system?*

# Writing the client backend

As mentioned above, your (Java) program starts things off by sending a UDP/datagram packet to the server, and then waits and receives packets from the server until all three files are completely received. When a file is complete, it should be written to disk using the file name sent in the header packet. When all three files have been written to disk, the client should terminate cleanly.

The main complication here is we don't control the order in which packets will be received. This means, among other things, that:

-   The header packet won't necessarily come first, so we might start receiving data for a file before we've gotten the header for it (and know the file name). In an extreme case, we might get *all* the data packets (including the one with the "last packet" bit set) before we get the header packet. (Remember that the "last packet" bit tells us how many packets there should be thanks to the packet number, but it doesn't mean that it's the last packet to arrive.)
-   The data packets can arrive in random order, so we'll have to store them in some fashion until we have them all, and then put them in order before we write them out to the file.

Other issues include:

-   Packets will arrive from all three files interleaved, so we need to make sure we can store them sensibly so we can separate out packets for the different files.
-   We don't know how many packets a file has been split up into until we see the packet with the "last packet" bit set.
-   You don't know what kind of file they're sending, so you have to make sure to handle the data as if it's binary data and can't ever convert it to strings.

Most of this is really a data structures problem. Before you start banging on the keyboard, take some time to talk about how you're going to unmarshall the packets and store their data. Having a good plan for that will make a huge difference.

### Testing

:bangbang: ***Write tests*** :bangbang:

While the network stuff is difficult to test, all the parsing and packet/file assembly logic is entirely testable. I would *strongly* encourage you to write some tests for that "data structures" part to help define the desired behavior and identify logic issues. Debugging logic problems when you're interacting with the actual server will really be a nuisance, so isolating that part as much as possible would be a Good Idea. You might, for example, have a `DataPacket` class (as distinct from the Java library `DatagramPacket` class) with a constructor that takes an array of bytes. You could then write tests that construct `DataPackets` and you could verify that the resulting `DataPackets` have the correct status bytes, file ID, packet number, and data. You could also have a `PacketManager` class that you hand packets to and which manages organizing and storing all the packets. You could then hand it a small set of test packets that you make up, and verify that it assembles the correct files. The `PacketManager` could, for example, create `ReceivedFile` objects. `RecievedFiles` could contain the packets for a file, and have getter methods for the file name, the number of packets (what if it isn't known yet?), the number actually received, whether the file is complete, and the data from those packets after sorting them in the correct order.

All of these ideas are just that: ideas. Your group should definitely spend some time discussing how you want to organize all this, and how you want to test that. If you're not clear on how you'd structure something for testing, *come ask* rather than just banging out a bunch of code that will just confuse us all later.

**If you've written unit tests for your data structures and they pass, you'll get partial credit even if the whole client is not correct**

In addition to your unit tests, there is an Aruba/Cucumber test that tests if you receive the files and assemble them correctly. You should make sure you run that test, and if it doesn't pass and you're not sure why, *definitely ask*! :bangbang: Don't just focus on passing that test from the start, though. It's a big end-to-end test, and it will probably be very difficult to try to debug your code by looking at how your program fails that rather complex test.
