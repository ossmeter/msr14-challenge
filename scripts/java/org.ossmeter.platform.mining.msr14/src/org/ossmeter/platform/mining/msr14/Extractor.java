package org.ossmeter.platform.mining.msr14;

import java.util.Date;
import java.util.Iterator;

import org.ossmeter.platform.mining.msr14.model.Artefact;
import org.ossmeter.platform.mining.msr14.model.Biodiversity;
import org.ossmeter.platform.mining.msr14.model.IssueEvent;
import org.ossmeter.platform.mining.msr14.model.IssueEventKind;
import org.ossmeter.platform.mining.msr14.model.Project;
import org.ossmeter.platform.mining.msr14.model.ProjectMembership;
import org.ossmeter.platform.mining.msr14.model.User;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

public class Extractor {
	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();

		Mongo msrMongo = new Mongo(new ServerAddress("localhost", 1234)); // GitHub
																			// challenge
																			// data
		Mongo bioMongo = new Mongo(new ServerAddress("localhost", 12345));// Extracted
																			// data

		// Create indexes
		Biodiversity bio = new Biodiversity(bioMongo.getDB("biodiversity"));
		bio.setClearPongoCacheOnSync(true);
		bioMongo.getDB("biodiversity").getCollection("users")
				.ensureIndex(new BasicDBObject("login", 1));

		BasicDBObject index = new BasicDBObject();
		index.put("name", 1);
		index.put("ownerName", 1);
		bioMongo.getDB("biodiversity").getCollection("projects")
				.ensureIndex(index);

		index = new BasicDBObject();
		index.put("projectName", 1);
		index.put("projectOwner", 1);
		bioMongo.getDB("biodiversity").getCollection("projectMemberships")
				.ensureIndex(index);

		index = new BasicDBObject();
		index.put("projectName", 1);
		index.put("userName", 1);
		bioMongo.getDB("biodiversity").getCollection("projectMemberships")
				.ensureIndex(index);

		bioMongo.getDB("biodiversity").getCollection("projectMemberships")
				.ensureIndex(new BasicDBObject("userName", 1));

		DB msrDb = msrMongo.getDB("msr14");

		// #1 User extraction
		System.out.println("Extracting users...");
		DBCursor cursor = msrDb.getCollection("users").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		Iterator<DBObject> it = cursor.iterator();

		int count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			User user = new User();
			user.setGhId(obj.getString("id"));
			user.setLogin(obj.getString("login"));
			user.setLocation(obj.getString("location"));
			user.setPublicRepos(obj.getInt("public_repos", 0));
			user.setJoinedDate(obj.getString("created_at"));
			user.setFollowerCount(obj.getInt("followers", 0));
			user.setFollowingCount(obj.getInt("following", 0));
			user.setPublicGists(obj.getInt("public_gists", 0));

			bio.getUsers().add(user);

			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #1.2 Project extraction
		System.out.println("Extracting projects...");
		cursor = msrDb.getCollection("repos").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			Project project = new Project();
			project.setName(obj.getString("name"));
			project.setGhId(obj.getString("id"));
			project.setCreatedAt(obj.getString("created_at"));
			project.setSize(obj.getInt("size", 0));
			project.setWatchersCount(obj.getInt("watchers", 0));
			project.setWatchersCount2(obj.getInt("watchers_count", 0));
			project.setLanguage(obj.getString("language"));
			project.setForks(obj.getInt("forks", 0));
			project.setForksCount(obj.getInt("forks_count", 0));
			project.setOpenIssues(obj.getInt("open_issues", 0));
			project.setOpenIssuesCount(obj.getInt("open_issues_count", 0));
			project.setOpenIssues(obj.getInt("open_issues", 0));
			project.setNetworkCount(obj.getInt("network_count", 0));

			BasicDBObject ownerObj = (BasicDBObject) obj.get("owner");
			User owner = null;
			if (ownerObj != null) {
				owner = bio.getUsers().findOne(
						User.LOGIN.eq(ownerObj.getString("login")));
				if (owner != null) {
					project.setOwner(owner);
					project.setOwnerName(owner.getLogin());
				}
			}
			bio.getProjects().add(project);

