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

package org.kitodo.production.forms;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.services.ServiceManager;
import org.kitodo.production.services.file.FileService;

@Named("MigrationForm")
@ViewScoped
public class MigrationForm implements Serializable {

    private static final Logger logger = LogManager.getLogger(MigrationForm.class);
    private List<Project> allProjects = new ArrayList<>();
    private List<Project> selectedProjects = new ArrayList<>();
    private List<Process> processList = new ArrayList<>();
    private boolean projectListShown;
    private boolean processListShown;

    /**
     * Migrates the meta.xml for all processes in the database (if it's in the old format).
     * @throws DAOException if database access fails
     */
    public void migrateMetadata() throws DAOException {
        List<Process> processes = ServiceManager.getProcessService().getAll();
        FileService fileService = ServiceManager.getFileService();
        URI metadataFilePath;
        for (Process process : processes) {
            try {
                metadataFilePath = fileService.getMetadataFilePath(process, true, true);
                ServiceManager.getDataEditorService().readData(metadataFilePath);
            } catch (IOException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
    }

    /**
     * Shows all projects for migration.
     */
    public void showPossibleProjects() {
        try {
            allProjects = ServiceManager.getProjectService().getAll();
            projectListShown = true;
        } catch (DAOException e) {
            Helper.setErrorMessage("Error during database access");
        }
    }

    /**
     * Shows all processes related to the selected projects.
     */
    public void showProcessesForProjects() {
        processList.clear();
        for (Project project : selectedProjects) {
            processList.addAll(project.getProcesses());
        }
        processListShown = true;

    }

    /**
     * Get allProjects.
     *
     * @return value of allProjects
     */
    public List<Project> getAllProjects() {
        return allProjects;
    }

    /**
     * Set selectedProjects.
     *
     * @param selectedProjects as List of Project
     */
    public void setSelectedProjects(List<Project> selectedProjects) {
        this.selectedProjects = selectedProjects;
    }

    /**
     * Get projectListShown.
     *
     * @return value of projectListShown
     */
    public boolean isProjectListShown() {
        return projectListShown;
    }

    /**
     * Get selectedProjects.
     *
     * @return value of selectedProjects
     */
    public List<Project> getSelectedProjects() {
        return selectedProjects;
    }

    /**
     * Get processList.
     *
     * @return value of processList
     */
    public List<Process> getProcessList() {
        return processList;
    }

    /**
     * Get processListShown.
     *
     * @return value of processListShown
     */
    public boolean isProcessListShown() {
        return processListShown;
    }

}