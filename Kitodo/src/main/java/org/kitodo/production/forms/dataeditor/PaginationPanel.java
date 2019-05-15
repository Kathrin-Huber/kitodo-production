/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.production.forms.dataeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.faces.model.SelectItem;

import org.kitodo.api.dataformat.MediaUnit;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.metadata.pagination.Paginator;
import org.kitodo.production.helper.metadata.pagination.PaginatorMode;
import org.kitodo.production.helper.metadata.pagination.PaginatorType;
import org.kitodo.production.helper.metadata.pagination.RomanNumeral;
import org.kitodo.production.services.ServiceManager;

/**
 * Backing bean for the pagination panel.
 */
public class PaginationPanel {

    private DataEditorForm dataEditor;
    private boolean fictitiousCheckboxChecked = false;
    private int newPagesCountValue = 0;
    private List<SelectItem> paginationSelectionItems;
    private List<Integer> paginationSelectionSelectedItems = new ArrayList<>();
    private String paginationStartValue = "1";
    private List<SelectItem> paginationTypeSelectItems;
    private PaginatorType paginationTypeSelectSelectedItem = PaginatorType.ARABIC;
    private List<IllustratedSelectItem> selectPaginationModeItems;
    private PaginatorMode selectPaginationModeSelectedItem = PaginatorMode.PAGES;
    private List<SelectItem> selectPaginationScopeItems;
    private Boolean selectPaginationScopeSelectedItem = Boolean.TRUE;

    /**
     * Constructor.
     *
     * @param dataEditor DataEditorForm instance
     */
    PaginationPanel(DataEditorForm dataEditor) {
        this.dataEditor = dataEditor;
        preparePaginationTypeSelectItems();
        prepareSelectPaginationModeItems();
        prepareSelectPaginationScopeItems();
    }

    /**
     * This method is invoked if the create pagination button is clicked.
     */
    public void createPaginationButtonClick() {
        ServiceManager.getFileService().searchForMedia(dataEditor.getProcess(), dataEditor.getWorkpiece());
        Paginator paginator = new Paginator(metsEditorDefaultPagination(1));
        List<MediaUnit> mediaUnits = dataEditor.getWorkpiece().getMediaUnit().getChildren();
        for (int i = 1; i < mediaUnits.size(); i++) {
            MediaUnit mediaUnit = mediaUnits.get(i);
            mediaUnit.setOrder(i);
            mediaUnit.setOrderlabel(paginator.next());
        }
    }

    /**
     * This method is invoked if the generate dummy images button is clicked.
     */
    public void generateDummyImagesButtonClick() {
        List<MediaUnit> mediaUnits = dataEditor.getWorkpiece().getMediaUnit().getChildren();
        int order = mediaUnits.isEmpty() ? 1 : mediaUnits.get(mediaUnits.size() - 1).getOrder() + 1;
        boolean withAutomaticPagination = ConfigCore.getBooleanParameter(ParameterCore.WITH_AUTOMATIC_PAGINATION);
        Paginator orderlabel = new Paginator(metsEditorDefaultPagination(order));
        for (int i = 1; i <= newPagesCountValue; i++) {
            MediaUnit mediaUnit = new MediaUnit();
            mediaUnit.setOrder(order++);
            if (withAutomaticPagination) {
                mediaUnit.setOrderlabel(orderlabel.next());
            }
            mediaUnits.add(mediaUnit);
        }
    }

    private static String metsEditorDefaultPagination(int first) {
        switch (ConfigCore.getParameter(ParameterCore.METS_EDITOR_DEFAULT_PAGINATION)) {
            case "arabic":
                return Integer.toString(first);
            case "roman":
                return RomanNumeral.format(first, true);
            case "uncounted":
                return " - ";
            default:
                return "";
        }
    }

    /**
     * Return newPagesCountValue.
     *
     * @return newPagesCountValue
     */
    public int getNewPagesCountValue() {
        return newPagesCountValue;
    }

    /**
     * Set newPagesCountValue.
     *
     * @param newPagesCountValue
     *          newPagesCountValue
     */
    public void setNewPagesCountValue(int newPagesCountValue) {
        this.newPagesCountValue = newPagesCountValue;
    }

