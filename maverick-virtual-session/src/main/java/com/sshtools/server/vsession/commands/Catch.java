package com.sshtools.server.vsession.commands;

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

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.Msh;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Catch extends Msh {

	public Catch() {
		super("catch", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("catch <command> <arg1> <arg2>.."),
				null);
		setDescription("Run a command, catching exceptions it might throw");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException {
		
		setCommandFactory(console.getShell().getCommandFactory());

		if (args.length < 2) {
			throw new IllegalArgumentException(
					"Expects at least a command name as an argument.");
		} else {
			try {
				String[] pArgs = new String[args.length - 1];
				System.arraycopy(args, 1, pArgs, 0, pArgs.length);
				doSpawn(console, pArgs, false);
			} catch (Exception e) {
				if (CliHelper.hasShortOption(args, 'x')) {
					console.print(e);
				} else {
					console.println(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
				}
				if (CliHelper.hasShortOption(args, 't')) {
					throw new IOException("An error occured. " + e.getMessage());
				}
			}
		}
	}

}
