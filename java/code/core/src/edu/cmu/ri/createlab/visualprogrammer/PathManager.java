package edu.cmu.ri.createlab.visualprogrammer;

import edu.cmu.ri.createlab.util.DirectoryPoller;
import edu.cmu.ri.createlab.util.FileProvider;
import edu.cmu.ri.createlab.xml.XmlFilenameFilter;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * <code>PathManager</code> is a singleton which helps manage filesystem paths for the Visual Programmer. This class
 * helps other Visual Programmer classes obtain the correct directories for tasks such as saving/loading expressions
 * and sequences.  These paths returned vary based on the {@link VisualProgrammerDevice} to which the Visual Programmer
 * is currently connected since the device name is included in the path.  This class provides an easy way to obtain
 * those path names.
 * </p>
 *
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class PathManager
   {
   private static final Logger LOG = Logger.getLogger(PathManager.class);

   private static final PathManager INSTANCE = new PathManager();
   public static final FileProvider EXPRESSIONS_DIRECTORY_FILE_PROVIDER =
         new FileProvider()
         {
         @Override
         public File getFile()
            {
            return INSTANCE.getExpressionsDirectory();
            }
         };
   public static final FileProvider SEQUENCES_DIRECTORY_FILE_PROVIDER =
         new FileProvider()
         {
         @Override
         public File getFile()
            {
            return INSTANCE.getSequencesDirectory();
            }
         };

   public static final FileProvider ARDUINO_DIRECTORY_FILE_PROVIDER =
           new FileProvider()
           {
               @Override
               public File getFile()
               {
                   return INSTANCE.getArduinoDirectory();
               }
           };

   public static PathManager getInstance()
      {
      return INSTANCE;
      }

   private final Lock lock = new ReentrantLock();

   private File visualProgrammerHomeDir = null;
   private File audioDirectory = null;
   private File expressionsDirectory = null;
   private File sequencesDirectory = null;
   private File arduinoDirectory = null;
   private DirectoryPoller expressionsDirectoryPoller = null;
   private DirectoryPoller sequencesDirectoryPoller = null;
   private final Set<DirectoryPoller.EventListener> expressionsDirectoryPollerEventListeners = new HashSet<DirectoryPoller.EventListener>();
   private final Set<DirectoryPoller.EventListener> sequencesDirectoryPollerEventListeners = new HashSet<DirectoryPoller.EventListener>();

   private PathManager()
      {
      // private to prevent instantiation
      }

   @NotNull
   public File getVisualProgrammerHomeDirectory()
      {
      lock.lock();  // block until condition holds
      try
         {
         return visualProgrammerHomeDir;
         }
      finally
         {
         lock.unlock();
         }
      }

   /**
    * Returns the audio directory.
    */
   @NotNull
   public File getAudioDirectory()
      {
      lock.lock();  // block until condition holds
      try
         {
         return audioDirectory;
         }
      finally
         {
         lock.unlock();
         }
      }

   @NotNull
   public File getFormerAudioDirectory()
      {
      return VisualProgrammerConstants.FilePaths.FORMER_AUDIO_DIR;
      }

   /**
    * Returns the expressions directory for the current {@link VisualProgrammerDevice}.  Returns <code>null</code> if
    * the PathManager has not been initialized, or was de-initialized.
    *
    * @see #initialize(File, VisualProgrammerDevice)
    * @see #deinitialize()
    */
   @Nullable
   public File getExpressionsDirectory()
      {
      lock.lock();  // block until condition holds
      try
         {
         return expressionsDirectory;
         }
      finally
         {
         lock.unlock();
         }
      }

   /**
    * Returns the sequences directory for the current {@link VisualProgrammerDevice}.  Returns <code>null</code> if
    * the PathManager has not been initialized, or was de-initialized.
    *
    * @see #initialize(File, VisualProgrammerDevice)
    * @see #deinitialize()
    */
   @Nullable
   public File getSequencesDirectory()
      {
      lock.lock();  // block until condition holds
      try
         {
         return sequencesDirectory;
         }
      finally
         {
         lock.unlock();
         }
      }

   public File getArduinoDirectory()
   {
       lock.lock();  // block until condition holds
       try
       {
           return arduinoDirectory;
       }
       finally
       {
           lock.unlock();
       }
   }

   public void registerExpressionsDirectoryPollerEventListener(final DirectoryPoller.EventListener listener)
      {
      registerDirectoryPollerEventListener(expressionsDirectoryPoller, expressionsDirectoryPollerEventListeners, listener);
      }

   public void unregisterExpressionsDirectoryPollerEventListener(final DirectoryPoller.EventListener listener)
      {
      unregisterDirectoryPollerEventListener(expressionsDirectoryPoller, expressionsDirectoryPollerEventListeners, listener);
      }

   public void registerSequencesDirectoryPollerEventListener(final DirectoryPoller.EventListener listener)
      {
      registerDirectoryPollerEventListener(sequencesDirectoryPoller, sequencesDirectoryPollerEventListeners, listener);
      }

   public void unregisterSequencesDirectoryPollerEventListener(final DirectoryPoller.EventListener listener)
      {
      unregisterDirectoryPollerEventListener(sequencesDirectoryPoller, sequencesDirectoryPollerEventListeners, listener);
      }

   private void registerDirectoryPollerEventListener(@Nullable final DirectoryPoller directoryPoller,
                                                     @NotNull final Set<DirectoryPoller.EventListener> listeners,
                                                     @Nullable final DirectoryPoller.EventListener listener)
      {
      if (listener != null)
         {
         lock.lock();  // block until condition holds
         try
            {
            listeners.add(listener);
            if (LOG.isDebugEnabled())
               {
               LOG.debug("PathManager.registerDirectoryPollerEventListener(): There are now [" + listeners.size() + "] listeners to the DirectoryPoller [" + directoryPoller + "]");
               }
            if (directoryPoller != null)
               {
               directoryPoller.addEventListener(listener);
               }
            }
         finally
            {
            lock.unlock();
            }
         }
      }

   private void unregisterDirectoryPollerEventListener(@Nullable final DirectoryPoller directoryPoller,
                                                       @NotNull final Set<DirectoryPoller.EventListener> listeners,
                                                       @Nullable final DirectoryPoller.EventListener listener)
      {
      if (listener != null)
         {
         lock.lock();  // block until condition holds
         try
            {
            listeners.remove(listener);
            if (directoryPoller != null)
               {
               directoryPoller.removeEventListener(listener);
               }
            }
         finally
            {
            lock.unlock();
            }
         }
      }

   public void forceExpressionsDirectoryPollerRefresh()
      {
      forceDirectoryPollerRefresh(expressionsDirectoryPoller);
      }

   public void forceSequencesDirectoryPollerRefresh()
      {
      forceDirectoryPollerRefresh(sequencesDirectoryPoller);
      }

   private void forceDirectoryPollerRefresh(@Nullable final DirectoryPoller directoryPoller)
      {
      if (directoryPoller != null)
         {
         if (SwingUtilities.isEventDispatchThread())
            {
            final SwingWorker sw =
                  new SwingWorker<Object, Object>()
                  {
                  @Override
                  protected Object doInBackground() throws Exception
                     {
                     directoryPoller.forceRefresh();
                     return null;
                     }
                  };
            sw.execute();
            }
         else
            {
            directoryPoller.forceRefresh();
            }
         }
      }

   /**
    * Initialize the PathManager with the given <code>visualProgrammerHomeDir</code> and
    * <code>visualProgrammerDevice</code>.  Both must be non-<code>null</code>, and this method assumes the
    * <code>visualProgrammerHomeDir</code> exists, is a directory, and is readable, writeable, and executable.
    */
   @SuppressWarnings("ResultOfMethodCallIgnored")
   public void initialize(@NotNull final File visualProgrammerHomeDir,
                          @NotNull final VisualProgrammerDevice visualProgrammerDevice)
      {
      lock.lock();  // block until condition holds
      try
         {
         if (LOG.isDebugEnabled())
            {
            LOG.debug("PathManager.setVisualProgrammerDevice(): " + visualProgrammerDevice);
            }

         final String visualProgramerDeviceName = visualProgrammerDevice.getDeviceName();
         this.visualProgrammerHomeDir = visualProgrammerHomeDir;
         audioDirectory = new File(visualProgrammerHomeDir, VisualProgrammerConstants.FilePaths.AUDIO_DIRECTORY_NAME);
         expressionsDirectory = new File(new File(this.visualProgrammerHomeDir, visualProgramerDeviceName), VisualProgrammerConstants.FilePaths.EXPRESSIONS_DIRECTORY_NAME);
         sequencesDirectory = new File(new File(this.visualProgrammerHomeDir, visualProgramerDeviceName), VisualProgrammerConstants.FilePaths.SEQUENCES_DIRECTORY_NAME);
         arduinoDirectory = new File(new File(this.visualProgrammerHomeDir, visualProgramerDeviceName), VisualProgrammerConstants.FilePaths.ARDUINO_DIRECTORY_NAME);

         audioDirectory.mkdirs();
         expressionsDirectory.mkdirs();
         sequencesDirectory.mkdirs();
         arduinoDirectory.mkdirs();

         shutdownDirectoryPoller(expressionsDirectoryPoller);
         shutdownDirectoryPoller(sequencesDirectoryPoller);
         this.expressionsDirectoryPoller = new DirectoryPoller(EXPRESSIONS_DIRECTORY_FILE_PROVIDER,
                                                               new XmlFilenameFilter(),  // TODO: beef this up to validate expressions
                                                               1,
                                                               TimeUnit.SECONDS);
         this.sequencesDirectoryPoller = new DirectoryPoller(SEQUENCES_DIRECTORY_FILE_PROVIDER,
                                                             new XmlFilenameFilter(),  // TODO: beef this up to validate sequences
                                                             1,
                                                             TimeUnit.SECONDS);

         if (LOG.isDebugEnabled())
            {
            LOG.debug("PathManager.setVisualProgrammerDevice(): adding [" + expressionsDirectoryPollerEventListeners.size() + "] listeners to the expressions DirectoryPoller");
            }
         for (final DirectoryPoller.EventListener listener : expressionsDirectoryPollerEventListeners)
            {
            this.expressionsDirectoryPoller.addEventListener(listener);
            }

         if (LOG.isDebugEnabled())
            {
            LOG.debug("PathManager.setVisualProgrammerDevice(): adding [" + sequencesDirectoryPollerEventListeners.size() + "] listeners to the sequences DirectoryPoller");
            }
         for (final DirectoryPoller.EventListener listener : sequencesDirectoryPollerEventListeners)
            {
            this.sequencesDirectoryPoller.addEventListener(listener);
            }
         this.expressionsDirectoryPoller.start();
         this.sequencesDirectoryPoller.start();
         }
      finally
         {
         lock.unlock();
         }
      }

   /**
    * De-initializes the PathManager.
    */
   public void deinitialize()
      {
      lock.lock();  // block until condition holds
      try
         {
         if (LOG.isDebugEnabled())
            {
            LOG.debug("PathManager.deinitialize()");
            }
         shutdownDirectoryPoller(expressionsDirectoryPoller);
         shutdownDirectoryPoller(sequencesDirectoryPoller);
         this.expressionsDirectoryPoller = null;
         this.sequencesDirectoryPoller = null;

         this.visualProgrammerHomeDir = null;
         this.audioDirectory = null;
         this.expressionsDirectory = null;
         this.sequencesDirectory = null;
         this.arduinoDirectory = null;
         }
      finally
         {
         lock.unlock();
         }
      }

   /**
    * Removes all {@link DirectoryPoller.EventListener event listeners} from the given {@link DirectoryPoller} and then
    * calls {@link DirectoryPoller#stop()} on it.  Does nothing if the given {@link DirectoryPoller} is <code>null</code>.
    */
   private void shutdownDirectoryPoller(@Nullable final DirectoryPoller directoryPoller)
      {
      if (directoryPoller != null)
         {
         directoryPoller.removeAllEventListeners();
         directoryPoller.stop();
         }
      }

   /**
    * Returns <code>true</code> if the given {@link File} is non-<code>null</code>, is a directory, and has read, write,
    * and execute permissions.
    */
   public boolean isValidDirectory(final File dir)
      {
      return dir != null &&
             dir.isDirectory() &&
             dir.canExecute() &&
             dir.canRead() &&
             dir.canWrite();
      }
   }
