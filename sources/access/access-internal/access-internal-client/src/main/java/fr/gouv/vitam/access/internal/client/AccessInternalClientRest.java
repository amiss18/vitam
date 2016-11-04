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

package fr.gouv.vitam.access.internal.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Access client <br>
 * <br>
 * 
 */

// TODO P1 : tenantId should be determined otherwise with a config or so

public class AccessInternalClientRest extends DefaultClient implements AccessInternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalClientRest.class);

    private static final String INVALID_PARSE_OPERATION = "Invalid Parse Operation";
    private static final String NOT_FOUND_EXCEPTION = "Not Found Exception";
    private static final String BLANK_DSL = "select DSL is blank";
    private static final String BLANK_UNIT_ID = "unit identifier should be filled";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_OBJECT_GROUP_ID = "object identifier should be filled";
    private static final String BLANK_USAGE = "usage should be filled";
    private static final String BLANK_VERSION = "usage version should be filled";

    private static final int TENANT_ID = 0;

    AccessInternalClientRest(AccessInternalClientFactory factory) {
        super(factory);
    }
    // FIXME P0 JsonNode en argument pour toutes les "query"
    @Override
    public JsonNode selectUnits(String selectQuery)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectQuery);
        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "units", headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR); // access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            }

            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectUnitbyId(String selectQuery, String idUnit)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectQuery);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, idUnit);

        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
        headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, "units/" + idUnit, headers,
                selectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR); // access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            }

            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, updateQuery);
        ParametersChecker.checkParameter(BLANK_DSL, updateQuery);
        ParametersChecker.checkParameter(BLANK_UNIT_ID, unitId);

        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
        Response response = null;
        try {
            response = performRequest(HttpMethod.PUT, "units/" + unitId, headers,
                updateQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR); // access-common
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) { // access-common
                throw new AccessInternalClientNotFoundException(NOT_FOUND_EXCEPTION);
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);// common
            }

            return response.readEntity(JsonNode.class);
        } catch (VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode selectObjectbyId(String selectObjectQuery, String objectId)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectObjectQuery);
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);

        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            headers.add(GlobalDataRest.X_TENANT_ID, TENANT_ID);
            headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
            response =
                performRequest(HttpMethod.POST, "objects/" + objectId, headers,
                    selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Response.Status status = Status.fromStatusCode(response.getStatus());
            if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);
            } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
                throw new AccessInternalClientNotFoundException(status.getReasonPhrase());
            } else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
            } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
                throw new AccessInternalClientServerException(response.getStatusInfo().getReasonPhrase());
            }

            return response.readEntity(JsonNode.class);

        } catch (VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    @Override
    public Response getObject(String selectObjectQuery, String objectGroupId, String usage, int version)
        throws InvalidParseOperationException, AccessInternalClientServerException,
        AccessInternalClientNotFoundException {
        ParametersChecker.checkParameter(BLANK_DSL, selectObjectQuery);
        ParametersChecker.checkParameter(BLANK_OBJECT_GROUP_ID, objectGroupId);
        ParametersChecker.checkParameter(BLANK_USAGE, usage);
        ParametersChecker.checkParameter(BLANK_VERSION, version);

        final GUID guid = GUIDFactory.newRequestIdGUID(TENANT_ID);
        Response response = null;
        Status status = Status.BAD_REQUEST;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, HttpMethod.GET);
            headers.add(GlobalDataRest.X_TENANT_ID, TENANT_ID);
            headers.add(GlobalDataRest.X_REQUEST_ID, guid.toString());
            headers.add(GlobalDataRest.X_QUALIFIER, usage);
            headers.add(GlobalDataRest.X_VERSION, version);
            response =
                performRequest(HttpMethod.POST, "objects/" + objectGroupId, headers,
                    selectObjectQuery, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR);
                case NOT_FOUND:
                    throw new AccessInternalClientNotFoundException(status.getReasonPhrase());
                case BAD_REQUEST:
                    throw new InvalidParseOperationException(INVALID_PARSE_OPERATION);
                case PRECONDITION_FAILED:
                    throw new AccessInternalClientServerException(response.getStatusInfo().getReasonPhrase());
                case OK:
                    break;
                default:
                    LOGGER.error(INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
                    throw new AccessInternalClientServerException(
                        INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase());
            }
            return response;
        } catch (VitamClientInternalException e) {
            throw new AccessInternalClientServerException(INTERNAL_SERVER_ERROR, e); // access-common
        } finally {
            if (status != Status.OK) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

}