			if (owner != null) { // This comes here as to reference the project,
									// we need to have added to the project list
									// first
				ProjectMembership pm = getProjectMembership(bio, owner, project);
				pm.setOwner(true);
			}

			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #2 Follower/following extraction
		System.out.println("Extracting followers...");
		cursor = msrDb.getCollection("followers").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String followerLogin = obj.getString("login");
			String followedLogin = obj.getString("follows");

			User follower = bio.getUsers()
					.findOne(User.LOGIN.eq(followerLogin));
			User followed = bio.getUsers()
					.findOne(User.LOGIN.eq(followedLogin));

			if (follower != null && followed != null) {
				follower.getFollowing().add(followed);
				followed.getFollowers().add(follower);
			} else {
				// System.err.println("Follower or followed is null. Follower: "
				// +follower + ". followed: " + followed);
			}
			if (follower != null)
				follower.setFollowingCount(follower.getFollowingCount() + 1);
			if (followed != null)
				followed.setFollowerCount(followed.getFollowerCount() + 1);

			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #3 Commits
		System.out.println("Extracting commits...");
		cursor = msrDb.getCollection("commits").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			// Author and committer
			BasicDBObject commitAuthor = (BasicDBObject) obj.get("author");
			BasicDBObject commitCommitter = (BasicDBObject) obj
					.get("committer");

			String authorLogin = "";
			if (commitAuthor != null)
				authorLogin = commitAuthor.getString("login");
			String committerLogin = "";
			if (commitCommitter != null)
				committerLogin = commitCommitter.getString("login");

			// Stats
			BasicDBObject stats = (BasicDBObject) obj.get("stats");
			if (stats == null)
				stats = new BasicDBObject(); // Create a new one so we can get
												// zeroed values
			int total = stats.getInt("total", 0);
			int additions = stats.getInt("additions", 0);
			int deletions = stats.getInt("deletions", 0);

			String commitDate = ((BasicDBObject) ((BasicDBObject) obj
					.get("commit")).get("author")).getString("date");

			BasicDBList files = (BasicDBList) obj.get("files");
			String[] url = convertUrlIntoProjectNameAndOwner(obj
					.getString("url"));

			ProjectMembership authorPm = null;
			ProjectMembership committerPm = null;

			if (authorLogin != null) {
				authorPm = getProjectMembership(bio, authorLogin, url[1],
						url[0]);
				authorPm.setCommitCount(authorPm.getCommitCount() + 1);
				authorPm.setCommitTotalChanges(authorPm.getCommitTotalChanges()
						+ total);
				authorPm.setCommitAdditions(authorPm.getCommitAdditions()
						+ additions);
				authorPm.setCommitDeletions(authorPm.getCommitDeletions()
						+ deletions);
				authorPm.setCommitsAsAuthor(authorPm.getCommitsAsAuthor() + 1);
				if (files != null)
					authorPm.setCommitTotalFiles(authorPm
							.getCommitTotalChanges() + files.size());
				authorPm.setAverageFilesPerCommit(authorPm
						.getCommitTotalFiles() / authorPm.getCommitCount());
				authorPm.getCommitTimes().add(commitDate);
			}

			if (authorLogin != null && !authorLogin.equals(committerLogin)) {
				committerPm = getProjectMembership(bio, committerLogin, url[1],
						url[0]);

				committerPm.setCommitCount(committerPm.getCommitCount() + 1);
				committerPm.setCommitsAsCommitter(committerPm
						.getCommitsAsCommitter() + 1);
				committerPm.setCommitTotalFiles(committerPm
						.getCommitTotalChanges() + files.size());
				committerPm.setAverageFilesPerCommit(committerPm
						.getCommitTotalFiles() / authorPm.getCommitCount());
				if (files != null)
					committerPm.setCommitTotalFiles(committerPm
							.getCommitTotalChanges() + files.size());
				committerPm.setAverageFilesPerCommit(committerPm
						.getCommitTotalFiles() / committerPm.getCommitCount());
				committerPm.getCommitTimes().add(commitDate);
			}

