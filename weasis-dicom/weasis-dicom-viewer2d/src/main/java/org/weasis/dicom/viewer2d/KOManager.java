package org.weasis.dicom.viewer2d;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.dcm4che3.data.Attributes;
import org.weasis.core.api.explorer.ObservableEvent;
import org.weasis.core.api.gui.util.ActionState;
import org.weasis.core.api.gui.util.ActionW;
import org.weasis.core.api.gui.util.ComboItemListener;
import org.weasis.core.api.gui.util.Filter;
import org.weasis.core.api.gui.util.JMVUtils;
import org.weasis.core.api.gui.util.SliderChangeListener;
import org.weasis.core.api.gui.util.SliderCineListener;
import org.weasis.core.api.gui.util.ToggleButtonListener;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.StringUtil;
import org.weasis.core.ui.editor.image.DefaultView2d;
import org.weasis.dicom.codec.DicomImageElement;
import org.weasis.dicom.codec.DicomMediaIO;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.KOSpecialElement;
import org.weasis.dicom.codec.macro.HierachicalSOPInstanceReference;
import org.weasis.dicom.codec.macro.KODocumentModule;
import org.weasis.dicom.codec.utils.DicomMediaUtils;
import org.weasis.dicom.explorer.DicomModel;
import org.weasis.dicom.explorer.LoadDicomObjects;

public final class KOManager {

