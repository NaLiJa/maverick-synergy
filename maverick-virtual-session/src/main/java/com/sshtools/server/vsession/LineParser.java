package com.sshtools.server.vsession;

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

import java.util.ArrayList;
import java.util.List;

import com.sshtools.server.vsession.CmdLine.Condition;


public class LineParser {

	private Environment environment;

	public LineParser(Environment environment) {
		this.environment = environment;
	}

	public List<String> parse(String commandline, int lastExitCode) {
		List<String> args = new ArrayList<String>();
		StringBuilder cmd = new StringBuilder();
		StringBuilder line = new StringBuilder();
		boolean quoted = false;
		StringBuilder var = null;
		boolean bracketedVar = false;
		boolean escaped = false;
		
		for (int i = 0; i < commandline.length(); i++) {
			char ch = commandline.charAt(i);
			
			if (ch == '\"' && !escaped) {
				quoted = !quoted;
			} else if (ch == '$' && var == null && !escaped) {
				var = new StringBuilder();
			} else if (ch == '\\' && !escaped) {
				escaped = true;
				line.append(ch);
				continue;
			} else if ((ch == ' ') && !quoted) {
				args.add(cmd.toString().trim());
				cmd.setLength(0);
			} else if (var != null && ch == '{' && !bracketedVar) {
				bracketedVar = true;
			} else if (var != null && ch == '}' && bracketedVar) {
				bracketedVar = false;
				cmd.append(environment.getOrDefault(var.toString(), ""));
				var = null;
				// finish and process var
			} else if (var != null && ch == ' ' && !bracketedVar) {
				// finish and process var
				cmd.append(environment.getOrDefault(var.toString(), ""));
				var = null;
			} else if (var != null) {
				var.append(ch);
			} else {
				cmd.append(ch);
			}
			escaped = false;
			line.append(ch);
		}
		if (var != null) {
			if(var.toString().equals("?")) {
				cmd.append(String.valueOf(lastExitCode));
			} else {
				cmd.append(environment.getOrDefault(var.toString(), ""));
			}
		}
		args.add(cmd.toString().trim());
		
		// Add last command
		return args;
	}
	
	public List<CmdLine> parseCommands(String str, int lastExitCode) {
		
		ArrayList<CmdLine> commands = new ArrayList<CmdLine>();
		
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder cmdline = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
		
			if (ch == '\"' && !escaped) {
				quoted = !quoted;
			} else if(ch == '\\' && !escaped) {
				escaped = true;
				continue;
			} else if(ch=='&' && !escaped && !quoted) {
				boolean doubleAmp = false;
				if(str.length() > i+1 && str.charAt(i+1)=='&') {
					doubleAmp = true;
					i++;
				}
				
				commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode), doubleAmp ? Condition.ExecNextCommandOnSuccess : Condition.ExecNextCommand, !doubleAmp));
				cmdline.setLength(0);
				continue;
			} else if(ch ==';' && !escaped && !quoted) {
				commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode),Condition.ExecNextCommand, false));
				cmdline.setLength(0);
				continue;
			}
			
			cmdline.append(ch);
			escaped = false;
		}
		
		if(cmdline.toString().trim().length() > 0) {
			commands.add(new CmdLine(cmdline.toString().trim(), parse(cmdline.toString().trim(), lastExitCode), Condition.ExecNextCommand, false));
		}
		
		return commands;		
	}

}