    /**
     * Return paginationSelectionSelectedItems.
     *
     * @return paginationSelectionSelectedItems
     */
    public List<Integer> getPaginationSelectionSelectedItems() {
        return paginationSelectionSelectedItems;
    }

    /**
     * Set paginationSelectionSelectedItems.
     *
     * @param paginationSelectionSelectedItems
     *          paginationSelectionSelectedItems
     */
    public void setPaginationSelectionSelectedItems(List<Integer> paginationSelectionSelectedItems) {
        this.paginationSelectionSelectedItems = paginationSelectionSelectedItems;
    }

    /**
     * Return paginationStartValue.
     *
     * @return paginationStartValue
     */
    public String getPaginationStartValue() {
        return paginationStartValue;
    }

    /**
     * Set paginationStartValue.
     *
     * @param paginationStartValue
     *          paginationStartValue
     */
    public void setPaginationStartValue(String paginationStartValue) {
        this.paginationStartValue = paginationStartValue;
    }

    /**
     * Return paginationTypeSelectSelectedItem.
     *
     * @return paginationTypeSelectSelectedItem
     */
    public PaginatorType getPaginationTypeSelectSelectedItem() {
        return paginationTypeSelectSelectedItem;
    }

    /**
     * Set paginationTypeSelectSelectedItem.
     *
     * @param paginationTypeSelectSelectedItem
     *          paginationTypeSelectSelectedItem
     */
    public void setPaginationTypeSelectSelectedItem(PaginatorType paginationTypeSelectSelectedItem) {
        this.paginationTypeSelectSelectedItem = paginationTypeSelectSelectedItem;
    }

    /**
     * Return selectPaginationModeSelectedItem.
     *
     * @return selectPaginationModeSelectedItem
     */
    public PaginatorMode getSelectPaginationModeSelectedItem() {
        return selectPaginationModeSelectedItem;
    }

    /**
     * Set selectPaginationModeSelectedItem.
     *
     * @param selectPaginationModeSelectedItem
     *          selectPaginationModeSelectedItem
     */
    public void setSelectPaginationModeSelectedItem(PaginatorMode selectPaginationModeSelectedItem) {
        this.selectPaginationModeSelectedItem = selectPaginationModeSelectedItem;
    }

    /**
     * Return selectPaginationScopeSelectedItem.
     *
     * @return selectPaginationScopeSelectedItem
     */
    public Boolean getSelectPaginationScopeSelectedItem() {
        return selectPaginationScopeSelectedItem;
    }

    /**
     * Set selectPaginationScopeSelectedItem.
     *
     * @param selectPaginationScopeSelectedItem
     *          selectPaginationScopeSelectedItem
     */
    public void setSelectPaginationScopeSelectedItem(Boolean selectPaginationScopeSelectedItem) {
        this.selectPaginationScopeSelectedItem = selectPaginationScopeSelectedItem;
    }

    /**
     * Return paginationSelectionItems.
     *
     * @return paginationSelectionItems
     */
    public List<SelectItem> getPaginationSelectionItems() {
        return paginationSelectionItems;
    }

    /**
     * Return paginationTypeSelectItems.
     *
     * @return paginationTypeSelectItems
     */
    public List<SelectItem> getPaginationTypeSelectItems() {
        return paginationTypeSelectItems;
    }

    /**
     * Return selectPaginationModeItems.
     *
     * @return selectPaginationModeItems
     */
    public List<IllustratedSelectItem> getSelectPaginationModeItems() {
        return selectPaginationModeItems;
    }

    /**
     * Return selectPaginationScopeItems.
     *
     * @return selectPaginationScopeItems
     */
    public List<SelectItem> getSelectPaginationScopeItems() {
        return selectPaginationScopeItems;
    }

    /**
     * Return fictitiousCheckboxChecked.
     *
     * @return fictitiousCheckboxChecked
     */
    public boolean isFictitiousCheckboxChecked() {
        return fictitiousCheckboxChecked;
    }

    /**
     * Set fictitiousCheckboxChecked.
     *
     * @param fictitiousCheckboxChecked
     *          fictitiousCheckboxChecked
     */
    public void setFictitiousCheckboxChecked(boolean fictitiousCheckboxChecked) {
        this.fictitiousCheckboxChecked = fictitiousCheckboxChecked;
    }

