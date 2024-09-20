package com.sshtools.agent.server;

/*-
 * #%L
 * Key Agent
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
import java.util.ServiceLoader;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.InMemoryKeyStore;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.openssh.OpenSSHConnectionFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

public class SshAgentServer {

	KeyStore keystore;
	SshAgentConnectionFactory connectionFactory;
	SshAgentAcceptor acceptor;
	
	public SshAgentServer(SshAgentConnectionFactory connectionFactory) {
		this(connectionFactory, new InMemoryKeyStore());
	}
	
	public SshAgentServer(SshAgentConnectionFactory connectionFactory, KeyStore keystore) {
		this.connectionFactory = connectionFactory;
		this.keystore = keystore;
	}
	
	public void startListener(SshAgentAcceptor acceptor) throws IOException {
		
		this.acceptor = acceptor;
		ServerThread t = new ServerThread(acceptor);
		t.start();
	}
	
	public void startListener(String location, AgentSocketType type) throws IOException {
		
		SshAgentAcceptor serverAcceptor = null;
		for(AgentProvider l : ServiceLoader.load(AgentProvider.class,
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			serverAcceptor = l.server(location, type);
			if(serverAcceptor != null)
				break;
		}
		if(serverAcceptor == null) {
			throw new IOException("No provider available. Do you have maverick-sshagent-jnr-sockets or maverick-sshagent-jdk16-sockets on the classpath?");
		}
		
		ServerThread t = new ServerThread(serverAcceptor);
		t.start();
	}
	
	public void startUnixSocketListener(String location) throws IOException {
		startListener(location, AgentSocketType.UNIX_DOMAIN);
	}
	
	public void close() throws IOException {
		Log.info("Agent server is closing down");
		if(acceptor!=null) {
			acceptor.close();
		}
	}
	
	class ServerThread extends Thread {

		SshAgentAcceptor socket;
		public ServerThread(SshAgentAcceptor socket) {
			super("Agent-Server-Thread");
			setDaemon(true);
			this.socket = socket;
		}
		
		public void run() {
			SshAgentTransport sock;
			try {
				while((sock = socket.accept())!=null) {
					
					SshAgentConnection c = connectionFactory.createConnection(keystore, 
							sock.getInputStream(), sock.getOutputStream(), sock);
					Thread t = new Thread(c);
					t.start();
				}
			} catch (IOException e) {
				Log.error("Agent server exited with error", e);
				try {
					socket.close();
				} catch (IOException e1) {
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		new SshAgentServer(new OpenSSHConnectionFactory()).startUnixSocketListener("/private/tmp/com.sshtools.agent");
	}
 }
