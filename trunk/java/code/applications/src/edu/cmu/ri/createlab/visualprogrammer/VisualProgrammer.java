package edu.cmu.ri.createlab.visualprogrammer;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.concurrent.ExecutionException;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import edu.cmu.ri.createlab.device.CreateLabDevicePingFailureEventListener;
import edu.cmu.ri.createlab.device.CreateLabDeviceProxy;
import edu.cmu.ri.createlab.expressionbuilder.ExpressionBuilder;
import edu.cmu.ri.createlab.sequencebuilder.SequenceBuilder;
import edu.cmu.ri.createlab.terk.services.ServiceManager;
import edu.cmu.ri.createlab.userinterface.component.Spinner;
import edu.cmu.ri.createlab.visualprogrammer.lookandfeel.VisualProgrammerLookAndFeelLoader;
import edu.cmu.ri.createlab.xml.LocalEntityResolver;
import edu.cmu.ri.createlab.xml.XmlHelper;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class VisualProgrammer
   {
   private static final Logger LOG = Logger.getLogger(VisualProgrammer.class);

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(VisualProgrammer.class.getName());

   private static final String APPLICATION_NAME = RESOURCES.getString("application.name");
   private final JFrame jFrame;

   public static void main(final String[] args)
      {
      // Load the look and feel
      VisualProgrammerLookAndFeelLoader.getInstance().loadLookAndFeel();

      //Schedule a job for the event-dispatching thread: creating and showing this application's GUI.
      SwingUtilities.invokeLater(
            new Runnable()
            {
            public void run()
               {
               final JFrame jFrame = new JFrame(APPLICATION_NAME);

               final VisualProgrammer application = new VisualProgrammer(jFrame);

               // set various properties for the JFrame
               jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
               jFrame.setBackground(Color.WHITE);
               jFrame.setResizable(true);
               jFrame.addWindowListener(
                     new WindowAdapter()
                     {
                     public void windowClosing(final WindowEvent event)
                        {
                        // ask if the user really wants to exit
                        final int selectedOption = JOptionPane.showConfirmDialog(jFrame,
                                                                                 RESOURCES.getString("dialog.message.exit-confirmation"),
                                                                                 RESOURCES.getString("dialog.title.exit-confirmation"),
                                                                                 JOptionPane.YES_NO_OPTION,
                                                                                 JOptionPane.QUESTION_MESSAGE);

                        if (selectedOption == JOptionPane.YES_OPTION)
                           {
                           final SwingWorker<Object, Object> worker =
                                 new SwingWorker<Object, Object>()
                                 {
                                 @Override
                                 protected Object doInBackground() throws Exception
                                    {
                                    application.shutdown();
                                    return null;
                                    }

                                 @Override
                                 protected void done()
                                    {
                                    System.exit(0);
                                    }
                                 };
                           worker.execute();
                           }
                        }
                     });

              jFrame.addComponentListener( new ComponentListener() {
                  @Override
                  public void componentResized(ComponentEvent e) {
                      Component source =  e.getComponent();
                      if (source.equals(jFrame))
                      {
                        Dimension size = source.getSize();
                        //jFrame.setMinimumSize(size);
                        //jFrame.setMaximumSize(size);
                        jFrame.setPreferredSize(size);
                      }
                  }

                  @Override
                  public void componentMoved(ComponentEvent e) {
                      //To change body of implemented methods use File | Settings | File Templates.
                  }

                  @Override
                  public void componentShown(ComponentEvent e) {
                      //To change body of implemented methods use File | Settings | File Templates.
                  }

                  @Override
                  public void componentHidden(ComponentEvent e) {
                      //To change body of implemented methods use File | Settings | File Templates.
                  }
              });


               jFrame.pack();
               jFrame.setLocationRelativeTo(null);    // center the window on the screen
               jFrame.setVisible(true);
               }
            });
      }

   private ServiceManager serviceManager = null;
   private CreateLabDeviceProxy createLabDeviceProxy = null;
   private final VisualProgrammerDeviceImplementationClassLoader visualProgrammerDeviceImplementationClassLoader = new VisualProgrammerDeviceImplementationClassLoader();

   private ExpressionBuilder expressionBuilder = null;
   private SequenceBuilder sequenceBuilder = null;

   private final JPanel mainPanel = new JPanel();
   private final Spinner spinnerPanel = new Spinner(RESOURCES.getString("label.scanning"));
   private final JTabbedPane tabbedPane = new JTabbedPane();

   private VisualProgrammer(@NotNull final JFrame jFrame)
      {
      this.jFrame = jFrame;

      XmlHelper.setLocalEntityResolver(LocalEntityResolver.getInstance());

      // Configure the main panel
      final GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
      mainPanel.setLayout(mainPanelLayout);

      mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(spinnerPanel)
      );
      mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(spinnerPanel)
      );

      // connect to the device...
      connectToDevice();

      jFrame.add(mainPanel);
      }

   private void connectToDevice()
      {
      if (!isConnected())
         {
         // connect to the device...
         final SwingWorker<VisualProgrammerDevice, Object> sw =
               new SwingWorker<VisualProgrammerDevice, Object>()
               {
               @Override
               protected VisualProgrammerDevice doInBackground() throws Exception
                  {
                  LOG.debug("VisualProgrammer.connectToDevice(): connecting to device...");

                  // first get the class names
                  final List<VisualProgrammerDevice> visualProgrammerDevices = visualProgrammerDeviceImplementationClassLoader.loadImplementationClasses();

                  if (visualProgrammerDevices.size() > 0)
                     {
                     // TODO: present the user with a choice.  For now, just take the first one...
                     final VisualProgrammerDevice visualProgrammerDevice = visualProgrammerDevices.get(0);

                     // connect to the device...
                     visualProgrammerDevice.connect();
                     LOG.debug("VisualProgrammer.connectToDevice(): Connected!");
                     createLabDeviceProxy = visualProgrammerDevice.getDeviceProxy();
                     serviceManager = visualProgrammerDevice.getServiceManager();

                     createLabDeviceProxy.addCreateLabDevicePingFailureEventListener(
                           new CreateLabDevicePingFailureEventListener()
                           {
                           @Override
                           public void handlePingFailureEvent()
                              {
                              LOG.debug("VisualProgrammer.handlePingFailureEvent(): disconnecting from device...");
                              disconnectFromDevice();

                              LOG.debug("VisualProgrammer.handlePingFailureEvent(): attempting reconnection to device...");
                              connectToDevice();   // TODO: is this good enough?
                              }
                           });

                     return visualProgrammerDevice;
                     }
                  else
                     {
                     // TODO: alert the user before shutting down
                     LOG.error("Could not find any valid implementations of class VisualProgrammerDevice.  Will now exit.");
                     System.exit(1);
                     }

                  return null;
                  }

               @Override
               protected void done()
                  {
                  try
                     {

                     LOG.error("Starting VisualProgrammerDevice");
                     final VisualProgrammerDevice visualProgrammerDevice = get();

                     expressionBuilder = new ExpressionBuilder(jFrame, visualProgrammerDevice);
                     sequenceBuilder = new SequenceBuilder((jFrame), visualProgrammerDevice);

                     final JPanel placeholderPanel = new JPanel();
                     placeholderPanel.add(new JLabel("Something will go here."));

                     // clear everything out of the mainPanel and recreate it

                     mainPanel.removeAll();
                     tabbedPane.removeAll();
                     tabbedPane.addTab(RESOURCES.getString("expression-builder-tab.name"), expressionBuilder.getPanel());
                     tabbedPane.addTab(RESOURCES.getString("sequence-builder-tab.name"), sequenceBuilder.getPanel());
                     tabbedPane.setFont(new Font("Verdana", Font.PLAIN, 11));

                     jFrame.setPreferredSize(new Dimension(1024, 728));


                     mainPanel.setLayout(new GridBagLayout());

                     final GridBagConstraints c = new GridBagConstraints();
                     c.fill = GridBagConstraints.BOTH;
                     c.gridx = 0;
                     c.gridy = 0;
                     c.weighty = 1.0;
                     c.weightx = 1.0;
                     c.anchor = GridBagConstraints.CENTER;
                     mainPanel.add(tabbedPane, c);

                     /*final GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
                     mainPanel.setLayout(mainPanelLayout);

                     mainPanelLayout.setHorizontalGroup(
                           mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                 .addComponent(tabbedPane)
                     );
                     mainPanelLayout.setVerticalGroup(
                           mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                 .addComponent(tabbedPane)
                     );*/

                     jFrame.pack();
                     jFrame.setLocationRelativeTo(null);    // center the window on the screen
                     LOG.error("VisualProgrammerDevice Launched");
                     }
                  catch (InterruptedException e)
                     {
                     LOG.error("InterruptedException while trying to get the visualProgrammerDevice", e);
                     }
                  catch (ExecutionException e)
                     {
                     LOG.error("ExecutionException while trying to get the visualProgrammerDevice", e);
                     }
                  }
               };
         sw.execute();
         }
      else
         {
         LOG.info("VisualProgrammer.connectToDevice(): doing nothing since we're already connected.");
         }
      }

   private void disconnectFromDevice()
      {
      if (isConnected())
         {
         LOG.debug("VisualProgrammer.disconnectFromDevice(): disconnecting from device...");

         try
            {
            createLabDeviceProxy.disconnect();
            LOG.debug("VisualProgrammer.disconnectFromDevice(): Disconnected!");
            }
         catch (Exception e)
            {
            LOG.error("Exception while trying to disconnect from the device.  Ignoring.", e);
            }
         finally
            {
            createLabDeviceProxy = null;
            serviceManager = null;

            expressionBuilder.performPostDisconnectCleanup();
            //TODO: sequenceBuilder.performPostDisconnectCleanup();

            expressionBuilder = null;
            sequenceBuilder = null;

            // TODO: do this in the Swing thread, and factor this out somewhere since it's duplicated in main()
            mainPanel.removeAll();
            final GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
            mainPanel.setLayout(mainPanelLayout);

            mainPanelLayout.setHorizontalGroup(
                  mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(spinnerPanel)
            );
            mainPanelLayout.setVerticalGroup(
                  mainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(spinnerPanel)
            );

            jFrame.pack();
            jFrame.setLocationRelativeTo(null);    // center the window on the screen
            }
         }
      else
         {
         LOG.info("VisualProgrammer.disconnectFromDevice(): doing nothing since we're already disconnected.");
         }
      }

   private boolean isConnected()
      {
      return serviceManager != null && createLabDeviceProxy != null;
      }

   private void shutdown()
      {
      LOG.debug("VisualProgrammer.shutdown()");
      disconnectFromDevice();
      }
   }