    private void preparePaginationSelectionItems() {
        List<MediaUnit> mediaUnits = dataEditor.getWorkpiece().getMediaUnit().getChildren();
        paginationSelectionItems = new ArrayList<>(mediaUnits.size());
        for (int i = 0; i < mediaUnits.size(); i++) {
            MediaUnit mediaUnit = mediaUnits.get(i);
            String label = Objects.isNull(mediaUnit.getOrderlabel()) ? Integer.toString(mediaUnit.getOrder())
                    : mediaUnit.getOrder() + " : " + mediaUnit.getOrderlabel();
            paginationSelectionItems.add(new SelectItem(Integer.toString(i), label));
        }
    }

    private void preparePaginationTypeSelectItems() {
        paginationTypeSelectItems = new ArrayList<>(5);
        paginationTypeSelectItems.add(new SelectItem(PaginatorType.ARABIC, Helper.getTranslation("arabic")));
        paginationTypeSelectItems.add(new SelectItem(PaginatorType.ROMAN, Helper.getTranslation("roman")));
        paginationTypeSelectItems.add(new SelectItem(PaginatorType.UNCOUNTED, Helper.getTranslation("uncounted")));
        paginationTypeSelectItems
                .add(new SelectItem(PaginatorType.FREETEXT, Helper.getTranslation("paginationFreetext")));
        paginationTypeSelectItems
                .add(new SelectItem(PaginatorType.ADVANCED, Helper.getTranslation("paginationAdvanced")));
    }

    private void prepareSelectPaginationModeItems() {
        selectPaginationModeItems = new ArrayList<>(6);
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.PAGES, Helper.getTranslation("pageCount"),
                "paginierung_seite.svg"));
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.DOUBLE_PAGES,
                Helper.getTranslation("columnCount"), "paginierung_spalte.svg"));
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.FOLIATION,
                Helper.getTranslation("blattzaehlung"), "paginierung_blatt.svg"));
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.RECTOVERSO_FOLIATION,
                Helper.getTranslation("blattzaehlungrectoverso"), "paginierung_blatt_rectoverso.svg"));
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.RECTOVERSO,
                Helper.getTranslation("pageCountRectoVerso"), "paginierung_seite_rectoverso.svg"));
        selectPaginationModeItems.add(new IllustratedSelectItem(PaginatorMode.DOUBLE_PAGES,
                Helper.getTranslation("pageCountDouble"), "paginierung_doppelseite.svg"));
    }

    private void prepareSelectPaginationScopeItems() {
        selectPaginationScopeItems = new ArrayList<>(2);
        selectPaginationScopeItems
                .add(new SelectItem(Boolean.TRUE, Helper.getTranslation("abDerErstenMarkiertenSeite")));
        selectPaginationScopeItems.add(new SelectItem(Boolean.FALSE, Helper.getTranslation("nurDieMarkiertenSeiten")));
    }

    /**
     * This method is invoked if the start pagination action button is clicked.
     */
    public void startPaginationClick() {
        if (paginationSelectionSelectedItems.isEmpty()) {
            Helper.setErrorMessage("fehlerBeimEinlesen", "No pages selected for pagination.");
            return;
        }
        String selectPaginationSeparatorSelectedItem = ConfigCore.getParameter(ParameterCore.PAGE_SEPARATORS)
                .split(",")[0];
        String initializer = paginationTypeSelectSelectedItem.format(selectPaginationModeSelectedItem,
                paginationStartValue, fictitiousCheckboxChecked, selectPaginationSeparatorSelectedItem);
        Paginator paginator = new Paginator(initializer);
        List<MediaUnit> mediaUnits = dataEditor.getWorkpiece().getMediaUnit().getChildren();
        if (selectPaginationScopeSelectedItem) {
            for (int i = paginationSelectionSelectedItems.get(0); i < mediaUnits.size(); i++) {
                mediaUnits.get(0).setOrderlabel(paginator.next());
            }
        } else {
            for (int i : paginationSelectionSelectedItems) {
                mediaUnits.get(i).setOrderlabel(paginator.next());
            }
        }
    }

    /**
     * Show.
     */
    public void show() {
        preparePaginationSelectionItems();
    }
}