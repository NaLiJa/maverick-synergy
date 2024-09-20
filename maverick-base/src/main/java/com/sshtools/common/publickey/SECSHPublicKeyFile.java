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

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class SECSHPublicKeyFile extends Base64EncodedFileFormat implements
		SshPublicKeyFile {

	static String BEGIN = "---- BEGIN SSH2 PUBLIC KEY ----";
	static String END = "---- END SSH2 PUBLIC KEY ----";
	String algorithm;
	byte[] encoded;

	SECSHPublicKeyFile(byte[] formattedkey) throws IOException {
		super(BEGIN, END);
		encoded = getKeyBlob(formattedkey);
		toPublicKey(); // Validate
	}

	SECSHPublicKeyFile(SshPublicKey key, String comment) throws IOException {
		super(BEGIN, END);
		try {
			algorithm = key.getAlgorithm();
			encoded = key.getEncoded();
			setComment(comment);
			toPublicKey(); // Validate
		} catch (SshException ex) {
			throw new IOException("Failed to encode public key");
		}
	}

	public String getComment() {
		return getHeaderValue("Comment");
	}

	public SshPublicKey toPublicKey() throws IOException {
		return SshPublicKeyFileFactory.decodeSSH2PublicKey(encoded);
	}

	public byte[] getFormattedKey() throws IOException {
		return formatKey(encoded);
	}

	public void setComment(String comment) {
		setHeaderValue("Comment", (comment.trim().startsWith("\"") ? "" : "\"")
				+ comment.trim() + (comment.trim().endsWith("\"") ? "" : "\""));
	}

	public String toString() {
		try {
			return new String(getFormattedKey(), "UTF-8");
		} catch (IOException ex) {
			return "Invalid encoding!";
		}

	}

	public String getOptions() {
		return null;
	}

}
