package com.dabsquared.gitlabjenkins.publisher;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static com.dabsquared.gitlabjenkins.publisher.TestUtility.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class GitLabApproveMergeRequestPublisherTest {

    @ClassRule
    public static MockServerRule mockServer = new MockServerRule(new Object());

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private MockServerClient mockServerClient;
    private BuildListener listener;

    @BeforeClass
    public static void setupClass() throws IOException {
        setupGitLabConnections(jenkins, mockServer);
    }

    @Before
    public void setup() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
        mockServerClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @After
    public void cleanup() {
        mockServerClient.reset();
    }

    @Test
    public void matrixAggregatable() throws InterruptedException, IOException {
        verifyMatrixAggregatable(GitLabApproveMergeRequestPublisher.class, listener);
    }

    @Test
    public void success_approve_unstable() throws IOException, InterruptedException {
        AbstractBuild build = mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS);

        HttpRequest approvalStatusRequest = prepareMergeRequestWithApproval(MERGE_REQUEST_ID, MERGE_REQUEST_IID, PROJECT_ID, true, false);
        HttpRequest approveRequest = prepareSendApprovalWithSuccessResponse(build, MERGE_REQUEST_ID);

        performApproval(build, true);

        mockServerClient.verify(approvalStatusRequest,approveRequest);

        // performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), MERGE_REQUEST_IID, true);
    }
    @Test
    public void success() throws IOException, InterruptedException {
        prepareMergeRequestWithApproval(MERGE_REQUEST_ID, MERGE_REQUEST_IID, PROJECT_ID, true, false);
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), MERGE_REQUEST_IID, false);
    }

    @Test
    public void unstable_approve_unstable() throws IOException, InterruptedException {
        prepareMergeRequestWithApproval(MERGE_REQUEST_ID, MERGE_REQUEST_IID, PROJECT_ID, true, false);
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE),  MERGE_REQUEST_IID, true);
    }

    @Test
    public void unstable_dontapprove() throws IOException, InterruptedException {
        prepareMergeRequestWithApproval(MERGE_REQUEST_ID, MERGE_REQUEST_IID, PROJECT_ID, true, false);
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.UNSTABLE),  MERGE_REQUEST_IID, false);
    }

    @Test
    public void failed_approve_unstable() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), MERGE_REQUEST_IID, true);
    }

    @Test
    public void failed() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), MERGE_REQUEST_IID, false);
    }

    @Test
    public void successAlreadyApproved() throws IOException, InterruptedException {
        performApprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.SUCCESS), MERGE_REQUEST_IID, false);
    }

    @Test
    public void failedAlreadyUnapproved() throws IOException, InterruptedException {
        performUnapprovalAndVerify(mockSimpleBuild(GITLAB_CONNECTION_V4, Result.FAILURE), MERGE_REQUEST_IID, false);
    }

    private void performApproval(AbstractBuild build, boolean approveUnstable) throws InterruptedException, IOException {
        GitLabApproveMergeRequestPublisher publisher = preparePublisher(new GitLabApproveMergeRequestPublisher(approveUnstable), build);
        publisher.perform(build, null, listener);
    }

    private void performApprovalAndVerify(AbstractBuild build, int mergeRequestId, boolean approveUnstable) throws InterruptedException, IOException {
        performApproval(build, approveUnstable);

        HttpRequest httpRequest = prepareSendApprovalWithSuccessResponse(build, mergeRequestId);
        mockServerClient.verify(httpRequest);
    }

    private void performUnapprovalAndVerify(AbstractBuild build, int mergeRequestId, boolean approveUnstable) throws InterruptedException, IOException {
        GitLabApproveMergeRequestPublisher publisher = preparePublisher(new GitLabApproveMergeRequestPublisher(approveUnstable), build);
        publisher.perform(build, null, listener);

        mockServerClient.verify(prepareSendUnapprovalWithSuccessResponse(build, mergeRequestId));
    }

    private HttpRequest prepareSendApprovalWithSuccessResponse(AbstractBuild build, int mergeRequestId) throws UnsupportedEncodingException {
        HttpRequest approvalRequest = prepareSendApproval(mergeRequestId);
        mockServerClient.when(approvalRequest).respond(response().withStatusCode(200));
        return approvalRequest;
    }

    private HttpRequest prepareSendUnapprovalWithSuccessResponse(AbstractBuild build, int mergeRequestId) throws UnsupportedEncodingException {
        HttpRequest unapprovalRequest = prepareSendUnapproval(mergeRequestId);
        mockServerClient.when(unapprovalRequest).respond(response().withStatusCode(200));
        return unapprovalRequest;
    }

    private HttpRequest prepareApprovalStatusRequest(int mergeRequestId){
        return request()
            .withPath("/gitlab/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/approvals")
            .withMethod("GET")
            .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpResponse prepareApprovalStatusResponse(int mergeRequestId, int iid, int projectId, boolean canApprove, boolean hasApproved) throws IOException {
        return response().
            withBody(getApprovalStatus("ApprovalStatus.json", mergeRequestId, iid, projectId, canApprove, hasApproved)).
            withStatusCode(200);
    }

    private HttpRequest prepareSendApproval(int mergeRequestId) {
        return request()
            .withPath("/gitlab/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/approve")
            .withMethod("POST")
            .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareSendUnapproval(int mergeRequestId) {
        return request()
            .withPath("/gitlab/api/v4/projects/" + PROJECT_ID + "/merge_requests/" + mergeRequestId + "/unapprove")
            .withMethod("POST")
            .withHeader("PRIVATE-TOKEN", "secret");
    }

    private String getApprovalStatus(String name, int mergeRequestId, int iid, int projectId, boolean canApprove, boolean hasApproved ) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name))
            .replace("${projectId}", projectId + "")
            .replace("${id}", mergeRequestId + "")
            .replace("${iid}", iid + "")
            .replace("${can_approve}", String.valueOf(canApprove))
            .replace("${has_approved}", String.valueOf(hasApproved));
    }

    private HttpRequest prepareMergeRequestWithApproval(int mergeRequestId, int iid, int projectId, boolean canApprove, boolean hasApproved) throws IOException{
        HttpRequest httpRequest = prepareApprovalStatusRequest(mergeRequestId);
        mockServerClient.when(httpRequest)
            .respond(prepareApprovalStatusResponse(mergeRequestId, iid, projectId, canApprove, hasApproved));
        return httpRequest;
    }

}
