package com.sshtools.common.publickey;

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

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist256Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist384Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist521Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEd25519Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshRsaCertificate;
import com.sshtools.common.util.UnsignedInteger64;

public class SshCertificateAuthority {

	public static SshCertificate generateUserCertificate(SshKeyPair key,
			long serial,
			String principalName,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, SshCertificate.SSH_CERT_TYPE_USER,
				principalName, principalName, validityDays, signedBy);
	}
	
	public static SshCertificate generateHostCertificate(SshKeyPair key,
			long serial,
			String hostname,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, SshCertificate.SSH_CERT_TYPE_HOST, hostname, Arrays.asList(hostname),
				validityDays, Collections.<CriticalOption>emptyList(), 
				new CertificateExtension.Builder().defaultExtensions().build(), signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			String principal,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, type, keyId, Arrays.asList(principal),
				validityDays, Collections.<CriticalOption>emptyList(), 
				new CertificateExtension.Builder().defaultExtensions().build(), signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			String principal,
			int validityDays,
			List<CertificateExtension> extensions,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, type, keyId, Arrays.asList(principal),
				validityDays, Collections.<CriticalOption>emptyList(), 
				extensions, signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			List<String> validPrincipals,
			int validityDays,
			List<CriticalOption> criticalOptions,
			List<CertificateExtension> extensions,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, type, keyId, validPrincipals,
				validityDays, Calendar.DAY_OF_MONTH, TimeZone.getTimeZone("UTC"),
				criticalOptions, extensions, signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			List<String> validPrincipals,
			int validity,
			int validityTimeUnit,
			TimeZone timeZone,
			List<CriticalOption> criticalOptions,
			List<CertificateExtension> extensions,
			SshKeyPair signedBy) throws SshException, IOException {
		
		switch(type) {
		case SshCertificate.SSH_CERT_TYPE_HOST:
		case SshCertificate.SSH_CERT_TYPE_USER:
			break;
		default:
			throw new SshException(
					String.format("Invalid certificate type %d", type), 
					SshException.BAD_API_USAGE);
		}
		
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.setTimeZone(timeZone);
		
		UnsignedInteger64 validAfter = new UnsignedInteger64(c.getTimeInMillis() / 1000);
		
		c.add(Calendar.DAY_OF_MONTH, validity);
		UnsignedInteger64 validBefore = new UnsignedInteger64(c.getTimeInMillis() / 1000);

		@SuppressWarnings("unused")
		String reserved = "";

		OpenSshCertificate cert;
		switch(key.getPublicKey().getAlgorithm()) {
		case "ssh-rsa":
		case "rsa-sha2-256":
		case "rsa-sha2-512":
			cert = new OpenSshRsaCertificate();
			break;
		case "ssh-ed25519":
			cert = new OpenSshEd25519Certificate();
			break;
		case "ecdsa-sha2-nistp256":
			cert = new OpenSshEcdsaSha2Nist256Certificate();
			break;
		case "ecdsa-sha2-nistp384":
			cert = new OpenSshEcdsaSha2Nist384Certificate();
			break;
		case "ecdsa-sha2-nistp521":
			cert = new OpenSshEcdsaSha2Nist521Certificate();
			break;
		default:
			throw new SshException(SshException.BAD_API_USAGE, 
					String.format("Unsupported certificate type %s", key.getPublicKey().getAlgorithm()));
		}
		
		cert.sign(key.getPublicKey(), new UnsignedInteger64(serial), type, keyId, validPrincipals,
				validAfter, validBefore, criticalOptions, extensions, signedBy);
		
		return new SshCertificate(key, cert);
	}
}