    public static List<Object> getKOElementListWithNone(DefaultView2d<DicomImageElement> currentView) {

        Collection<KOSpecialElement> koElements =
            currentView != null ? DicomModel.getKoSpecialElements(currentView.getSeries()) : null;

        int koElementNb = (koElements == null) ? 0 : koElements.size();

        List<Object> koElementListWithNone = new ArrayList<Object>(koElementNb + 1);
        koElementListWithNone.add(ActionState.NONE);

        if (koElementNb > 0) {
            koElementListWithNone.addAll(koElements);
        }
        return koElementListWithNone;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Test if current sopInstanceUID is referenced in the selected KEY_OBJECT of the given currentView. If not, search
     * if there is a more suitable new KEY_OBJECT element. Ask the user if needed.
     */

    public static KOSpecialElement getValidKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        KOSpecialElement currentSelectedKO = getCurrentKOSelection(view2d);
        DicomImageElement currentImage = view2d.getImage();

        KOSpecialElement newKOSelection = null;
        Attributes newDicomKO = null;

        if (currentSelectedKO == null) {

            KOSpecialElement validKOSelection = findValidKOSelection(view2d);

            if (validKOSelection != null) {

                String message = Messages.getString("KOManager.select_KO_msg"); //$NON-NLS-1$
                Object[] options =
                    { Messages.getString("KOManager.select_last_ko"), Messages.getString("KOManager.new_ko") }; //$NON-NLS-1$ //$NON-NLS-2$

                int response = JOptionPane.showOptionDialog(view2d, message, Messages.getString("KOManager.ko_title"), //$NON-NLS-1$
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                if (response == 0) {
                    newKOSelection = validKOSelection;
                } else if (response == 1) {
                    newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                } else if (response == JOptionPane.CLOSED_OPTION) {
                    return null;
                }
            } else {
                newDicomKO = createNewDicomKeyObject(currentImage, view2d);
            }

        } else {
            if (currentSelectedKO.getMediaReader().isEditableDicom()) {

                String studyInstanceUID = (String) currentImage.getTagValue(TagW.StudyInstanceUID);

                if (currentSelectedKO.isEmpty()
                    || currentSelectedKO.containsStudyInstanceUIDReference(studyInstanceUID)) {

                    newKOSelection = currentSelectedKO;
                } else {

                    String message = Messages.getString("KOManager.no_ko_msg"); //$NON-NLS-1$
                    Object[] options =
                        { Messages.getString("KOManager.use_ko"), Messages.getString("KOManager.new_ko") }; //$NON-NLS-1$ //$NON-NLS-2$

                    int response =
                        JOptionPane.showOptionDialog(view2d, message, Messages.getString("KOManager.ko_title"), //$NON-NLS-1$
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                    if (response == 0) {
                        newKOSelection = currentSelectedKO;
                    } else if (response == 1) {
                        newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                    } else if (response == JOptionPane.CLOSED_OPTION) {
                        return null;
                    }
                }

            } else {

                String message = Messages.getString("KOManager.ko_readonly"); //$NON-NLS-1$
                Object[] options =
                    { Messages.getString("KOManager.new_ko"), Messages.getString("KOManager.new_ko_from") }; //$NON-NLS-1$ //$NON-NLS-2$

                int response = JOptionPane.showOptionDialog(view2d, message, Messages.getString("KOManager.ko_title"), //$NON-NLS-1$
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

                if (response == 0) {
                    newDicomKO = createNewDicomKeyObject(currentImage, view2d);
                } else if (response == 1) {
                    newDicomKO = createNewDicomKeyObject(currentSelectedKO, view2d);
                } else if (response == JOptionPane.CLOSED_OPTION) {
                    return null;
                }
            }
        }

        if (newDicomKO != null) {
            if (view2d != null) {
                // Deactivate filter for new KO
                ActionState koFilterAction = view2d.getEventManager().getAction(ActionW.KO_FILTER);
                if (koFilterAction instanceof ToggleButtonListener) {
                    ((ToggleButtonListener) koFilterAction).setSelected(false);
                }
            }
            newKOSelection = loadDicomKeyObject(view2d.getSeries(), newDicomKO);
        }

        return newKOSelection;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static KOSpecialElement loadDicomKeyObject(MediaSeries<DicomImageElement> dicomSeries, Attributes newDicomKO) {

        DicomModel dicomModel = (DicomModel) dicomSeries.getTagValue(TagW.ExplorerModel);

        new LoadDicomObjects(dicomModel, newDicomKO).addSelectionAndnotify(); // must be executed in the EDT

        for (KOSpecialElement koElement : DicomModel.getKoSpecialElements(dicomSeries)) {
            if (koElement.getMediaReader().getDicomObject().equals(newDicomKO)) {
                return koElement;
            }
        }

        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static Attributes createNewDicomKeyObject(MediaElement<?> dicomMediaElement, Component parentComponent) {

        if (dicomMediaElement != null && dicomMediaElement.getMediaReader() instanceof DicomMediaIO) {
            Attributes dicomSourceAttribute = ((DicomMediaIO) dicomMediaElement.getMediaReader()).getDicomObject();

            String message = Messages.getString("KOManager.ko_desc"); //$NON-NLS-1$
            String defautDescription = Messages.getString("KOManager.ko_name"); //$NON-NLS-1$

            String description =
                (String) JOptionPane.showInputDialog(parentComponent, message,
                    Messages.getString("KOManager.ko_title"), //$NON-NLS-1$
                    JOptionPane.INFORMATION_MESSAGE, null, null, defautDescription);

            // description==null means the user canceled the input
            if (StringUtil.hasText(description)) {
                Attributes ko = DicomMediaUtils.createDicomKeyObject(dicomSourceAttribute, description, null);

                if (dicomMediaElement instanceof KOSpecialElement) {
                    Collection<HierachicalSOPInstanceReference> referencedStudySequence =
                        new KODocumentModule(dicomSourceAttribute).getCurrentRequestedProcedureEvidences();

                    new KODocumentModule(ko).setCurrentRequestedProcedureEvidences(referencedStudySequence);
                }
                return ko;
            }
        }
        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get an editable Dicom KeyObject Selection suitable to handle current Dicom Image. A valid object should either
     * reference the studyInstanceUID of the current Dicom Image or simply be empty ...
     */

    public static KOSpecialElement findValidKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        MediaSeries<DicomImageElement> dicomSeries = view2d.getSeries();
        DicomImageElement currentImage = view2d.getImage();
        if (currentImage != null && dicomSeries != null) {
            String currentStudyInstanceUID = (String) currentImage.getTagValue(TagW.StudyInstanceUID);
            Collection<KOSpecialElement> koElementsWithReferencedSeriesInstanceUID =
                DicomModel.getKoSpecialElements(dicomSeries);

            if (koElementsWithReferencedSeriesInstanceUID != null) {

                for (KOSpecialElement koElement : koElementsWithReferencedSeriesInstanceUID) {
                    if (koElement.getMediaReader().isEditableDicom()) {
                        if (koElement.containsStudyInstanceUIDReference(currentStudyInstanceUID)) {
                            return koElement;
                        }
                    }
                }

                for (KOSpecialElement koElement : koElementsWithReferencedSeriesInstanceUID) {
                    if (koElement.getMediaReader().isEditableDicom()) {
                        if (koElement.isEmpty()) {
                            return koElement;
                        }
                    }
                }
            }
        }
        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static KOSpecialElement getCurrentKOSelection(final DefaultView2d<DicomImageElement> view2d) {

        Object actionValue = view2d.getActionValue(ActionW.KO_SELECTION.cmd());
        if (actionValue instanceof KOSpecialElement) {
            return (KOSpecialElement) actionValue;
        }

        return null;
    }

    public static boolean setKeyObjectReference(boolean selectedState, final DefaultView2d<DicomImageElement> view2d) {

        KOSpecialElement validKOSelection = getValidKOSelection(view2d);

        if (validKOSelection == null) {
            return false; // canceled
        }

        KOSpecialElement currentSelectedKO = KOManager.getCurrentKOSelection(view2d);

        boolean hasKeyObjectReferenceChanged = false;

        if (validKOSelection != currentSelectedKO) {
            ActionState koSelection = view2d.getEventManager().getAction(ActionW.KO_SELECTION);
            if (koSelection instanceof ComboItemListener) {
                ((ComboItemListener) koSelection).setSelectedItem(validKOSelection);
            }
        }

        if (validKOSelection == currentSelectedKO || currentSelectedKO == null) {
            DicomImageElement currentImage = view2d.getImage();
            hasKeyObjectReferenceChanged = validKOSelection.setKeyObjectReference(selectedState, currentImage);

            if (hasKeyObjectReferenceChanged) {
                DicomModel dicomModel = (DicomModel) view2d.getSeries().getTagValue(TagW.ExplorerModel);
                // Fire an event since any view in any View2dContainer may have its KO selected state changed
                if (dicomModel != null) {
                    dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Update, view2d, null,
                        validKOSelection));
                }
            }
        }

        return hasKeyObjectReferenceChanged;
    }

    public static void setKeyObjectReferenceAllSeries(boolean selectedState,
        final DefaultView2d<DicomImageElement> view2d) {

        KOSpecialElement validKOSelection = getValidKOSelection(view2d);

        if (validKOSelection == null) {
            ((EventManager) view2d.getEventManager()).updateKeyObjectComponentsListener(view2d);
            return; // canceled
        }

        KOSpecialElement currentSelectedKO = KOManager.getCurrentKOSelection(view2d);

        if (validKOSelection != currentSelectedKO) {
            ActionState koSelection = view2d.getEventManager().getAction(ActionW.KO_SELECTION);
            if (koSelection instanceof ComboItemListener) {
                ((ComboItemListener) koSelection).setSelectedItem(validKOSelection);
            }
        }

        validKOSelection.setKeyObjectReference(selectedState, view2d.getSeries().getSortedMedias(null));

        DicomModel dicomModel = (DicomModel) view2d.getSeries().getTagValue(TagW.ExplorerModel);
        if (dicomModel != null) {
            dicomModel.firePropertyChange(new ObservableEvent(ObservableEvent.BasicAction.Update, view2d, null,
                validKOSelection));
        }

    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void updateKOFilter(DefaultView2d<DicomImageElement> view2D, Object newSelectedKO,
        Boolean enableFilter, int imgSelectionIndex) {
        updateKOFilter(view2D, newSelectedKO, enableFilter, imgSelectionIndex, true);
    }

    public static void updateKOFilter(DefaultView2d<DicomImageElement> view2D, Object newSelectedKO,
        Boolean enableFilter, int imgSelectionIndex, boolean updateImage) {

        if (view2D instanceof View2d) {
            boolean tiledMode = imgSelectionIndex >= 0;
            boolean koFilter = false;
            KOSpecialElement selectedKO = null;
            if (newSelectedKO == null) {
                Object actionValue = view2D.getActionValue(ActionW.KO_SELECTION.cmd());
                if (actionValue instanceof KOSpecialElement) {
                    selectedKO = (KOSpecialElement) actionValue;

                    // test if current ko_selection action in view do still exist
                    Collection<KOSpecialElement> koElements =
                        (view2D != null && view2D.getSeries() != null) ? DicomModel.getKoSpecialElements(view2D
                            .getSeries()) : null;
                    if (koElements != null && koElements.contains(selectedKO) == false) {
                        selectedKO = null;
                        newSelectedKO = ActionState.NONE;
                        view2D.setActionsInView(ActionW.KO_SELECTION.cmd(), newSelectedKO);
                    }
                }
            } else {
                if (newSelectedKO instanceof KOSpecialElement) {
                    selectedKO = (KOSpecialElement) newSelectedKO;
                }
                view2D.setActionsInView(ActionW.KO_SELECTION.cmd(), newSelectedKO);
            }

            if (enableFilter == null) {
                koFilter = JMVUtils.getNULLtoFalse(view2D.getActionValue(ActionW.KO_FILTER.cmd()));
            } else {
                koFilter = enableFilter;
            }

            if (tiledMode && selectedKO == null) {
                // Unselect the filter with the None KO selection
                koFilter = false;
            }

            view2D.setActionsInView(ActionW.KO_FILTER.cmd(), koFilter);
            view2D.setActionsInView(ActionW.FILTERED_SERIES.cmd(), null);

            if (selectedKO == null || view2D.getSeries() == null || (view2D.getImage() == null && !tiledMode)) {
                if (newSelectedKO != null) {
                    // Update the None KO selection
                    ((View2d) view2D).updateKOButtonVisibleState();
                }
                return;
            }

            DicomSeries dicomSeries = (DicomSeries) view2D.getSeries();
            String seriesInstanceUID = (String) dicomSeries.getTagValue(TagW.SeriesInstanceUID);
            Filter<DicomImageElement> sopInstanceUIDFilter = null;

            if (koFilter && selectedKO.containsSeriesInstanceUIDReference(seriesInstanceUID)) {
                sopInstanceUIDFilter = selectedKO.getSOPInstanceUIDFilter();
            }
            view2D.setActionsInView(ActionW.FILTERED_SERIES.cmd(), sopInstanceUIDFilter);

            if (updateImage) {

                /*
                 * The getFrameIndex() returns a valid index for the current image displayed according to the current
                 * FILTERED_SERIES and the current SortComparator
                 */
                int newImageIndex = view2D.getFrameIndex();
                if (tiledMode) {
                    newImageIndex = view2D.getTileOffset() + imgSelectionIndex;
                }

                if (koFilter && newImageIndex < 0) {
                    if (dicomSeries.size(sopInstanceUIDFilter) > 0 && view2D.getImage() != null) {

                        double[] val = (double[]) view2D.getImage().getTagValue(TagW.SlicePosition);
                        if (val != null) {
                            double location = val[0] + val[1] + val[2];
                            // Double offset = (Double) view2D.getActionValue(ActionW.STACK_OFFSET.cmd());
                            // if (offset != null) {
                            // location += offset;
                            // }
                            newImageIndex =
                                dicomSeries.getNearestImageIndex(location, view2D.getTileOffset(),
                                    sopInstanceUIDFilter, view2D.getCurrentSortComparator());
                        }
                    } else {
                        // If there is no more image in KO series filtered then disable the KO_FILTER
                        sopInstanceUIDFilter = null;
                        view2D.setActionsInView(ActionW.KO_FILTER.cmd(), false);
                        view2D.setActionsInView(ActionW.FILTERED_SERIES.cmd(), sopInstanceUIDFilter);
                        newImageIndex = view2D.getFrameIndex();
                    }
                }

                if (view2D == view2D.getEventManager().getSelectedViewPane()) {
                    /*
                     * Update the sliceAction action according to nearest image when the filter hides the image of the
                     * previous state.
                     */
                    ActionState seqAction = view2D.getEventManager().getAction(ActionW.SCROLL_SERIES);
                    if (seqAction instanceof SliderCineListener) {
                        SliderChangeListener moveTroughSliceAction = (SliderChangeListener) seqAction;
                        moveTroughSliceAction.setMinMaxValue(1, dicomSeries.size(sopInstanceUIDFilter),
                            newImageIndex + 1);
                    }
                } else {
                    DicomImageElement newImage =
                        dicomSeries.getMedia(newImageIndex, sopInstanceUIDFilter, view2D.getCurrentSortComparator());
                    if (newImage != null && !newImage.isImageAvailable()) {
                        newImage.getImage();
                    }
                    ((View2d) view2D).setImage(newImage);
                }
            }
            ((View2d) view2D).updateKOButtonVisibleState();
        }
    }
}
