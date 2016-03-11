package org.eclipse.dltk.core.index.lucene;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

public class LucenePlugin extends Plugin {

	public static final Object LUCENE_JOB_FAMILY = new Object();

	private static LucenePlugin plugin;
	
	public static LucenePlugin getDefault() {
		return plugin;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			Job.getJobManager().join(LUCENE_JOB_FAMILY, null);
			LuceneManager.INSTANCE.shutdown();
			plugin = null;
		} finally {
			super.stop(context);
		}
	}

}
