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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.shell.ShellPolicy;
import com.sshtools.server.vsession.Command;
import com.sshtools.server.vsession.ShellCommand;
import com.sshtools.server.vsession.ShellUtilities;
import com.sshtools.server.vsession.VirtualConsole;

public class Help<T extends AbstractFile> extends ShellCommand {

	public Help() {
		super("help", SUBSYSTEM_HELP, "help <command>", "Display information about the available commands.");
	}

	public boolean isHidden() {
		return true;
	}
	
	public void run(String[] args, VirtualConsole console) throws IOException {
		java.util.Set<String> cmds = console.getShell().getCommandFactory().getSupportedCommands();

		if (args.length == 2 && cmds.contains(args[1])) {
			console.getContext().getPolicy(ShellPolicy.class).checkPermission(
					console.getConnection(), ShellPolicy.EXEC, args[1]);
			try {

				Command cmd = console.getShell().getCommandFactory().createCommand(args[1], console.getConnection());

				console.println(cmd.getUsage());
			} catch (Exception e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		} else {
			// Create a list of subsystems and their commands
			HashMap<String, Map<String, Command>> subsystems = new HashMap<String, Map<String, Command>>();
			Iterator<String> it = cmds.iterator();
			Command cmd;
			Map<String, Command> comandMap;

			while (it.hasNext()) {
				try {
					String cmdName = (String) it.next();

					if (console.getContext().getPolicy(ShellPolicy.class).checkPermission(
							console.getConnection(), ShellPolicy.EXEC,  cmdName)) {
						cmd = console.getShell().getCommandFactory().createCommand(cmdName, console.getConnection());

						if(!cmd.isHidden()) {
							if (!subsystems.containsKey(cmd.getSubsystem())) {
								comandMap = new HashMap<String, Command>();
								comandMap.put(cmd.getCommandName(), cmd);
								subsystems.put(cmd.getSubsystem(), comandMap);
							} else {
								comandMap = subsystems.get(cmd.getSubsystem());
								comandMap.put(cmd.getCommandName(), cmd);
							}
						}
					}
				} catch (Exception e) {
				}
			}

			console.println();
			console.println("The following commands are available:");
			console.println();
			Iterator<Map.Entry<String, Map<String, Command>>> subsystemsIterator = subsystems.entrySet().iterator();
			Map.Entry<String, Map<String, Command>> entry;
			while (subsystemsIterator.hasNext()) {
				entry = subsystemsIterator.next();
				console.println((String) entry.getKey() + " commands:");
				comandMap = entry.getValue();
				List<Command> values = new ArrayList<>(comandMap.values());
				Collections.sort(values, new Comparator<Command>() {
					@Override
					public int compare(Command o1, Command o2) {
						return o1.getCommandName().compareTo(o2.getCommandName());
					}
				});
				
				for (Command shellCmd : values) {
					console.println(ShellUtilities.padString("", 5)
						+ ShellUtilities.padString(shellCmd.getCommandName(), 30) + shellCmd.getDescription());
				}

				console.println();
			}
			
			console.println(ShellUtilities.padString("", 5)
					+ ShellUtilities.padString("help [command]", 15)
				    + "Display command signature.");
		}
	}
}
