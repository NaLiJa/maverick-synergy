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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.UsageHelper;
import com.sshtools.server.vsession.VirtualConsole;

public class Ls extends SftpCommand {

	private static final String LISTING_LONG_EXTENDED = "extended";
	private static final String LISTING_LONG_ALL = "all";
	private static final String LISTING_LONG_DIRECTORY = "directory";
	private static final String LISTING_LONG_LONG = "long";
	private static final String LISTING_SHORT_EXTENDED = "x";
	private static final String LISTING_SHORT_ALL = "a";
	private static final String LISTING_SHORT_DIRECTORY = "d";
	private static final String LISTING_SHORT_LONG = "l";

	public Ls() {
		super("ls", "SFTP", UsageHelper.build("ls [options] path...",
				"-l, --long						        Show details for each individual file/folder",
				"-a, --all                              Show all files",
				"-d, --directory                        List directories themeselves, not their contents",
				"-x, --extended                         Show extended attributes"), 
				"List the contents of a directory.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {

		try {
			
			if (args.length == 1) {
				SftpFile[] sftpFiles = this.sftp.ls();
				
					processSftpFilesForPrinting(args, console, sftpFiles);
				
			} else {
				List<String> paths = new ArrayList<>();
				for(int i=1; i<args.length;i++) {
					if(!isLsOption(args[i])) {
						paths.add(args[i]);
					}
				}
				if(paths.isEmpty()) {
					SftpFile[] sftpFiles = this.sftp.ls();
					processSftpFilesForPrinting(args, console, sftpFiles);
				} else {
					for (String path : paths) {
						SftpFile[] sftpFiles = this.sftp.ls(path);
						processSftpFilesForPrinting(args, console, sftpFiles);
					}
				}
			}
		} catch (SftpStatusException | SshException | IOException | PermissionDeniedException e) {
			throw new IllegalStateException(String.format("Problem in processing ls command with args %s", Arrays.toString(args)), e);
		}
	}

	private void processSftpFilesForPrinting(String[] args, VirtualConsole console, SftpFile[] sftpFiles)
			throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		for (SftpFile sftpFile : sftpFiles) {
			if (sftpFile.attributes().isFile() && (isOption(args, LISTING_SHORT_DIRECTORY) || isOption(args, LISTING_LONG_DIRECTORY))) {
				continue;
			}
			
			printFile(args, console, sftpFile);
		}
	}

