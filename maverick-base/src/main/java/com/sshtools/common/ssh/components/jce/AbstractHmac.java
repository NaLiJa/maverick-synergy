package com.sshtools.common.ssh.components.jce;

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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshHmac;

/**
 * An abstract class that implements the {@link com.sshtools.common.ssh.components.SshHmac}
 * interface to provide support for JCE based message authentication.
 * 
 * @author Lee David Painter
 *
 */
public abstract class AbstractHmac implements SshHmac {

	protected Mac mac;
	protected int macSize;
	protected int macLength;
	protected String jceAlgorithm;
	final private SecurityLevel securityLevel;
	final int priority;
	
	public AbstractHmac(String jceAlgorithm, int macLength, SecurityLevel securityLevel, int priority) {
		this(jceAlgorithm, macLength, macLength, securityLevel, priority);
	}

	public AbstractHmac(String jceAlgorithm, int macSize, int outputLength, SecurityLevel securityLevel, int priority) {
		this.jceAlgorithm = jceAlgorithm;
		this.macSize = macSize;
		this.macLength = outputLength;
		this.securityLevel = securityLevel;
		this.priority = priority;
	}
	
	public SecurityLevel getSecurityLevel() {
		return securityLevel;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public void generate(long sequenceNo, byte[] data, int offset, int len,
			byte[] output, int start) {
		
        byte[] sequenceBytes = new byte[4];
        sequenceBytes[0] = (byte) (sequenceNo >> 24);
        sequenceBytes[1] = (byte) (sequenceNo >> 16);
        sequenceBytes[2] = (byte) (sequenceNo >> 8);
        sequenceBytes[3] = (byte) (sequenceNo >> 0);
        mac.update(sequenceBytes);
        mac.update(data, offset, len);

        byte[] tmp = mac.doFinal();
        
        System.arraycopy(tmp, 0, output, start, macLength);

	}
	
	public void update(byte[] b) {
		mac.update(b);
	}
	
	public byte[] doFinal() {
		return mac.doFinal();
	}

	public abstract String getAlgorithm();

	public String getProvider() {
		if(mac==null) {
			return null;
		}
		return mac.getProvider().getName();
	}
	
	public int getMacSize() {
		return macSize;
	}
	
	public int getMacLength() {
		return macLength;
	}
	
	public boolean isETM() {
		return false;
	}

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[macSize];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}

	public boolean verify(long sequenceNo, byte[] data, int start, int len,
			byte[] mac, int offset) {
        
		int length = getMacLength();
        byte[] generated = new byte[length];
        
        generate(sequenceNo, data, start, len, generated, 0);
        
        for(int i=0;i<generated.length;i++) {
        	if(mac[i+offset]!=generated[i])
        		return false;
        }
        return true;
	}

}
