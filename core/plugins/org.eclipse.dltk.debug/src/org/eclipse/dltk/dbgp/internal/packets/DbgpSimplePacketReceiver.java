/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.dbgp.internal.packets;

import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.dbgp.internal.DbgpRawPacket;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.w3c.dom.Document;

public class DbgpSimplePacketReceiver {
	private final DbgpResponcePacketWaiter responseWaiter;
	private final DbgpPacketWaiter notifyWaiter;
	private final DbgpPacketWaiter streamWaiter;
	private final DbgpPackageProcessor processor;

	private IDbgpRawLogger logger;

	public DbgpSimplePacketReceiver() {
		this.notifyWaiter = new DbgpPacketWaiter();
		this.streamWaiter = new DbgpPacketWaiter();
		this.responseWaiter = new DbgpResponcePacketWaiter();
		this.processor = new DbgpPackageProcessor();
	}

	public void close() {
		responseWaiter.terminate();
		notifyWaiter.terminate();
		streamWaiter.terminate();
	}

	protected void addDocument(Document doc) {
		processor
				.processPacket(doc, notifyWaiter, responseWaiter, streamWaiter);
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

	public void setLogger(IDbgpRawLogger logger) {
		this.logger = logger;
	}

	public void process(DbgpRawPacket packet) {
		if (logger != null) {
			logger.log(packet);
		}

		try {
			addDocument(packet.getParsedXml());
		} catch (DbgpException e) {
			DLTKDebugPlugin.logError(e.getMessage(), e);
		}
	}
}
