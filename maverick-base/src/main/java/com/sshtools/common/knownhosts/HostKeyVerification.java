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

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;


/**
 * <p>This interface provides a callback method so that the user can verify the identity of the server
 * (by checking the public key) during the initial protocol negotiation. This check is performed at the
 * beginning of each connection to prevent trojan horses (by routing or DNS spoofing) and man-in-the-middle
 * attacks.
 * </p>
 * <p>The user should verify that the key is acceptable; the most usual method being a local database
 * file called <em>known_hosts</em>. The core J2SSH Maverick engine does not enforce any specific host key
 * verification in order that the engine can be used on Java platforms that do not have File objects. A
 * <em>known_hosts</em> implementation
 * <a href="../../sshtools/publickey/AbstractKnownHostsKeyVerification.html">AbstractKnownHostsKeyVerification</a>
 * can be found in the SSHTools utility classes supplied with the J2SSH Maverick API. This also includes the
 * basic <a href="../../sshtools/publickey/ConsoleKnownHostsKeyVerification.html">ConsoleKnownHostsKeyVerification</a>
 * which performs the check by prompting the user through stdin/stdout.
 * </p>
 * <p>
 * The public key instances supplied to the <a href="#verifyHost(java.lang.String, com.maverick.ssh.components.SshPublicKey)">
 * verifyHost</a> method will be one of the following implementations:<br>
 * <br>
 * For SSH1 the key will always be <a href="../ssh1/Ssh1RsaPublicKey.html">Ssh1RsaPublicKey</a><br>
 * <br>
 * For SSH2 the key will either be <a href="../ssh2/Ssh2RsaPublicKey.html">Ssh2RsaPublicKey</a> or
 * <a href="../ssh2/Ssh2DsaPublicKey.html">Ssh2DsaPublicKey</a>.
 * </p>
 * <p>To set a host key verification you must get an instance of the
     * SshConnector and configure the SSH version context's with your implementation.
 * The following example shows how to set a verification instance for both SSH1 and
 * SSH2:
 * <blockquote><pre>
 * SshConnector con = SshConnector.getInstance();
 *
 * HostKeyVerification hkv = new HostKeyVerification() {
 *   public boolean verifyHost(String name, SshPublicKey key)
 *              throws IOException {
 *      // Verify the host somehow???
 *      return true;
 *   }
 * };
 * SshContext context = con.getContext(SshConnector.SSH1);
 * context.setHostKeyVerification(hkv);
 *
 * context = con.getContext(SshConnector.SSH2);
 * context.setHostKeyVerification(hkv);
 * </pre></blockquote>
 * </p>
 * <p>You could also set different verification implementations according to protocol, for
 * example if you were using <em>known_hosts</em> for SSH1 and <em>known_hosts2</em>
 * for SSH2 you could use the previously described utility classes to set different
 * lookup databases.</p>
 * <blockquote><pre>
 * SshConnector con = SshConnector.getInstance();
 *
 * SshContext context = con.getContext(SshConnector.SSH1);
 * context.setHostKeyVerification(new ConsoleKnownHostsKeyVerification(
 *                                 System.getProperty("user.home")
 *                                 + "/.ssh/known_hosts"));
 *
 * context = con.getContext(SshConnector.SSH2);
 * context.setHostKeyVerification(new ConsoleKnownHostsKeyVerification(
 *                                 System.getProperty("user.home")
 *                                 + "/.ssh/known_hosts2"));
 * </pre></blockquote>
 * </p>
 *
 * @author Lee David Painter
 */
public interface HostKeyVerification {

  /**
   * Verify that the public key is acceptable for the host.
   * @param host the name of the connected host
   * @param pk   the public key supplied by the host
   * @return <code>true</code> if the host key is acceptable otherwise
   *         <code>false</code>
   * @throws SshException
   */
  public boolean verifyHost(String host, SshPublicKey pk) throws
      SshException;
}
