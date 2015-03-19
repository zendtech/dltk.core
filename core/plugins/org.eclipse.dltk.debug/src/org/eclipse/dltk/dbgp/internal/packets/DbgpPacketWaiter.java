package org.eclipse.dltk.dbgp.internal.packets;

import java.util.LinkedList;

public final class DbgpPacketWaiter {
	private static final String DBGP_PACKET_RECEIVER_PACKET_WAITER_TERMINATED = Messages.DbgpPacketReceiver_packetWaiterTerminated;
	private final LinkedList queue;
	private boolean terminated;

	public DbgpPacketWaiter() {
		terminated = false;
		this.queue = new LinkedList();
	}

	public synchronized void put(DbgpPacket obj) {
		queue.addLast(obj);
		notifyAll();
	}

	public synchronized DbgpPacket waitPacket() throws InterruptedException {
		while (!terminated && queue.isEmpty()) {
			wait();
		}

		if (terminated) {
			throw new InterruptedException(
					DBGP_PACKET_RECEIVER_PACKET_WAITER_TERMINATED);
		}

		return (DbgpPacket) queue.removeFirst();
	}

	public synchronized void terminate() {
		terminated = true;
		notifyAll();
	}
}