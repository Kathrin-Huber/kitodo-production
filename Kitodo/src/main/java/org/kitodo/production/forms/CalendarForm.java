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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.helper.XMLUtils;
import org.kitodo.production.helper.tasks.GeneratesNewspaperProcessesThread;
import org.kitodo.production.helper.tasks.TaskManager;
import org.kitodo.production.model.bibliography.course.Block;
import org.kitodo.production.model.bibliography.course.Cell;
import org.kitodo.production.model.bibliography.course.Course;
import org.kitodo.production.model.bibliography.course.Granularity;
import org.kitodo.production.model.bibliography.course.Issue;
import org.kitodo.production.services.ServiceManager;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * The class CalendarForm provides the screen logic for a JSF calendar editor to
 * enter the course of appearance of a newspaper.
 */
@Named("CalendarForm")
@SessionScoped
public class CalendarForm implements Serializable {
    private static final Logger logger = LogManager.getLogger(CalendarForm.class);

    private static final String BLOCK = "calendar.block.";
    private static final String BLOCK_NEGATIVE = BLOCK + "negative";
    private static final String UPLOAD_ERROR = "calendar.upload.error";
    private static final String DEFAULT_REFERER = "processes";
    private static final Integer[] MONTHS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    /**
     * This is a regular expression to parse date inputs in a flexible way.
     */
    private static final Pattern FLEXIBLE_DATE = Pattern.compile("\\D*(\\d+)\\D+(\\d+)\\D+(\\d+)\\D*");

    /**
     * The constant field issueColours holds the colors used to represent the
     * issues in the calendar editor.
     */
    private static String[] issueColours;

    /**
     * The constant field START_RELATION hold the date the course of publication
     * of the the German-language “Relation aller Fürnemmen und gedenckwürdigen
     * Historien”, which is often recognized as the first newspaper, began. If
     * the user tries to create a block before that date, a hint will be shown.
     */
    private static final LocalDate START_RELATION = LocalDate.of(1605, 9, 12);

    private String referer = DEFAULT_REFERER;
    private Granularity granularity = Granularity.ISSUES;
    private int numberOfPagesPerIssue = 0;
    protected int yearShowing = 1979;
    private UploadedFile uploadedFile;

    /**
     * The field course holds the course of appearance currently under edit by
     * this calendar form instance.
     */
    protected Course course;

    /**
     * The constant field today hold the date of today. Reading the system clock
     * requires much synchronisation throughout the JVM and is therefore only
     * done once on form creation.
     */
    private final LocalDate today = LocalDate.now();

    /**
     * Empty constructor. Creates a new form without yet any data.
     *
     * <p>
     * The issue color presets are samples which have been chosen to provide
     * distinguishability also for users with red-green color vision deficiency.
     * Arbitrary colors can be defined in kitodo_config.properties by setting
     * the property “issue.colours”.
     */
    public CalendarForm() {
        issueColours = ConfigCore.getParameterOrDefaultValue(ParameterCore.ISSUE_COLOURS).split(";");
        course = new Course();
    }

    /**
     * Get referer.
     *
     * @return value of referer
     */
    public String getReferer() {
        return referer;
    }

    /**
     * Set referer.
     *
     * @param referer as java.lang.String
     */
    public void setReferer(String referer) {
        if (Objects.nonNull(referer) && !referer.isEmpty()) {
            this.referer = referer;
        }
    }

    /**
     * Get all possible granularities.
     *
     * @return list of Granularity objects
     */
    public List<Granularity> getGranularities() {
        return Arrays.asList(Granularity.values());
    }

    /**
     * Get granularity.
     *
     * @return value of granularity
     */
    public Granularity getGranularity() {
        return granularity;
    }

    /**
     * Set granularity.
     *
     * @param granularity as org.kitodo.production.model.bibliography.course.Granularity
     */
    public void setGranularity(Granularity granularity) {
        this.granularity = granularity;
        course.splitInto(granularity);
    }

    /**
     * Get array representing the months of a year.
     *
     * @return value of MONTHS
     */
    public static Integer[] getMonths() {
        return MONTHS;
    }

    /**
     * Get the currently displayed year.
     *
     * @return the year to be displayed as java.lang.String
     */
    public String getYear() {
        return Integer.toString(yearShowing);
    }

