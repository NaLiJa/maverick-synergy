package com.sshtools.server.vsession.commands.sftp;

/*-
 * #%L
 * Virtual Sessions
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

import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * put [-afPpr] local-path [remote-path] Upload local-path and store it on the
 * remote machine. If the remote path name is not specified, it is given the
 * same name it has on the local machine. local-path may contain glob(7) charac‐
 * ters and may match multiple files. If it does and remote-path is specified,
 * then remote-path must specify a directory.
 * 
 * If the -a flag is specified, then attempt to resume partial transfers of
 * existing files. Note that resumption assumes that any partial copy of the
 * remote file matches the local copy. If the local file contents differ from
 * the remote local copy then the resultant file is likely to be corrupt.
 * 
 * If the -f flag is specified, then a request will be sent to the server to
 * call fsync(2) after the file has been transferred. Note that this is only
 * supported by servers that implement the "fsync@openssh.com" extension.
 * 
 * If either the -P or -p flag is specified, then full file permis‐ sions and
 * access times are copied too.
 * 
 * If the -r flag is specified then directories will be copied recursively. Note
 * that sftp does not follow symbolic links when performing recursive transfers.
 * 
 * @author user
 *
 */
public class Put extends SftpCommand {

	public Put() {
		super("put", "SFTP", "put", "Transfers a file from local to remote server.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		try {
			if (args[1].startsWith("-")) {
				// we have options
				String options = args[1].substring(1);
				SftpFileTransferOptions transferOptions = SftpFileTransferOptions.parse(options);
				if (args.length == 4) {
					if (transferOptions.isRecurse()) {
						this.sftp.putLocalDirectory(args[2], args[3], true, false, true, null);
					} else {
						this.sftp.put(args[2], args[3], transferOptions.isResume());
					}
				} else if (args.length == 3) {
					this.sftp.put(args[2], transferOptions.isResume());
				} else {
					throw new IllegalStateException("Cannot understand `put` command.");
				}
			} else {
				// no options
				if (args.length == 3) {
					this.sftp.put(args[1], args[2]);
				} else if (args.length == 2) {
					this.sftp.put(args[1]);
				} else {
					throw new IllegalStateException("Cannot understand `put` command.");
				}
			}

		} catch (SftpStatusException | SshException | TransferCancelledException | IOException
				| PermissionDeniedException e) {
			throw new IllegalStateException("Problem in transfer for file", e);
		}
	}
}
