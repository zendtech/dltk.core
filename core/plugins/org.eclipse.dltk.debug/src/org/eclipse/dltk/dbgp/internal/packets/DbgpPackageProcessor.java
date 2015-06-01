package org.eclipse.dltk.dbgp.internal.packets;

import org.eclipse.dltk.dbgp.internal.utils.DbgpXmlPacketParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DbgpPackageProcessor {
	private static final String INIT_TAG = "init"; //$NON-NLS-1$
	private static final String RESPONSE_TAG = "response"; //$NON-NLS-1$
	private static final String STREAM_TAG = "stream"; //$NON-NLS-1$
	private static final String NOTIFY_TAG = "notify"; //$NON-NLS-1$

	public void processPacket(Document doc, DbgpPacketWaiter notifyWaiter,
			DbgpResponcePacketWaiter responseWaiter,
			DbgpPacketWaiter streamWaiter) {
		Element element = (Element) doc.getFirstChild();
		String tag = element.getTagName();

		// TODO: correct init tag handling without this hack
		if (tag.equals(INIT_TAG)) {
			responseWaiter.put(new DbgpResponsePacket(element, -1));
		} else if (tag.equals(RESPONSE_TAG)) {
			DbgpResponsePacket packet = DbgpXmlPacketParser
					.parseResponsePacket(element);
			responseWaiter.put(packet);
		} else if (tag.equals(STREAM_TAG)) {
			streamWaiter.put(DbgpXmlPacketParser.parseStreamPacket(element));
		} else if (tag.equals(NOTIFY_TAG)) {
			notifyWaiter.put(DbgpXmlPacketParser.parseNotifyPacket(element));
		}
	}

}
