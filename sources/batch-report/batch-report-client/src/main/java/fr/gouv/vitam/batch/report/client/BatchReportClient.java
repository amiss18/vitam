/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.batch.report.client;

import fr.gouv.vitam.batch.report.model.Report;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.BasicClient;

/**
 * BatchReportClient
 */
public interface BatchReportClient extends BasicClient {

    /**
     * Generate elimination action unit report for the specified process Id
     * Report is stored in JSONL format in workspace.
     *
     * @param processId the given process Id to aggregate
     * @param reportExportRequest report export request
     * @throws VitamClientInternalException VitamClientInternalException
     */
    void generateEliminationActionUnitReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException;

    /**
     * Generate elimination action object group report for the specified process Id
     * Report is stored in JSONL format in workspace.
     *
     * @param processId processId
     * @param reportExportRequest report export request
     * @throws VitamClientInternalException VitamClientInternalException
     */
    void generateEliminationActionObjectGroupReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException;

    /**
     * Generate elimination action district object groups for units by status and process Id.
     * Report is stored in JSONL format in workspace.
     *
     * @param processId
     * @param reportExportRequest report export request
     * @throws VitamClientInternalException
     */
    void generateEliminationActionDistinctObjectGroupInUnitReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException;

    /**
     * Append report entries
     *
     * @param reportBody the given entry document.
     */
    void appendReportEntries(ReportBody reportBody) throws VitamClientInternalException;

    void storeReport(Report reportInfo) throws VitamClientInternalException;

    /**
     * Generate elimination action accession register for deleted units by status and process Id.
     * Report is stored in JSONL format in workspace ORDERED BY opi.
     *
     * @param processId
     * @param reportExportRequest report export request
     * @throws VitamClientInternalException
     */
    void generateEliminationActionAccessionRegisterReport(String processId,
        ReportExportRequest reportExportRequest) throws VitamClientInternalException;

    /**
     * Generate preservation report
     *
     * @param processId processId
     * @param reportExportRequest reportExportRequest
     * @return report in workspace
     * @throws VitamClientInternalException
     */
    void generatePreservationReport(String processId, ReportExportRequest reportExportRequest)
        throws VitamClientInternalException;

    /**
     * Clean all entries with the given process Id tenant and reportType
     *
     * @param processId the given process Id
     * @param reportType report type
     */
    void cleanupReport(String processId, ReportType reportType)
        throws VitamClientInternalException;
}
