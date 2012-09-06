package org.eclipse.dltk.dbgp;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.dltk.dbgp.internal.DbgpDebugingEngine;
import org.eclipse.dltk.dbgp.internal.DbgpSession;
import org.eclipse.dltk.dbgp.internal.DbgpWorkingThread;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;

public class DbgpClient extends DbgpWorkingThread {
	private final int port;

	private final int clientTimeout;

	private IDbgpServerListener listener;

	private DbgpDebugingEngine dbgpDebugingEngine;

	private DbgpSession session;

	protected synchronized void workingCycle() throws Exception, IOException {
		try {
			if (listener != null) {
				Socket client = new Socket("127.0.0.1",port);
				client.setSoTimeout(clientTimeout);
					
					try {
						dbgpDebugingEngine = new DbgpDebugingEngine(client);
						session = new DbgpSession(dbgpDebugingEngine);
						listener.clientConnected(session);
					}
					catch (Exception e) {
						DLTKDebugPlugin.log(e);
						if (dbgpDebugingEngine != null)
							dbgpDebugingEngine.requestTermination();
					}
				}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public DbgpClient(int port,  int clientTimeout) {
		super("DbgpServer");

		this.port = port;
		this.clientTimeout = clientTimeout;
	}

	public synchronized void requestTermination() {
		if(session != null) session.requestTermination();
		super.requestTermination();
	}

	public synchronized void  setListener(IDbgpServerListener listener) {
		this.listener = listener;
	}
}
