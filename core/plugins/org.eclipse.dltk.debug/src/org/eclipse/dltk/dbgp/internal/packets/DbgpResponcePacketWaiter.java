package org.eclipse.dltk.dbgp.internal.packets;

import java.util.HashMap;

public final class DbgpResponcePacketWaiter {
	private static final int MIN_TIMEOUT = 5;
	private final HashMap map;
	private boolean terminated;

	public DbgpResponcePacketWaiter() {
		map = new HashMap();
		terminated = false;
	}

	public synchronized void put(DbgpResponsePacket packet) {
		int id = packet.getTransactionId();
		map.put(new Integer(id), packet);
		notifyAll();
	}

	public synchronized DbgpResponsePacket waitPacket(int id, int timeout)
			throws InterruptedException {
		Integer key = new Integer(id);
		long endTime = 0;
		if (timeout > 0) {
			endTime = System.currentTimeMillis() + timeout;
		}
		while (!terminated && !map.containsKey(key)) {
			long current = System.currentTimeMillis();
			if (endTime != 0 && current >= endTime) {
				break;
			}
			if (endTime == 0)
				wait();
			else
				wait(endTime - current);
		}

		if (map.containsKey(key)) {
			return (DbgpResponsePacket) map.remove(key);
		}

		if (terminated) {
			throw new InterruptedException(
					Messages.DbgpPacketReceiver_responsePacketWaiterTerminated);
		}

		return null;
	}

	public synchronized void terminate() {
		terminated = true;
		notifyAll();
	}
}