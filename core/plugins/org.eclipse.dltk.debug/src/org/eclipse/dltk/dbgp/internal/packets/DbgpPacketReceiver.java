/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.dbgp.internal.packets;

import java.io.InputStream;

import org.eclipse.dltk.dbgp.internal.DbgpRawPacket;
import org.eclipse.dltk.dbgp.internal.DbgpWorkingThread;
import org.w3c.dom.Document;

public class DbgpPacketReceiver extends DbgpWorkingThread {
	private final DbgpResponcePacketWaiter responseWaiter;
	private final DbgpPacketWaiter notifyWaiter;
	private final DbgpPacketWaiter streamWaiter;
	private final DbgpPackageProcessor packatProcessor;

	private final InputStream input;
	private IDbgpRawLogger logger;

	protected void workingCycle() throws Exception {
		try {
			while (!Thread.interrupted()) {
				DbgpRawPacket packet = DbgpRawPacket.readPacket(input);

				if (logger != null) {
					logger.log(packet);
				}

				addDocument(packet.getParsedXml());
			}
		} finally {
			responseWaiter.terminate();
			notifyWaiter.terminate();
			streamWaiter.terminate();
		}
	}

	protected void addDocument(Document doc) {

		packatProcessor.processPacket(doc, notifyWaiter, responseWaiter,
				streamWaiter);
	}

	public DbgpNotifyPacket getNotifyPacket() throws InterruptedException {
		return (DbgpNotifyPacket) notifyWaiter.waitPacket();
	}

	public DbgpStreamPacket getStreamPacket() throws InterruptedException {
		return (DbgpStreamPacket) streamWaiter.waitPacket();
	}

	public DbgpResponsePacket getResponsePacket(int transactionId, int timeout)
			throws InterruptedException {
		return responseWaiter.waitPacket(transactionId, timeout);
	}

	public DbgpPacketReceiver(InputStream input) {
		super("DBGP - Packet receiver"); //$NON-NLS-1$

		if (input == null) {
			throw new IllegalArgumentException();
		}

		this.input = input;
		this.notifyWaiter = new DbgpPacketWaiter();
		this.streamWaiter = new DbgpPacketWaiter();
		this.responseWaiter = new DbgpResponcePacketWaiter();
		this.packatProcessor = new DbgpPackageProcessor();
	}

	public void setLogger(IDbgpRawLogger logger) {
		this.logger = logger;
	}
}
