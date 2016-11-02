package edu.cmu.ri.createlab.expressionbuilder.controlpanel.services.led;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.Set;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.cmu.ri.createlab.expressionbuilder.controlpanel.AbstractServiceControlPanel;
import edu.cmu.ri.createlab.expressionbuilder.controlpanel.AbstractServiceControlPanelDevice;
import edu.cmu.ri.createlab.expressionbuilder.controlpanel.ControlPanelManager;
import edu.cmu.ri.createlab.expressionbuilder.controlpanel.ServiceControlPanelDevice;
import edu.cmu.ri.createlab.expressionbuilder.widgets.DeviceSlider;
import edu.cmu.ri.createlab.expressionbuilder.widgets.IntensitySlider;
import edu.cmu.ri.createlab.terk.services.Service;
import edu.cmu.ri.createlab.terk.services.led.FullColorLEDService;
import edu.cmu.ri.createlab.terk.xml.XmlParameter;
import edu.cmu.ri.createlab.userinterface.util.AbstractTimeConsumingAction;
import edu.cmu.ri.createlab.userinterface.util.ImageUtils;
import edu.cmu.ri.createlab.userinterface.util.SwingUtils;
import org.apache.log4j.Logger;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public final class FullColorLEDServiceControlPanel extends AbstractServiceControlPanel
   {
   private static final Logger LOG = Logger.getLogger(FullColorLEDServiceControlPanel.class);

   private static final PropertyResourceBundle RESOURCES = (PropertyResourceBundle)PropertyResourceBundle.getBundle(FullColorLEDServiceControlPanel.class.getName());

   private static final String OPERATION_NAME = "setColor";
   private static final String PARAMETER_NAME_RED = "red";
   private static final String PARAMETER_NAME_GREEN = "green";
   private static final String PARAMETER_NAME_BLUE = "blue";
   private static final Set<String> PARAMETER_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(PARAMETER_NAME_RED, PARAMETER_NAME_GREEN, PARAMETER_NAME_BLUE)));
   private static final Map<String, Set<String>> OPERATIONS_TO_PARAMETERS_MAP;

   static
      {
      final Map<String, Set<String>> operationsToParametersMap = new HashMap<String, Set<String>>();
      operationsToParametersMap.put(OPERATION_NAME, PARAMETER_NAMES);
      OPERATIONS_TO_PARAMETERS_MAP = Collections.unmodifiableMap(operationsToParametersMap);
      }

   private final FullColorLEDService service;
   private final ControlPanelManager controlPanelManager;

   public FullColorLEDServiceControlPanel(final ControlPanelManager controlPanelManager, final FullColorLEDService service)
      {
      super(controlPanelManager, service, OPERATIONS_TO_PARAMETERS_MAP);
      this.service = service;
      this.controlPanelManager = controlPanelManager;
      }

   public String getDisplayName()
      {
      return RESOURCES.getString("control-panel.title");
      }

   public String getSingleName()
      {
      return RESOURCES.getString("control-panel.name");
      }

   public String getShortDisplayName()
      {
      return RESOURCES.getString("control-panel.short-title");
      }

   public JLabel getLabelImage(String imageName)
      {
      final JLabel icon = new JLabel(ImageUtils.createImageIcon(RESOURCES.getString(imageName)));
      icon.setToolTipText(getDisplayName());
      return icon;
      }

   public void refresh()
      {
      LOG.debug("FullColorLEDServiceControlPanel.refresh()");

      // get the current state
      final Color[] colors = service.getColors();

      if (colors != null)
         {
         for (final ServiceControlPanelDevice device : getDevices())
            {
            if (device.getDeviceIndex() >= 0 && device.getDeviceIndex() < colors.length)
               {
               ((ControlPanelDevice)device).updateGUI(colors[device.getDeviceIndex()]);
               }
            }
         }
      }

   protected ServiceControlPanelDevice createServiceControlPanelDevice(final Service service, final int deviceIndex)
      {
      return new ControlPanelDevice(service, deviceIndex);
      }

   private final class ControlPanelDevice extends AbstractServiceControlPanelDevice
      {
      private static final int DEFAULT_ACTUAL_MIN_VALUE = 0;
      private static final int DEFAULT_ACTUAL_MAX_VALUE = 255;
      private static final int DISPLAY_MIN_VALUE = 0;
      private static final int DISPLAY_MAX_VALUE = 255;
      private static final int DISPLAY_INITIAL_VALUE = 0;

      private final int minAllowedIntensity;
      private final int maxAllowedIntensity;
      private final JPanel panel = new JPanel();
      private final DeviceSlider deviceSliderR;
      private final DeviceSlider deviceSliderG;
      private final DeviceSlider deviceSliderB;

      private final DeviceSlider fakeDeviceSliderR;
      private final DeviceSlider fakeDeviceSliderG;
      private final DeviceSlider fakeDeviceSliderB;

      private final MyExecutionStrategy executionStrategy = new MyExecutionStrategy();
      private final int dIndex;

      private int value;
      private JLabel blockIcon = new JLabel();

      private final ImageIcon act_icon = ImageUtils.createImageIcon(RESOURCES.getString("image.yellow"));
      private final ImageIcon dis_icon = ImageUtils.createImageIcon(RESOURCES.getString("image.yellowdisabled"));
      private final ImageIcon off_icon = ImageUtils.createImageIcon(RESOURCES.getString("image.yellowoff"));

      final JPanel act_box = new JPanel();
      final JPanel dis_box = new JPanel();
      final JPanel off_box = new disabledPanel();

      private final JPanel mainPanel = new JPanel();
      private final CardLayout cards = new CardLayout();
      private final String activeCard = "Active Card";
      private final String disabledCard = "Disabled Card";
      private final String offCard = "Off Card";

      private ControlPanelDevice(final Service service, final int deviceIndex)
         {
         super(deviceIndex);
         dIndex = deviceIndex;
         value = DISPLAY_INITIAL_VALUE;

         // try to read service properties, using defaults if undefined
         this.minAllowedIntensity = getServicePropertyAsInt(service, FullColorLEDService.PROPERTY_NAME_MIN_INTENSITY, DEFAULT_ACTUAL_MIN_VALUE);
         this.maxAllowedIntensity = getServicePropertyAsInt(service, FullColorLEDService.PROPERTY_NAME_MAX_INTENSITY, DEFAULT_ACTUAL_MAX_VALUE);
         if (LOG.isDebugEnabled())
            {
            LOG.debug("FullColorLEDServiceControlPanel$ControlPanelDevice.ControlPanelDevice(" + deviceIndex + "): minAllowedIntensity=[" + minAllowedIntensity + "]");
            LOG.debug("FullColorLEDServiceControlPanel$ControlPanelDevice.ControlPanelDevice(" + deviceIndex + "): maxAllowedIntensity=[" + maxAllowedIntensity + "]");
            }

         deviceSliderR = new IntensitySlider(deviceIndex,
                                             DISPLAY_MIN_VALUE,
                                             DISPLAY_MAX_VALUE,
                                             DISPLAY_INITIAL_VALUE,
                                             100,
                                             500,
                                             executionStrategy,
                                             "redLED");
         deviceSliderG = new IntensitySlider(deviceIndex,
                                             DISPLAY_MIN_VALUE,
                                             DISPLAY_MAX_VALUE,
                                             DISPLAY_INITIAL_VALUE,
                                             100,
                                             500,
                                             executionStrategy,
                                             "greenLED");
         deviceSliderB = new IntensitySlider(deviceIndex,
                                             DISPLAY_MIN_VALUE,
                                             DISPLAY_MAX_VALUE,
                                             DISPLAY_INITIAL_VALUE,
                                             100,
                                             500,
                                             executionStrategy,
                                             "blueLED");

         fakeDeviceSliderR = new IntensitySlider(deviceIndex,
                                                 DISPLAY_MIN_VALUE,
                                                 DISPLAY_MAX_VALUE,
                                                 DISPLAY_INITIAL_VALUE,
                                                 100,
                                                 500,
                                                 new DeviceSlider.ExecutionStrategy()
                                                    {
                                                    public void execute(final int deviceIndex, final int value)
                                                       {
                                                       }
                                                    },
                                                 "redLED");
         fakeDeviceSliderG = new IntensitySlider(deviceIndex,
                                                 DISPLAY_MIN_VALUE,
                                                 DISPLAY_MAX_VALUE,
                                                 DISPLAY_INITIAL_VALUE,
                                                 100,
                                                 500,
                                                 new DeviceSlider.ExecutionStrategy()
                                                    {
                                                    public void execute(final int deviceIndex, final int value)
                                                       {
                                                       }
                                                    },
                                                 "greenLED");
         fakeDeviceSliderB = new IntensitySlider(deviceIndex,
                                                 DISPLAY_MIN_VALUE,
                                                 DISPLAY_MAX_VALUE,
                                                 DISPLAY_INITIAL_VALUE,
                                                 100,
                                                 500,
                                                 new DeviceSlider.ExecutionStrategy()
                                                    {
                                                    public void execute(final int deviceIndex, final int value)
                                                       {
                                                       }
                                                    },
                                                 "blueLED");
         fakeDeviceSliderR.setValue(0);
         fakeDeviceSliderG.setValue(0);
         fakeDeviceSliderB.setValue(0);
         fakeDeviceSliderR.setEnabled(false);
         fakeDeviceSliderG.setEnabled(false);
         fakeDeviceSliderB.setEnabled(false);

         ChangeListener sliderListener = new ChangeListener()
            {
            public void stateChanged(final ChangeEvent e)
               {
               final JSlider source = (JSlider)e.getSource();
               if (deviceSliderR.slider.getValue() == 0 && deviceSliderG.slider.getValue() == 0 && deviceSliderB.slider.getValue() == 0)
                  {
                  value = 0;
                  }
               else
                  {
                  value = 1;
                  }
               updateBlockIcon();
               }
            };

         deviceSliderR.slider.addChangeListener(sliderListener);
         deviceSliderG.slider.addChangeListener(sliderListener);
         deviceSliderB.slider.addChangeListener(sliderListener);

         // layout
         //final JPanel colorPanel = new JPanel(new SpringLayout());
         final JPanel colorPanel = new JPanel(new GridBagLayout());
         colorPanel.setBackground(Color.WHITE);
         //colorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

         final GridBagConstraints c = new GridBagConstraints();

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 0;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel.add(SwingUtils.createLabel(RESOURCES.getString("label.red")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 0;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel.add(deviceSliderR.getComponent(), c);

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 1;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel.add(SwingUtils.createLabel(RESOURCES.getString("label.green")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 1;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel.add(deviceSliderG.getComponent(), c);

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 2;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel.add(SwingUtils.createLabel(RESOURCES.getString("label.blue")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 2;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel.add(deviceSliderB.getComponent(), c);

         final JLabel icon = new JLabel(ImageUtils.createImageIcon(RESOURCES.getString("image.enabled")));
         final JPanel iconTitle = new JPanel();
         iconTitle.setLayout(new BoxLayout(iconTitle, BoxLayout.X_AXIS));
         iconTitle.add(icon);
         iconTitle.add(SwingUtils.createRigidSpacer(2));
         iconTitle.add(SwingUtils.createLabel(getSingleName()));
         iconTitle.add(SwingUtils.createRigidSpacer(5));
         iconTitle.add(SwingUtils.createLabel(String.valueOf(deviceIndex + 1)));
         iconTitle.setName("iconTitle");

         icon.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(service.getTypeId(), dIndex, ActivityLevels.OFF);
               }
            });
         icon.setCursor(new Cursor(Cursor.HAND_CURSOR));

         iconTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
         colorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
         panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
         panel.add(iconTitle);
         panel.add(colorPanel);
         panel.setName("enabledServicePanel");

         final JLabel iconDis = new JLabel(ImageUtils.createImageIcon(RESOURCES.getString("image.disabled")));
         iconDis.setAlignmentX(Component.LEFT_ALIGNMENT);
         iconDis.setToolTipText(getSingleName() + " " + String.valueOf(dIndex + 1) + " is disabled");

         act_box.setName("active_service_box");
         act_box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
         act_box.setLayout(new BoxLayout(act_box, BoxLayout.Y_AXIS));
         act_box.add(panel);

         final JLabel icon2 = new JLabel(ImageUtils.createImageIcon(RESOURCES.getString("image.disabled")));
         icon2.setAlignmentX(Component.LEFT_ALIGNMENT);
         off_box.setName("off_service_box");
         off_box.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

         final JLabel fakeIcon = new JLabel(ImageUtils.createImageIcon(RESOURCES.getString("image.enabled")));
         final JPanel fakeIconTitle = new JPanel();
         fakeIconTitle.setLayout(new BoxLayout(fakeIconTitle, BoxLayout.X_AXIS));
         fakeIconTitle.add(fakeIcon);
         fakeIconTitle.add(SwingUtils.createRigidSpacer(2));
         fakeIconTitle.add(SwingUtils.createLabel(getSingleName()));
         fakeIconTitle.add(SwingUtils.createRigidSpacer(5));
         fakeIconTitle.add(SwingUtils.createLabel(String.valueOf(dIndex + 1)));
         fakeIconTitle.setName("iconTitle");
         fakeIconTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
         final Dimension itSize2 = fakeIconTitle.getPreferredSize();
         fakeIconTitle.setBounds(0, 0, itSize2.width, itSize2.height);
         fakeIcon.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(service.getTypeId(), dIndex, ActivityLevels.STAY);
               }
            });
         fakeIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));
         off_box.setLayout(new BoxLayout(off_box, BoxLayout.Y_AXIS));

         off_box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
         off_box.setLayout(new BoxLayout(off_box, BoxLayout.Y_AXIS));

         icon2.setToolTipText(getSingleName() + " " + String.valueOf(dIndex + 1) + " is off");
         off_box.setPreferredSize(act_box.getPreferredSize());
         off_box.setMinimumSize(act_box.getMinimumSize());
         off_box.setMaximumSize(act_box.getMaximumSize());
         off_box.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(FullColorLEDService.TYPE_ID, dIndex, ActivityLevels.STAY);
               }
            });
         icon2.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(FullColorLEDService.TYPE_ID, dIndex, ActivityLevels.STAY);
               }
            });
         off_box.setCursor(new Cursor(Cursor.HAND_CURSOR));
         final JPanel colorPanel2 = new JPanel(new GridBagLayout());
         colorPanel2.setBackground(Color.WHITE);
         //colorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 0;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel2.add(SwingUtils.createLabel(RESOURCES.getString("label.red")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 0;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel2.add(fakeDeviceSliderR.getComponent(), c);

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 1;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel2.add(SwingUtils.createLabel(RESOURCES.getString("label.green")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 1;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel2.add(fakeDeviceSliderG.getComponent(), c);

         c.fill = GridBagConstraints.NONE;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 0;
         c.gridy = 2;
         c.weighty = 0.0;
         c.weightx = 0.0;
         c.anchor = GridBagConstraints.LINE_START;
         c.insets = new Insets(0, 0, 0, 5);
         colorPanel2.add(SwingUtils.createLabel(RESOURCES.getString("label.blue")), c);

         c.fill = GridBagConstraints.HORIZONTAL;
         c.gridwidth = 1;
         c.gridheight = 1;
         c.gridx = 1;
         c.gridy = 2;
         c.weighty = 0.0;
         c.weightx = 1.0;
         c.anchor = GridBagConstraints.CENTER;
         c.insets = new Insets(0, 0, 5, 0);
         colorPanel2.add(fakeDeviceSliderB.getComponent(), c);
         fakeIconTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
         colorPanel2.setAlignmentX(Component.LEFT_ALIGNMENT);
         off_box.setLayout(new BoxLayout(off_box, BoxLayout.Y_AXIS));
         off_box.add(fakeIconTitle);
         off_box.add(colorPanel2);

         dis_box.setName("disabled_service_box");
         dis_box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
         dis_box.setLayout(new BoxLayout(dis_box, BoxLayout.Y_AXIS));
         dis_box.add(iconDis);
         dis_box.setPreferredSize(act_box.getPreferredSize());
         dis_box.setMinimumSize(act_box.getMinimumSize());
         dis_box.setMaximumSize(act_box.getMaximumSize());

         dis_box.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(FullColorLEDService.TYPE_ID, dIndex, ActivityLevels.SET);
               }
            });

         iconDis.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
               {
               controlPanelManager.setDeviceActive(FullColorLEDService.TYPE_ID, dIndex, ActivityLevels.SET);
               controlPanelManager.setDeviceActive(FullColorLEDService.TYPE_ID, dIndex, ActivityLevels.SET);
               }
            });

         dis_box.setCursor(new Cursor(Cursor.HAND_CURSOR));
         mainPanel.setLayout(cards);

         mainPanel.add(act_box, activeCard);
         mainPanel.add(dis_box, disabledCard);
         mainPanel.add(off_box, offCard);

         cards.show(mainPanel, disabledCard);
         }

      public Component getBlockIcon()
         {

         updateBlockIcon();

         return blockIcon;
         }

      public void updateBlockIcon()
         {
         if (this.isActive() == ActivityLevels.SET)
            {
            if (this.value == 0)
               {
               blockIcon.setIcon(off_icon);
               }
            else
               {
               blockIcon.setIcon(act_icon);
               }
            }
         else if (this.isActive() == ActivityLevels.STAY)
            {
            blockIcon.setIcon(dis_icon);
            }
         else
            {
            blockIcon.setIcon(off_icon);
            }
         }

      public void updateComponent()
         {
         if (this.isActive() == ActivityLevels.SET)
            {
            cards.show(mainPanel, activeCard);
            LOG.debug("Updating SimpleLEDServiceControlPanel Component Control Panel: activeCard");
            }
         else if (this.isActive() == ActivityLevels.STAY)
            {
            cards.show(mainPanel, disabledCard);
            LOG.debug("Updating SimpleLEDServiceControlPanel Component Control Panel: disabledCard");
            }
         else
            {
            deviceSliderR.setValue(0);
            deviceSliderG.setValue(0);
            deviceSliderB.setValue(0);
            cards.show(mainPanel, offCard);
            LOG.debug("Updating SimpleLEDServiceControlPanel Component Control Panel: offCard");
            }
         }

      public void getFocus()
         {
         deviceSliderR.getFocus();
         }

      public Component getComponent()
         {
         return mainPanel;
         }

      private void updateGUI(final Color color)
         {
         // Update the sliders, but we don't want to rely on the execution strategy in order for the call to the
         // service to be made since the execution strategy won't get executed if there's no change in the slider's
         // value.  This can happen if the device's state is changed by some other means than via the service, such
         // as calling emergency stop.
         deviceSliderR.setValueNoExecution(scaleToDisplay(color.getRed()));
         deviceSliderG.setValueNoExecution(scaleToDisplay(color.getGreen()));
         deviceSliderB.setValueNoExecution(scaleToDisplay(color.getBlue()));
         }

      public boolean execute(final String operationName, final Map<String, String> parameterMap)
         {
         if (OPERATION_NAME.equals(operationName))
            {
            final String rStr = parameterMap.get(PARAMETER_NAME_RED);
            final String gStr = parameterMap.get(PARAMETER_NAME_GREEN);
            final String bStr = parameterMap.get(PARAMETER_NAME_BLUE);
            try
               {
               final int r = Integer.parseInt(rStr);
               final int g = Integer.parseInt(gStr);
               final int b = Integer.parseInt(bStr);
               final Color color = new Color(r, g, b);

               // update the GUI
               updateGUI(color);

               // now execute the operation on the service
               service.set(getDeviceIndex(), color);

               return true;
               }
            catch (NumberFormatException e)
               {
               LOG.error("NumberFormatException while trying to convert one of the color values to an integer.", e);
               }
            }
         return false;
         }

      public String getCurrentOperationName()
         {
         return OPERATION_NAME;
         }

      public Set<XmlParameter> buildParameters()
         {
         final Integer r = deviceSliderR.getValue();
         final Integer g = deviceSliderG.getValue();
         final Integer b = deviceSliderB.getValue();

         if (r != null && g != null && b != null)
            {
            final Set<XmlParameter> parameters = new HashSet<XmlParameter>();
            parameters.add(new XmlParameter(PARAMETER_NAME_RED, scaleToActual(r)));
            parameters.add(new XmlParameter(PARAMETER_NAME_GREEN, scaleToActual(g)));
            parameters.add(new XmlParameter(PARAMETER_NAME_BLUE, scaleToActual(b)));
            return parameters;
            }

         return null;
         }

      private final class MyExecutionStrategy implements DeviceSlider.ExecutionStrategy
         {
         public void execute(final int deviceIndex, final int value)
            {
            LOG.debug("FullColorLEDServiceControlPanel$ControlPanelDevice$MyExecutionStrategy.execute()");
            service.set(deviceIndex, new Color(scaleToActual(deviceSliderR.getValue()),
                                               scaleToActual(deviceSliderG.getValue()),
                                               scaleToActual(deviceSliderB.getValue())));
            }
         }

      private int scaleToActual(final int value)
         {
         return scaleValue(value, DISPLAY_MIN_VALUE, DISPLAY_MAX_VALUE, minAllowedIntensity, maxAllowedIntensity);
         }

      private int scaleToDisplay(final int value)
         {
         return scaleValue(value, minAllowedIntensity, maxAllowedIntensity, DISPLAY_MIN_VALUE, DISPLAY_MAX_VALUE);
         }
      }

   private class disabledPanel extends JPanel
      {

      public void paint(Graphics g)
         {
         super.paint(g);
         g.setColor(new Color(226, 223, 255, 150));
         g.fillRect(0, 0, this.getWidth(), this.getHeight());
         }
      }
   }