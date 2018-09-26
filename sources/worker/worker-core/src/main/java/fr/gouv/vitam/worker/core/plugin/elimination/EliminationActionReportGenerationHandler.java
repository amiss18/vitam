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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.exception.EliminationException;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportService;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportService;
import fr.gouv.vitam.worker.core.utils.EnumValueCounter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;


/**
 * Elimination action finalization handler.
 */
public class EliminationActionReportGenerationHandler extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionReportGenerationHandler.class);

    private static final String ELIMINATION_ACTION_REPORT_GENERATION = "ELIMINATION_ACTION_REPORT_GENERATION";

    static final String REPORT_JSON = "report.json";

    private final EliminationActionUnitReportService eliminationActionUnitReportService;
    private final EliminationActionObjectGroupReportService eliminationActionObjectGroupReportService;
    private final BackupService backupService;

    /**
     * Default constructor
     */
    public EliminationActionReportGenerationHandler() {
        this(
            new EliminationActionUnitReportService(),
            new EliminationActionObjectGroupReportService(),
            new BackupService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionReportGenerationHandler(
        EliminationActionUnitReportService eliminationActionUnitReportService,
        EliminationActionObjectGroupReportService eliminationActionObjectGroupReportService,
        BackupService backupService) {
        this.eliminationActionUnitReportService = eliminationActionUnitReportService;
        this.eliminationActionObjectGroupReportService = eliminationActionObjectGroupReportService;
        this.backupService = backupService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try {

            File report = generateEliminationReport(param, handler);

            storeEliminationReport(handler, report);

            LOGGER.info("Elimination action finalization succeeded");
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, StatusCode.OK, null);

        } catch (EliminationException e) {
            LOGGER.error(
                String.format("Elimination action finalization failed with status [%s]", e.getStatusCode()), e);
            return buildItemStatus(ELIMINATION_ACTION_REPORT_GENERATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private File generateEliminationReport(WorkerParameters param, HandlerIO handler) throws EliminationException {
        File report = handler.getNewLocalFile(REPORT_JSON);

        EnumValueCounter<EliminationActionUnitStatus> unitStatusStats = new EnumValueCounter<>(
            EliminationActionUnitStatus.class);
        EnumValueCounter<EliminationActionObjectGroupStatus> objectGroupStatusStats =
            new EnumValueCounter<>(EliminationActionObjectGroupStatus.class);

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(report));
            JsonGenerator jsonGenerator = JsonHandler.createJsonGenerator(outputStream)) {

            jsonGenerator.writeStartObject();

            jsonGenerator.writeFieldName("units");
            jsonGenerator.writeStartArray();

            try (CloseableIterator<EliminationActionUnitReportEntry>
                unitIterator = eliminationActionUnitReportService.exportUnits(param.getContainerName())) {

                while (unitIterator.hasNext()) {
                    EliminationActionUnitReportEntry unitEliminationExport = unitIterator.next();
                    jsonGenerator.writeObject(unitEliminationExport);

                    unitStatusStats.increment(unitEliminationExport.getStatus());
                }
            }

            jsonGenerator.writeEndArray();

            jsonGenerator.writeFieldName("objectGroups");
            jsonGenerator.writeStartArray();

            try (CloseableIterator<EliminationActionObjectGroupReportEntry> objectGroupIterator =
                eliminationActionObjectGroupReportService.exportObjectGroups(param.getContainerName())) {

                while (objectGroupIterator.hasNext()) {
                    EliminationActionObjectGroupReportEntry objectGroupEliminationExport = objectGroupIterator.next();
                    jsonGenerator.writeObject(objectGroupEliminationExport);

                    objectGroupStatusStats.increment(objectGroupEliminationExport.getStatus());
                }
            }

            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();

        } catch (IOException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not export elimination report", e);
        }
        return report;
    }

    private void storeEliminationReport(HandlerIO handler, File report) throws EliminationException {
        try {
            backupService.backup(new FileInputStream(report), DataCategory.REPORT,
                handler.getContainerName() + ".json");

        } catch (IOException | BackupServiceException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not store elimination report", e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_REPORT_GENERATION;
    }
}
