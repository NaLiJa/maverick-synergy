package com.sshtools.client.tasks;

/*-
 * #%L
 * Client API
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

import static com.sshtools.common.util.Utils.translatePathString;

import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.common.sftp.SftpFileAttributes;

/**
 * An SFTP {@link Task} that uploads complete files.
 * You cannot directly create a {@link StatTask}, instead use {@link StatTaskBuilder}.
 * <pre>
 * client.addTask(StatTaskBuilder.create().
 *      withPath("/path/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class StatTask extends AbstractFileTask {

	/**
	 * Builder for {@link StatTask}.
	 */
	public final static class StatTaskBuilder extends AbstractFileTaskBuilder<StatTaskBuilder, StatTask> {
		private Optional<Path> remote = Optional.empty();
		
		private StatTaskBuilder() {
		}

		/**
		 * Create a new {@link StatTaskBuilder}

		 * @return builder
		 */
		public static StatTaskBuilder create() {
			return new StatTaskBuilder();
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemote(Optional<Path> remote) {
			this.remote = remote;
			return this;
		}
		
		/**
		 * Set the remote path to stat
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public StatTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}
		
		@Override
		public StatTask build() {
			return new StatTask(this);
		}
	}

	private final Path remote;
	private SftpFileAttributes attrs = null;

	private StatTask(StatTaskBuilder builder) {
		super(builder);
		remote = builder.remote.orElseThrow(() -> new IllegalStateException("Remote path must be supplied."));
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> attrs = self.stat(translatePathString(remote))));
	}

	public SftpFileAttributes getAttributes() {
		return attrs;
	}

}
