package org.eclipse.pde.internal.ui.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;

import com.iw.plugins.spindle.TapestryPlugin;

public class SystemFileStorage extends PlatformObject implements IStorage {
	private File file;
	/**
	 * Constructor for SystemFileStorage.
	 */
	public SystemFileStorage(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}
	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			IStatus status =
				new Status(IStatus.ERROR, TapestryPlugin.getDefault().getPluginId(), IStatus.OK, null, e);
			throw new CoreException(status);
		}
	}
	public IPath getFullPath() {
		return new Path(file.getAbsolutePath());
	}
	public String getName() {
		return file.getName();
	}
	public boolean isReadOnly() {
		return true;
	}

	public boolean equals(Object object) {
		return object instanceof SystemFileStorage
			&& getFile().equals(((SystemFileStorage) object).getFile());
	}

	public int hashCode() {
		return getFile().hashCode();
	}
}