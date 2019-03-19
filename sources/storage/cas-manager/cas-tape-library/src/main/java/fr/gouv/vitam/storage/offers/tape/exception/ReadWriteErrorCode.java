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
package fr.gouv.vitam.storage.offers.tape.exception;

public enum ReadWriteErrorCode {
    INTERNAL_ERROR_SERVER,
    TAPE_NOT_FOUND_IN_CATALOG,
    NO_EMPTY_SLOT_FOUND,
    /**
     * Previous location should not be a drive, but a slot or mailbox
     * Current location should not be null
     */
    TAPE_LOCATION_CONFLICT,
    FILE_NOT_FOUND,
    NULL_CURRENT_TAPE,
    KO_DB_PERSIST,
    KO_ON_LOAD_TAPE,
    KO_ON_UNLOAD_TAPE,
    KO_ON_WRITE_TO_TAPE, // TODO CloseIncident Tape
    KO_ON_WRITE_REWIND_TAPE,
    KO_ON_WRITE_REWIND_FSF_TAPE,
    KO_END_OF_TAPE,

    KO_ON_STATUS,
    KO_ON_LOAD_THEN_STATUS,
    KO_ON_UNLOAD_THEN_STATUS,
    KO_ON_WRITE_THEN_STATUS,



}
