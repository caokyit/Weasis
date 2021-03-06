package org.weasis.dicom.viewer2d;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.util.Collection;

import javax.swing.AbstractButton;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.dcm4che3.data.Attributes;
import org.weasis.core.api.gui.util.ActionState;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.gui.util.DropButtonIcon;
import org.weasis.core.api.gui.util.DropDownButton;
import org.weasis.core.api.gui.util.GroupRadioMenu;
import org.weasis.core.api.gui.util.ToggleButtonListener;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.ui.editor.image.DefaultView2d;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.util.WtoolBar;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomSpecialElement;
import org.weasis.dicom.codec.KOSpecialElement;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.viewer2d.KOComponentFactory.SelectedImageFilter;

@SuppressWarnings("serial")
public class KeyObjectToolBar extends WtoolBar {

    public static final ImageIcon KO_STAR_ICON = new ImageIcon(View2d.class.getResource("/icon/24x24/star_bw.png")); //$NON-NLS-1$
    public static final ImageIcon KO_STAR_ICON_SELECTED;
    public static final ImageIcon KO_FILTER_ICON = new ImageIcon(View2d.class.getResource("/icon/24x24/synch-KO.png")); //$NON-NLS-1$
    public static final ImageIcon KO_FILTER_ICON_SELECTED;

    public static final ImageIcon KO_EDIT_SELECTION_ICON = new ImageIcon(
        View2d.class.getResource("/icon/24x24/ko_edit_selection.png")); //$NON-NLS-1$

    static {
        ImageFilter imageFilter = new SelectedImageFilter(new float[] { 1.0f, 0.78f, 0.0f }); // ORANGE

        ImageProducer imageProducer = new FilteredImageSource(KO_STAR_ICON.getImage().getSource(), imageFilter);
        KO_STAR_ICON_SELECTED = new ImageIcon(Toolkit.getDefaultToolkit().createImage(imageProducer));

        imageProducer = new FilteredImageSource(KO_FILTER_ICON.getImage().getSource(), imageFilter);
        KO_FILTER_ICON_SELECTED = new ImageIcon(Toolkit.getDefaultToolkit().createImage(imageProducer));
    }

