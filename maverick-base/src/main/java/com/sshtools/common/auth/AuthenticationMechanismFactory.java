package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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

import java.util.Collection;

import com.sshtools.common.ssh.Context;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.UnsupportedChannelException;
import com.sshtools.common.sshd.AbstractServerTransport;

public interface AuthenticationMechanismFactory<C extends Context> {
	
	public static final String NONE = "none";
	public static final String PASSWORD_AUTHENTICATION = "password";
	public static final String PUBLICKEY_AUTHENTICATION = "publickey";
	public static final String KEYBOARD_INTERACTIVE_AUTHENTICATION = "keyboard-interactive";
	
	AuthenticationMechanism createInstance(String name,
			AbstractServerTransport<C> transport, 
			AbstractAuthenticationProtocol<C> authentication, 
			SshConnection con) throws UnsupportedChannelException;

	String[] getRequiredMechanisms(SshConnection con);
	
	String[] getSupportedMechanisms();
	
	Authenticator[] getProviders(String name, SshConnection con);

	void addProvider(Authenticator provider);

	void addProviders(Collection<Authenticator> providers);

	boolean isSupportedMechanism(String method);

}