    /**
     * Set the currently displayed year.
     *
     * @param year to be displayed as java.lang.String
     */
    public void setYear(String year) {
        yearShowing = Integer.parseInt(year);
    }

    /**
     * Display the previous year in the calendar.
     */
    public void previousYear() {
        yearShowing -= 1;
    }

    /**
     * Display the next year in the calendar.
     */
    public void nextYear() {
        yearShowing += 1;
    }

    /**
     * Get estimated number of pages per issue.
     *
     * @return value of numberOfPagesPerIssue
     */
    public int getNumberOfPagesPerIssue() {
        return numberOfPagesPerIssue;
    }

    /**
     * Set estimated number of pages per issue.
     *
     * @param numberOfPagesPerIssue as int
     */
    public void setNumberOfPagesPerIssue(int numberOfPagesPerIssue) {
        this.numberOfPagesPerIssue = numberOfPagesPerIssue;
    }

    /**
     * Get the number of pages of every process for the chosen granularity.
     * Formatted as String with one decimal place.
     *
     * @return number of images as java.lang.String
     */
    public String getNumberOfPagesPerProcessFormatted() {
        DecimalFormat decimalFormat = new DecimalFormat("#.#");
        return decimalFormat.format(getNumberOfPagesPerProcess());
    }

    /**
     * Get the number of pages of every process for the chosen granularity.
     *
     * @return number of pages as long
     */
    public double getNumberOfPagesPerProcess() {
        return course.countIndividualIssues() / ((double) Math.max(course.getNumberOfProcesses(), 1)) * numberOfPagesPerIssue;
    }

    /**
     * The function checkBlockPlausibility compares the dates entered against
     * some plausibility assumptions and sets hints otherwise.
     */
    public void checkBlockPlausibility(Block block) {
        LocalDate firstAppearance = block.getFirstAppearance();
        LocalDate lastAppearance = block.getLastAppearance();
        if (Objects.nonNull(firstAppearance) && Objects.nonNull(lastAppearance)) {
            if (firstAppearance.plusYears(100).isBefore(lastAppearance)) {
                Helper.setMessage(BLOCK + "long");
            }
            if (firstAppearance.isAfter(lastAppearance)) {
                Helper.setErrorMessage(BLOCK_NEGATIVE);
            }
            if (firstAppearance.isBefore(START_RELATION)) {
                Helper.setMessage(BLOCK + "firstAppearance.early");
            }
            if (firstAppearance.isAfter(today)) {
                Helper.setMessage(BLOCK + "firstAppearance.fiction");
            }
            if (lastAppearance.isBefore(START_RELATION)) {
                Helper.setMessage(BLOCK + "lastAppearance.early");
            }
            if (lastAppearance.isAfter(today)) {
                Helper.setMessage(BLOCK + "lastAppearance.fiction");
            }
            this.setYear(String.valueOf(firstAppearance.getYear()));
            if (Objects.nonNull(PrimeFaces.current())) {
                PrimeFaces.current().ajax().update("editForm:calendarTabView:calendarDetailsLayout");
            }
        }
    }

    /**
     * Change whether the selected issue appeared on the selected date.
     * Depending on the regular interval of appearance this will change the additions and exclusions for this issue.
     *
     * @param selectedIssue issue to be modified
     * @param selectedDate date for which the issue will be modified
     */
    public void changeMatch(Issue selectedIssue, LocalDate selectedDate) {
        if (selectedIssue.isMatch(selectedDate) && selectedIssue.getAdditions().contains(selectedDate)) {
            selectedIssue.removeAddition(selectedDate);
        } else if (selectedIssue.isMatch(selectedDate) && !selectedIssue.getAdditions().contains(selectedDate)) {
            selectedIssue.addExclusion(selectedDate);
        } else if (!selectedIssue.isMatch(selectedDate) && selectedIssue.getExclusions().contains(selectedDate)) {
            selectedIssue.removeExclusion(selectedDate);
        } else if (!selectedIssue.isMatch(selectedDate) && !selectedIssue.getExclusions().contains(selectedDate)) {
            selectedIssue.addAddition(selectedDate);
        }
    }

