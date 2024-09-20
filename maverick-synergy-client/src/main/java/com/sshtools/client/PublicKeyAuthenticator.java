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
import java.nio.ByteBuffer;

import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.SignaturePolicy;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SignatureGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;
import com.sshtools.common.ssh.components.jce.OpenSshRsaCertificate;
import com.sshtools.common.ssh.components.jce.OpenSshRsaSha256Certificate;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA256;
import com.sshtools.common.ssh.components.jce.Ssh2RsaPublicKeySHA512;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.ssh.Connection;
import com.sshtools.synergy.ssh.SshContext;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public abstract class PublicKeyAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator {

	public final static int SSH_MSG_USERAUTH_PK_OK = 60;
	
	boolean isAuthenticating = false;
	TransportProtocolClient transport;
	String username;
	
	SignatureGenerator signatureGenerator;
	SshPublicKey currentKey;
	String signingAlgorithm;
	
	public PublicKeyAuthenticator() {

	}

	private boolean setupNextKey() throws IOException, SshException {
		
		do {
			currentKey = getNextKey();
			signingAlgorithm = currentKey.getSigningAlgorithm();

			SignaturePolicy policy = transport.getContext().getPolicy(SignaturePolicy.class);
			
			if(!policy.getSupportedSignatures().isEmpty()) {
				if(currentKey instanceof SshRsaPublicKey && currentKey.getBitLength() >= 1024) {
					if(policy.getSupportedSignatures().contains(SshContext.PUBLIC_KEY_RSA_SHA512)) {
						signingAlgorithm = SshContext.PUBLIC_KEY_RSA_SHA512;
						currentKey = new Ssh2RsaPublicKeySHA512((SshRsaPublicKey)currentKey);
					} else if(policy.getSupportedSignatures().contains(SshContext.PUBLIC_KEY_RSA_SHA256)) {
						signingAlgorithm = SshContext.PUBLIC_KEY_RSA_SHA256;
						currentKey = new Ssh2RsaPublicKeySHA256((SshRsaPublicKey)currentKey);
					} else {
						Log.debug("Server does not support {} signature for key {}",
								currentKey.getSigningAlgorithm(),
								SshKeyUtils.getOpenSSHFormattedKey(currentKey));
						continue;
					}
					if(Log.isDebugEnabled()) {
						Log.debug("Upgrading key {} to use {} signature", currentKey.getAlgorithm(), signingAlgorithm);
					}
				} else if(currentKey instanceof OpenSshRsaCertificate && currentKey.getBitLength() >= 1024) {
					if(policy.getSupportedSignatures().contains(SshContext.PUBLIC_KEY_RSA_SHA512)) {
						signingAlgorithm = SshContext.PUBLIC_KEY_RSA_SHA512;
						currentKey = new OpenSshRsaSha256Certificate().init(currentKey.getEncoded());
					} else if(policy.getSupportedSignatures().contains(SshContext.PUBLIC_KEY_RSA_SHA256)) {
						signingAlgorithm = SshContext.PUBLIC_KEY_RSA_SHA256;
						currentKey =  new OpenSshRsaSha256Certificate().init(currentKey.getEncoded());
					} else {
						Log.debug("Server does not support {} signature for key {}",
								currentKey.getSigningAlgorithm(),
								SshKeyUtils.getOpenSSHFormattedKey(currentKey));
						continue;
					}
					if(Log.isDebugEnabled()) {
						Log.debug("Upgrading certificate {} to use {} signature", currentKey.getAlgorithm(), signingAlgorithm);
					}
				} else if(policy.isStrictMode() && !policy.getSupportedSignatures().contains(signingAlgorithm)) {
					Log.debug("Server does not support {} signature for key {}",
							currentKey.getSigningAlgorithm(),
							SshKeyUtils.getOpenSSHFormattedKey(currentKey));
					continue;
				}
			}
			
			if(Log.isDebugEnabled()) {
				Log.debug("Authenticating with {}", SshKeyUtils.getOpenSSHFormattedKey(currentKey));
			}
			
			
			return true;
		
		}
		while(hasCredentialsRemaining());
		
		return false;
	}
	
	@Override
	public void authenticate(TransportProtocolClient transport, String username) throws IOException, SshException {
		
		onStartAuthentication(transport.getConnection());
		
		this.transport = transport;
		this.username = username;

		if(hasCredentialsRemaining()) {
			setupNextKey();
			doPublicKeyAuth();
		} else {
			if(Log.isDebugEnabled()) {
				Log.debug("No more credentials", getName());
			}
			done(false);
		}

	}

	protected void onStartAuthentication(Connection<SshClientContext> con) {
		
	}
	
	void doPublicKeyAuth() throws SshException, IOException {
		
		try {


			final byte[] msg = generateAuthenticationRequest(generateSignatureData());
			
			transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "publickey") {

				@Override
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					super.writeMessageIntoBuffer(buf);
					buf.put(msg);
					return true;
				}
				
			});
		} catch (IOException e) {
			Log.error("Public key operation failed",e);
			failure();
		} catch (SshException e) {
			Log.error("Public key operation failed",e);
			failure();
		} catch(InvalidPassphraseException e) {
			Log.error("Public key operation failed",e);
			failure();
		}
	}
	
	byte[] generateSignatureData() throws IOException,
			SshException, InvalidPassphraseException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeBinaryString(transport.getSessionKey());
			baw.write(AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
			baw.writeString(username);
			baw.writeString("ssh-connection");
			baw.writeString("publickey");
			baw.writeBoolean(isAuthenticating);
			writePublicKey(baw, currentKey);
			
			return baw.toByteArray();

		} 
	}
	
	protected abstract SshPublicKey getNextKey() throws IOException;
	
	protected abstract SshKeyPair getAuthenticatingKey() throws IOException, InvalidPassphraseException;
	
	protected abstract boolean hasCredentialsRemaining();
	
	private void writePublicKey(ByteArrayWriter baw, SshPublicKey key) throws IOException, SshException {

		baw.writeString(key.getAlgorithm());
		baw.writeBinaryString(key.getEncoded());
		
	}


	byte[] generateAuthenticationRequest(byte[] data) throws IOException, SshException, InvalidPassphraseException {

		ByteArrayWriter baw = new ByteArrayWriter();

		try {
			baw.writeBoolean(isAuthenticating);

			if(!isAuthenticating) {
				if(Log.isDebugEnabled()) {
					Log.debug("Verifying key {}", currentKey.getAlgorithm());
					Log.debug("Encoded key{}{}", System.lineSeparator(), Utils.bytesToHex(currentKey.getEncoded(), 32, true, true));
				}
			}
			
			writePublicKey(baw, currentKey);
	
			if (isAuthenticating) {

					if(Log.isDebugEnabled()) {
						Log.debug("Signing authentication request with {}", signingAlgorithm);
					}
					
					byte[] signature = getSignatureGenerator().sign(currentKey, 
							signingAlgorithm, 
							data);
					baw.writeBinaryString(signature);
			}
			
			return baw.toByteArray();
		
		} finally {
			baw.close();
		}

	}

	protected SignatureGenerator getSignatureGenerator() throws IOException, InvalidPassphraseException {
		return getAuthenticatingKey();
	}

	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException, SshException {
		
		switch(msg.read()) {
		case SSH_MSG_USERAUTH_PK_OK:
		{
 			if(Log.isDebugEnabled()) {
 				Log.debug("Received SSH_MSG_USERAUTH_PK_OK");
 				Log.debug("Server accepts {} {}", currentKey.getAlgorithm(), SshKeyUtils.getFingerprint(currentKey));
 			}
 			
			isAuthenticating = true;
			try {
				doPublicKeyAuth();
			} catch (SshException | IOException e) {
				Log.error("Public key operation failed",e);
				failure();
			}
			return true;
		}
		case AuthenticationProtocolClient.SSH_MSG_USERAUTH_FAILURE:
		{
			if(hasCredentialsRemaining()) {
				setupNextKey();
				isAuthenticating = false;
				doPublicKeyAuth();
				return true;
			} else {
				if(Log.isDebugEnabled()) {
					Log.debug("No more credentials", getName());
				}
				done(false);
			}
		}
		}

		return false;
	}

	public byte[] sign(SshPrivateKey prv, String signingAlgorithm, byte[] data) throws SshException {

		try {
			return prv.sign(data, signingAlgorithm);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	@Override
	public String getName() {
		return "publickey";
	}
}
