package com.commit451.gitlab.api;

import com.commit451.gitlab.model.Branch;
import com.commit451.gitlab.model.DeleteResponse;
import com.commit451.gitlab.model.Diff;
import com.commit451.gitlab.model.DiffLine;
import com.commit451.gitlab.model.Group;
import com.commit451.gitlab.model.Issue;
import com.commit451.gitlab.model.Milestone;
import com.commit451.gitlab.model.Note;
import com.commit451.gitlab.model.Project;
import com.commit451.gitlab.model.Session;
import com.commit451.gitlab.model.TreeItem;
import com.commit451.gitlab.model.User;

import java.util.List;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

public interface GitLab {
	
	/* --- LOGIN --- */
	
	@POST("/session")
	void getSessionByUsername(@Query("login") String login, @Query("password") String password,
							  @Body String blankBodySoRetrofitDoesntCry, Callback<Session> cb);
	
	@POST("/session")
	void getSessionByEmail(@Query("email") String email, @Query("password") String password,
						   @Body String blankBodySoRetrofitDoesntCry, Callback<Session> cb);
	
	/* --- MAIN --- */
	
	@GET("/groups?per_page=100")
	void getGroups(Callback<List<Group>> cb);
	
	@GET("/users?per_page=100")
	void getUsers(Callback<List<User>> cb);
	
	@GET("/projects?per_page=100")
	void getProjects(Callback<List<Project>> cb);
	
	/* --- MISC --- */
	
	@GET("/projects/{id}/repository/branches?per_page=100")
	void getBranches(@Path("id") long projectId, Callback<List<Branch>> cb);
	
	@GET("/projects/{id}/milestones?per_page=100")
	void getMilestones(@Path("id") long projectId, Callback<List<Milestone>> cb);
	
	@GET("/projects/{id}/members?per_page=100")
	void getUsersFallback(@Path("id") long projectId, Callback<List<User>> cb);
	
	/* --- COMMITS --- */
	
	@GET("/projects/{id}/repository/commits?per_page=100")
	void getCommits(@Path("id") long projectId, @Query("ref_name") String branchName, Callback<List<DiffLine>> cb);

	@GET("/projects/{id}/repository/commits/{sha}")
	void getCommit(@Path("id") long projectId, @Path("sha") String commitSHA, Callback<DiffLine> cb);
	
	@GET("/projects/{id}/repository/commits/{sha}/diff")
	void getCommitDiff(@Path("id") long projectId, @Path("sha") String commitSHA, Callback<List<Diff>> cb);

	/* --- ISSUE --- */
	
	@GET("/projects/{id}/issues?per_page=100")
	void getIssues(@Path("id") long projectId, Callback<List<Issue>> cb);
	
	@POST("/projects/{id}/issues")
	void postIssue(@Path("id") long projectId, @Query("title") String title, @Query("description") String description,
				   @Body String blankBodySoRetrofitDoesntCry, Callback<Issue> cb);
	
	@PUT("/projects/{id}/issues/{issue_id}")
	void editIssue(@Path("id") long projectId, @Path("issue_id") long issueId, @Query("state_event") String stateEvent, @Query("assignee_id") long assigneeId, @Query("milestone_id") long milestoneId,
				   @Body String blankBodySoRetrofitDoesntCry, Callback<Issue> cb);

	@PUT("/projects/{id}/issues/{issue_id}")
	void editIssue(@Path("id") long projectId,
				   @Path("issue_id") long issueId,
				   @Query("state_event") String stateEvent,
				   @Body String blankBodySoRetrofitDoesntCry,
				   Callback<Issue> cb);
	
	@GET("/projects/{id}/issues/{issue_id}/notes?per_page=100")
	void getIssueNotes(@Path("id") long projectId, @Path("issue_id") long issueId, Callback<List<Note>> cb);
	
	@POST("/projects/{id}/issues/{issue_id}/notes")
	void postIssueNote(@Path("id") long projectId, @Path("issue_id") long issueId, @Query("body") String body,
					   @Body String blankBodySoRetrofitDoesntCry, Callback<Note> cb);
	
	/* --- FILES --- */
	
	@GET("/projects/{id}/repository/tree?per_page=100")
	void getTree(@Path("id") long projectId, @Query("ref_name") String branchName, @Query("path") String path, Callback<List<TreeItem>> cb);
	
	@GET("/projects/{id}/repository/commits/{sha}/blob")
	void getBlob(@Path("id") long projectId, @Path("sha") String commitId, @Query("filepath") String path, Callback<Response> cb);
	
	/* --- USER --- */
	
	@GET("/groups/{id}/members?per_page=100")
	void getGroupMembers(@Path("id") long groupId, Callback<List<User>> cb);
	
	@POST("/groups/{id}/members")
	void addGroupMember(@Path("id") long groupId, @Query("user_id") long userId, @Query("access_level") String accessLevel,
						@Body String blankBodySoRetrofitDoesntCry, Callback<User> cb);
	
	@DELETE("/groups/{id}/members/{user_id}")
	void removeGroupMember(@Path("id") long groupId, @Path("user_id") long userId, Callback<DeleteResponse> cb);
}