    /**
     * Creates and adds a copy of the currently
     * showing block.
     */
    public void copyBlock(Block block) {
        Block copy = block.clone(course);
        LocalDate lastAppearance = course.getLastAppearance();
        if (Objects.nonNull(lastAppearance)) {
            LocalDate firstAppearance = lastAppearance.plusDays(1);
            copy.setFirstAppearance(firstAppearance);
            copy.setLastAppearance(firstAppearance);
            course.add(copy);
            navigate(copy);
        }
    }

    /**
     * The function is executed if the user clicks the action
     * link to “export” the calendar data. If the course of appearance doesn’t
     * yet contain generated processes—which is always the case, except that the
     * user just came from uploading a data file and didn’t change anything
     * about it—process data will be generated. Then an XML file will be made
     * out of it and sent to the user’s browser. If the granularity was
     * temporarily added, it will be removed afterwards so that the user will
     * not be presented with the option to generate processes “as imported” if
     * he or she never ran an import before.
     *
     * <p>
     * Note: The process data will be generated with a granularity of “days”
     * (each day forms one process). This setting can be changed later after the
     * data has been re-imported, but it will remain if the user uploads the
     * saved data and then proceeds right to the next page and creates processes
     * with the granularity “as imported”. However, since this is possible
     * and—as to our knowledge in late 2014, when this was written—this is the
     * best option of all, this default has been chosen here.
     */
    public StreamedContent download() {
        boolean granularityWasTemporarilyAdded = false;
        try {
            if (Objects.isNull(course) || course.countIndividualIssues() == 0) {
                Helper.setErrorMessage("errorDataIncomplete", "calendar.isEmpty");
                return null;
            }
            if (course.getNumberOfProcesses() == 0) {
                granularityWasTemporarilyAdded = true;
                course.splitInto(Granularity.DAYS);
            }

            byte[] data = XMLUtils.documentToByteArray(course.toXML(), 4);
            return new DefaultStreamedContent(new ByteArrayInputStream(data), "application/xml", "newspaper.xml");
            //FacesUtils.sendDownload(data, "course.xml");
        } catch (TransformerException e) {
            Helper.setErrorMessage("granularity.download.error", "errorTransformerException", logger, e);
        } catch (IOException e) {
            Helper.setErrorMessage("granularity.download.error", e.getLocalizedMessage(), logger, e);
        } finally {
            if (granularityWasTemporarilyAdded) {
                course.clearProcesses();
            }
        }
        return null;
    }

    /**
     * Returns whether the calendar editor is in mint
     * condition, i.e. there is no block defined yet, as read-only property
     * “blank”.
     *
     * <p>
     * Side note: “empty” is a reserved word in JSP and cannot be used as
     * property name.
     *
     * @return whether there is no block yet
     */
    public boolean getBlank() {
        return course.isEmpty();
    }

    /**
     * Returns the data required to build the
     * calendar sheet as read-only property "calendarSheet". The outer list
     * contains 31 entries, each representing a row of the calendar (the days
     * 1−31), each line then contains 12 cells representing the months. This is
     * due to HTML table being produced line by line.
     *
     * @return the table cells to build the calendar sheet
     */
    public List<List<Cell>> getCalendarSheet() {
        List<List<Cell>> calendarSheet = getEmptySheet();
        populateByCalendar(calendarSheet);
        return calendarSheet;
    }

    /**
     * The function will return the course created with this editor
     * as read-only property "course" to pass it to the next form.
     *
     * @return the course of appearance data model
     */
    public Course getCourse() {
        return course;
    }

    /**
     * Builds the empty calendar sheet with 31 rows
     * of twelve cells with empty objects of type Cell().
     *
     * @return an empty calendar sheet
     */
    private List<List<Cell>> getEmptySheet() {
        List<List<Cell>> emptySheet = new ArrayList<>(31);
        for (int day = 1; day <= 31; day++) {
            ArrayList<Cell> row = new ArrayList<>(12);
            for (int month = 1; month <= 12; month++) {
                row.add(new Cell());
            }
            emptySheet.add(row);
        }
        return emptySheet;
    }

