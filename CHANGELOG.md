## Changelog

## Next

## 0.1.0-rc16
- Fix fix null safe in repositories.
- GitHubRepository Provider API for not using global connection.

## 0.1.0-rc15
- Null safe in repositories.

## 0.1.0-rc14

- [PR] Inject comment body and matched group.
- Escape strings for env variables.
- [PR] Test and logging for PR description body.
- [BRANCH] name restriction event shouldn't trigger on match.

## 0.1.0-rc13
- [PR] Skip PR number.
- Text enhancements.
- Few public java APIs for rebuild actions.

## 0.1.0-rc12
- [PR] [BRANCH] whitelist params for security-170.
- [BRANCH] Refactor code to use java8 functions. 
- [BRANCH] Allow whitelist branches via SkipEvent. 
- [PR] Job-dsl: support whitelist branches definitiion.
- [PR] Comment event: do not skip the first comment.

## 0.1.0-rc11 
- [PR] Fixed (?) triggering regression.

## 0.1.0-rc10
  :question: [PR] Regression https://github.com/KostyaSha/github-integration-plugin/issues/141
 * Verbose logging for exceptions.
 * Java 8 required as it was written in docs for a long time.
 * Support more job types.
 * [PR] Update all local PRs with fresh information on every check.
 * Fixed non chrome browsers support for clicking buttons.
 * [PR] Survive GH outages/bad fetched PR/Issues (NPE in event).
 * Clean-up test class.

## 0.1.0-rc9
 * [BRANCH] New `GitHub Branch Trigger`.
 * [DOC] Updated docs.
 * Jucies is optional dep.

## 0.1.0-rc8 
 * [PR] Insert PR state as variable.
 * [PR] Add job-dsl for BuildStep status updater.

## 0.1.0-rc7
 * [PR] Fixed cancel queued builds.
 * [PR] New feature: abort running builds.

## 0.1.0-rc6
 * [PR] Show badge in build history instead raw html.
 * [PR] Post link to project for Pending status when job in queue.
 * [PR] Get quiet period from the parameterizable job.
 * [PR] Support matrix-project job type.
 * [PR] Workflow step for setting pr statuses.
 * [PR] Advanced context name resolution for pr statuses.
 * [PR] Reworked polling log action (sidepanel button/page).

## 0.0.1-rc5
 * [PR] More job-dsl plugin configuration.
 * [PR] Fixed skip logic for events.

## 0.0.1-rc4
 * [PR] New label not exists event.

## 0.0.1-rc2, 0.1.0-rc3
 - Failed maven-release-plugin.
 
## 0.0.1-rc1
 * [PR] Fixed UI configs.

## 0.0.1-beta17

* [PR] Clarified help texts and UI info.
* [PR] Support Job.class (workflow-plugin) for hooks triggering. 
* [PR] Update block-queued-job-plugin and Extension for it to support Job.class (workflow-plugin)
* [PR] Added checkstyle, pmd.

## 0.0.1-beta16

* Fixed UI settings configuration.

## 0.0.1-beta15

* Support Job type (workflow-plugin support)
* Reorganised to multi-module maven structure
* Renamed to `github-integration-plugin`

## 0.0.1-beta14

* Provide default configuration in UI

## 0.0.1-beta13

* Indent trigger configuration UI
* Prevent NPE in weird missconfigured case
* Fixed check api url method in global config
* Replace custom repo name parsing with github-plugin class
* Fix NPE after pulls clearing in GHRepo action

## 0.0.1-beta12

* Optimise hooks based check
* Replace all jdk logging with slf4j-jdk

## 0.0.1-beta11

* Fixed trigger by comment UI entry

## 0.0.1-beta10

* Fix broken polling page
* Feature: Experimental hooks support
* Fix: Don't persist project object

## 0.0.1-beta9

* skip events refactoring
* clean-ups

## 0.0.1-beta8
Version forward: maven-release-plugin failed

## 0.0.1-beta7

* Polling log in project/build
* Waterfall conditioning
* Added blockedQueue optional Extension (block-queued-job-plugin)
* Fixed: Allow save job that had no GH project property before

