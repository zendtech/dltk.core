package org.eclipse.dltk.internal.debug.core.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.dbgp.DbgpClient;
import org.eclipse.dltk.dbgp.IDbgpServerListener;
import org.eclipse.dltk.dbgp.IDbgpSession;
import org.eclipse.dltk.dbgp.IDbgpThreadAcceptor;
import org.eclipse.dltk.dbgp.internal.IDbgpTerminationListener;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.DLTKDebugPreferenceConstants;
import org.eclipse.dltk.debug.core.IDbgpService;

public class RemoteDbgpService implements IDbgpService,
		IDbgpTerminationListener, IDbgpServerListener {

	private static final int CLIENT_SOCKET_TIMEOUT = 10000000;

	private DbgpClient server;

	private Map acceptors;

	private int clientPort;

	private int getPreferencePort() {
		return DLTKDebugPlugin.getDefault().getPluginPreferences().getInt(
				DLTKDebugPreferenceConstants.PREF_DBGP_REMOTE_PORT);
	}

	protected void restartServer(int port) {
		if (server != null) {
			server.setListener(null);
			server.removeTerminationListener(this);

			server.requestTermination();
			try {
				server.waitTerminated();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		clientPort = port;

		server = new DbgpClient(port, CLIENT_SOCKET_TIMEOUT);
		server.addTerminationListener(this);
		server.setListener(this);
		server.start();
	}

	public RemoteDbgpService() {
		this.acceptors = Collections.synchronizedMap(new HashMap());
		this.clientPort = getPreferencePort();
		if (clientPort == -1)
			clientPort = 9999;
	}

	public void connect() {
		restartServer(clientPort);
	}

	public void shutdown() {
		if (server != null) {
			server.removeTerminationListener(this);
			server.setListener(null);

			server.requestTermination();
			try {
				server.waitTerminated();
			} catch (InterruptedException e) {
				DLTKDebugPlugin.log(e);
			}
		}
	}

	public int getPort() {
		return clientPort;
	}

	// Acceptors
	public void registerAcceptor(String id, IDbgpThreadAcceptor acceptor) {
		acceptors.put(id, acceptor);
	}

	public IDbgpThreadAcceptor unregisterAcceptor(String id) {
		return (IDbgpThreadAcceptor) acceptors.remove(id);
	}

	// IDbgpTerminationListener
	public void objectTerminated(Object object, Exception e) {
		if (e != null) {
			DLTKDebugPlugin.log(e);
			restartServer(clientPort);
		}
	}

	public boolean available() {
		return true;
	}

	// INewDbgpServerListener
	public void clientConnected(IDbgpSession session) {
		final String id = session.getInfo().getIdeKey();

		final IDbgpThreadAcceptor acceptor = (IDbgpThreadAcceptor) acceptors
				.get(id);

		if (acceptor != null) {
			acceptor.acceptDbgpThread(session, new NullProgressMonitor());
		} else {
			session.requestTermination();
		}
	}
}
