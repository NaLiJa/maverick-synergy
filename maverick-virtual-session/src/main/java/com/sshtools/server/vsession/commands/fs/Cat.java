package com.sshtools.server.vsession.commands.fs;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Cat extends ShellCommand {
	public Cat() {
		super("cat", ShellCommand.SUBSYSTEM_FILESYSTEM, 
		UsageHelper.build("cat [options] <filename>...",
				"-E                      Display $ at end of each line",
				"-n, --number            Number all output lines",
				"-s, --squeeze-blank     Suppress repeated empty output lines",
				"-T, --show-tabs         Displays TAB characters as ^I",
				"-v, --show-nonprinting  Use ^ and M- notation, except for LFD and TAB"), 
		"Output the contents of a file");
	}

	public void run(String[] args, VirtualConsole process) throws IOException, PermissionDeniedException {

		if (args.length < 2)
			throw new IOException("At least one argument required");
		for (int i = 1; i < args.length; i++) {
			if(args[i].startsWith("-")) {
				continue;
			}
			AbstractFile obj = process.getCurrentDirectory().resolveFile(args[i]);
			BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getInputStream()));
			
			try {
				String line = null;
				int n = 1;
				boolean lastLineBlank = false;
				while ((line = reader.readLine()) != null) {
					if(lastLineBlank && line.equals("") && CliHelper.hasShortOption(args, 's')) {
						continue;
					}
					lastLineBlank = line.equals("");
					if(CliHelper.hasShortOption(args, 'n')) {
						process.println("  " + n++ + " ");
					}
					if(CliHelper.hasShortOption(args, 'v')) {
						for(int x=0;x<line.length();x++) {
							char c = line.charAt(x);
							if((c & 0x80) == 0x80) {
								c &= ~0x80;
							}
							if(c == 9 || c == 10) {
								process.print(c);
							} else if(c < 32) {
								process.print('^');
								process.print((char) (c+64));
							} else if(c < 127) {
								process.print(c);
							} else {
								process.print("^?");
							}
						}
					} else {
						process.print(line);
					}
					if(CliHelper.hasShortOption(args, 'E')) {
						process.println("$");
					} else {
						process.println();
					}

				}
			} finally {
				reader.close();
			}
		}
	}
	

}
