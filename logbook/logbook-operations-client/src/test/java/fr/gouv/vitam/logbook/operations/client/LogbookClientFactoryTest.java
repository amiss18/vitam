/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.operations.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.operations.client.LogbookClientFactory.LogbookClientType;

/**
 * Test class for client (and parameters) factory
 */
public class LogbookClientFactoryTest {

    @Before
    public void initFileConfiguration() {
        LogbookClientFactory.getInstance().changeConfigurationFile("logbook-client.conf");
    }

    @Test
    public void getClientInstanceTest() {
        try {
            LogbookClientFactory.setConfiguration(LogbookClientType.OPERATIONS, null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            LogbookClientFactory.setConfiguration(LogbookClientType.OPERATIONS, "localhost", -10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            LogbookClientFactory.setConfiguration(null, null, 10);
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        LogbookClientFactory.setConfiguration(LogbookClientType.MOCK_OPERATIONS, null, -1);

        final LogbookClient client =
            LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertNotNull(client);

        final LogbookClient client2 =
            LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertNotNull(client2);

        assertNotSame(client, client2);
    }

    @Test
    public void changeDefaultClientTypeTest()  {
        final LogbookClient client =
            LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertTrue(client instanceof LogbookOperationsClientRest);
        final LogbookClientFactory.LogbookClientType type = LogbookClientFactory.getDefaultLogbookClientType();
        assertNotNull(type);
        assertEquals(LogbookClientType.OPERATIONS, type);

        LogbookClientFactory.setConfiguration(LogbookClientType.MOCK_OPERATIONS, null, 0);
        final LogbookClient client2 = LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertTrue(client2 instanceof LogbookOperationsClientMock);
        final LogbookClientFactory.LogbookClientType type2 = LogbookClientFactory.getDefaultLogbookClientType();
        assertNotNull(type2);
        assertEquals(LogbookClientType.MOCK_OPERATIONS, type2);

        LogbookClientFactory.setConfiguration(LogbookClientFactory.LogbookClientType.OPERATIONS, "server", 1025);
        final LogbookClient client3 = LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertTrue(client3 instanceof LogbookOperationsClientRest);
        final LogbookClientFactory.LogbookClientType type3 = LogbookClientFactory.getDefaultLogbookClientType();
        assertNotNull(type3);
        assertEquals(LogbookClientType.OPERATIONS, type3);
    }

    @Test
    public void testInitWithoutConfigurationFile() {
        // assume that a fake file is like no file
        LogbookClientFactory.getInstance().changeConfigurationFile("tmp");
        final LogbookClient client = LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertTrue(client instanceof LogbookOperationsClientMock);
        final LogbookClientFactory.LogbookClientType type = LogbookClientFactory.getDefaultLogbookClientType();
        assertNotNull(type);
        assertEquals(LogbookClientType.MOCK_OPERATIONS, type);
    }

    @Test
    public void testInitWithConfigurationFile() {
        final LogbookClient client =
            LogbookClientFactory.getInstance().getLogbookOperationClient();
        assertTrue(client instanceof LogbookOperationsClientRest);
        final LogbookClientFactory.LogbookClientType type = LogbookClientFactory.getDefaultLogbookClientType();
        assertNotNull(type);
        assertEquals(LogbookClientType.OPERATIONS, type);
    }
}