    public KeyObjectToolBar(int index) {
        super(Messages.getString("KeyObjectToolBar.title"), index); //$NON-NLS-1$

        final EventManager evtMgr = EventManager.getInstance();

        // --------------------------------------------------------------------------------------------------
        final ToggleButtonListener koToggleAction = (ToggleButtonListener) evtMgr.getAction(ActionW.KO_TOOGLE_STATE);
        final JToggleButton toggleKOSelectionBtn = new JToggleButton();

        toggleKOSelectionBtn.setToolTipText(ActionW.KO_TOOGLE_STATE.getTitle());
        toggleKOSelectionBtn.setIcon(KO_STAR_ICON);
        toggleKOSelectionBtn.setSelectedIcon(KO_STAR_ICON_SELECTED);

        koToggleAction.registerActionState(toggleKOSelectionBtn);
        add(toggleKOSelectionBtn);

        // --------------------------------------------------------------------------------------------------
        final ToggleButtonListener koFilterAction = (ToggleButtonListener) evtMgr.getAction(ActionW.KO_FILTER);
        final JToggleButton koFilterBtn = new JToggleButton();

        koFilterBtn.setToolTipText(ActionW.KO_FILTER.getTitle());
        koFilterBtn.setIcon(KO_FILTER_ICON);
        koFilterBtn.setSelectedIcon(KO_FILTER_ICON_SELECTED);

        koFilterAction.registerActionState(koFilterBtn);
        add(koFilterBtn);

        // --------------------------------------------------------------------------------------------------
        final ComboItemListener koSelectionAction = (ComboItemListener) evtMgr.getAction(ActionW.KO_SELECTION);
        GroupRadioMenu koSelectionMenu = koSelectionAction.createGroupRadioMenu();

        final DropDownButton koSelectionButton =
            new DropDownButton(ActionW.KO_SELECTION.cmd(), buildKoSelectionIcon(), koSelectionMenu) {
                @Override
                protected JPopupMenu getPopupMenu() {
                    JPopupMenu popupMenu =
                        (getMenuModel() == null) ? new JPopupMenu() : getMenuModel().createJPopupMenu();
                    popupMenu.setInvoker(this);
                    return popupMenu;
                }
            };

        koSelectionButton.setToolTipText(ActionW.KO_SELECTION.getTitle());
        koSelectionAction.registerActionState(koSelectionButton);
        add(koSelectionButton);

        // --------------------------------------------------------------------------------------------------
        JButton koEditSelectionBtn = new JButton(KO_EDIT_SELECTION_ICON);

        // final String[] optionsNoSelection = { "Delete KeyObject", "Create a new KeyObject" };
        // final String[] optionsWithSelection = { "Delete KeyObject", "Copy selected KeyObject" };

        koEditSelectionBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                ImageViewerPlugin<DicomImageElement> selectedView2dContainer;
                DefaultView2d<DicomImageElement> selectedView2d;
                MediaSeries<DicomImageElement> selectedDicomSeries;

                if ((selectedView2dContainer = evtMgr.getSelectedView2dContainer()) == null) {
                    return;
                }
                if ((selectedView2d = evtMgr.getSelectedViewPane()) == null) {
                    return;
                }
                if ((selectedDicomSeries = selectedView2d.getSeries()) == null) {
                    return;
                }

                // List<Object> koElementList = new ArrayList<Object>(Arrays.asList(koSelectionAction.getAllItem()));
                // koElementList.remove(ActionState.NONE);
                // JList list = new JList(koElementList.toArray());

                // MediaSeriesGroup patientGroup = view2dContainer.getGroupID();
                // if (patientGroup == null) {
                // return;
                // }
                // Collection<KOSpecialElement> koElementCollection = DicomModel.getKoSpecialElements(patientGroup);

                Collection<KOSpecialElement> koElementCollection = DicomModel.getKoSpecialElements(selectedDicomSeries);

                final JList list = new JList();
                list.setSelectionModel(new ToggleSelectionModel());

                if (koElementCollection != null) {
                    list.setListData(koElementCollection.toArray());
                }
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                // list.setVisibleRowCount(-1);

                JScrollPane scrollList = new JScrollPane(list);
                // scrollList.setPreferredSize(new Dimension(500, 50));

                final JButton deleteBtn = new JButton(Messages.getString("KeyObjectToolBar.delete")); //$NON-NLS-1$
                final JButton createBtn = new JButton(Messages.getString("KeyObjectToolBar.new")); //$NON-NLS-1$
                final JButton copyBtn = new JButton(Messages.getString("KeyObjectToolBar.copy")); //$NON-NLS-1$

                int maxBtnWidth = 0;
                maxBtnWidth = Math.max(maxBtnWidth, deleteBtn.getPreferredSize().width);
                maxBtnWidth = Math.max(maxBtnWidth, createBtn.getPreferredSize().width);
                maxBtnWidth = Math.max(maxBtnWidth, copyBtn.getPreferredSize().width);

                deleteBtn.setPreferredSize(new Dimension(maxBtnWidth, deleteBtn.getPreferredSize().height));
                createBtn.setPreferredSize(new Dimension(maxBtnWidth, createBtn.getPreferredSize().height));
                copyBtn.setPreferredSize(new Dimension(maxBtnWidth, copyBtn.getPreferredSize().height));

                Object[] message = { Messages.getString("KeyObjectToolBar.k0_list"), scrollList }; //$NON-NLS-1$

                final JOptionPane pane =
                    new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                        new JButton[] { deleteBtn, createBtn }, createBtn);

                deleteBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pane.setValue(deleteBtn);
                    }
                });
                createBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pane.setValue(createBtn);
                    }
                });
                copyBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pane.setValue(copyBtn);
                    }
                });

                list.addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }

                        if (list.isSelectionEmpty()) {
                            pane.setOptions(new JButton[] { deleteBtn, createBtn });
                            pane.setInitialValue(createBtn);
                        } else {
                            pane.setOptions(new JButton[] { deleteBtn, copyBtn });
                            pane.setInitialValue(copyBtn);
                        }

                        deleteBtn.setEnabled(!list.isSelectionEmpty());
                    }
                });

                if (list.getModel().getSize() > 0) {
                    Object selectedItem = koSelectionAction.getSelectedItem();
                    if (selectedItem.equals(ActionState.NONE)) {
                        selectedItem = null;
                    }
                    // list.setSelectedValue(selectedItem, true);
                }

                deleteBtn.setEnabled(!list.isSelectionEmpty());

                pane.setComponentOrientation(selectedView2dContainer.getComponentOrientation());
                JDialog dialog =
                    pane.createDialog(selectedView2dContainer, Messages.getString("KeyObjectToolBar.edit")); //$NON-NLS-1$

                pane.selectInitialValue();

                dialog.setVisible(true);
                dialog.dispose();

                Object selectedValue = pane.getValue();

                if (selectedValue != null) {

                    if (selectedValue.equals(deleteBtn)) {
                        System.out.println("Delete KeyObject"); //$NON-NLS-1$

                        DicomModel dicomModel = (DicomModel) selectedDicomSeries.getTagValue(TagW.ExplorerModel);
                        if (dicomModel != null) {
                            dicomModel.removeSpecialElement((DicomSpecialElement) list.getSelectedValue());
                        }

                    } else {
                        Attributes newDicomKO = null;

                        if (selectedValue.equals(createBtn)) {
                            System.out.println("Create new KeyObject"); //$NON-NLS-1$
                            newDicomKO =
                                KOManager.createNewDicomKeyObject(selectedView2d.getImage(), selectedView2dContainer);

                        } else if (selectedValue.equals(copyBtn)) {
                            System.out.println("Copy selected KeyObject : " + list.getSelectedValue().toString()); //$NON-NLS-1$
                            newDicomKO =
                                KOManager.createNewDicomKeyObject((MediaElement<?>) list.getSelectedValue(),
                                    selectedView2dContainer);
                        }

                        if (newDicomKO != null) {

                            // Deactivate filter for new KO
                            koFilterAction.setSelected(false);

                            KOSpecialElement newKOSelection =
                                KOManager.loadDicomKeyObject(selectedDicomSeries, newDicomKO);

                            koSelectionAction.setSelectedItem(newKOSelection);
                        }
                    }
                }
            }
        });

        add(koEditSelectionBtn);
    }

    private Icon buildKoSelectionIcon() {
        final Icon mouseIcon = new ImageIcon(View2d.class.getResource("/icon/24x24/dcm-KO.png")); //$NON-NLS-1$

        return new DropButtonIcon(new Icon() {

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                if (c instanceof AbstractButton) {
                    AbstractButton model = (AbstractButton) c;
                    Icon icon = null;
                    if (!model.isEnabled()) {
                        icon = UIManager.getLookAndFeel().getDisabledIcon(model, mouseIcon);
                    }
                    if (icon == null) {
                        icon = mouseIcon;
                    }
                    icon.paintIcon(c, g, x, y);
                }
            }

            @Override
            public int getIconWidth() {
                return mouseIcon.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return mouseIcon.getIconHeight();
            }
        });
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    class ToggleSelectionModel extends DefaultListSelectionModel {
        private boolean gestureStarted = false;

        @Override
        public void setSelectionInterval(int index0, int index1) {
            // Toggle only one element while the user is dragging the mouse
            if (!gestureStarted) {
                if (isSelectedIndex(index0)) {
                    super.removeSelectionInterval(index0, index1);
                } else {
                    if (getSelectionMode() == SINGLE_SELECTION) {
                        super.setSelectionInterval(index0, index1);
                    } else {
                        super.addSelectionInterval(index0, index1);
                    }
                }
            }

            // Disable toggling till the adjusting is over, or keep it
            // enabled in case setSelectionInterval was called directly.
            gestureStarted = getValueIsAdjusting();

        }

        @Override
        public void setValueIsAdjusting(boolean isAdjusting) {
            super.setValueIsAdjusting(isAdjusting);

            if (isAdjusting == false) {
                // Enable toggling
                gestureStarted = false;
            }
        }
    }

}
