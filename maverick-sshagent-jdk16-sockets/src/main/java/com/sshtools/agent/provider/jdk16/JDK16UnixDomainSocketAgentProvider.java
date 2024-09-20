package com.sshtools.agent.provider.jdk16;

/*-
 * #%L
 * JDK16+ Unix Domain Socket Agent Provider
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.sshtools.agent.AgentProvider;
import com.sshtools.agent.client.AgentSocketType;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.server.SshAgentAcceptor;
import com.sshtools.agent.server.SshAgentTransport;

public class JDK16UnixDomainSocketAgentProvider implements AgentProvider {

	@Override
	public SshAgentAcceptor server(String location, AgentSocketType type) throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(location);
			ServerSocketChannel serverSocket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
	        serverSocket.bind(unixSocketAddress);
	        return new SshAgentAcceptor() {
				
				@Override
				public void close() throws IOException {
					serverSocket.close();
				}
				
				@Override
				public SshAgentTransport accept() throws IOException {
					SocketChannel socket = serverSocket.accept();
					return new SshAgentTransport() {
						
						@Override
						public OutputStream getOutputStream() throws IOException {
							return Channels.newOutputStream(socket);
						}
						
						@Override
						public InputStream getInputStream() throws IOException {
							return Channels.newInputStream(socket);
						}
						
						@Override
						public void close() throws IOException {
							socket.close();
						}
					};
				}
			};
		}
		return null;
	}

	@Override
	public SshAgentClient client(String application, String location, AgentSocketType type, boolean RFCAgent)
			throws IOException {
		if(type == AgentSocketType.UNIX_DOMAIN) {
			UnixDomainSocketAddress unixSocketAddress = UnixDomainSocketAddress.of(location);
			SocketChannel channel = SocketChannel.open(unixSocketAddress);
	        channel.configureBlocking(true);
			return new SshAgentClient(false, application, channel, Channels.newInputStream(channel), Channels.newOutputStream(channel),
					false);
		}
		return null;
	}

}
