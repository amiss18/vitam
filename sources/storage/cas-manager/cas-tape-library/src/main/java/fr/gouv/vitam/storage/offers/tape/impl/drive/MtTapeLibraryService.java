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
package fr.gouv.vitam.storage.offers.tape.impl.drive;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.parser.TapeDriveStatusParser;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;

public class MtTapeLibraryService implements TapeDriveCommandService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MtTapeLibraryService.class);

    public static final String F = "-f";
    public static final String STATUS = "status";
    public static final String FSF = "fsf";
    public static final String REWIND = "rewind";
    public static final String EOD = "eod";
    private final TapeDriveConf tapeDriveConf;
    private final ProcessExecutor processExecutor;
    /**
     * Only the current thread can handle lock unlock
     */
    private final Lock driveLock;

    public MtTapeLibraryService(TapeDriveConf tapeDriveConf, ProcessExecutor processExecutor) {
        ParametersChecker.checkParameter("All params are required", tapeDriveConf, processExecutor);
        this.tapeDriveConf = tapeDriveConf;
        this.processExecutor = processExecutor;
        this.driveLock = tapeDriveConf.getLock();
    }

    @Override
    public TapeDriveSpec status() {
        List<String> args = Lists.newArrayList(F, tapeDriveConf.getDevice(), STATUS);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getMtPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getMtPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        return parseTapeDriveState(output);
    }

    @Override
    public TapeResponse goToPosition(Integer position) {
        ParametersChecker.checkParameter("Arguments position is required", position);
        List<String> args = Lists.newArrayList(F, tapeDriveConf.getDevice(), FSF, position.toString());
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getMtPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getMtPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        return parseCommonResponse(output);
    }

    @Override
    public TapeResponse rewind() {
        List<String> args = Lists.newArrayList(F, tapeDriveConf.getDevice(), REWIND);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getMtPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getMtPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        return parseCommonResponse(output);
    }

    @Override
    public TapeResponse goToEnd() {
        List<String> args = Lists.newArrayList(F, tapeDriveConf.getDevice(), EOD);
        LOGGER.debug("Execute script : {},timeout: {}, args : {}", tapeDriveConf.getMtPath(),
            tapeDriveConf.getTimeoutInMilliseconds(),
            args);
        Output output =
            getExecutor().execute(tapeDriveConf.getMtPath(), tapeDriveConf.getTimeoutInMilliseconds(), args);
        return parseCommonResponse(output);
    }


    @Override
    public ProcessExecutor getExecutor() {
        return processExecutor;
    }

    @Override
    public boolean begin() {
        return driveLock.tryLock();
    }

    @Override
    public void end() {
        driveLock.unlock();
    }


    private TapeDriveSpec parseTapeDriveState(Output output) {
        if (output.getExitCode() == 0) {
            final TapeDriveStatusParser tapeDriveStatusParser = new TapeDriveStatusParser();
            TapeDriveState tapeDriveState = tapeDriveStatusParser.parse(output.getStdout());
            tapeDriveState.setEntity(output);
            return tapeDriveState;
        } else {
            TapeDriveState tapeDriveState =
                new TapeDriveState(output, output.getExitCode() == -1 ? StatusCode.WARNING : StatusCode.KO);
            tapeDriveState.setEntity(output);
            return tapeDriveState;
        }
    }

    private TapeResponse parseCommonResponse(Output output) {
        TapeResponse response;

        if (output.getExitCode() == 0) {
            response = new TapeResponse(output, StatusCode.OK);
        } else {
            response = new TapeResponse(output, output.getExitCode() == -1 ? StatusCode.WARNING : StatusCode.KO);
        }

        return response;
    }
}
