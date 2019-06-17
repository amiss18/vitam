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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.request.OfferSyncRequest;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.server.offersynchronization.OfferSyncStatus;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.util.Lists.newArrayList;

/**
 * StorageTwoOffersIT class
 */
public class StorageTwoOffersIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StorageTwoOffersIT.class);
    static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    static final String DEFAULT_SECOND_CONF = "storage-test/storage-default-offer2-ssl.conf";
    static final String STORAGE_CONF = "storage-test/storage-engine.conf";
    static final int PORT_SERVICE_WORKSPACE = 8987;
    static final int PORT_SERVICE_STORAGE = 8583;
    static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private static final int TENANT_0 = 0;
    private static final String DIGEST = "digest";
    private static final String SECOND_OFFER_ID = "default2";
    private static final String OFFER_ID = "default";
    private static final String STRATEGY_ID = "default";
    private static final String DB_OFFER1 = "vitamoffer1";
    private static final String DB_OFFER2 = "vitamoffer2";

    static StorageClient storageClient;
    static WorkspaceClient workspaceClient;
    static final String STORAGE_CONF_FILE_NAME = "default-storage.conf";

    private static final String BASIC_AUTHN_USER = "user";
    private static final String BASIC_AUTHN_PWD = "pwd";

    @ClassRule
    public static TempFolderRule tempFolder = new TempFolderRule();

    public static String OFFER_FOLDER;

    public static String SECOND_FOLDER;


    @ClassRule
    public static MongoRule mongoRuleOffer1 = new MongoRule(DB_OFFER1, VitamCollection.getMongoClientOptions());

    @ClassRule
    public static MongoRule mongoRuleOffer2 = new MongoRule(DB_OFFER2, VitamCollection.getMongoClientOptions());

    private static OfferSyncAdminResource offerSyncAdminResource;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        OFFER_FOLDER = tempFolder.newFolder().getAbsolutePath();
        SECOND_FOLDER = tempFolder.newFolder().getAbsolutePath();

        OfferCollections.OFFER_LOG.setPrefix(GUIDFactory.newGUID().getId());
        OfferCollections.OFFER_SEQUENCE.setPrefix(GUIDFactory.newGUID().getId());
        mongoRuleOffer1.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRuleOffer1.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());
        mongoRuleOffer2.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRuleOffer2.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());


        VitamConfiguration.setRestoreBulkSize(15);

        SetupStorageAndOffers.setupStorageAndTwoOffer();

        // reconstruct service interface - replace non existing client
        // uncomment timeouts for debug mode
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(600, TimeUnit.SECONDS)
            .connectTimeout(600, TimeUnit.SECONDS)
            .build();
        Retrofit retrofit =
            new Retrofit.Builder().client(okHttpClient)
                .baseUrl("http://localhost:" + SetupStorageAndOffers.storageEngineAdminPort)
                .addConverterFactory(JacksonConverterFactory.create()).build();
        offerSyncAdminResource = retrofit.create(OfferSyncAdminResource.class);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongoRuleOffer1.handleAfterClass();
        mongoRuleOffer2.handleAfterClass();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void init() throws IOException {
        cleanOffer(OFFER_FOLDER);
        cleanOffer(SECOND_FOLDER);
    }

    @After
    public void cleanup() throws IOException {
        cleanOffer(OFFER_FOLDER);
        cleanOffer(SECOND_FOLDER);
        mongoRuleOffer1.handleAfter();
        mongoRuleOffer2.handleAfter();
    }

    private void cleanOffer(String offerFolder) throws IOException {
        File offerDir = new File(offerFolder);
        if (offerDir.exists()) {
            File[] containerDirs = offerDir.listFiles(File::isDirectory);
            if (containerDirs != null) {
                for (File container : containerDirs) {
                    FileUtils.cleanDirectory(container);
                }
            }
        }
    }

    private void storeObjectInAllOffers(String id, DataCategory category, InputStream inputStream) throws Exception {
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID(id);
        description.setWorkspaceObjectURI(id);
        workspaceClient.createContainer(id);
        workspaceClient.putObject(id, id, inputStream);
        StreamUtils.closeSilently(inputStream);
        storageClient.storeFileFromWorkspace(STRATEGY_ID, category, id, description);
    }

    private void storeObjectInOffers(String objectId, DataCategory dataCategory, byte[] data,
        String... offerIds) throws InvalidParseOperationException, StorageServerClientException {
        storageClient.create(VitamConfiguration.getDefaultStrategy(), objectId, dataCategory, new ByteArrayInputStream(data), (long) data.length,
            Arrays.asList(offerIds));
    }

    @Test
    @RunWithCustomExecutor
    public void checkStoreInOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        //Given
        String id = GUIDFactory.newGUID().getId();

        // When
        storeObjectInOffers(id, OBJECT, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent(id, OBJECT, true, id.getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void testCopyFromOneOfferToAnother() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String id = GUIDFactory.newGUID().getId();
        String id2 = GUIDFactory.newGUID().getId();
        //WHEN
        storeObjectInAllOffers(id, OBJECT, new ByteArrayInputStream(id.getBytes()));
        storeObjectInAllOffers(id2, OBJECT, new ByteArrayInputStream(id2.getBytes()));

        JsonNode information;
        information =
            storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID), true);
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        alterFileInSecondOffer(id);
        //verify that offer2 is modified
        information =
            storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID), true);
        assertThat(information.get(OFFER_ID).get(DIGEST)).isNotEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

        // correct the offer 2
        storageClient.copyObjectToOneOfferAnother(id, DataCategory.OBJECT, OFFER_ID, SECOND_OFFER_ID);

        // verify That the copy has been correctly done
        information =
            storageClient.getInformation(STRATEGY_ID, OBJECT, id, newArrayList(OFFER_ID, SECOND_OFFER_ID), true);
        assertThat(information.get(OFFER_ID).get(DIGEST)).isEqualTo(information.get(SECOND_OFFER_ID).get(DIGEST));

    }

    // write directly in file of second offer
    private void alterFileInSecondOffer(String id) throws IOException {
        String path = SECOND_FOLDER + File.separator + "0_object" + File.separator + id;
        try (Writer writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(id + "test");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void whenDeleteInOneOfferVerifyObjectDoNotExist() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        //GIVEN
        String object = GUIDFactory.newGUID().getId();
        String object2 = GUIDFactory.newGUID().getId();
        storeObjectInAllOffers(object, OBJECT, new ByteArrayInputStream(object.getBytes()));
        storeObjectInAllOffers(object2, OBJECT, new ByteArrayInputStream(object2.getBytes()));

        JsonNode informationObject1 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object, newArrayList(OFFER_ID,
                SECOND_OFFER_ID), true);
        JsonNode informationObject2 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object2, newArrayList(OFFER_ID,
                SECOND_OFFER_ID), true);

        //just verify the  object  stored and equal
        assertThat(informationObject1.get(OFFER_ID).get(DIGEST))
            .isEqualTo(informationObject1.get(SECOND_OFFER_ID).get(DIGEST));

        String digestObject1SecondOffer = informationObject1.get(SECOND_OFFER_ID).get(DIGEST).textValue();
        String digestObject2SecondOffer = informationObject2.get(SECOND_OFFER_ID).get(DIGEST).textValue();


        //WHEN
        //delete object1 in second offer
        deleteObjectFromOffers(object, OBJECT, SECOND_OFFER_ID);

        deleteObjectFromOffers(object2, OBJECT, OFFER_ID, SECOND_OFFER_ID);


        //THEN
        //delete object2 in the two offers
        informationObject2 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object2, newArrayList(OFFER_ID, SECOND_OFFER_ID), true);

        informationObject1 =
            storageClient.getInformation(STRATEGY_ID, OBJECT, object, newArrayList(OFFER_ID,
                SECOND_OFFER_ID), true);
        //verify Object2 is  deleted in the two offers
        assertThat(informationObject2.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject2.get(OFFER_ID)).isNull();

        //verify Object1 is  deleted in only offer 2
        assertThat(informationObject1.get(SECOND_OFFER_ID)).isNull();
        assertThat(informationObject1.get(OFFER_ID)).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersNewFile() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithDifferentContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When / Then
        Assertions.assertThatThrownBy(
            () -> storeObjectInOffers("file1", OBJECT, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID)
        ).isInstanceOf(StorageServerClientException.class);
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithSameContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateNonRewritableObjectWithSameContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithDifferentContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1-V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithSameContent() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithSameContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void storeObjectInOffersUpdateRewritableObjectWithDifferentContentOnNonSyncOffers() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", UNIT, "data1".getBytes(), OFFER_ID);

        // When
        storeObjectInOffers("file1", UNIT, "data1_V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);

        // Then
        checkFileExistenceAndContent("file1", UNIT, true, "data1_V2".getBytes(), OFFER_ID, SECOND_OFFER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherFromScratch() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        Random random = new Random();
        int NB_ACTIONS = 400;
        List<String> existingFileNames = new ArrayList<>();
        int cpt = 0;

        for (int i = 0; i < NB_ACTIONS; i++) {

            if (existingFileNames.isEmpty() || random.nextInt(5) != 0) {
                cpt++;
                String filename = "ObjectId" + cpt;
                byte[] data = ("Data" + cpt).getBytes(StandardCharsets.UTF_8);
                storeObjectInOffers(filename, OBJECT, data, OFFER_ID);
                existingFileNames.add(filename);
            } else {
                int fileIndexToDelete = random.nextInt(existingFileNames.size());
                String filename = existingFileNames.remove(fileIndexToDelete);
                deleteObjectFromOffers(filename, OBJECT, OFFER_ID);
            }
        }

        // When
        Response<Void> offerSyncResponseItemCall = startSynchronization(null);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall, null, NB_ACTIONS);


        for (int i = 0; i < cpt; i++) {

            String filename = "ObjectId" + i;

            boolean exists = existingFileNames.contains(filename);
            byte[] expectedData = ("Data" + i).getBytes(StandardCharsets.UTF_8);

            checkFileExistenceAndContent(filename, OBJECT, exists, expectedData, SECOND_OFFER_ID);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherAlreadySynchronized() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);
        storeObjectInOffers("file2", OBJECT, "data2".getBytes(), OFFER_ID);
        storeObjectInOffers("file3", OBJECT, "data3".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file2", OBJECT, OFFER_ID);

        for (int i = 0; i < 2; i++) {

            // When
            Response<Void> offerSyncResponseItemCall = startSynchronization(null);

            // Then
            verifyOfferSyncStatus(offerSyncResponseItemCall, null, 4);


            checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), SECOND_OFFER_ID);
            checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
            checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);

        }
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeOneOfferFromAnotherStartingFromOffset() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        storeObjectInOffers("file1", OBJECT, "data1".getBytes(), OFFER_ID);
        storeObjectInOffers("file2", OBJECT, "data2".getBytes(), OFFER_ID);
        storeObjectInOffers("file3", OBJECT, "data3".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file2", OBJECT, OFFER_ID);

        // When
        Response<Void> offerSyncResponseItemCall = startSynchronization(null);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall, null, 4L);
        checkFileExistenceAndContent("file1", OBJECT, true, "data1".getBytes(), SECOND_OFFER_ID);
        checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);

        // Given
        storeObjectInOffers("file4", OBJECT, "data4".getBytes(), OFFER_ID);
        deleteObjectFromOffers("file1", OBJECT, OFFER_ID);

        // When
        Response<Void> offerSyncResponseItemCall2 = startSynchronization(4L);

        // Then
        verifyOfferSyncStatus(offerSyncResponseItemCall2, 4L, 6L);
        checkFileExistenceAndContent("file1", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file2", OBJECT, false, null, SECOND_OFFER_ID);
        checkFileExistenceAndContent("file3", OBJECT, true, "data3".getBytes(), SECOND_OFFER_ID);
        checkFileExistenceAndContent("file4", OBJECT, true, "data4".getBytes(), SECOND_OFFER_ID);
    }

    private Response<Void> startSynchronization(Long offset) throws IOException {
        return offerSyncAdminResource.startSynchronization(new OfferSyncRequest()
                .setSourceOffer(OFFER_ID)
                .setTargetOffer(SECOND_OFFER_ID)
                .setOffset(offset)
                .setContainer(DataCategory.OBJECT.getCollectionName())
                .setStrategyId(STRATEGY_ID)
                .setTenantId(TENANT_0),
            getBasicAuthnToken()).execute();
    }

    private void verifyOfferSyncStatus(Response<Void> offerSyncResponseItemCall, Long startOffset, long expectedOffset)
        throws IOException {
        assertThat(offerSyncResponseItemCall.code()).isEqualTo(200);

        awaitSynchronizationTermination(60);

        Response<OfferSyncStatus> offerSyncStatusResponse =
            offerSyncAdminResource.getLastOfferSynchronizationStatus(getBasicAuthnToken()).execute();
        assertThat(offerSyncStatusResponse.code()).isEqualTo(200);
        OfferSyncStatus offerSyncStatus = offerSyncStatusResponse.body();
        assertThat(offerSyncStatus.getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerSyncStatus.getStartDate()).isNotNull();
        assertThat(offerSyncStatus.getEndDate()).isNotNull();
        assertThat(offerSyncStatus.getSourceOffer()).isEqualTo(OFFER_ID);
        assertThat(offerSyncStatus.getTargetOffer()).isEqualTo(SECOND_OFFER_ID);
        assertThat(offerSyncStatus.getContainer()).isEqualTo(DataCategory.OBJECT.getCollectionName());
        assertThat(offerSyncStatus.getRequestId())
            .isEqualTo(offerSyncResponseItemCall.headers().get(X_REQUEST_ID));
        assertThat(offerSyncStatus.getStartOffset()).isEqualTo(startOffset);
        assertThat(offerSyncStatus.getCurrentOffset()).isEqualTo(expectedOffset);
    }

    private void deleteObjectFromOffers(String filename, DataCategory dataCategory,
        String... offerIds) throws StorageServerClientException {
        storageClient.delete(STRATEGY_ID, dataCategory, filename, Arrays.asList(offerIds));
    }

    private void awaitSynchronizationTermination(int timeoutInSeconds) throws IOException {
        StopWatch stopWatch = StopWatch.createStarted();
        boolean isRunning = true;
        while (isRunning && stopWatch.getTime(TimeUnit.SECONDS) < timeoutInSeconds) {
            Response offerSynchronizationRunning =
                offerSyncAdminResource.isOfferSynchronizationRunning(getBasicAuthnToken()).execute();
            assertThat(offerSynchronizationRunning.code()).isEqualTo(200);
            isRunning = Boolean.parseBoolean(offerSynchronizationRunning.headers().get("Running"));
        }
        if (isRunning) {
            fail("Synchronization took too long");
        }
    }

    private void checkFileExistenceAndContent(String objectId,
        DataCategory dataCategory, boolean exists, byte[] expectedData,
        String... offerIds)
        throws StorageServerClientException, StorageNotFoundClientException {

        JsonNode information = storageClient
            .getInformation(STRATEGY_ID, dataCategory, objectId, Arrays.asList(offerIds), true);
        assertThat(information).hasSize(exists ? offerIds.length : 0);

        if (exists) {
            Digest expectedDigest = new Digest(DigestType.SHA512);
            expectedDigest.update(expectedData);
            for (String offerId : offerIds) {
                assertThat(information.get(offerId).get("digest").textValue())
                    .isEqualTo(expectedDigest.digestHex());
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds));

        // Then
        assertThat(bulkObjectStoreResponse.getDigestType()).isEqualTo(DigestType.SHA512.getName());
        assertThat(bulkObjectStoreResponse.getOfferIds()).containsExactlyInAnyOrder(OFFER_ID, SECOND_OFFER_ID);

        Map<String, String> fileDigests = new HashMap<>();
        for (int i = 0; i < nbFiles; i++) {
            fileDigests.put(objectIds.get(i), new Digest(DigestType.SHA512).update(files.get(i)).digestHex());
        }
        assertThat(bulkObjectStoreResponse.getObjectDigests()).isEqualTo(fileDigests);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(objectIds.get(i), DataCategory.OBJECT, true,
                FileUtils.readFileToByteArray(files.get(i)), OFFER_ID, SECOND_OFFER_ID);
        }

        checkOfferLog(mongoRuleOffer1, nbFiles);
        checkOfferLog(mongoRuleOffer2, nbFiles);
        checkOfferSequence(mongoRuleOffer1, nbFiles);
        checkOfferSequence(mongoRuleOffer2, nbFiles);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // Unknown uri at index 5
        workspaceUris.set(5, GUIDFactory.newGUID().toString());

        // When / Then
        assertThatThrownBy(() -> storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds))
        ).isInstanceOf(StorageServerClientException.class);

        // Check files have NOT been stored in offers
        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(objectIds.get(i), DataCategory.OBJECT, false,
                FileUtils.readFileToByteArray(files.get(i)), OFFER_ID, SECOND_OFFER_ID);
        }

        checkOfferLog(mongoRuleOffer1, 0);
        checkOfferLog(mongoRuleOffer2, 0);
        checkOfferSequence(mongoRuleOffer1, 0);
        checkOfferSequence(mongoRuleOffer2, 0);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceIdempotency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris, files);

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse1 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds));

        BulkObjectStoreResponse bulkObjectStoreResponse2 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris, OBJECT, objectIds));

        // Then
        assertThat(bulkObjectStoreResponse1).isEqualToComparingFieldByFieldRecursively(
            bulkObjectStoreResponse2);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(objectIds.get(i), DataCategory.OBJECT, true,
                FileUtils.readFileToByteArray(files.get(i)), OFFER_ID, SECOND_OFFER_ID);
        }

        checkOfferLog(mongoRuleOffer1, nbFiles * 2);
        checkOfferLog(mongoRuleOffer2, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer1, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer2, nbFiles * 2);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceOverrideRewritableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris1 = new ArrayList<>();
        List<String> workspaceUris2 = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris1.add(GUIDFactory.newGUID().toString());
            workspaceUris2.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files1 = createTempFileSet(nbFiles);
        List<File> files2 = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris1, files1);
        writeFilesToWorkspace(workspaceContainer, workspaceUris2, files2);

        storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris1, UNIT, objectIds));

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse2 = storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris2, UNIT, objectIds));

        // Then
        assertThat(bulkObjectStoreResponse2.getDigestType()).isEqualTo(DigestType.SHA512.getName());
        assertThat(bulkObjectStoreResponse2.getOfferIds()).containsExactlyInAnyOrder(OFFER_ID, SECOND_OFFER_ID);

        Map<String, String> fileDigests = new HashMap<>();
        for (int i = 0; i < nbFiles; i++) {
            fileDigests.put(objectIds.get(i), new Digest(DigestType.SHA512).update(files2.get(i)).digestHex());
        }
        assertThat(bulkObjectStoreResponse2.getObjectDigests()).isEqualTo(fileDigests);

        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(objectIds.get(i), DataCategory.UNIT, true,
                FileUtils.readFileToByteArray(files2.get(i)), OFFER_ID, SECOND_OFFER_ID);
        }

        checkOfferLog(mongoRuleOffer1, nbFiles * 2);
        checkOfferLog(mongoRuleOffer2, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer1, nbFiles * 2);
        checkOfferSequence(mongoRuleOffer2, nbFiles * 2);
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkStoreFilesFromWorkspaceKoWhenTryOverrideNonRewritableContainer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);

        // Given
        int nbFiles = 10;
        String workspaceContainer = GUIDFactory.newGUID().toString();
        List<String> workspaceUris1 = new ArrayList<>();
        List<String> workspaceUris2 = new ArrayList<>();
        List<String> objectIds = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            workspaceUris1.add(GUIDFactory.newGUID().toString());
            workspaceUris2.add(GUIDFactory.newGUID().toString());
            objectIds.add(GUIDFactory.newGUID().toString());
        }

        List<File> files1 = createTempFileSet(nbFiles);
        List<File> files2 = createTempFileSet(nbFiles);

        writeFilesToWorkspace(workspaceContainer, workspaceUris1, files1);

        storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris1, OBJECT, objectIds));

        writeFilesToWorkspace(workspaceContainer, workspaceUris2, files2);

        // When / Then
        assertThatThrownBy(() -> storageClient.bulkStoreFilesFromWorkspace(
            STRATEGY_ID, new BulkObjectStoreRequest(workspaceContainer, workspaceUris2, OBJECT, objectIds))
        ).isInstanceOf(StorageAlreadyExistsClientException.class);
        for (int i = 0; i < nbFiles; i++) {
            checkFileExistenceAndContent(objectIds.get(i), DataCategory.OBJECT, true,
                FileUtils.readFileToByteArray(files1.get(i)), OFFER_ID, SECOND_OFFER_ID);
        }

        checkOfferLog(mongoRuleOffer1, nbFiles);
        checkOfferLog(mongoRuleOffer2, nbFiles);
        checkOfferSequence(mongoRuleOffer1, nbFiles);
        checkOfferSequence(mongoRuleOffer2, nbFiles);
    }

    private void writeFilesToWorkspace(String workspaceContainer, List<String> workspaceUris,
        List<File> files)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException, IOException {
        // Write to workspace
        if (!workspaceClient.isExistingContainer(workspaceContainer)) {
            workspaceClient.createContainer(workspaceContainer);
        }
        for (int i = 0; i < files.size(); i++) {
            try (InputStream inputStream = new FileInputStream(files.get(i))) {
                workspaceClient.putObject(workspaceContainer, workspaceUris.get(i), inputStream);
            }
        }
    }

    private List<File> createTempFileSet(int nbFiles) throws IOException {
        // Create test files
        List<File> files = new ArrayList<>();
        for (int i = 0; i < nbFiles; i++) {
            File file = tempFolder.newFile();
            FileUtils.writeStringToFile(file,
                RandomStringUtils.random(ThreadLocalRandom.current().nextInt(100, 10000)),
                StandardCharsets.US_ASCII);
            files.add(file);
        }
        return files;
    }

    private void checkOfferLog(MongoRule mongoRuleOffer1, int size) {
        assertThat(mongoRuleOffer1.getMongoCollection(OfferCollections.OFFER_LOG.getName()).count())
            .isEqualTo(size);
    }

    private void checkOfferSequence(MongoRule mongoRule, int size) {
        Document seqDoc = mongoRule.getMongoCollection(OfferCollections.OFFER_SEQUENCE.getName()).find().first();
        long offerSeq = seqDoc == null? 0L : ((Number) seqDoc.get(OfferSequence.COUNTER_FIELD)).longValue();
        assertThat(offerSeq).isEqualTo(size);
    }

    @AfterClass
    public static void afterClass() throws Exception {

        SetupStorageAndOffers.close();

        cleanOffer(new File(OFFER_FOLDER));
        cleanOffer(new File(SECOND_FOLDER));
    }

    private static void cleanOffer(File folder) {
        if (folder.exists()) {
            try {
                cleanDirectory(folder);
                deleteDirectory(folder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleaning offer:", e);
            }

        }
    }

    private String getBasicAuthnToken() {
        return Credentials.basic(BASIC_AUTHN_USER, BASIC_AUTHN_PWD);
    }

    public interface OfferSyncAdminResource {

        @POST("/storage/v1/offerSync")
        @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
        })
        Call<Void> startSynchronization(
            @Body OfferSyncRequest offerSyncRequest,
            @Header("Authorization") String basicAuthnToken);

        @HEAD("/storage/v1/offerSync")
        Call<Void> isOfferSynchronizationRunning(
            @Header("Authorization") String basicAuthnToken);

        @GET("/storage/v1/offerSync")
        @Headers({
            "Content-Type: application/json"
        })
        Call<OfferSyncStatus> getLastOfferSynchronizationStatus(
            @Header("Authorization") String basicAuthnToken);

    }
}
