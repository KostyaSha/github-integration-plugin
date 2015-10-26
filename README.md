GitHub Pull Request Plugin
==========================

[![Coverage](https://img.shields.io/sonar/http/sonar.lanwen.ru/org.jenkins-ci.plugins:github-pullrequest/coverage.svg?style=flat)](http://sonar.lanwen.ru/dashboard/index?id=org.jenkins-ci.plugins:github-pullrequest)


This is rewritten version of [ghprb-plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+pull+request+builder+plugin) that after many years is in complex code state when keeping backward compatibility with fixing all bugs almost impossible.
Code inspired by github-plugin, git-plugin, ghprb-plugin and others.

## Description:

- By design it extensible and new features can be added easily
- Restriction features are not bundled into main code and can be extended separately. Available as API for doing checks from other plugin parts
- Trigger events are splitted to separate classes and can do:
  - Label: added, removed, exists
  - PR states: closed, opened, commit changed
  - Comment triggering
- UI/configuration changes:
  - No tones of checkboxes
  - Trigger section contains only configuration related to triggering
  - All messages configurable
- Publishers are in post-build actions:
  - slitted to separate publishers to have ability choose required order
    - Set build status. Job can configured to not set PR statuses
    - Add labels
    - Remove labels
    - Close PR
    - Post comment with TokenMacro and Groovy Templates from [email-ext-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin) support
- Trigger check modes (planned):
  - Cron checks with persistence
  - Hooks with persisting state
  - Hooks with persistance and periodic cron check (to avoid lost events)
  - Lightweight using GH hooks without persisting state (penalty: lost events)
- Built sources variants:
  - Build only merged state. Note: merged to target branch only in moment when PR was built.
  - Build only 'head' state. This is what is really contains in PR without merge to target branch.
  - Conditional build with `GITHUB_PR_COND_REF`. When PR is mergeable it contains 'merge' word and 'head' when not.
- Persisted state is stored near build configuration in `*.runtime.xml` file to exclude useless saves with [jobConfigHistory-plugin](https://wiki.jenkins-ci.org/display/JENKINS/JobConfigHistory+Plugin)
- All input variables starts from `GITHUB_PR_*`
- Refspec for [git-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin) is not hardcoded, branch specifier can be constructed using input variables
- Configuration and code tried to be workflow friendly

Integration with [github-plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin) can

- optimize/reuse code for:
  - [x] searching projects
  - [x] using one hook url
  - [x] hooks configuration (with periodic cleaning?)
  - [x] have one Global configuration
  - [x] reuse credentials lookup

## TODO:

 - [x] review injected variables in Cause and String parameters
 - [ ] integrate with git-plugin by providing patch for BuildData get through BuildChooser?
 - [x] implement hooks triggering
 - [ ] more tests

## Configuration:

### Triggering
 - Configure github-plugin https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Plugin
 - Set GitHub project property with link to your GH repository in Job settings
 - Configure GIT SCM: add any repo name i.e. 'origin-pull' and set refspec to `+refs/pull/${GITHUB_PR_NUMBER}/merge:refs/remotes/origin-pull/pull/${GITHUB_PR_NUMBER}/merge` if you want run build for merged state or '/head' for building exact PR commits, or `$GITHUB_PR_COND_REF` if you want 'head' state when PR is not mergeable (according to GH state). Set branch specifier to `origin-pull/pull/${GITHUB_PR_NUMBER}/merge`. This exact link allows to speedup fetch sources.
 - Enable "Build GitHub pull requests" trigger and configure it.
 - If you want do gatekeepering, then add second repository with i.e. origin. Add "Merge" extension in from Git SCM and configure post build action for push action.

### Commit/PR status
- If you want to set commit status right before job was put to Jenkins queue, enable "Set status before build" checkbox in trigger configuration (it will have no links because there is no real builds in jenkins, only queue item that is not a build)
- In "Build" section "Add Build step" called "Set pull request status to "pending" on GitHub" and enter some message like "Build #${BUILD_NUMBER} started"
- In "Post-build Actions" add "GitHub PR: set PR status" and configure message "Build #${BUILD_NUMBER} ended"