    /**
     * The function is the getter method for the property
     * "uploadedFile" which is write-only, however Faces requires is.
     *
     * @return always null
     */
    public UploadedFile getUploadedFile() {
        return null;
    }

    /**
     * Alters the year the calendar sheet is shown for so
     * that something of the current block is visible to prevent the user from
     * needing to click through centuries manually to get there.
     */
    protected void navigate(Block block) {
        try {
            if (yearShowing > block.getLastAppearance().getYear()) {
                yearShowing = block.getLastAppearance().getYear();
            }
            if (yearShowing < block.getFirstAppearance().getYear()) {
                yearShowing = block.getFirstAppearance().getYear();
            }
        } catch (NullPointerException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Tries to interpret a string entered by the user as a date as flexible as
     * possible. Supports two-digit years and imperial date field order
     * (month/day/year). In case of flexible interpretations, hints will be
     * displayed to put the user on the right track what happened to his input.
     *
     * <p>
     * If the user clicks the link to upload a course of appearance file, no
     * warning message shall show. Therefore an alternate white-space character
     * (U+00A0) will be appended to the value string by Javascript on the user
     * side because the setter methods will be called by Faces before the link
     * action will be executed, but we want to skip the error message generation
     * in that case, too.
     *
     * @param value
     *            value entered by the user
     * @param input
     *            input element, one of "firstAppearance" or "lastAppearance"
     * @return the date if found, or null otherwise
     */
    private LocalDate parseDate(String value, String input) {
        Matcher dateParser = FLEXIBLE_DATE.matcher(value);
        int[] numbers = new int[3];
        if (dateParser.matches()) {
            for (int i = 0; i < 3; i++) {
                numbers[i] = Integer.parseInt(dateParser.group(i + 1));
            }
            if (numbers[2] < 100) {
                numbers[2] += 100 * Math.floor((double) today.getYear() / 100);
                if (numbers[2] > today.getYear()) {
                    numbers[2] -= 100;
                }
                Helper.setMessage(Helper.getTranslation(BLOCK + input + ".yearCompleted",
                    Arrays.asList(dateParser.group(3), Integer.toString(numbers[2]))));
            }
            try {
                return LocalDate.of(numbers[2], numbers[1], numbers[0]);
            } catch (DateTimeException invalidDate) {
                try {
                    LocalDate swapped = LocalDate.of(numbers[2], numbers[0], numbers[1]);
                    Helper.setMessage(BLOCK + input + ".swapped");
                    return swapped;
                } catch (DateTimeException stillInvalid) {
                    Helper.setErrorMessage(invalidDate.getLocalizedMessage(), logger, stillInvalid);
                }
            }
        }
        if (!value.contains("\u00A0")) {
            Helper.setErrorMessage(BLOCK + input + ".invalid");
        }
        return null;
    }

    /**
     * Populates an empty calendar sheet by
     * iterating on LocalDate.
     *
     * @param sheet
     *            calendar sheet to populate
     */
    protected void populateByCalendar(List<List<Cell>> sheet) {
        Map<Integer, List<Issue>> issuesMap = new HashMap<>();
        Block currentBlock = null;
        LocalDate nextYear = LocalDate.of(yearShowing + 1, Month.JANUARY, 1);
        for (LocalDate date = LocalDate.of(yearShowing, Month.JANUARY, 1); date
                .isBefore(nextYear); date = date.plusDays(1)) {
            Cell cell = sheet.get(date.getDayOfMonth() - 1).get(date.getMonthValue() - 1);
            cell.setDate(date);
            if (Objects.isNull(currentBlock) || !currentBlock.isMatch(date)) {
                currentBlock = course.isMatch(date);
            }
            if (Objects.isNull(currentBlock)) {
                cell.setOnBlock(false);
            } else {
                Integer hashCode = currentBlock.hashCode();
                if (!issuesMap.containsKey(hashCode)) {
                    issuesMap.put(hashCode, currentBlock.getIssues());
                }
                cell.setIssues(issuesMap.get(hashCode));
            }
        }
    }

    /**
     * Add a block to the course.
     */
    public void addBlock() {
        course.add(new Block(course));
    }

    /**
     * Remove block.
     *
     * @param block
     *          The Block to be removed from the course.
     */
    public void removeBlock(Block block) {
        int index = course.indexOf(block);
        course.remove(block);
        if (index > 0) {
            index--;
        }
        if (course.size() > 0) {
            navigate(course.get(index));
        }
    }

    /**
     * Get the color from the list of defined colors for the given index.
     * These colors are used to highlight and distinguish the different issues in the calendar.
     *
     * @param index index to retrieve color for from list of colors
     * @return The color represented by a String containing the color's hex value
     */
    public String getIssueColor(int index) {
        if (index >= 0 && index < issueColours.length) {
            return issueColours[index];
        }
        return "";
    }

    /**
     * The method will be called by Faces to store the new
     * value of the read-write property "uploadedFile", which is a reference to
     * the binary data the user provides for upload.
     *
     * @param data
     *            the UploadedFile object generated by the Tomahawk library
     */
    public void setUploadedFile(UploadedFile data) {
        uploadedFile = data;
    }

    /**
     * Upload an XML file to import a course of appearance.
     * Overrides the existing contents of course with the contents
     * of the XML file.
     */
    public void upload() {
        try {
            if (Objects.isNull(uploadedFile)) {
                Helper.setMessage(UPLOAD_ERROR, "calendar.upload.isEmpty");
                return;
            }
            Document xml = XMLUtils.load(uploadedFile.getInputstream());
            course = new Course(xml);
            Helper.removeManagedBean("GranularityForm");
            navigate(course.get(0));
        } catch (SAXException e) {
            Helper.setErrorMessage(UPLOAD_ERROR, "errorSAXException", logger, e);
        } catch (IOException e) {
            Helper.setErrorMessage(UPLOAD_ERROR, e.getLocalizedMessage(), logger, e);
        } catch (IllegalArgumentException e) {
            Helper.setErrorMessage("calendar.upload.overlappingDateRanges", logger, e);
        } catch (NoSuchElementException e) {
            Helper.setErrorMessage(UPLOAD_ERROR, "calendar.upload.missingMandatoryElement", logger, e);
        } catch (NullPointerException e) {
            Helper.setErrorMessage("calendar.upload.missingMandatoryValue", logger, e);
        } finally {
            uploadedFile = null;
        }
    }

    /**
     * Create processes for the modelled course of appearance and chosen granularity.
     */
    public void createProcesses() throws DAOException {
        int processId = Integer.parseInt(Helper.getRequestParameter("ID"));
        Process process = ServiceManager.getProcessService().getById(processId);
        TaskManager.addTask(new GeneratesNewspaperProcessesThread(process, course));
    }

    public String formatString(String messageKey, String... replacements) {
        return Helper.getTranslation(messageKey, Arrays.asList(replacements));
    }

    /**
     * Get the first day of the year.
     * This might differ from January 1st as business years might have a different range of time.
     * The used PrimeFaces component requires a Date object including a specific year,
     * however the year is irrelevant for yearStart itself.
     *
     * @return Date representing the first day of the year
     */
    public Date getYearStart() {
        Calendar calendar = new GregorianCalendar(
                today.getYear(), course.getYearStart().getMonth().ordinal(), course.getYearStart().getDayOfMonth());
        return calendar.getTime();
    }

    /**
     * Set the first day of the year.
     * This might differ from January 1st as business years might have a different range of time.
     * The used PrimeFaces component passes a Date object including a specific year,
     * however the year is irrelevant for yearStart itself.
     *
     * @param date Date representing the first day of the year
     */
    public void setYearStart(Date date) {
        if (Objects.nonNull(date)) {
            course.setYearStart(MonthDay.of(date.getMonth() + 1, date.getDate()));
        }
    }

    /**
     * Returns the name of the year. The name of the year is optional and maybe
     * empty. Typical values are “Business year”, “Fiscal year”, or “Season”.
     *
     * @return the name of the year
     */
    public String getYearName() {
        return course.getYearName();
    }

    /**
     * Sets the year name of the course.
     *
     * @param yearName
     *            the yearName to set
     */
    public void setYearName(String yearName) {
        course.setYearName(yearName);
    }

    /**
     * Get today.
     *
     * @return value of today
     */
    public LocalDate getToday() {
        return today;
    }
}
