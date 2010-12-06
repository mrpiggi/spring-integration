/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.synchronizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class charged with knowing how to connect to a remote file system,
 * scan it for new files and then download the files.
 * <p/>
 * The implementation should run through any configured
 * {@link org.springframework.integration.file.filters.FileListFilter}s to
 * ensure the file entry is acceptable.
 * 
 * @author Josh Long
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractInboundFileSynchronizer<F> implements InboundFileSynchronizer, InitializingBean {

	/**
	 * Extension used when downloading files. We change it right after we know it's downloaded.
	 */
	static final String INCOMPLETE_EXTENSION = ".INCOMPLETE";


	protected final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * the path on the remote mount as a String.
	 */
	private volatile String remoteDirectory;

	/**
	 * the {@link SessionFactory} for acquiring remote file Sessions.
	 */
	private final SessionFactory sessionFactory;

	/**
	 * An {@link FileListFilter} that runs against the <emphasis>remote</emphasis> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * Should we <emphasis>delete</emphasis> the remote <b>source</b> files
	 * after copying to the local directory? By default this is false.
	 */
	private boolean deleteRemoteFiles;


	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 */
	public AbstractInboundFileSynchronizer(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	/**
	 * Specify the full path to the remote directory.
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	public final void afterPropertiesSet() {
		Assert.notNull(this.remoteDirectory, "remoteDirectory must not be null");
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	public void synchronizeToLocalDirectory(File localDirectory) {
		Session session = null;
		try {
			session = this.sessionFactory.getSession();
			Assert.state(session != null, "failed to acquire a Session");
			F[] files = session.<F>list(this.remoteDirectory);
			if (!ObjectUtils.isEmpty(files)) {
				Collection<F> filteredFiles = this.filterFiles(files);
				for (F file : filteredFiles) {
					if (file != null) {
						this.copyFileToLocalDirectory(this.remoteDirectory, file, localDirectory, session);
					}
				}
			}
		}
		catch (IOException e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
		finally {
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception ignored) {
					if (logger.isDebugEnabled()) {
						logger.debug("failed to close Session", ignored);
					}
				}
			}
		}
	}

	private void copyFileToLocalDirectory(String remoteDirectoryPath, F remoteFile, File localDirectory, Session session) throws IOException {
		String remoteFileName = this.getFilename(remoteFile);
		String remoteFilePath = remoteDirectoryPath + File.separator + remoteFileName;
		if (!this.isFile(remoteFile)) {
			if (logger.isDebugEnabled()) {
				logger.debug("cannot copy, not a file: " + remoteFilePath);
			}
			return;
		}
		File localFile = new File(localDirectory, remoteFileName);
		if (!localFile.exists()) {
			String tempFileName = localFile.getAbsolutePath() + INCOMPLETE_EXTENSION;
			File tempFile = new File(tempFileName);
			InputStream inputStream = null;
			FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
			try {
				session.copy(remoteFilePath, fileOutputStream);
			}
			catch (Exception e) {
				if (e instanceof RuntimeException){
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
			finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
				}
				catch (Exception ignored1) {
				}
				try {
					fileOutputStream.close();
				}
				catch (Exception ignored2) {
				}
			}
			if (tempFile.renameTo(localFile)) {
				if (this.deleteRemoteFiles) {
					session.remove(remoteFilePath);
					if (logger.isDebugEnabled()) {
						logger.debug("deleted " + remoteFilePath);
					}
				}
			}
		}
	}

	protected abstract boolean isFile(F file);

	protected abstract String getFilename(F file);

}
