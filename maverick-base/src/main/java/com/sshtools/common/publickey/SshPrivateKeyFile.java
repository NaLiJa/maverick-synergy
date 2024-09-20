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

import com.sshtools.common.ssh.components.SshKeyPair;

/**
 * Interface which all private key formats must implement to provide decoding
 * and decryption of the private key into a suitable format for the API.
 *
 * @author Lee David Painter
 */
public interface SshPrivateKeyFile {

  /**
   * Determine if the private key file is protected by a passphrase.
   * @return <tt>true</tt> if the key file is encrypted with a passphrase, otherwise
   * <tt>false</tt>
   * @throws IOException
   */
  public boolean isPassphraseProtected();

  /**
   * Decode the private key using the users passphrase.
   * @param passphrase the users passphrase
   * @return the key pair stored in this private key file.
   * @throws IOException
   * @throws InvalidPassphraseException
   */
  public SshKeyPair toKeyPair(String passphrase) throws IOException, InvalidPassphraseException;

  /**
   * Method to determine whether the format supports changing of passphrases. This
   * typically would indicate that the format is read-only and that keys cannot
   * be saved into this format.
   * @return boolean
   */
  public boolean supportsPassphraseChange();

  /**
   * Get a description of the format type e.g. "OpenSSH"
   * @return String
   */
  public String getType();

  /**
   * Change the passphrase of the key file.
   * @param oldpassphrase the old passphrase
   * @param newpassprase the new passphrase
   * @throws IOException
   * @throws InvalidPassphraseException
   */
  public void changePassphrase(String oldpassphrase, String newpassprase) throws
      IOException, InvalidPassphraseException;

  /**
   * Get the formatted key
   * @return byte[]
   * @throws IOException
   */
  public byte[] getFormattedKey() throws IOException;

  /**
   * The private key comment (if any).
   * @return
   */
  public String getComment();
}
