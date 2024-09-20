package com.sshtools.common.knownhosts;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class HostKeyVerificationManager implements HostKeyVerification {

	List<HostKeyVerification> verifiers = new ArrayList<HostKeyVerification>();
	
	public HostKeyVerificationManager(Collection<? extends HostKeyVerification> verifiers) {
		this.verifiers.addAll(verifiers);
	}
	
	public HostKeyVerificationManager(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public HostKeyVerificationManager(HostKeyVerification... verifs) {
		this.verifiers.addAll(Arrays.asList(verifs));
	}
	
	public void addVerifier(HostKeyVerification verif) {
		this.verifiers.add(verif);
	}
	
	public boolean verifyHost(String host, SshPublicKey pk) throws SshException {
		
		for(HostKeyVerification v : verifiers) {
			if(v.verifyHost(host, pk)) {
				return true;
			}
		}
		return true;
	}

}