	protected void printFile(String[] args, VirtualConsole console, SftpFile file) throws IOException, 
		PermissionDeniedException, SftpStatusException, SshException {
		
		var fileAttributes = file.attributes();
		
		if (!(isHidden(file)) || (isOption(args, LISTING_SHORT_ALL) || isOption(args, LISTING_LONG_ALL))) {
			
			if (isOption(args, LISTING_SHORT_ALL) || isOption(args, LISTING_LONG_ALL)) {
				
				String lastModifiedTime = "";
				long size = 0;
				if (file.attributes().isFile()) {
					size = fileAttributes.size().longValue();
				} else if (fileAttributes.isDirectory()) {
					size = 0;
				}
				SimpleDateFormat df;
		        long mt = (fileAttributes.lastModifiedTime().toMillis() * 1000L);
		        long now = System.currentTimeMillis();

		        if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
		            df = new SimpleDateFormat("MMM dd  yyyy", console.getConnection().getLocale());
		        } else {
		            df = new SimpleDateFormat("MMM dd HH:mm", console.getConnection().getLocale());
		        }

		        lastModifiedTime = df.format(new Date(mt));
				int linkCount = 0;
				console.println(String.format("%s %-3d %-8s %-8s %10d %-14s %-30s", 
						fileAttributes.toPermissionsString(), 
						linkCount, 
						fileAttributes.bestUsername(),
						fileAttributes.bestGroup(),
						size, 
						lastModifiedTime, 
						file.getFilename()));
			} else {
				console.println(file.getFilename());
			}
			if(isOption(args, LISTING_SHORT_EXTENDED) || isOption(args, LISTING_LONG_EXTENDED)) {
				for(var name : fileAttributes.extendedAttributes().keySet()) {
					var val = fileAttributes.extendedAttributes().get(name);
					console.println(String.format("%" + (CliHelper.hasShortOption(args,'l') ? 64 : 4)+ "s%s", "", name.toString() + "=" + (val == null ? "" : val.toString())));
				}
			}
		}
	}
	
	private boolean isHidden(SftpFile sftpFile) throws SftpStatusException, SshException {
		var fileAttributes = sftpFile.attributes();
		if (fileAttributes.hasAttributeBits() && fileAttributes.isHidden()) {
			return true;
		}
		
		if (sftpFile.getFilename().startsWith(".")) {
			return true;
		}
		
		return false;
	}
	
	private static boolean isOption(String[] args, String option) {
		for(String arg : args) {
			if(arg.startsWith("--")) {
				String value = arg.substring(2);
				return isMatchinLongOptionValue(option, value);
			} else if(arg.startsWith("-")) {
				String value = arg.substring(1);
				return isMatchinShortOption(option, value);
			}
		}
		
		return false;
	}

	private static boolean isMatchinShortOption(String option, String value) {
		String[] parts = value.split("");
		boolean result = false;
		for (String part : parts) {
			switch (option) {
				case LISTING_SHORT_LONG:
					result = isShortL(part);
					break;
				case LISTING_SHORT_DIRECTORY:
					result = isShortD(part);
					break;
				case LISTING_SHORT_ALL:
					result = isShortA(part);
					break;
				case LISTING_SHORT_EXTENDED:
					result = isShortX(part);
					break;

				default:
					result = false;
			}
			
			if (result) return true;
		}
		
		return result;
	}

	private static boolean isMatchinLongOptionValue(String option, String value) {
		switch (option) {
			case LISTING_LONG_LONG:
				return isLongL(value);
			case LISTING_LONG_DIRECTORY:
				return isLongD(value);
			case LISTING_LONG_ALL:
				return isLongA(value);
			case LISTING_LONG_EXTENDED:
				return isLongX(value);	

			default:
				return false;
		}
	}
	
	private static boolean isLsOption(String arg) {
		if(arg.startsWith("--")) {
			return isLsLongOption(arg.substring(2));
		} else if(arg.startsWith("-")) {
			return isLsShortOption(arg.substring(1));
		}
		
		return false;
	}
	
	private static boolean isLsLongOption(String arg) {
		return (isLongL(arg) || isLongD(arg) 
				|| isLongA(arg) || isLongX(arg));
	}
	
	private static boolean isLsShortOption(String arg) {
		String[] parts = arg.split("");
		
		for (String part: parts) {
			if (isShortL(part) || isShortD(part) || isShortA(part) || isShortX(part))  {
				return true;
			}
		}
		
		return false;
	}
	
	private static boolean isShortL(String value) {
		return Objects.equals(LISTING_SHORT_LONG, value);
	}
	
	private static boolean isShortD(String value) {
		return Objects.equals(LISTING_SHORT_DIRECTORY, value);
	}
	
	private static boolean isShortA(String value) {
		return Objects.equals(LISTING_SHORT_ALL, value);
	}
	
	private static boolean isShortX(String value) {
		return Objects.equals(LISTING_SHORT_EXTENDED, value);
	}
	
	private static boolean isLongL(String value) {
		return Objects.equals(LISTING_LONG_LONG, value);
	}
	
	private static boolean isLongD(String value) {
		return Objects.equals(LISTING_LONG_DIRECTORY, value);
	}
	
	private static boolean isLongA(String value) {
		return Objects.equals(LISTING_LONG_ALL, value);
	}
	
	private static boolean isLongX(String value) {
		return Objects.equals(LISTING_LONG_EXTENDED, value);
	}
}
