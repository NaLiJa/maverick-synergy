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

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * Usage: echo [-n] [string]
 * @author lee
 *
 */
public class Echo extends ShellCommand {

	public Echo() {
		super("echo", ShellCommand.SUBSYSTEM_SHELL, UsageHelper.build("echo [options] <string>",
				"-n       Don't print newline"), "Echo a message to the screen");
		setBuiltIn(true);
	}

	public void run(String[] args, VirtualConsole console) throws IOException {
		StringBuilder bui = new StringBuilder();

		for (int i = 1 ; i < args.length; i++) {
			if (bui.length() > 0) {
				bui.append(' ');
			}
			bui.append(args[i]);
		}
		if (CliHelper.hasShortOption(args, 'n')) {
			console.print(bui.toString());
		} else {
			console.println(bui.toString());
		}
	}
}