			bio.sync();
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		cursor.close();
		bio.sync();
		System.out.println();

		// #4 Commit comments
		System.out.println("Extracting commit comments...");
		cursor = msrDb.getCollection("commit_comments").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "user", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				System.err
						.println("Found commit comment with unrecognised user: "
								+ username);
				continue;
			}

			user.setNumberOfCommitComments(user.getNumberOfCommitComments() + 1);

			// Only a very small number of commit comments actually reference
			// the repo
			// Instead we're going to have to strip the string
			String[] url = convertUrlIntoProjectNameAndOwner(obj
					.getString("url"));

			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(url[1]), Project.OWNERNAME.eq(url[0]))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();
				if (project != null) {
					project.setNumberOfCommitComments(project
							.getNumberOfCommitComments() + 1);

					if (!project.getDbObject().containsField(
							"commitCommentTimes")) {
						project.getDbObject().put("commitCommentTimes",
								new BasicDBList());
					}
					project.getCommitCommentTimes().add(
							obj.getString("created_at"));

					ProjectMembership pm = getProjectMembership(bio, user,
							project);
					pm.setNumberOfCommitComments(pm.getNumberOfCommitComments() + 1);

					if (!pm.getDbObject().containsField("commitCommentTimes")) {
						pm.getDbObject().put("commitCommentTimes",
								new BasicDBList());
					}
					pm.getCommitCommentTimes().add(obj.getString("created_at"));
				}
			}
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		cursor.close();
		bio.sync();
		System.out.println();

		// #5 Pull requests
		System.out.println("Extracting pull requests...");
		cursor = msrDb.getCollection("pull_requests").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {

			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "user", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				continue;
			}

			if (!user.getDbObject().containsField("pullRequestTimes")) {
				user.getDbObject().put("pullRequestTimes", new BasicDBList());
			}
			user.getPullRequestTimes().add(obj.getString("created_at"));

			user.setNumberOfPullRequests(user.getNumberOfPullRequests() + 1);

			// Project
			System.out.println(obj.getString("repo") + " "
					+ obj.getString("owner") + obj.getString("_id"));

			ProjectMembership pm = getProjectMembership(bio, user.getLogin(),
					obj.getString("repo"), obj.getString("owner"));
			pm.setNumberOfPullRequests(pm.getNumberOfPullRequests() + 1);

			if (!pm.getDbObject().containsField("pullRequestTimes")) {
				pm.getDbObject().put("pullRequestTimes", new BasicDBList());
			}
			pm.getPullRequestTimes().add(obj.getString("created_at"));

			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();
				if (project != null) {
					project.setNumberOfPullRequests(project
							.getNumberOfPullRequests() + 1);

					if (!project.getDbObject()
							.containsField("pullRequestTimes")) {
						project.getDbObject().put("pullRequestTimes",
								new BasicDBList());
					}
					project.getPullRequestTimes().add(
							obj.getString("created_at"));

				}
			} else {
				System.err.println("Didn't find project:"
						+ obj.getString("repo") + ":" + obj.getString("owner"));
			}

			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
				System.gc();
			}
		}
		bio.sync();
		System.out.println();

		// #6 Pull request comments
		System.out.println("Extracting pull request comments...");
		cursor = msrDb.getCollection("pull_request_comments").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "user", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				continue;
			}

			if (!user.getDbObject().containsField("pullRequestCommentTimes")) {
				user.getDbObject().put("pullRequestCommentTimes",
						new BasicDBList());
			}
			user.getPullRequestCommentTimes().add(obj.getString("created_at"));
			user.setNumberOfPullRequestComments(user
					.getNumberOfPullRequestComments() + 1);

			// Project
			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			// if (repoIt.hasNext()) {
			Project project = repoIt.next();
			if (project != null) {
				project.setNumberOfPullRequestComments(project
						.getNumberOfPullRequestComments() + 1);
				if (!project.getDbObject().containsField(
						"pullRequestCommentTimes")) {
					project.getDbObject().put("pullRequestCommentTimes",
							new BasicDBList());
				}
				project.getPullRequestCommentTimes().add(
						obj.getString("created_at"));

				ProjectMembership pm = getProjectMembership(bio, user, project);
				pm.setNumberOfPullRequestComments(pm
						.getNumberOfPullRequestComments() + 1);

				if (!pm.getDbObject().containsField("pullRequestCommentTimes")) {
					pm.getDbObject().put("pullRequestCommentTimes",
							new BasicDBList());
				}
				pm.getPullRequestCommentTimes()
						.add(obj.getString("created_at"));
			}
			// }
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #7 Issues
		System.out.println("Extracting issues...");
		cursor = msrDb.getCollection("issues").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "user", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				// System.err.println("Found issue with unrecognised user:" +
				// username);
				continue;
			}

			if (!user.getDbObject().containsField("issueTimes")) {
				user.getDbObject().put("issueTimes", new BasicDBList());
			}
			user.getIssueTimes().add(obj.getString("created_at"));
			user.setNumberOfIssues(user.getNumberOfIssues() + 1);

			// Project
			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();
				if (project != null) {
					project.setNumberOfIssues(project.getNumberOfIssues() + 1);

					if (!project.getDbObject().containsField("issueTimes")) {
						project.getDbObject().put("issueTimes",
								new BasicDBList());
					}
					project.getIssueTimes().add(obj.getString("created_at"));

					ProjectMembership pm = getProjectMembership(bio, user,
							project);
					pm.setNumberOfIssues(pm.getNumberOfIssues() + 1);

					if (!pm.getDbObject().containsField("issueTimes")) {
						pm.getDbObject().put("issueTimes", new BasicDBList());
					}
					pm.getIssueTimes().add(obj.getString("created_at"));
				}
			}
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #8 Issue comments
		System.out.println("Extracting issue comments...");
		cursor = msrDb.getCollection("issue_comments").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "user", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				continue;
			}

			if (!user.getDbObject().containsField("issueCommentTimes")) {
				user.getDbObject().put("issueCommentTimes", new BasicDBList());
			}
			user.getIssueCommentTimes().add(obj.getString("created_at"));
			user.setNumberOfIssueComments(user.getNumberOfIssueComments() + 1);

			// Project
			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();
				if (project != null) {
					project.setNumberOfIssueComments(project
							.getNumberOfIssueComments() + 1);

					if (!project.getDbObject().containsField(
							"issueCommentTimes")) {
						project.getDbObject().put("issueCommentTimes",
								new BasicDBList());
					}
					project.getIssueCommentTimes().add(
							obj.getString("created_at"));

					ProjectMembership pm = getProjectMembership(bio, user,
							project);
					pm.setNumberOfIssueComments(pm.getNumberOfIssueComments() + 1);

					if (!pm.getDbObject().containsField("issueCommentTimes")) {
						pm.getDbObject().put("issueCommentTimes",
								new BasicDBList());
					}
					pm.getIssueCommentTimes().add(obj.getString("created_at"));
				}
			}
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// #9 Issue events
		System.out.println("Extracting issue events...");
		cursor = msrDb.getCollection("issue_events").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String username = getUserLoginName(bio, "actor", "login", obj);
			User user = bio.getUsers().findOne(User.LOGIN.eq(username));
			if (user == null) {
				continue;
			}

			String eventKind = obj.getString("event");
			IssueEventKind kind = null;

			switch (eventKind) {
			case "closed":
				kind = IssueEventKind.CLOSED;
				break;
			case "assigned":
				kind = IssueEventKind.ASSIGNED;
				break;
			case "mentioned":
				kind = IssueEventKind.MENTIONED;
				break;
			case "merged":
				kind = IssueEventKind.MERGED;
				break;
			case "referenced":
				kind = IssueEventKind.REFERENCED;
				break;
			case "reopened":
				kind = IssueEventKind.REOPENED;
				break;
			case "subscribed":
				kind = IssueEventKind.SUBSCRIBED;
				break;
			case "head_ref_deleted":
				kind = IssueEventKind.HEAD_REF_DELETED;
				break;
			case "head_ref_restored":
				kind = IssueEventKind.HEAD_REF_RESTORED;
				break;
			case "head_ref_cleaned":
				kind = IssueEventKind.HEAD_REF_CLEANED;
				break;
			case "unsubscribed":
				kind = IssueEventKind.UNSUBSCRIBED;
				break;
			default:
				System.err.println("Unrecognised issue event kind: "
						+ eventKind);
			}
			if (kind == null)
				continue;

			boolean eventKindFound = false;

			if (!user.getDbObject().containsField("issueEvents")) {
				user.getDbObject().put("issueEvents", new BasicDBList());
			}

			for (IssueEvent ie : user.getIssueEvents()) {
				if (ie.getEventKind().equals(kind)) {
					ie.setCount(ie.getCount() + 1);
					eventKindFound = true;
					break;
				}
			}
			if (!eventKindFound) {
				IssueEvent ie = new IssueEvent();
				ie.setEventKind(kind);
				ie.setCount(1);
				user.getIssueEvents().add(ie);
			}

			// Project
			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();

				if (!project.getDbObject().containsField("issueEvents")) {
					project.getDbObject().put("issueEvents", new BasicDBList());
				}

				eventKindFound = false;
				for (IssueEvent ie : project.getIssueEvents()) {
					if (ie.getEventKind().equals(kind)) {
						ie.setCount(ie.getCount() + 1);
						eventKindFound = true;
						break;
					}
				}
				if (!eventKindFound) {
					IssueEvent ie = new IssueEvent();
					ie.setEventKind(kind);
					ie.setCount(1);
					project.getIssueEvents().add(ie);
				}

				ProjectMembership pm = getProjectMembership(bio, user, project);

				if (!pm.getDbObject().containsField("issueEvents")) {
					pm.getDbObject().put("issueEvents", new BasicDBList());
				}

				eventKindFound = false;
				for (IssueEvent ie : pm.getIssueEvents()) {
					if (ie.getEventKind().equals(kind)) {
						ie.setCount(ie.getCount() + 1);
						eventKindFound = true;
						break;
					}
				}
				if (!eventKindFound) {
					IssueEvent ie = new IssueEvent();
					ie.setEventKind(kind);
					ie.setCount(1);
					pm.getIssueEvents().add(ie);
				}
			}

			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();
		System.exit(0);

		// Watchers
		System.out.println("Extracting watchers...");
		cursor = msrDb.getCollection("watchers").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			User user = bio.getUsers().findOne(
					User.LOGIN.eq(obj.getString("login")));
			if (user == null)
				continue;

			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.NAME.eq(obj.getString("repo")),
							Project.OWNERNAME.eq(obj.getString("owner")))
					.iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();
				if (project != null && !project.getWatchers().contains(user))
					project.getWatchers().add(user);
				if (!user.getWatches().contains(project))
					user.getWatches().add(project);
			}
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();
		System.out.println();

		// Repo collaborators
		System.out.println("Extracting repo collaborators...");
		cursor = msrDb.getCollection("repo_collaborators").find();
		cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		it = cursor.iterator();

		count = 0;
		while (it.hasNext()) {
			BasicDBObject obj = (BasicDBObject) it.next();

			String login = obj.getString("login");
			String projectName = obj.getString("repo");
			String ownerName = obj.getString("owner");

			User user = bio.getUsers().findOne(User.LOGIN.eq(login));
			Iterator<Project> repoIt = bio
					.getProjects()
					.find(Project.OWNERNAME.eq(ownerName),
							Project.NAME.eq(projectName)).iterator();
			if (repoIt.hasNext()) {
				Project project = repoIt.next();

				ProjectMembership pm = getProjectMembership(bio, user, project);
				pm.setCollaborator(true);
			} else {
				System.err.println("Couldn't find repo. owner: " + ownerName
						+ ", repo: " + projectName);
			}
			count++;
			if (count % 1000 == 0) {
				System.out.print(count + ", ");
				bio.sync();
			}
		}
		bio.sync();

		long end = System.currentTimeMillis();
		System.out.println("Finished at " + new Date());

		long duration = end - start;
		System.out.println("Duration: " + duration);

	}

	protected static ProjectMembership getProjectMembership(Biodiversity bio,
			User user, Project project) {

		Iterator<ProjectMembership> it = bio
				.getProjectMemberships()
				.find(ProjectMembership.USERNAME.eq(user.getLogin()),
						ProjectMembership.PROJECTNAME.eq(project.getName()))
				.iterator();
		if (it.hasNext()) {
			return it.next();
		}

		ProjectMembership pm = new ProjectMembership();
		pm.setProject(project);
		pm.setProjectName(project.getName());
		pm.setProjectOwner(project.getOwnerName());
		pm.setUser(user);
		pm.setUserName(user.getLogin());
		bio.getProjectMemberships().add(pm);
		user.getProjects().add(pm);
		return pm;
	}

	protected static ProjectMembership getProjectMembership(Biodiversity bio,
			String user, String projectName, String ownerName) {

		Iterator<ProjectMembership> it = bio
				.getProjectMemberships()
				.find(ProjectMembership.USERNAME.eq(user),
						ProjectMembership.PROJECTNAME.eq(projectName))
				.iterator();
		if (it.hasNext()) {
			return it.next();
		}

		ProjectMembership pm = new ProjectMembership();
		// pm.setProject(project);
		pm.setProjectName(projectName);
		pm.setProjectOwner(ownerName);
		// pm.setUser(user);
		pm.setUserName(user);
		bio.getProjectMemberships().add(pm);
		// user.getProjects().add(pm);
		return pm;
	}

	/**
	 * Some 'user' field in commit_comments are Strings. Some are objects.
	 * Seriously.
	 * 
	 * @param bio
	 * @param obj
	 * @return
	 */
	protected static String getUserLoginName(Biodiversity bio,
			String userField, String loginField, BasicDBObject obj) {
		String username = null;
		if (obj.get(userField) == null)
			return null;
		if (obj.get(userField) instanceof String) {
			username = obj.getString(userField);
		} else {
			username = ((BasicDBObject) obj.get(userField))
					.getString(loginField);
		}
		return username;
	}

	protected static String[] convertUrlIntoProjectNameAndOwner(String url) {
		url = url.replaceAll("https://api.github.com/repos/", "");
		String owner = url.substring(0, url.indexOf("/"));
		url = url.substring(url.indexOf("/") + 1);
		String repo = url.substring(0, url.indexOf("/"));
		return new String[] { owner, repo };
	}

	protected static void addArtefact(User user, String extension) {
		boolean artefactFound = false;
		for (Artefact a : user.getArtefacts()) {
			if (a.getExtension().equals(extension)) {
				a.setCount(a.getCount() + 1);
				artefactFound = true;
				break;
			}
		}
		if (!artefactFound) {
			Artefact a = new Artefact();
			a.setExtension(extension);
			a.setCount(1);
			user.getArtefacts().add(a);
		}
	}

	protected static void addArtefact(Project project, String extension) {
		boolean artefactFound = false;
		for (Artefact a : project.getArtefacts()) {
			if (a.getExtension().equals(extension)) {
				a.setCount(a.getCount() + 1);
				artefactFound = true;
				break;
			}
		}
		if (!artefactFound) {
			Artefact a = new Artefact();
			a.setExtension(extension);
			a.setCount(1);
			project.getArtefacts().add(a);
		}
	}
}
