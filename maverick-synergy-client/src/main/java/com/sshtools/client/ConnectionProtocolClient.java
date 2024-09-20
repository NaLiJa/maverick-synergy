package com.sshtools.client;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;

import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ExecutorOperationSupport;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.synergy.ssh.ChannelNG;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.ConnectionProtocol;
import com.sshtools.synergy.ssh.ConnectionStateListener;
import com.sshtools.synergy.ssh.ConnectionTaskWrapper;
import com.sshtools.synergy.ssh.TransportProtocol;

/**
 * Implements the client side of the SSH connection prototocol
 */
public class ConnectionProtocolClient extends ConnectionProtocol<SshClientContext> {

	
	public ConnectionProtocolClient(TransportProtocol<SshClientContext> transport, String username) {
		super(transport, username);
	}

	@Override
	protected boolean isClient() {
		return true;
	}
	
	@Override
	protected void onStart() {

		SshClientContext context = getContext();
		con = context.getConnectionManager().registerConnection(this);

		addTask(ExecutorOperationSupport.EVENTS, new ConnectionTaskWrapper(con, new Runnable() {
			public void run() {
				for (ConnectionStateListener stateListener : getContext().getStateListeners()) {
					stateListener.ready(con);
				}
			}
		}));

	}

	protected void onStop() {
		
	}
	/**
	 * Start local port forwarding. Listening on a local interface and forwarding the data to a host on the remote network.
	 * 
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return
	 * @throws UnauthorizedException
	 * @throws SshException
	 */
	public int startLocalForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort)
			throws UnauthorizedException, SshException {

		if(Log.isInfoEnabled()) {
			Log.info("Requesting local forwarding on " + addressToBind + ":" + portToBind + " to " + destinationHost
					+ ":" + destinationPort);
		}

		if (!getContext().getForwardingPolicy().checkInterfacePermitted(con, addressToBind, portToBind)) {
			if(Log.isInfoEnabled()) {
				Log.info("User not permitted to forward on " + addressToBind + ":" + portToBind);
			}
			throw new UnauthorizedException();
		}

		int port = getContext().getForwardingManager().startListening(addressToBind, portToBind, con, destinationHost, destinationPort);
		
		if(Log.isInfoEnabled()) {
			Log.info("Local forwarding is now active on local interface " + addressToBind + ":" + portToBind 
					+ " forwarding to remote " + destinationHost + ":" + destinationPort);
		}
		
		return port;
	}
	
	
	public void stopLocalForwarding() {
		getContext().getForwardingManager().stopForwarding(getConnection());
	}
	
	public void stopLocalForwarding(String addressToBind, int portToBind) {
		stopLocalForwarding(addressToBind + ":" + portToBind);
	}
	
	public void stopLocalForwarding(String key) {
		getContext().getForwardingManager().stopForwarding(key, getConnection());
	}
	
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		getContext().getForwardingManager().stopRemoteForwarding(addressToBind, portToBind, this);
	}

	public void stopRemoteForwarding() throws SshException {
		getContext().getForwardingManager().stopRemoteForwarding(this);
	}
	
	/**
	 * Start remote port forwarding. Requests that the server starts a listening socket on the remote network and delivers
	 * data to a host on the local network.
	 * @param addressToBind
	 * @param portToBind
	 * @param destinationHost
	 * @param destinationPort
	 * @return actual destination port
	 * @throws SshException
	 */
	public int startRemoteForwarding(String addressToBind,
			int portToBind, String destinationHost, int destinationPort) throws SshException {
		return getContext().getForwardingManager().startRemoteForwarding(addressToBind, portToBind, destinationHost, destinationPort, this);
	}

	@Override
	public SshClientContext getContext() {
		return getTransport().getContext();
	}

	/**
	 * Process remote forwarding cancel request. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processTCPIPCancel(ByteArrayReader bar, ByteArrayWriter msg) throws IOException {
		return false;
	}

	/**
	 * Process a request for remote forwarding. This method does nothing since the client does not support opening 
	 * of remote forwarding channels.
	 */
	@Override
	protected boolean processTCPIPForward(ByteArrayReader bar, ByteArrayWriter response) throws IOException {
		return false;
	}

	/**
	 * The name of the ssh service i.e. ssh-connection
	 */
	public String getName() {
		return "ssh-connection";
	}

	/**
	 * Create an SSH channel. This method delegates creation to the ChannelFactory installed on the current
	 * SshContext.
	 * @throws ChannelOpenException 
	 */
	@Override
	protected ChannelNG<SshClientContext> createChannel(String channeltype, Connection<SshClientContext> con)
			throws UnsupportedChannelException, PermissionDeniedException, ChannelOpenException {
		return getContext().getChannelFactory().createChannel(channeltype, con);
	}

}
