package com.sshtools.client.tasks;

import static com.sshtools.common.util.Utils.translatePathString;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import com.sshtools.client.sftp.SftpClientTask;

/**
 * An SFTP {@link Task} that uploads complete files.
 * You cannot directly create a {@link UploadFileTask}, instead use {@link UploadFileTaskBuilder}.
 * <pre>
 * client.addTask(StatTaskBuilder.create().
 * 		withLocalFile(new File("local.txt")).
 *      withRemotePath("/path/on/remote/remote.txt").
 *      build());
 * </pre>
 *
 */
public class UploadFileTask extends AbstractFileTask {

	/**
	 * Builder for {@link UploadFileTask}.
	 */
	public final static class UploadFileTaskBuilder extends AbstractFileTaskBuilder<UploadFileTaskBuilder, UploadFileTask> {
		private Optional<Path> path = Optional.empty();
		private Optional<Path> local = Optional.empty();
		
		private UploadFileTaskBuilder() {
		}

		/**
		 * Create a new {@link UploadFileTaskBuilder}

		 * @return builder
		 */
		public static UploadFileTaskBuilder create() {
			return new UploadFileTaskBuilder();
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemotePath(Optional<String> remote) {
			return withRemote(remote.map(Path::of).orElse(null));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemote(Path remote) {
			return withRemote(Optional.of(remote));
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remmote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemote(Optional<Path> remmote) {
			this.path = remmote;
			return this;
		}
		
		/**
		 * Set the remote path to upload the file to. If empty, will be uploaded
		 * the current remote working directory
		 * 
		 * @param remote remote path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withRemotePath(String remote) {
			return withRemotePath(Optional.of(remote));
		}
		
		/**
		 * Set the local file to upload. This is required.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withLocalFile(File file) {
			return withLocal(file.toPath());
		}
		
		/**
		 * Set the local file to upload. This is required.
		 * 
		 * @param path path
		 * @return builder for chaining
		 */
		public UploadFileTaskBuilder withLocal(Path path) {
			this.local = Optional.of(path);
			return this;
		}

		@Override
		public UploadFileTask build() {
			return new UploadFileTask(this);
		}
	}

	final Optional<Path> remote;
	final Path local;

	private UploadFileTask(UploadFileTaskBuilder builder) {
		super(builder);
		remote = builder.path;
		local = builder.local.orElseThrow(() -> new IllegalStateException("Local file must be supplied."));
	}

	@Override
	public void doTask() {
		doTaskUntilDone(new SftpClientTask(con, (self) -> {
			if(remote.isEmpty()) {
				self.put(local.toAbsolutePath().toString(), progress.orElse(null));
			} else {
				self.put(local.toAbsolutePath().toString(), translatePathString(remote.get()), progress.orElse(null));
			}
		}));
	}
}
